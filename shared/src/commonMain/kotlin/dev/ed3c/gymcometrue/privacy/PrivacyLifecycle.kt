package dev.ed3c.gymcometrue.privacy

import kotlinx.serialization.Serializable

private val isoDatePattern = Regex("^\\d{4}-\\d{2}-\\d{2}$")

@Serializable
enum class PersonalDataCategory {
    ACCOUNT_IDENTITY,
    HEALTH_MEASUREMENT,
    SUPPLEMENT_INTAKE,
    LABEL_SCAN_IMAGE,
    OCR_TEXT,
    PROTOCOL_SCHEDULE,
    ENTITLEMENT_RECEIPT,
    DIAGNOSTIC_TELEMETRY,
    CONSENT_RECORD,
}

@Serializable
enum class ProcessingPurpose {
    SERVICE_DELIVERY,
    SAFETY_EVIDENCE,
    LEGAL_OBLIGATION,
    FRAUD_PREVENTION,
    PRODUCT_ANALYTICS,
    ADVERTISING,
    MODEL_TRAINING,
}

@Serializable
enum class LegalBasis { ABSENT, CONSENT, CONTRACT, LEGAL_OBLIGATION, VITAL_INTEREST }

/** Physical location of record. A UI projection is never a location of record. */
@Serializable
enum class DataStore {
    DEVICE_LOCAL,
    ACCOUNT_DATABASE,
    EVIDENCE_ARCHIVE,
    ANALYTICS_PIPELINE,
    BACKUP_SNAPSHOT,
    PROVIDER_LEDGER,
    UI_PROJECTION,
}

@Serializable
enum class StorageRegion { UNSPECIFIED, TW, EU, US }

@Serializable
enum class ExportTreatment { INCLUDED, REDACTED, EXCLUDED }

/**
 * One row of the privacy inventory: one category of personal data, held in one store, for one
 * purpose. The inventory is the subject every other privacy operation is checked against, so a flow
 * that is not inventoried cannot be exported, retained, or erased.
 */
@Serializable
data class PrivacyInventoryEntry(
    val entryId: String,
    val category: PersonalDataCategory,
    val purpose: ProcessingPurpose,
    val store: DataStore,
    val storageRegion: StorageRegion,
    val retentionDays: Int? = null,
    val legalBasis: LegalBasis = LegalBasis.ABSENT,
    val legalBasisRef: String? = null,
    val exportTreatment: ExportTreatment = ExportTreatment.INCLUDED,
    val exportExclusionRef: String? = null,
    val redactedFields: List<String> = emptyList(),
    val erasableOnDelete: Boolean = true,
    val retentionExemptionRef: String? = null,
    val note: String,
) {
    init { require(entryId.isNotBlank() && note.isNotBlank()) }
}

@Serializable
enum class ConsentState { NEVER_GIVEN, GRANTED, WITHDRAWN }

@Serializable
data class ConsentEvent(
    val sequence: Int,
    val purpose: ProcessingPurpose,
    val granted: Boolean,
    val occurredAtIsoDate: String,
    val policyVersion: String,
    val evidenceRef: String,
) {
    init {
        require(sequence > 0)
        require(policyVersion.isNotBlank() && evidenceRef.isNotBlank())
    }
}

@Serializable
data class ConsentHistoryResult(
    val states: Map<ProcessingPurpose, ConsentState>,
    val blockers: List<String>,
)

/**
 * Consent history is append-only. Withdrawal is a new event, never an edit or a deletion of the
 * grant that preceded it, and a gap in the sequence means a lost record rather than a silent
 * "still granted".
 */
