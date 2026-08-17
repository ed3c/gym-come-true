package dev.ed3c.gymcometrue.privacy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrivacyLifecycleTest {
    private val today = "2026-08-18"
    private val account = "account-under-test"

    private fun entry(
        entryId: String,
        category: PersonalDataCategory,
        purpose: ProcessingPurpose = ProcessingPurpose.SERVICE_DELIVERY,
        store: DataStore = DataStore.ACCOUNT_DATABASE,
        storageRegion: StorageRegion = StorageRegion.TW,
        retentionDays: Int? = 365,
        legalBasis: LegalBasis = LegalBasis.CONTRACT,
        legalBasisRef: String? = "privacy-policy-2026-08#contract",
        exportTreatment: ExportTreatment = ExportTreatment.INCLUDED,
        exportExclusionRef: String? = null,
        redactedFields: List<String> = emptyList(),
        erasableOnDelete: Boolean = true,
        retentionExemptionRef: String? = null,
    ): PrivacyInventoryEntry = PrivacyInventoryEntry(
        entryId = entryId,
        category = category,
        purpose = purpose,
        store = store,
        storageRegion = storageRegion,
        retentionDays = retentionDays,
        legalBasis = legalBasis,
        legalBasisRef = legalBasisRef,
        exportTreatment = exportTreatment,
        exportExclusionRef = exportExclusionRef,
        redactedFields = redactedFields,
        erasableOnDelete = erasableOnDelete,
        retentionExemptionRef = retentionExemptionRef,
        note = "Synthetic inventory fixture; no real account data exists.",
    )

    private fun identityEntry() = entry("account-identity-db", PersonalDataCategory.ACCOUNT_IDENTITY)

    private fun intakeEntry() = entry("supplement-intake-db", PersonalDataCategory.SUPPLEMENT_INTAKE)

    private fun consentRecordEntry() = entry(
        entryId = "consent-record-db",
        category = PersonalDataCategory.CONSENT_RECORD,
        purpose = ProcessingPurpose.LEGAL_OBLIGATION,
        retentionDays = null,
        legalBasis = LegalBasis.LEGAL_OBLIGATION,
        legalBasisRef = "tw-record-keeping",
        erasableOnDelete = false,
        retentionExemptionRef = "tw-record-keeping#consent-proof",
    )

    private fun baseInventory() = listOf(identityEntry(), intakeEntry(), consentRecordEntry())

    private fun consent(vararg events: ConsentEvent): ConsentHistoryResult =
        ConsentHistoryResolver.resolve(events.toList(), today)

    private fun consentEvent(
        sequence: Int,
        purpose: ProcessingPurpose,
        granted: Boolean,
        occurredAtIsoDate: String,
    ): ConsentEvent = ConsentEvent(
        sequence = sequence,
        purpose = purpose,
        granted = granted,
        occurredAtIsoDate = occurredAtIsoDate,
        policyVersion = "2026-08-01",
        evidenceRef = "consent-log#$sequence",
    )

    private fun validate(
        inventory: List<PrivacyInventoryEntry>,
        consentHistory: ConsentHistoryResult = consent(),
        production: Boolean = true,
    ): PrivacyInventoryResult = PrivacyInventoryValidator.validate(inventory, consentHistory, production)

    private fun deletionRequest(confirmed: Boolean = true) = AccountDeletionRequest(
        requestId = "deletion-1",
        accountId = account,
        requestedAtIsoDate = "2026-08-16",
        confirmedByAccountHolder = confirmed,
    )

    private fun erasure(
        entryId: String,
        store: DataStore = DataStore.ACCOUNT_DATABASE,
        method: ErasureMethod = ErasureMethod.HARD_DELETE,
        receiptRef: String? = "erasure-receipt-$entryId",
        executedAtIsoDate: String = "2026-08-17",
    ) = ErasureExecution(entryId, store, method, executedAtIsoDate, receiptRef)

    @Test
    fun baseInventoryIsAConsistentContract() {
        val result = validate(baseInventory())
        assertTrue(result.contractSatisfied, result.blockers.toString())
    }

    @Test
    fun healthDataCanNeverBeUsedForAdvertising() {
        val granted = consent(consentEvent(1, ProcessingPurpose.ADVERTISING, true, "2026-08-01"))
        val inventory = baseInventory() + entry(
            entryId = "intake-ads",
            category = PersonalDataCategory.SUPPLEMENT_INTAKE,
            purpose = ProcessingPurpose.ADVERTISING,
            legalBasis = LegalBasis.CONSENT,
            legalBasisRef = "privacy-policy-2026-08#advertising",
        )
        val result = validate(inventory, granted)
        assertFalse(result.contractSatisfied)
        assertTrue(result.blockers.any { it.contains("can never be processed for advertising") })
    }

    @Test
    fun healthDataCanNeverBeUsedForModelTraining() {
        val granted = consent(consentEvent(1, ProcessingPurpose.MODEL_TRAINING, true, "2026-08-01"))
        val inventory = baseInventory() + entry(
            entryId = "ocr-training",
            category = PersonalDataCategory.OCR_TEXT,
            purpose = ProcessingPurpose.MODEL_TRAINING,
            legalBasis = LegalBasis.CONSENT,
            legalBasisRef = "privacy-policy-2026-08#training",
        )
        val result = validate(inventory, granted)
        assertFalse(result.contractSatisfied)
        assertTrue(result.blockers.any { it.contains("model training") })
    }

    @Test
    fun healthDataCannotEnterTheAnalyticsPipeline() {
        val inventory = baseInventory() + entry(
            entryId = "intake-analytics",
            category = PersonalDataCategory.SUPPLEMENT_INTAKE,
            store = DataStore.ANALYTICS_PIPELINE,
        )
        val result = validate(inventory)
        assertFalse(result.contractSatisfied)
        assertTrue(result.blockers.any { it.contains("analytics pipeline") })
    }

    @Test
    fun retainedRecordRequiresAnExplicitLegalBasis() {
        val inventory = baseInventory() + entry(
            entryId = "unexplained-archive",
            category = PersonalDataCategory.HEALTH_MEASUREMENT,
            store = DataStore.EVIDENCE_ARCHIVE,
            legalBasis = LegalBasis.ABSENT,
            legalBasisRef = null,
        )
        val result = validate(inventory)
        assertFalse(result.contractSatisfied)
        assertTrue(result.blockers.any { it.contains("no explicit legal basis") })
    }

    @Test
    fun indefiniteRetentionRequiresALegalObligation() {
        val inventory = listOf(
            identityEntry(),
            intakeEntry(),
            consentRecordEntry(),
            entry(
                entryId = "forever-telemetry",
                category = PersonalDataCategory.DIAGNOSTIC_TELEMETRY,
                store = DataStore.EVIDENCE_ARCHIVE,
                retentionDays = null,
            ),
        )
        val result = validate(inventory)
        assertFalse(result.contractSatisfied)
        assertTrue(result.blockers.any { it.contains("Indefinite retention") })
    }

    @Test
    fun withdrawnConsentStopsConsentGatedProcessing() {
        val analytics = entry(
            entryId = "telemetry-analytics",
            category = PersonalDataCategory.DIAGNOSTIC_TELEMETRY,
            purpose = ProcessingPurpose.PRODUCT_ANALYTICS,
            store = DataStore.ANALYTICS_PIPELINE,
            legalBasis = LegalBasis.CONSENT,
            legalBasisRef = "privacy-policy-2026-08#analytics",
        )
        val inventory = baseInventory() + analytics

        val grantedOnly = consent(consentEvent(1, ProcessingPurpose.PRODUCT_ANALYTICS, true, "2026-08-01"))
        assertTrue(validate(inventory, grantedOnly).contractSatisfied)

        val withdrawn = consent(
            consentEvent(1, ProcessingPurpose.PRODUCT_ANALYTICS, true, "2026-08-01"),
            consentEvent(2, ProcessingPurpose.PRODUCT_ANALYTICS, false, "2026-08-10"),
        )
        assertEquals(ConsentState.WITHDRAWN, withdrawn.states[ProcessingPurpose.PRODUCT_ANALYTICS])
        val result = validate(inventory, withdrawn)
        assertFalse(result.contractSatisfied)
        assertTrue(result.blockers.any { it.contains("processing must stop") })
    }

    @Test
    fun consentHistoryIsAppendOnlyAndContiguous() {
        val gap = ConsentHistoryResolver.resolve(
            listOf(
                consentEvent(1, ProcessingPurpose.PRODUCT_ANALYTICS, true, "2026-08-01"),
                consentEvent(3, ProcessingPurpose.PRODUCT_ANALYTICS, false, "2026-08-10"),
            ),
            today,
        )
        assertTrue(gap.blockers.any { it.contains("contiguous") })

        val backdated = ConsentHistoryResolver.resolve(
            listOf(
                consentEvent(1, ProcessingPurpose.PRODUCT_ANALYTICS, true, "2026-08-10"),
                consentEvent(2, ProcessingPurpose.PRODUCT_ANALYTICS, false, "2026-08-01"),
            ),
            today,
        )
        assertTrue(backdated.blockers.any { it.contains("append-only") })
    }

    @Test
    fun unresolvedConsentHistoryStopsConsentGatedProcessing() {
        val analytics = entry(
            entryId = "telemetry-analytics",
            category = PersonalDataCategory.DIAGNOSTIC_TELEMETRY,
            purpose = ProcessingPurpose.PRODUCT_ANALYTICS,
            store = DataStore.ANALYTICS_PIPELINE,
            legalBasis = LegalBasis.CONSENT,
            legalBasisRef = "privacy-policy-2026-08#analytics",
        )
        val broken = ConsentHistoryResolver.resolve(
            listOf(consentEvent(2, ProcessingPurpose.PRODUCT_ANALYTICS, true, "2026-08-01")),
            today,
        )
        val result = validate(baseInventory() + analytics, broken)
        assertFalse(result.contractSatisfied)
        assertTrue(result.blockers.any { it.contains("Consent history is unresolved") })
    }

    @Test
    fun aCategoryHeldOnlyAsAUiProjectionHasNoRecordOfTruth() {
        val inventory = baseInventory() + entry(
            entryId = "health-card",
            category = PersonalDataCategory.HEALTH_MEASUREMENT,
            store = DataStore.UI_PROJECTION,
        )
        val result = validate(inventory)
        assertFalse(result.contractSatisfied)
        assertTrue(result.blockers.any { it.contains("location of record is ABSENT") })
    }

    @Test
    fun deletionCannotBeUiOnly() {
        val inventory = listOf(identityEntry(), intakeEntry())
        val result = AccountDeletionValidator.validate(
            deletionRequest(),
            inventory,
            listOf(
                erasure("account-identity-db"),
                erasure("supplement-intake-db", method = ErasureMethod.UI_HIDE),
            ),
        )
        assertEquals(DeletionOutcome.INCOMPLETE, result.outcome)
        assertTrue(result.blockers.any { it.contains("UI-only") })
        assertEquals(listOf("account-identity-db"), result.erasedEntryIds)
    }

    @Test
    fun deletionMustReachEveryInventoriedStore() {
        val inventory = listOf(
            identityEntry(),
            entry("identity-backup", PersonalDataCategory.ACCOUNT_IDENTITY, store = DataStore.BACKUP_SNAPSHOT),
        )
        val result = AccountDeletionValidator.validate(
            deletionRequest(),
            inventory,
            listOf(erasure("account-identity-db")),
        )
        assertEquals(DeletionOutcome.INCOMPLETE, result.outcome)
        assertTrue(result.blockers.any { it.contains("did not erase identity-backup") })
    }

    @Test
    fun anonymisationIsNotErasureForHealthData() {
        val result = AccountDeletionValidator.validate(
            deletionRequest(),
            listOf(intakeEntry()),
            listOf(erasure("supplement-intake-db", method = ErasureMethod.ANONYMIZE)),
        )
        assertEquals(DeletionOutcome.INCOMPLETE, result.outcome)
        assertTrue(result.blockers.any { it.contains("Anonymisation is not erasure") })
    }

    @Test
    fun erasureWithoutAReceiptIsNotComplete() {
        val result = AccountDeletionValidator.validate(
            deletionRequest(),
            listOf(identityEntry()),
            listOf(erasure("account-identity-db", receiptRef = null)),
        )
        assertEquals(DeletionOutcome.INCOMPLETE, result.outcome)
        assertTrue(result.blockers.any { it.contains("no execution receipt") })
    }

    @Test
    fun unconfirmedDeletionIsRejected() {
        val result = AccountDeletionValidator.validate(
            deletionRequest(confirmed = false),
            listOf(identityEntry()),
            listOf(erasure("account-identity-db")),
        )
        assertEquals(DeletionOutcome.REJECTED, result.outcome)
    }

    @Test
    fun completeDeletionSeparatesErasedFromLegallyRetainedRecords() {
        val executions = listOf(
            erasure("account-identity-db"),
            erasure("supplement-intake-db", method = ErasureMethod.CRYPTO_SHRED),
        )
        val result = AccountDeletionValidator.validate(deletionRequest(), baseInventory(), executions)
        assertEquals(DeletionOutcome.COMPLETE_WITH_RETAINED_RECORDS, result.outcome)
        assertEquals(listOf("account-identity-db", "supplement-intake-db"), result.erasedEntryIds)
        assertEquals(listOf("consent-record-db"), result.retainedEntryIds)
        assertTrue(result.blockers.isEmpty())

        val withoutRetention = AccountDeletionValidator.validate(
            deletionRequest(),
            listOf(identityEntry(), intakeEntry()),
            executions,
        )
        assertEquals(DeletionOutcome.COMPLETE, withoutRetention.outcome)
    }

    @Test
    fun exportMustCoverEveryInventoriedRecord() {
        val inventory = baseInventory()
        val incomplete = AccountExportValidator.validate(
            AccountExportRequest("export-1", account, today, listOf("account-identity-db")),
            inventory,
        )
        assertFalse(incomplete.complete)
        assertEquals(listOf("consent-record-db", "supplement-intake-db"), incomplete.missingEntryIds)

        val complete = AccountExportValidator.validate(
            AccountExportRequest(
                "export-2",
                account,
                today,
                listOf("account-identity-db", "supplement-intake-db", "consent-record-db"),
            ),
            inventory,
        )
        assertTrue(complete.complete, complete.blockers.toString())
    }

    @Test
    fun exportCannotIncludeARecordMarkedExcluded() {
        val inventory = listOf(
            identityEntry(),
            entry(
                entryId = "fraud-signals",
                category = PersonalDataCategory.DIAGNOSTIC_TELEMETRY,
                purpose = ProcessingPurpose.FRAUD_PREVENTION,
                exportTreatment = ExportTreatment.EXCLUDED,
                exportExclusionRef = "fraud-defence#disclosure-limit",
            ),
        )
        val result = AccountExportValidator.validate(
            AccountExportRequest("export-3", account, today, listOf("account-identity-db", "fraud-signals")),
            inventory,
        )
        assertFalse(result.complete)
        assertTrue(result.blockers.any { it.contains("marked EXCLUDED") })
    }

    @Test
    fun healthDataCannotBeExcludedFromTheExport() {
        val inventory = baseInventory() + entry(
            entryId = "health-hidden",
            category = PersonalDataCategory.HEALTH_MEASUREMENT,
            store = DataStore.EVIDENCE_ARCHIVE,
            exportTreatment = ExportTreatment.EXCLUDED,
            exportExclusionRef = "internal#too-noisy",
        )
        val result = validate(inventory)
        assertFalse(result.contractSatisfied)
        assertTrue(result.blockers.any { it.contains("cannot be excluded from the account export") })
    }

    @Test
    fun providerLedgerRecordsMustBeRedactedInExport() {
        val raw = entry(
            entryId = "entitlement-ledger",
            category = PersonalDataCategory.ENTITLEMENT_RECEIPT,
            store = DataStore.PROVIDER_LEDGER,
        )
        val rawResult = validate(baseInventory() + raw)
        assertFalse(rawResult.contractSatisfied)
        assertTrue(rawResult.blockers.any { it.contains("must be redacted in export") })

        val redacted = entry(
            entryId = "entitlement-ledger",
            category = PersonalDataCategory.ENTITLEMENT_RECEIPT,
            store = DataStore.PROVIDER_LEDGER,
            exportTreatment = ExportTreatment.REDACTED,
            redactedFields = listOf("providerPayload", "serverVerificationSignatureSha256"),
        )
        assertTrue(validate(baseInventory() + redacted).contractSatisfied)
    }

    @Test
    fun storageRegionMustBeDeclaredForProduction() {
        val inventory = listOf(
            entry("account-identity-db", PersonalDataCategory.ACCOUNT_IDENTITY, storageRegion = StorageRegion.UNSPECIFIED),
        )
        assertFalse(validate(inventory, production = true).contractSatisfied)
        val draft = validate(inventory, production = false)
        assertTrue(draft.contractSatisfied)
        assertTrue(draft.reviewNotes.any { it.contains("Storage region") })
    }
}
