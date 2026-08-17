package dev.ed3c.gymcometrue.domain

import kotlinx.serialization.Serializable

/**
 * Issue #24 (TW1) — consented Traditional Chinese supplement corpus contract.
 *
 * State transition delivered here: `CORPUS_UNKNOWN -> CONSENT_CONTRACT_DRAFT`.
 *
 * Two invariants drive every decision below:
 *
 * 1. Consent that is `UNKNOWN`, expired, withdrawn, or unverifiable denies use. There is no
 *    "probably still fine" path, and absence of a grant is never read as a grant.
 * 2. Deletion cannot be manifest-only. Flipping a flag in a manifest is a claim about deletion,
 *    not deletion. Every declared storage location must carry its own verified erasure receipt.
 *
 * The real consent receipts, the real storage systems, and the real deletion executor are all
 * outside this repository. This file owns only the deterministic decision made from evidence that
 * has already been produced elsewhere; it never fabricates that evidence.
 */

/** What a subject actually agreed to. Consent for one purpose never implies another. */
@Serializable
enum class ConsentScope {
    OCR_EVALUATION,
    RULE_PACK_TESTING,
    PRODUCT_IMPROVEMENT,
}

/**
 * Resolved consent, as of a specific date.
 *
 * Every state below is emitted by [ConsentResolver.resolve]; none exists only for tests. Only
 * [ACTIVE] permits use, and it is the single state that requires positive evidence to reach.
 */
@Serializable
enum class ConsentState {
    /** No grant is bound to the record at all. */
    UNKNOWN,

    /** A grant exists but cannot be verified: no receipt, no reviewed wording, or no bounded window. */
    UNVERIFIABLE,

    /** The subject withdrew consent on or before the evaluation date. */
    WITHDRAWN,

    /** The consent window closed before the evaluation date. */
    EXPIRED,

    /** Verifiable, unwithdrawn, and inside its bounded window. */
    ACTIVE,
}

/**
 * A consent grant reference.
 *
 * [consentReceiptSha256] and [consentTextSha256] identify artifacts held in the out-of-repository
 * consent store: the signed receipt, and the exact wording the subject was shown. Both are
 * nullable because absence is the honest default — a missing hash resolves to [ConsentState.UNVERIFIABLE]
 * rather than being invented.
 */
@Serializable
data class ConsentGrant(
    val grantId: String,
    val subjectPseudonymousId: String,
    val scopes: Set<ConsentScope> = emptySet(),
    val consentReceiptSha256: String? = null,
    val consentTextSha256: String? = null,
    val grantedAtIsoDate: String? = null,
    val expiresAtIsoDate: String? = null,
    val withdrawnAtIsoDate: String? = null,
) {
    init {
        require(grantId.isNotBlank()) { "A consent grant requires a stable grant id." }
        require(subjectPseudonymousId.isNotBlank()) { "A consent grant requires a pseudonymous subject id." }
    }
}

@Serializable
data class ConsentResolution(
    val state: ConsentState,
    val reasons: List<String>,
)

object ConsentResolver {
    /**
     * Resolves a grant against [asOfIsoDate].
     *
     * A null [grant] is [ConsentState.UNKNOWN], not an error and not permission. An unparsable
     * [asOfIsoDate] resolves to [ConsentState.UNVERIFIABLE]: if the evaluation date cannot be
     * established, neither expiry nor withdrawal can be, so use must stop.
     */
    fun resolve(grant: ConsentGrant?, asOfIsoDate: String): ConsentResolution {
        if (grant == null) {
            return ConsentResolution(
                ConsentState.UNKNOWN,
                listOf("No consent grant is bound to this record."),
            )
        }

        val asOf = asOfIsoDate.taiwanIsoDateKey()
        val granted = grant.grantedAtIsoDate.taiwanIsoDateKey()
        val expires = grant.expiresAtIsoDate.taiwanIsoDateKey()
        val withdrawn = grant.withdrawnAtIsoDate.taiwanIsoDateKey()

        // Withdrawal is the subject's own act and outranks every other reading of the grant.
        if (grant.withdrawnAtIsoDate != null) {
            return if (withdrawn == null) {
                ConsentResolution(
                    ConsentState.UNVERIFIABLE,
                    listOf("Withdrawal is recorded with an invalid date; use stops until it is resolved."),
                )
            } else if (asOf == null || withdrawn <= asOf) {
                ConsentResolution(ConsentState.WITHDRAWN, listOf("The subject withdrew consent."))
            } else {
                ConsentResolution(
                    ConsentState.UNVERIFIABLE,
                    listOf("A withdrawal is dated after the evaluation date; the ledger is inconsistent."),
                )
            }
        }

        val unverifiable = mutableListOf<String>()
        if (asOf == null) unverifiable += "The evaluation date is not a valid YYYY-MM-DD date."
        if (!grant.consentReceiptSha256.isTaiwanSha256()) {
            unverifiable += "Consent has no verifiable receipt SHA-256."
        }
        if (!grant.consentTextSha256.isTaiwanSha256()) {
            unverifiable += "The exact consent wording shown to the subject is not hash-bound."
        }
        if (grant.scopes.isEmpty()) unverifiable += "Consent declares no purpose scope."
        if (grant.grantedAtIsoDate == null || granted == null) {
            unverifiable += "Consent has no valid grant date."
        }
        if (grant.expiresAtIsoDate == null) {
            unverifiable += "Consent has no expiry; an unbounded consent window cannot be verified as current."
        } else if (expires == null) {
            unverifiable += "Consent expiry is not a valid YYYY-MM-DD date."
        }
        if (granted != null && expires != null && granted > expires) {
            unverifiable += "The consent window is inverted."
        }
        if (unverifiable.isNotEmpty()) {
            return ConsentResolution(ConsentState.UNVERIFIABLE, unverifiable)
        }
        if (asOf == null || granted == null || expires == null) {
            return ConsentResolution(
                ConsentState.UNVERIFIABLE,
                listOf("Consent dates are incomplete."),
            )
        }

        if (asOf > expires) {
            return ConsentResolution(ConsentState.EXPIRED, listOf("Consent expired on ${grant.expiresAtIsoDate}."))
        }
        if (asOf < granted) {
            return ConsentResolution(
                ConsentState.UNVERIFIABLE,
                listOf("The evaluation date precedes the grant date; the ledger is inconsistent."),
            )
        }
        return ConsentResolution(ConsentState.ACTIVE, emptyList())
    }
}

