package dev.ed3c.gymcometrue.entitlement

import kotlinx.serialization.Serializable

private val sha256Pattern = Regex("^[0-9a-f]{64}$")
private val isoDatePattern = Regex("^\\d{4}-\\d{2}-\\d{2}$")

@Serializable
enum class EntitlementProvider { APPLE_APP_STORE, GOOGLE_PLAY, WEB_MERCHANT }

/**
 * Where an entitlement assertion physically came from.
 *
 * Only [SERVER_TO_PROVIDER] describes a verification our own server performed against the provider.
 * Every other channel is a client-side or unverified signal and can never grant access.
 */
@Serializable
enum class EntitlementAssertionChannel {
    SERVER_TO_PROVIDER,
    CLIENT_PURCHASE_CALLBACK,
    CLIENT_LOCAL_CACHE,
    PAYWALL_UI_STATE,
    WEBHOOK_ARRIVAL_UNVERIFIED,
}

@Serializable
enum class EntitlementEnvironment { SANDBOX, PRODUCTION }

@Serializable
enum class EntitlementAdmission { REJECTED, REVIEW_REQUIRED, ADMITTED }

/**
 * A receipt produced by our server after it verified one provider transaction.
 *
 * The signature and payload hashes are the server's own verification evidence. They are `null`
 * (`ABSENT`) in this repository because no verification server exists yet, and an absent hash is a
 * blocker rather than a default-allow.
 */
@Serializable
data class ProviderVerificationReceipt(
    val receiptId: String,
    val provider: EntitlementProvider,
    val channel: EntitlementAssertionChannel,
    val environment: EntitlementEnvironment,
    val accountId: String,
    val productId: String,
    val providerTransactionId: String,
    val verifiedAtIsoDate: String,
    val serverVerificationSignatureSha256: String? = null,
    val providerPayloadSha256: String? = null,
    val priceMinorUnits: Long? = null,
    val currencyCode: String? = null,
    val note: String,
) {
    init {
        require(receiptId.isNotBlank() && accountId.isNotBlank() && productId.isNotBlank())
        require(providerTransactionId.isNotBlank() && note.isNotBlank())
    }
}

@Serializable
data class ReceiptValidationResult(
    val admission: EntitlementAdmission,
    val blockers: List<String>,
    val reviewNotes: List<String>,
)

object ProviderVerificationReceiptValidator {
    fun validate(
        receipt: ProviderVerificationReceipt,
        asOfIsoDate: String,
        production: Boolean,
    ): ReceiptValidationResult {
        val blockers = mutableListOf<String>()
        val notes = mutableListOf<String>()

        if (receipt.channel != EntitlementAssertionChannel.SERVER_TO_PROVIDER) {
            blockers += "Channel ${receipt.channel} cannot grant access; only SERVER_TO_PROVIDER verification can."
        }
        if (!receipt.serverVerificationSignatureSha256.isSha256()) {
            blockers += "Server verification signature SHA-256 is ABSENT for receipt ${receipt.receiptId}."
        }
        if (!receipt.providerPayloadSha256.isSha256()) {
            blockers += "Verified provider payload SHA-256 is ABSENT for receipt ${receipt.receiptId}."
        }

        val verifiedAt = receipt.verifiedAtIsoDate.isoDateKey()
        val asOf = asOfIsoDate.isoDateKey()
        if (verifiedAt == null) blockers += "verifiedAtIsoDate must be a valid YYYY-MM-DD date."
        if (asOf == null) blockers += "asOfIsoDate must be a valid YYYY-MM-DD date."
        if (verifiedAt != null && asOf != null && verifiedAt > asOf) {
            blockers += "Receipt ${receipt.receiptId} claims verification after the as-of date."
        }
        if (production && receipt.environment != EntitlementEnvironment.PRODUCTION) {
            blockers += "Production access rejects a ${receipt.environment} receipt."
        }

        if (receipt.priceMinorUnits != null || receipt.currencyCode != null) {
            notes += "Price and currency are recorded for support only; they never grant access."
        }
        if (receipt.environment == EntitlementEnvironment.SANDBOX) {
            notes += "Sandbox verification is draft evidence and is never production access."
        }

        val admission = when {
            blockers.isNotEmpty() -> EntitlementAdmission.REJECTED
            receipt.environment == EntitlementEnvironment.SANDBOX -> EntitlementAdmission.REVIEW_REQUIRED
            else -> EntitlementAdmission.ADMITTED
        }
        return ReceiptValidationResult(admission, blockers.distinct(), notes.distinct())
    }
}