object ConsentHistoryResolver {
    fun resolve(events: List<ConsentEvent>, asOfIsoDate: String): ConsentHistoryResult {
        val blockers = mutableListOf<String>()
        val asOf = asOfIsoDate.isoDateKey()
        if (asOf == null) blockers += "asOfIsoDate must be a valid YYYY-MM-DD date."

        val ordered = events.sortedBy { it.sequence }
        val sequences = ordered.map { it.sequence }
        if (sequences.distinct().size != sequences.size) {
            blockers += "Consent history sequences must be unique."
        }
        if (sequences.isNotEmpty() && sequences != (1..sequences.size).toList()) {
            blockers += "Consent history must be contiguous from 1; a gap is a lost consent record."
        }

        val latest = mutableMapOf<ProcessingPurpose, ConsentState>()
        var previous: Int? = null
        for (event in ordered) {
            val occurredAt = event.occurredAtIsoDate.isoDateKey()
            if (occurredAt == null) {
                blockers += "Consent event ${event.sequence} has an invalid occurredAtIsoDate."
                continue
            }
            val earlier = previous
            if (earlier != null && occurredAt < earlier) {
                blockers += "Consent history is not append-only: event ${event.sequence} predates its predecessor."
            }
            previous = occurredAt
            if (asOf != null && occurredAt > asOf) {
                blockers += "Consent event ${event.sequence} occurs after the as-of date."
                continue
            }
            latest[event.purpose] = if (event.granted) ConsentState.GRANTED else ConsentState.WITHDRAWN
        }

        return ConsentHistoryResult(
            states = ProcessingPurpose.entries.associateWith { latest[it] ?: ConsentState.NEVER_GIVEN },
            blockers = blockers.distinct(),
        )
    }
}

@Serializable
data class PrivacyInventoryResult(
    val contractSatisfied: Boolean,
    val blockers: List<String>,
    val reviewNotes: List<String>,
)

object PrivacyInventoryValidator {
    /**
     * Categories that describe a person's body, intake, or health routine. Advertising and model
     * training are denied for these regardless of any consent record.
     */
    val healthDerivedCategories: Set<PersonalDataCategory> = setOf(
        PersonalDataCategory.HEALTH_MEASUREMENT,
        PersonalDataCategory.SUPPLEMENT_INTAKE,
        PersonalDataCategory.LABEL_SCAN_IMAGE,
        PersonalDataCategory.OCR_TEXT,
        PersonalDataCategory.PROTOCOL_SCHEDULE,
    )

    private val consentGatedPurposes = setOf(
        ProcessingPurpose.PRODUCT_ANALYTICS,
        ProcessingPurpose.ADVERTISING,
        ProcessingPurpose.MODEL_TRAINING,
    )

    fun validate(
        entries: List<PrivacyInventoryEntry>,
        consent: ConsentHistoryResult,
        production: Boolean,
    ): PrivacyInventoryResult {
        val blockers = mutableListOf<String>()
        val notes = mutableListOf<String>()

        entries.groupBy { it.entryId }
            .filterValues { it.size > 1 }
            .keys
            .sorted()
            .forEach { blockers += "Privacy inventory declares $it more than once." }

        val consentUsable = consent.blockers.isEmpty()
        if (!consentUsable) {
            blockers += "Consent history is unresolved; consent-gated processing must stop."
        }

        PersonalDataCategory.entries.forEach { category ->
            val stores = entries.filter { it.category == category }.map { it.store }.toSet()
            if (stores == setOf(DataStore.UI_PROJECTION)) {
                blockers += "Category $category is inventoried only as a UI projection; " +
                    "its location of record is ABSENT."
            }
        }

        entries.sortedBy { it.entryId }.forEach { entry ->
            val health = entry.category in healthDerivedCategories

            if (health && entry.purpose == ProcessingPurpose.ADVERTISING) {
                blockers += "Health-derived record ${entry.entryId} can never be processed for advertising; " +
                    "consent cannot unlock it."
            }
            if (health && entry.purpose == ProcessingPurpose.MODEL_TRAINING) {
                blockers += "Health-derived record ${entry.entryId} can never be used for model training."
            }
            if (health && entry.store == DataStore.ANALYTICS_PIPELINE) {
                blockers += "Health-derived record ${entry.entryId} must not enter the analytics pipeline."
            }

            if (entry.legalBasis == LegalBasis.ABSENT) {
                blockers += "Retained record ${entry.entryId} has no explicit legal basis."
            } else if (entry.legalBasisRef.isNullOrBlank()) {
                blockers += "Legal basis for ${entry.entryId} has no recorded reference."
            }

            if (entry.purpose in consentGatedPurposes) {
                if (entry.legalBasis != LegalBasis.CONSENT) {
                    blockers += "Purpose ${entry.purpose} for ${entry.entryId} requires a CONSENT legal basis."
                }
                val state = if (consentUsable) {
                    consent.states[entry.purpose] ?: ConsentState.NEVER_GIVEN
                } else {
                    ConsentState.NEVER_GIVEN
                }
                if (state != ConsentState.GRANTED) {
                    blockers += "Purpose ${entry.purpose} for ${entry.entryId} is $state; processing must stop."
                }
            }

            val retentionDays = entry.retentionDays
            if (retentionDays == null) {
                if (entry.legalBasis != LegalBasis.LEGAL_OBLIGATION) {
                    blockers += "Indefinite retention of ${entry.entryId} requires a legal-obligation basis."
                } else if (entry.retentionExemptionRef.isNullOrBlank()) {
                    blockers += "Indefinite retention of ${entry.entryId} requires an explicit exemption reference."
                }
            } else if (retentionDays <= 0) {
                blockers += "Retention for ${entry.entryId} must be a positive number of days."
            }

            if (!entry.erasableOnDelete && entry.retentionExemptionRef.isNullOrBlank()) {
                blockers += "Record ${entry.entryId} is excluded from erasure without an explicit exemption."
            }

            when (entry.exportTreatment) {
                ExportTreatment.EXCLUDED -> {
                    if (entry.exportExclusionRef.isNullOrBlank()) {
                        blockers += "Export exclusion for ${entry.entryId} requires an explicit reference."
                    }
                    if (health) {
                        blockers += "Health-derived record ${entry.entryId} cannot be excluded from the account export."
                    }
                }

                ExportTreatment.REDACTED ->
                    if (entry.redactedFields.isEmpty()) {
                        blockers += "Redacted export of ${entry.entryId} must name the redacted fields."
                    }

                ExportTreatment.INCLUDED ->
                    if (entry.store == DataStore.PROVIDER_LEDGER) {
                        blockers += "Provider ledger record ${entry.entryId} must be redacted in export; " +
                            "raw provider payloads can carry provider credentials."
                    }
            }

            if (entry.storageRegion == StorageRegion.UNSPECIFIED) {
                if (production) {
                    blockers += "Storage region for ${entry.entryId} is ABSENT."
                } else {
                    notes += "Storage region for ${entry.entryId} is ABSENT."
                }
            }
        }

        val distinct = blockers.distinct()
        return PrivacyInventoryResult(distinct.isEmpty(), distinct, notes.distinct())
    }
}