/**
 * Where a subject's corpus data can physically come to rest.
 *
 * [DERIVED_OCR_METRICS] is listed because derived artifacts are the location most often forgotten:
 * deleting the label image while keeping per-record OCR rows is not deletion.
 */
@Serializable
enum class CorpusStorageLocation {
    DEVICE_CACHE,
    OBJECT_STORAGE,
    DATABASE_ROW,
    SEARCH_INDEX,
    ANALYTICS_EXPORT,
    ENCRYPTED_BACKUP,
    DERIVED_OCR_METRICS,
}

/**
 * Proof that erasure ran at one location.
 *
 * [verifiedAbsent] is a separate field from [executedAtIsoDate] on purpose: "we ran the delete job"
 * and "we confirmed the data is gone" are different claims, and only the second one counts.
 */
@Serializable
data class DeletionExecutionReceipt(
    val location: CorpusStorageLocation,
    val executedAtIsoDate: String,
    val verifiedAbsent: Boolean = false,
    val verificationEvidenceSha256: String? = null,
    val operatorSignatureSha256: String? = null,
)

@Serializable
data class CorpusDeletionRequest(
    val requestId: String,
    val subjectPseudonymousId: String,
    val requestedAtIsoDate: String,
    val declaredLocations: Set<CorpusStorageLocation> = emptySet(),
    val usedForOcrEvaluation: Boolean = false,
    val manifestUpdated: Boolean = false,
    val receipts: List<DeletionExecutionReceipt> = emptyList(),
) {
    init {
        require(requestId.isNotBlank()) { "A deletion request requires a stable request id." }
        require(subjectPseudonymousId.isNotBlank()) { "A deletion request requires a pseudonymous subject id." }
    }
}

@Serializable
enum class DeletionCompleteness {
    NOT_STARTED,

    /** A manifest claims deletion while no location has a verified erasure receipt. */
    MANIFEST_ONLY,

    PARTIAL,
    VERIFIED_COMPLETE,
}

@Serializable
data class DeletionValidationResult(
    val completeness: DeletionCompleteness,
    val verifiedLocations: List<CorpusStorageLocation>,
    val unverifiedLocations: List<CorpusStorageLocation>,
    val blockers: List<String>,
)