@Serializable
enum class EntitlementEventKind {
    PURCHASE_VERIFIED,
    RENEWAL_VERIFIED,
    GRACE_PERIOD_STARTED,
    AUTO_RENEW_CANCELLED,
    EXPIRED,
    REFUNDED,
    REVOKED,
    RESTORED,
}

/**
 * One immutable provider event. [providerEventId] is the replay key: the same provider event may
 * arrive any number of times through retries, webhooks, and restore flows.
 */
@Serializable
data class EntitlementEvent(
    val providerEventId: String,
    val kind: EntitlementEventKind,
    val occurredAtIsoDate: String,
    val receipt: ProviderVerificationReceipt,
    val entitledUntilIsoDate: String? = null,
) {
    init { require(providerEventId.isNotBlank()) }
}

@Serializable
enum class LedgerAppendOutcome { APPENDED, DUPLICATE_IGNORED, CONFLICTING_REPLAY }

@Serializable
data class LedgerAppendResult(
    val outcome: LedgerAppendOutcome,
    val ledger: List<EntitlementEvent>,
    val reason: String?,
)

/**
 * Append-only, replay-safe ledger. A byte-identical replay is idempotent; a replay that carries
 * different content is a conflict and never silently overwrites recorded history.
 */
object EntitlementEventLedger {
    fun append(ledger: List<EntitlementEvent>, event: EntitlementEvent): LedgerAppendResult {
        val existing = ledger.firstOrNull { it.providerEventId == event.providerEventId }
        return when {
            existing == null -> LedgerAppendResult(LedgerAppendOutcome.APPENDED, ledger + event, null)
            existing == event -> LedgerAppendResult(
                LedgerAppendOutcome.DUPLICATE_IGNORED,
                ledger,
                "Provider event ${event.providerEventId} was replayed byte-identically and was ignored.",
            )
            else -> LedgerAppendResult(
                LedgerAppendOutcome.CONFLICTING_REPLAY,
                ledger,
                "Provider event ${event.providerEventId} was replayed with different content; " +
                    "the append-only ledger was not mutated.",
            )
        }
    }
}

@Serializable
enum class EntitlementAccess { DENIED, GRACE, ENTITLED }

/** Deterministic access snapshot consumed by every platform shell. */
@Serializable
data class EntitlementSnapshot(
    val accountId: String,
    val access: EntitlementAccess,
    val entitledProductIds: List<String>,
    val entitledUntilIsoDate: String?,
    val productionEvaluation: Boolean,
    val projectedEventIds: List<String>,
    val blockers: List<String>,
    val reasons: List<String>,
)

private data class ProductGrant(
    val untilKey: Int,
    val untilIsoDate: String,
    val grace: Boolean,
)

/**
 * Projects a ledger into an access snapshot.
 *
 * The projection is a pure function of (account, ledger contents, as-of date, production flag): the
 * insertion order of the ledger cannot change the result. Grants require an admitted server
 * verification; revocations apply regardless of receipt quality, so a bad receipt can only ever
 * remove access.
 */
object EntitlementProjector {
    private val revokingKinds = setOf(
        EntitlementEventKind.EXPIRED,
        EntitlementEventKind.REFUNDED,
        EntitlementEventKind.REVOKED,
    )