@Serializable
enum class ErasureMethod { NONE, UI_HIDE, ANONYMIZE, HARD_DELETE, CRYPTO_SHRED }

@Serializable
data class ErasureExecution(
    val entryId: String,
    val store: DataStore,
    val method: ErasureMethod,
    val executedAtIsoDate: String,
    val receiptRef: String? = null,
) {
    init { require(entryId.isNotBlank()) }
}

@Serializable
data class AccountDeletionRequest(
    val requestId: String,
    val accountId: String,
    val requestedAtIsoDate: String,
    val confirmedByAccountHolder: Boolean,
) {
    init { require(requestId.isNotBlank() && accountId.isNotBlank()) }
}

@Serializable
enum class DeletionOutcome { REJECTED, INCOMPLETE, COMPLETE_WITH_RETAINED_RECORDS, COMPLETE }

@Serializable
data class AccountDeletionResult(
    val outcome: DeletionOutcome,
    val erasedEntryIds: List<String>,
    val retainedEntryIds: List<String>,
    val blockers: List<String>,
)

/**
 * Account deletion is complete only when every inventoried store produced an erasure receipt.
 * Hiding a record from the UI, or leaving it in a backup snapshot, is not deletion.
 */
object AccountDeletionValidator {
    private val anonymizableCategories = setOf(PersonalDataCategory.DIAGNOSTIC_TELEMETRY)