object CorpusDeletionValidator {
    fun validate(request: CorpusDeletionRequest, asOfIsoDate: String): DeletionValidationResult {
        val blockers = mutableListOf<String>()
        val requestedAt = request.requestedAtIsoDate.taiwanIsoDateKey()
        val asOf = asOfIsoDate.taiwanIsoDateKey()
        if (requestedAt == null) blockers += "requestedAtIsoDate must be a valid YYYY-MM-DD date."
        if (asOf == null) blockers += "The validation date must be a valid YYYY-MM-DD date."

        if (request.declaredLocations.isEmpty()) {
            blockers += "A deletion request must declare every storage location holding the subject's data."
        }
        if (request.usedForOcrEvaluation &&
            CorpusStorageLocation.DERIVED_OCR_METRICS !in request.declaredLocations
        ) {
            blockers += "A record used for OCR evaluation must declare DERIVED_OCR_METRICS for deletion."
        }

        val duplicates = request.receipts
            .groupingBy { it.location }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        if (duplicates.isNotEmpty()) {
            blockers += "Duplicate deletion receipts for: ${duplicates.map { it.name }.sorted()}."
        }

        val verified = mutableSetOf<CorpusStorageLocation>()
        request.receipts.forEach { receipt ->
            val name = receipt.location.name
            if (receipt.location !in request.declaredLocations) {
                blockers += "Receipt for $name refers to an undeclared storage location."
                return@forEach
            }
            val executedAt = receipt.executedAtIsoDate.taiwanIsoDateKey()
            var usable = true
            if (executedAt == null) {
                blockers += "Receipt for $name has an invalid execution date."
                usable = false
            }
            if (!receipt.verifiedAbsent) {
                blockers += "Receipt for $name does not confirm the data is absent."
                usable = false
            }
            if (!receipt.verificationEvidenceSha256.isTaiwanSha256()) {
                blockers += "Receipt for $name requires verification evidence SHA-256."
                usable = false
            }
            if (!receipt.operatorSignatureSha256.isTaiwanSha256()) {
                blockers += "Receipt for $name requires an operator signature SHA-256."
                usable = false
            }
            if (executedAt != null && requestedAt != null && executedAt < requestedAt) {
                blockers += "Receipt for $name is dated before the deletion request."
                usable = false
            }
            if (executedAt != null && asOf != null && executedAt > asOf) {
                blockers += "Receipt for $name is dated after the validation date."
                usable = false
            }
            if (usable) verified += receipt.location
        }

        val unverified = (request.declaredLocations - verified).sortedBy { it.ordinal }
        val completeness = when {
            request.declaredLocations.isNotEmpty() && unverified.isEmpty() && blockers.isEmpty() ->
                DeletionCompleteness.VERIFIED_COMPLETE
            verified.isNotEmpty() -> DeletionCompleteness.PARTIAL
            request.manifestUpdated -> DeletionCompleteness.MANIFEST_ONLY
            else -> DeletionCompleteness.NOT_STARTED
        }
        if (completeness == DeletionCompleteness.MANIFEST_ONLY) {
            blockers += "Deletion cannot be manifest-only: no storage location has a verified erasure receipt."
        }
        if (completeness != DeletionCompleteness.VERIFIED_COMPLETE && unverified.isNotEmpty()) {
            blockers += "Erasure is unverified at: ${unverified.map { it.name }}."
        }

        return DeletionValidationResult(
            completeness = completeness,
            verifiedLocations = verified.sortedBy { it.ordinal },
            unverifiedLocations = unverified,
            blockers = blockers.distinct(),
        )
    }
}

/**
 * A corpus record together with the consent and deletion evidence that governs it.
 *
 * [LabelCorpusRecord] already carries the record's own retention decision; this wrapper adds the
 * subject-side evidence that decides whether the record may be used at all.
 */
@Serializable
data class ConsentedCorpusRecord(
    val record: LabelCorpusRecord,
    val grant: ConsentGrant? = null,
    val deletionRequest: CorpusDeletionRequest? = null,
)

@Serializable
data class CorpusUseDecision(
    val admission: EvidenceAdmission,
    val consentState: ConsentState,
    val reasons: List<String>,
)

object ConsentedCorpusAdmissionValidator {
    /**
     * Decides whether [entry] may be used for [purpose] on [asOfIsoDate].
     *
     * Repository-authored synthetic records have no human subject, so they need no grant; they
     * still run through [LabelCorpusAdmissionValidator] for retention and hash checks. Every other
     * record needs an `ACTIVE` grant whose scopes cover [purpose].
     *
     * A pending deletion request denies use regardless of consent state: a subject who asked for
     * erasure has already withdrawn the benefit of the doubt.
     */
    fun validate(
        entry: ConsentedCorpusRecord,
        purpose: ConsentScope,
        asOfIsoDate: String,
    ): CorpusUseDecision {
        val base = LabelCorpusAdmissionValidator.validate(entry.record)
        val synthetic = entry.record.consent == LabelCorpusConsent.SYNTHETIC
        val resolution = if (synthetic && entry.grant == null) {
            ConsentResolution(ConsentState.ACTIVE, emptyList())
        } else {
            ConsentResolver.resolve(entry.grant, asOfIsoDate)
        }

        val blockers = mutableListOf<String>()
        if (base.admission == EvidenceAdmission.DENY) blockers += base.reasons
        if (resolution.state != ConsentState.ACTIVE) {
            blockers += "Consent is ${resolution.state.name}; the record may not be used."
            blockers += resolution.reasons
        } else if (!synthetic && entry.grant?.scopes?.contains(purpose) != true) {
            blockers += "Consent does not cover ${purpose.name}."
        }

        entry.deletionRequest?.let { request ->
            val deletion = CorpusDeletionValidator.validate(request, asOfIsoDate)
            blockers += "A deletion request is open for this subject (${deletion.completeness.name})."
            blockers += deletion.blockers
        }

        val notes = if (base.admission == EvidenceAdmission.REVIEW) base.reasons else emptyList()
        return when {
            blockers.isNotEmpty() -> CorpusUseDecision(
                EvidenceAdmission.DENY,
                resolution.state,
                blockers.distinct(),
            )
            notes.isNotEmpty() -> CorpusUseDecision(EvidenceAdmission.REVIEW, resolution.state, notes)
            else -> CorpusUseDecision(EvidenceAdmission.ALLOW, resolution.state, emptyList())
        }
    }
}