    fun project(
        accountId: String,
        ledger: List<EntitlementEvent>,
        asOfIsoDate: String,
        production: Boolean,
    ): EntitlementSnapshot {
        val blockers = mutableListOf<String>()
        val reasons = mutableListOf<String>()
        val asOf = asOfIsoDate.isoDateKey()
        if (asOf == null) blockers += "asOfIsoDate must be a valid YYYY-MM-DD date."

        ledger.groupBy { it.providerEventId }
            .filterValues { it.size > 1 }
            .keys
            .sorted()
            .forEach { blockers += "Ledger replay is unresolved: provider event $it appears more than once." }

        val ordered = ledger
            .filter { it.receipt.accountId == accountId }
            .sortedWith(compareBy({ it.occurredAtIsoDate }, { it.providerEventId }))

        val grants = mutableMapOf<String, ProductGrant>()
        val projected = mutableListOf<String>()

        for (event in ordered) {
            val occurredAt = event.occurredAtIsoDate.isoDateKey()
            if (occurredAt == null) {
                blockers += "Event ${event.providerEventId} has an invalid occurredAtIsoDate."
                continue
            }
            if (asOf != null && occurredAt > asOf) {
                reasons += "Event ${event.providerEventId} occurs after the as-of date and was not projected."
                continue
            }
            val productId = event.receipt.productId

            if (event.kind in revokingKinds) {
                grants.remove(productId)
                projected += event.providerEventId
                reasons += "Event ${event.providerEventId} (${event.kind}) removed access to $productId; " +
                    "removal never waits for receipt quality."
                continue
            }

            val result = ProviderVerificationReceiptValidator.validate(event.receipt, asOfIsoDate, production)
            val usable = result.admission == EntitlementAdmission.ADMITTED ||
                (result.admission == EntitlementAdmission.REVIEW_REQUIRED && !production)
            if (!usable) {
                reasons += "Event ${event.providerEventId} granted nothing: ${result.blockers.joinToString("; ")}"
                continue
            }

            when (event.kind) {
                EntitlementEventKind.PURCHASE_VERIFIED,
                EntitlementEventKind.RENEWAL_VERIFIED,
                EntitlementEventKind.RESTORED -> {
                    val grant = event.grantOrNull(blockers, grace = false)
                    if (grant != null) {
                        grants[productId] = grant
                        projected += event.providerEventId
                    }
                }

                EntitlementEventKind.GRACE_PERIOD_STARTED -> {
                    val grant = event.grantOrNull(blockers, grace = true)
                    if (grant != null) {
                        grants[productId] = grant
                        projected += event.providerEventId
                    }
                }

                EntitlementEventKind.AUTO_RENEW_CANCELLED -> {
                    val current = grants[productId]
                    if (current == null) {
                        reasons += "Event ${event.providerEventId} cancelled auto-renew for $productId with no active grant."
                    } else {
                        projected += event.providerEventId
                        reasons += "Auto-renew cancellation does not revoke the paid term for $productId."
                    }
                }

                EntitlementEventKind.EXPIRED,
                EntitlementEventKind.REFUNDED,
                EntitlementEventKind.REVOKED -> Unit
            }
        }

        val active = mutableMapOf<String, ProductGrant>()
        grants.toList().sortedBy { it.first }.forEach { (productId, grant) ->
            if (asOf != null && grant.untilKey >= asOf) {
                active[productId] = grant
            } else if (asOf != null) {
                reasons += "Entitlement for $productId expired on ${grant.untilIsoDate}."
            }
        }

        val distinctBlockers = blockers.distinct()
        val access = when {
            distinctBlockers.isNotEmpty() -> EntitlementAccess.DENIED
            active.isEmpty() -> EntitlementAccess.DENIED
            active.values.any { it.grace } -> EntitlementAccess.GRACE
            else -> EntitlementAccess.ENTITLED
        }
        val denied = access == EntitlementAccess.DENIED
        return EntitlementSnapshot(
            accountId = accountId,
            access = access,
            entitledProductIds = if (denied) emptyList() else active.keys.sorted(),
            entitledUntilIsoDate = if (denied) null else active.values.maxByOrNull { it.untilKey }?.untilIsoDate,
            productionEvaluation = production,
            projectedEventIds = projected.sorted(),
            blockers = distinctBlockers,
            reasons = reasons.distinct(),
        )
    }

    private fun EntitlementEvent.grantOrNull(
        blockers: MutableList<String>,
        grace: Boolean,
    ): ProductGrant? {
        val until = entitledUntilIsoDate
        val untilKey = until.isoDateKey()
        if (until == null || untilKey == null) {
            blockers += "Event $providerEventId must declare a valid entitledUntilIsoDate."
            return null
        }
        return ProductGrant(untilKey, until, grace)
    }
}

private fun String?.isSha256(): Boolean = this != null && sha256Pattern.matches(this)

private fun String?.isoDateKey(): Int? {
    if (this == null || !isoDatePattern.matches(this)) return null
    val year = substring(0, 4).toIntOrNull() ?: return null
    val month = substring(5, 7).toIntOrNull() ?: return null
    val day = substring(8, 10).toIntOrNull() ?: return null
    if (month !in 1..12) return null
    val leap = year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)
    val maxDay = when (month) {
        2 -> if (leap) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
    if (day !in 1..maxDay) return null
    return year * 10_000 + month * 100 + day
}