    fun validate(
        request: AccountDeletionRequest,
        inventory: List<PrivacyInventoryEntry>,
        executions: List<ErasureExecution>,
    ): AccountDeletionResult {
        val blockers = mutableListOf<String>()
        if (!request.confirmedByAccountHolder) {
            blockers += "Account deletion requires an explicit account-holder confirmation."
        }
        val requestedAt = request.requestedAtIsoDate.isoDateKey()
        if (requestedAt == null) blockers += "requestedAtIsoDate must be a valid YYYY-MM-DD date."

        val knownIds = inventory.map { it.entryId }.toSet()
        executions.filter { it.entryId !in knownIds }
            .map { it.entryId }
            .distinct()
            .sorted()
            .forEach { blockers += "Erasure execution references $it, which is not in the privacy inventory." }

        val erased = mutableListOf<String>()
        val retained = mutableListOf<String>()

        inventory.sortedBy { it.entryId }.forEach { entry ->
            if (!entry.erasableOnDelete) {
                retained += entry.entryId
                if (entry.retentionExemptionRef.isNullOrBlank()) {
                    blockers += "Retained record ${entry.entryId} has no explicit retention exemption."
                }
                if (entry.legalBasis != LegalBasis.LEGAL_OBLIGATION) {
                    blockers += "Retained record ${entry.entryId} must be held under a legal obligation."
                }
                return@forEach
            }

            val before = blockers.size
            val execution = executions.firstOrNull { it.entryId == entry.entryId && it.store == entry.store }
            if (execution == null) {
                blockers += "Account deletion did not erase ${entry.entryId} in ${entry.store}."
                return@forEach
            }

            when (execution.method) {
                ErasureMethod.UI_HIDE ->
                    blockers += "Deletion of ${entry.entryId} is UI-only: hiding a record in ${entry.store} is not erasure."

                ErasureMethod.NONE ->
                    blockers += "Deletion of ${entry.entryId} executed no erasure."

                ErasureMethod.ANONYMIZE ->
                    if (entry.category !in anonymizableCategories) {
                        blockers += "Anonymisation is not erasure for ${entry.category}; " +
                            "${entry.entryId} must be hard deleted or crypto shredded."
                    }

                ErasureMethod.HARD_DELETE, ErasureMethod.CRYPTO_SHRED -> Unit
            }

            if (execution.receiptRef.isNullOrBlank()) {
                blockers += "Erasure of ${entry.entryId} produced no execution receipt."
            }
            val executedAt = execution.executedAtIsoDate.isoDateKey()
            if (executedAt == null) {
                blockers += "Erasure of ${entry.entryId} has an invalid executedAtIsoDate."
            } else if (requestedAt != null && executedAt < requestedAt) {
                blockers += "Erasure of ${entry.entryId} is dated before the deletion request."
            }

            if (blockers.size == before) erased += entry.entryId
        }

        val distinct = blockers.distinct()
        val outcome = when {
            !request.confirmedByAccountHolder -> DeletionOutcome.REJECTED
            distinct.isNotEmpty() -> DeletionOutcome.INCOMPLETE
            retained.isNotEmpty() -> DeletionOutcome.COMPLETE_WITH_RETAINED_RECORDS
            else -> DeletionOutcome.COMPLETE
        }
        return AccountDeletionResult(outcome, erased, retained.sorted(), distinct)
    }
}

@Serializable
data class AccountExportRequest(
    val requestId: String,
    val accountId: String,
    val generatedAtIsoDate: String,
    val includedEntryIds: List<String>,
) {
    init { require(requestId.isNotBlank() && accountId.isNotBlank()) }
}

@Serializable
data class AccountExportResult(
    val complete: Boolean,
    val missingEntryIds: List<String>,
    val blockers: List<String>,
)

object AccountExportValidator {
    fun validate(
        request: AccountExportRequest,
        inventory: List<PrivacyInventoryEntry>,
    ): AccountExportResult {
        val blockers = mutableListOf<String>()
        if (request.generatedAtIsoDate.isoDateKey() == null) {
            blockers += "generatedAtIsoDate must be a valid YYYY-MM-DD date."
        }

        val included = request.includedEntryIds.toSet()
        if (included.size != request.includedEntryIds.size) {
            blockers += "Account export lists the same record more than once."
        }

        val byId = inventory.associateBy { it.entryId }
        val expected = inventory
            .filter { it.exportTreatment != ExportTreatment.EXCLUDED }
            .map { it.entryId }
            .toSet()
        val missing = (expected - included).sorted()
        missing.forEach { blockers += "Account export omits $it." }

        included.sorted().forEach { entryId ->
            val entry = byId[entryId]
            if (entry == null) {
                blockers += "Account export includes unknown record $entryId."
            } else if (entry.exportTreatment == ExportTreatment.EXCLUDED) {
                blockers += "Account export includes $entryId, which is marked EXCLUDED."
            }
        }

        val distinct = blockers.distinct()
        return AccountExportResult(distinct.isEmpty(), missing, distinct)
    }
}

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
