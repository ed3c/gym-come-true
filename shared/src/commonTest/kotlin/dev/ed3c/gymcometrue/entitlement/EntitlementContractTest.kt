package dev.ed3c.gymcometrue.entitlement

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EntitlementContractTest {
    private val account = "account-under-test"
    private val product = "coach.pro.monthly"
    private val today = "2026-08-18"

    private fun hash(character: Char): String = character.toString().repeat(64)

    private fun receipt(
        receiptId: String = "receipt-1",
        channel: EntitlementAssertionChannel = EntitlementAssertionChannel.SERVER_TO_PROVIDER,
        environment: EntitlementEnvironment = EntitlementEnvironment.PRODUCTION,
        signature: String? = hash('a'),
        payload: String? = hash('b'),
        productId: String = product,
        accountId: String = account,
        transactionId: String = "txn-1",
        verifiedAtIsoDate: String = "2026-08-01",
        priceMinorUnits: Long? = null,
        currencyCode: String? = null,
    ): ProviderVerificationReceipt = ProviderVerificationReceipt(
        receiptId = receiptId,
        provider = EntitlementProvider.APPLE_APP_STORE,
        channel = channel,
        environment = environment,
        accountId = accountId,
        productId = productId,
        providerTransactionId = transactionId,
        verifiedAtIsoDate = verifiedAtIsoDate,
        serverVerificationSignatureSha256 = signature,
        providerPayloadSha256 = payload,
        priceMinorUnits = priceMinorUnits,
        currencyCode = currencyCode,
        note = "Synthetic in-memory contract fixture; no provider account exists.",
    )

    private fun event(
        providerEventId: String = "provider-event-1",
        kind: EntitlementEventKind = EntitlementEventKind.PURCHASE_VERIFIED,
        occurredAtIsoDate: String = "2026-08-01",
        entitledUntilIsoDate: String? = "2026-09-01",
        receipt: ProviderVerificationReceipt = receipt(),
    ): EntitlementEvent = EntitlementEvent(
        providerEventId = providerEventId,
        kind = kind,
        occurredAtIsoDate = occurredAtIsoDate,
        receipt = receipt,
        entitledUntilIsoDate = entitledUntilIsoDate,
    )

    private fun project(
        ledger: List<EntitlementEvent>,
        production: Boolean = true,
        asOfIsoDate: String = today,
    ): EntitlementSnapshot = EntitlementProjector.project(account, ledger, asOfIsoDate, production)

    @Test
    fun serverVerifiedPurchaseGrantsAccess() {
        val snapshot = project(listOf(event()))
        assertEquals(EntitlementAccess.ENTITLED, snapshot.access)
        assertEquals(listOf(product), snapshot.entitledProductIds)
        assertEquals("2026-09-01", snapshot.entitledUntilIsoDate)
        assertTrue(snapshot.blockers.isEmpty())
    }

    @Test
    fun clientAssertedChannelsNeverGrantAccess() {
        val clientChannels = listOf(
            EntitlementAssertionChannel.CLIENT_PURCHASE_CALLBACK,
            EntitlementAssertionChannel.CLIENT_LOCAL_CACHE,
            EntitlementAssertionChannel.PAYWALL_UI_STATE,
            EntitlementAssertionChannel.WEBHOOK_ARRIVAL_UNVERIFIED,
        )
        clientChannels.forEach { channel ->
            val snapshot = project(listOf(event(receipt = receipt(channel = channel))))
            assertEquals(EntitlementAccess.DENIED, snapshot.access, "channel $channel granted access")
            assertTrue(snapshot.entitledProductIds.isEmpty())
            assertNull(snapshot.entitledUntilIsoDate)
            assertTrue(snapshot.reasons.any { it.contains(channel.name) })
        }
    }

    @Test
    fun missingServerVerificationEvidenceDeniesAccess() {
        val unsigned = project(listOf(event(receipt = receipt(signature = null))))
        assertEquals(EntitlementAccess.DENIED, unsigned.access)
        assertTrue(unsigned.reasons.any { it.contains("signature SHA-256 is ABSENT") })

        val unhashed = project(listOf(event(receipt = receipt(payload = null))))
        assertEquals(EntitlementAccess.DENIED, unhashed.access)
        assertTrue(unhashed.reasons.any { it.contains("payload SHA-256 is ABSENT") })
    }

    @Test
    fun priceAndCurrencyCannotChangeAccess() {
        val free = project(listOf(event(receipt = receipt(priceMinorUnits = 0, currencyCode = "TWD"))))
        val expensive = project(listOf(event(receipt = receipt(priceMinorUnits = 99900, currencyCode = "TWD"))))
        assertEquals(free, expensive)
        assertEquals(EntitlementAccess.ENTITLED, free.access)

        val pricedButUnverified = project(
            listOf(
                event(
                    receipt = receipt(
                        channel = EntitlementAssertionChannel.PAYWALL_UI_STATE,
                        priceMinorUnits = 99900,
                        currencyCode = "TWD",
                    ),
                ),
            ),
        )
        assertEquals(EntitlementAccess.DENIED, pricedButUnverified.access)
    }

    @Test
    fun sandboxReceiptCannotGrantProductionAccess() {
        val sandbox = event(receipt = receipt(environment = EntitlementEnvironment.SANDBOX))
        assertEquals(EntitlementAccess.DENIED, project(listOf(sandbox), production = true).access)
        assertEquals(EntitlementAccess.ENTITLED, project(listOf(sandbox), production = false).access)
    }

    @Test
    fun identicalReplayIsIdempotent() {
        val first = EntitlementEventLedger.append(emptyList(), event())
        assertEquals(LedgerAppendOutcome.APPENDED, first.outcome)

        val replay = EntitlementEventLedger.append(first.ledger, event())
        assertEquals(LedgerAppendOutcome.DUPLICATE_IGNORED, replay.outcome)
        assertEquals(1, replay.ledger.size)
        assertEquals(project(first.ledger), project(replay.ledger))
    }

    @Test
    fun conflictingReplayNeverMutatesTheLedger() {
        val first = EntitlementEventLedger.append(emptyList(), event())
        val conflicting = EntitlementEventLedger.append(
            first.ledger,
            event(entitledUntilIsoDate = "2099-01-01"),
        )
        assertEquals(LedgerAppendOutcome.CONFLICTING_REPLAY, conflicting.outcome)
        assertEquals(first.ledger, conflicting.ledger)
        assertEquals("2026-09-01", project(conflicting.ledger).entitledUntilIsoDate)
    }

    @Test
    fun duplicatedEventIdsFailClosed() {
        val snapshot = project(listOf(event(), event(entitledUntilIsoDate = "2099-01-01")))
        assertEquals(EntitlementAccess.DENIED, snapshot.access)
        assertTrue(snapshot.blockers.any { it.contains("appears more than once") })
    }

    @Test
    fun refundRevokesAccessEvenWithoutAnAdmittedReceipt() {
        val ledger = listOf(
            event(),
            event(
                providerEventId = "provider-event-refund",
                kind = EntitlementEventKind.REFUNDED,
                occurredAtIsoDate = "2026-08-10",
                entitledUntilIsoDate = null,
                receipt = receipt(
                    receiptId = "receipt-refund",
                    channel = EntitlementAssertionChannel.WEBHOOK_ARRIVAL_UNVERIFIED,
                    signature = null,
                    payload = null,
                    transactionId = "txn-refund",
                    verifiedAtIsoDate = "2026-08-10",
                ),
            ),
        )
        val snapshot = project(ledger)
        assertEquals(EntitlementAccess.DENIED, snapshot.access)
        assertTrue(snapshot.entitledProductIds.isEmpty())
    }

    @Test
    fun restoreAfterRefundRequiresItsOwnServerVerification() {
        val refunded = listOf(
            event(),
            event(
                providerEventId = "provider-event-refund",
                kind = EntitlementEventKind.REFUNDED,
                occurredAtIsoDate = "2026-08-10",
                entitledUntilIsoDate = null,
                receipt = receipt(receiptId = "receipt-refund", transactionId = "txn-refund", verifiedAtIsoDate = "2026-08-10"),
            ),
        )
        val clientRestore = refunded + event(
            providerEventId = "provider-event-restore",
            kind = EntitlementEventKind.RESTORED,
            occurredAtIsoDate = "2026-08-12",
            entitledUntilIsoDate = "2026-09-01",
            receipt = receipt(
                receiptId = "receipt-restore",
                channel = EntitlementAssertionChannel.CLIENT_LOCAL_CACHE,
                transactionId = "txn-restore",
                verifiedAtIsoDate = "2026-08-12",
            ),
        )
        assertEquals(EntitlementAccess.DENIED, project(clientRestore).access)

        val serverRestore = refunded + event(
            providerEventId = "provider-event-restore",
            kind = EntitlementEventKind.RESTORED,
            occurredAtIsoDate = "2026-08-12",
            entitledUntilIsoDate = "2026-09-01",
            receipt = receipt(receiptId = "receipt-restore", transactionId = "txn-restore", verifiedAtIsoDate = "2026-08-12"),
        )
        assertEquals(EntitlementAccess.ENTITLED, project(serverRestore).access)
    }

    @Test
    fun gracePeriodIsItsOwnStateAndStillExpires() {
        val grace = listOf(
            event(),
            event(
                providerEventId = "provider-event-grace",
                kind = EntitlementEventKind.GRACE_PERIOD_STARTED,
                occurredAtIsoDate = "2026-08-15",
                entitledUntilIsoDate = "2026-08-25",
                receipt = receipt(receiptId = "receipt-grace", transactionId = "txn-grace", verifiedAtIsoDate = "2026-08-15"),
            ),
        )
        assertEquals(EntitlementAccess.GRACE, project(grace).access)
        assertEquals(EntitlementAccess.DENIED, project(grace, asOfIsoDate = "2026-08-26").access)
    }

    @Test
    fun autoRenewCancellationKeepsThePaidTerm() {
        val ledger = listOf(
            event(),
            event(
                providerEventId = "provider-event-cancel",
                kind = EntitlementEventKind.AUTO_RENEW_CANCELLED,
                occurredAtIsoDate = "2026-08-05",
                entitledUntilIsoDate = null,
                receipt = receipt(receiptId = "receipt-cancel", transactionId = "txn-cancel", verifiedAtIsoDate = "2026-08-05"),
            ),
        )
        assertEquals(EntitlementAccess.ENTITLED, project(ledger).access)
        assertEquals(EntitlementAccess.DENIED, project(ledger, asOfIsoDate = "2026-09-02").access)
    }

    @Test
    fun projectionIsIndependentOfLedgerOrder() {
        val ledger = listOf(
            event(),
            event(
                providerEventId = "provider-event-renewal",
                kind = EntitlementEventKind.RENEWAL_VERIFIED,
                occurredAtIsoDate = "2026-08-16",
                entitledUntilIsoDate = "2026-10-01",
                receipt = receipt(receiptId = "receipt-renewal", transactionId = "txn-renewal", verifiedAtIsoDate = "2026-08-16"),
            ),
            event(
                providerEventId = "provider-event-cancel",
                kind = EntitlementEventKind.AUTO_RENEW_CANCELLED,
                occurredAtIsoDate = "2026-08-17",
                entitledUntilIsoDate = null,
                receipt = receipt(receiptId = "receipt-cancel", transactionId = "txn-cancel", verifiedAtIsoDate = "2026-08-17"),
            ),
        )
        assertEquals(project(ledger), project(ledger.reversed()))
        assertEquals("2026-10-01", project(ledger.reversed()).entitledUntilIsoDate)
    }

    @Test
    fun anotherAccountsReceiptCannotGrantAccess() {
        val snapshot = project(listOf(event(receipt = receipt(accountId = "someone-else"))))
        assertEquals(EntitlementAccess.DENIED, snapshot.access)
    }

    @Test
    fun eventsAfterTheAsOfDateAreNotProjected() {
        val snapshot = project(listOf(event(occurredAtIsoDate = "2026-08-19")), asOfIsoDate = today)
        assertEquals(EntitlementAccess.DENIED, snapshot.access)
        assertTrue(snapshot.reasons.any { it.contains("after the as-of date") })
    }

    @Test
    fun receiptValidatorSeparatesSandboxFromProductionAdmission() {
        val sandbox = ProviderVerificationReceiptValidator.validate(
            receipt(environment = EntitlementEnvironment.SANDBOX),
            today,
            production = false,
        )
        assertEquals(EntitlementAdmission.REVIEW_REQUIRED, sandbox.admission)

        val rejected = ProviderVerificationReceiptValidator.validate(
            receipt(environment = EntitlementEnvironment.SANDBOX),
            today,
            production = true,
        )
        assertEquals(EntitlementAdmission.REJECTED, rejected.admission)

        val admitted = ProviderVerificationReceiptValidator.validate(receipt(), today, production = true)
        assertEquals(EntitlementAdmission.ADMITTED, admitted.admission)
    }
}
