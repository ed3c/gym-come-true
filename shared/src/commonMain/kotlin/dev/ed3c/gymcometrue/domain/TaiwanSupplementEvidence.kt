package dev.ed3c.gymcometrue.domain

import kotlinx.serialization.Serializable

private val sha256Pattern = Regex("^[0-9a-f]{64}$")
private val isoDatePattern = Regex("^\\d{4}-\\d{2}-\\d{2}$")

@Serializable
enum class LabelCorpusConsent {
    SYNTHETIC,
    EXPLICIT_OPT_IN,
    WITHDRAWN,
    UNKNOWN,
}

@Serializable
enum class LabelRetentionPolicy {
    DO_NOT_STORE_IMAGE,
    ENCRYPTED_WITH_EXPIRY,
}

@Serializable
data class ServingDefinition(
    val amount: Double?,
    val unit: String?,
    val servingsPerContainer: Double? = null,
) {
    init {
        require(amount == null || amount > 0.0)
        require(servingsPerContainer == null || servingsPerContainer > 0.0)
    }
}

@Serializable
data class ProductVariantIdentity(
    val productId: String,
    val productName: String,
    val brand: String? = null,
    val barcode: String? = null,
    val market: String = "TW",
    val formulation: String,
    val labelRevision: String,
    val labelEffectiveDate: String? = null,
    val serving: ServingDefinition,
) {
    init {
        require(productId.isNotBlank())
        require(productName.isNotBlank())
        require(market.isNotBlank())
        require(formulation.isNotBlank())
        require(labelRevision.isNotBlank())
    }

    val variantKey: String
        get() = listOf(
            market.trim().uppercase(),
            barcode?.trim().orEmpty(),
            productId.trim(),
            formulation.trim().lowercase(),
            labelRevision.trim(),
        ).joinToString("|")
}

@Serializable
data class LabelCorpusRecord(
    val recordId: String,
    val product: ProductVariantIdentity,
    val rawTextSha256: String,
    val imageSha256: String? = null,
    val consent: LabelCorpusConsent,
    val retentionPolicy: LabelRetentionPolicy,
    val storesRawImage: Boolean = false,
    val encryptedAtRest: Boolean = false,
    val deleteAfterIsoDate: String? = null,
    val provenanceNote: String,
) {
    init {
        require(recordId.isNotBlank())
        require(provenanceNote.isNotBlank())
    }
}

@Serializable
enum class EvidenceAdmission {
    ALLOW,
    REVIEW,
    DENY,
}

@Serializable
data class CorpusAdmissionResult(
    val admission: EvidenceAdmission,
    val reasons: List<String>,
)

object LabelCorpusAdmissionValidator {
    fun validate(record: LabelCorpusRecord): CorpusAdmissionResult {
        val blockers = mutableListOf<String>()
        val reviewNotes = mutableListOf<String>()

        if (!record.rawTextSha256.isSha256()) {
            blockers += "Raw label text requires a lowercase SHA-256 evidence identifier."
        }
        when (record.consent) {
            LabelCorpusConsent.WITHDRAWN -> blockers += "Consent was withdrawn."
            LabelCorpusConsent.UNKNOWN -> blockers += "Consent is unknown."
            LabelCorpusConsent.SYNTHETIC -> Unit
            LabelCorpusConsent.EXPLICIT_OPT_IN -> reviewNotes += "Verify the retained consent receipt before corpus admission."
        }

        if (record.storesRawImage) {
            if (record.retentionPolicy != LabelRetentionPolicy.ENCRYPTED_WITH_EXPIRY) {
                blockers += "A stored raw image requires encrypted retention with an expiry."
            }
            if (!record.encryptedAtRest) {
                blockers += "A stored raw image must be encrypted at rest."
            }
            if (record.imageSha256?.isSha256() != true) {
                blockers += "A stored raw image requires a SHA-256 identifier."
            }
            if (!record.deleteAfterIsoDate.isIsoDate()) {
                blockers += "A stored raw image requires an ISO deletion date."
            }
        } else {
            if (record.retentionPolicy != LabelRetentionPolicy.DO_NOT_STORE_IMAGE) {
                reviewNotes += "No image is stored; prefer DO_NOT_STORE_IMAGE for an unambiguous lifecycle."
            }
            if (record.imageSha256 != null) {
                reviewNotes += "An image hash is present even though the raw image is not stored."
            }
        }

        if (record.product.labelEffectiveDate != null && !record.product.labelEffectiveDate.isIsoDate()) {
            blockers += "Product label effective date must use YYYY-MM-DD."
        }
        if (record.product.serving.amount == null || record.product.serving.unit.isNullOrBlank()) {
            reviewNotes += "Serving size is unresolved; the record may train OCR but cannot drive intake arithmetic."
        }

        return when {
            blockers.isNotEmpty() -> CorpusAdmissionResult(EvidenceAdmission.DENY, blockers + reviewNotes)
            reviewNotes.isNotEmpty() -> CorpusAdmissionResult(EvidenceAdmission.REVIEW, reviewNotes)
            else -> CorpusAdmissionResult(EvidenceAdmission.ALLOW, emptyList())
        }
    }
}

@Serializable
enum class OcrFieldType {
    PRODUCT_NAME,
    BRAND,
    BARCODE,
    INGREDIENT_NAME,
    AMOUNT,
    UNIT,
    SERVING_SIZE,
    WARNING,
    EXPIRY,
}

@Serializable
data class OcrFieldObservation(
    val fieldType: OcrFieldType,
    val expected: String,
    val observed: String? = null,
    val corrected: String? = null,
)

@Serializable
data class OcrFieldMetric(
    val fieldType: OcrFieldType,
    val total: Int,
    val exactMatches: Int,
    val correctionsRequired: Int,
    val correctionsCompleted: Int,
    val unresolved: Int,
    val exactAccuracy: Double,
    val correctionCompletion: Double?,
)

object OcrMetricCompiler {
    fun summarize(observations: List<OcrFieldObservation>): List<OcrFieldMetric> =
        observations.groupBy { it.fieldType }
            .toSortedMap(compareBy { it.ordinal })
            .map { (fieldType, rows) ->
                val exact = rows.count { it.observed.normalizedOcrValue() == it.expected.normalizedOcrValue() }
                val correctionNeeded = rows.size - exact
                val corrected = rows.count { row ->
                    row.observed.normalizedOcrValue() != row.expected.normalizedOcrValue() &&
                        row.corrected.normalizedOcrValue() == row.expected.normalizedOcrValue()
                }
                OcrFieldMetric(
                    fieldType = fieldType,
                    total = rows.size,
                    exactMatches = exact,
                    correctionsRequired = correctionNeeded,
                    correctionsCompleted = corrected,
                    unresolved = correctionNeeded - corrected,
                    exactAccuracy = if (rows.isEmpty()) 0.0 else exact.toDouble() / rows.size,
                    correctionCompletion = if (correctionNeeded == 0) null else corrected.toDouble() / correctionNeeded,
                )
            }
}

@Serializable
enum class PrimarySourceType {
    LAW_OR_REGULATION,
    REGULATOR_DATASET,
    REGULATOR_GUIDANCE,
    PRODUCT_LABEL,
    QUALIFIED_REVIEW,
}

@Serializable
data class PrimarySourceSnapshot(
    val sourceId: String,
    val sourceType: PrimarySourceType,
    val publisher: String,
    val title: String,
    val jurisdiction: String = "TW",
    val canonicalUrl: String,
    val archivedUrl: String? = null,
    val snapshotSha256: String? = null,
    val publishedDate: String? = null,
    val effectiveFrom: String? = null,
    val effectiveUntil: String? = null,
    val retrievedAt: String,
    val licenseOrTerms: String,
) {
    init {
        require(sourceId.isNotBlank())
        require(publisher.isNotBlank())
        require(title.isNotBlank())
        require(canonicalUrl.isNotBlank())
        require(retrievedAt.isNotBlank())
        require(licenseOrTerms.isNotBlank())
    }
}

@Serializable
enum class RuleConditionKind {
    IU_REQUIRES_INGREDIENT_SPECIFIC_RULE,
    MISSING_SERVING_SIZE,
    DUPLICATE_INGREDIENT,
    PROPRIETARY_BLEND,
    MEDICATION_CONTEXT,
    ADVERSE_SYMPTOM,
    SOURCE_CONFLICT,
}

@Serializable
enum class DeterministicRuleEffect {
    LOG_ONLY,
    REVIEW_REQUIRED,
    BLOCK_AUTOMATION,
}

@Serializable
data class DeterministicSupplementRule(
    val ruleId: String,
    val ingredientKey: String? = null,
    val condition: RuleConditionKind,
    val effect: DeterministicRuleEffect,
    val priority: Int,
    val sourceIds: List<String>,
    val userMessageKey: String,
) {
    init {
        require(ruleId.isNotBlank())
        require(priority >= 0)
        require(sourceIds.isNotEmpty())
        require(userMessageKey.isNotBlank())
    }

    internal fun conflictKey(): String = listOf(
        ingredientKey?.normalizedIngredientKey().orEmpty().ifBlank { "*" },
        condition.name,
        priority.toString(),
    ).joinToString("|")
}

@Serializable
data class ReviewerAttestation(
    val reviewerPseudonymousId: String,
    val qualification: String,
    val scope: String,
    val signedAtIsoDate: String,
    val signatureSha256: String,
    val userFacingWordingSha256: String,
    val reviewedRuleIds: Set<String>,
) {
    init {
        require(reviewerPseudonymousId.isNotBlank())
        require(qualification.isNotBlank())
        require(scope.isNotBlank())
    }
}

@Serializable
data class TaiwanRulePackManifest(
    val packId: String,
    val version: String,
    val jurisdiction: String = "TW",
    val status: RulePackStatus = RulePackStatus.DRAFT,
    val generatedAtIsoDate: String,
    val effectiveFrom: String? = null,
    val effectiveUntil: String? = null,
    val contentSha256: String? = null,
    val sources: List<PrimarySourceSnapshot> = emptyList(),
    val rules: List<DeterministicSupplementRule> = emptyList(),
    val testCaseIds: Set<String> = emptySet(),
    val reviewerAttestation: ReviewerAttestation? = null,
    val rollbackToVersion: String? = null,
) {
    init {
        require(packId.isNotBlank())
        require(version.isNotBlank())
    }
}

@Serializable
enum class RulePackAdmission {
    REJECTED,
    REVIEW_REQUIRED,
    ADMITTED,
}

@Serializable
data class RulePackValidationResult(
    val admission: RulePackAdmission,
    val blockers: List<String>,
    val reviewNotes: List<String>,
)

object TaiwanRulePackAdmissionValidator {
    val requiredSafetyCaseIds: Set<String> = setOf(
        "iu-unresolved",
        "missing-serving",
        "duplicate-ingredient",
        "proprietary-blend",
        "medication-context",
        "adverse-symptom",
        "source-conflict",
    )

    fun validate(
        pack: TaiwanRulePackManifest,
        asOfIsoDate: String,
    ): RulePackValidationResult {
        val blockers = mutableListOf<String>()
        val reviewNotes = mutableListOf<String>()
        val reviewed = pack.status == RulePackStatus.CLINICALLY_REVIEWED

        if (pack.jurisdiction != "TW") blockers += "The Taiwan rule pack jurisdiction must be TW."
        val generatedDate = pack.generatedAtIsoDate.isoDateKey()
        val asOfDate = asOfIsoDate.isoDateKey()
        val effectiveFrom = pack.effectiveFrom?.isoDateKey()
        val effectiveUntil = pack.effectiveUntil?.isoDateKey()
        if (generatedDate == null) blockers += "generatedAtIsoDate must be a valid YYYY-MM-DD date."
        if (asOfDate == null) blockers += "The validation date must be a valid YYYY-MM-DD date."
        if (pack.effectiveFrom != null && effectiveFrom == null) blockers += "effectiveFrom must be a valid YYYY-MM-DD date."
        if (pack.effectiveUntil != null && effectiveUntil == null) blockers += "effectiveUntil must be a valid YYYY-MM-DD date."
        if (effectiveFrom != null && effectiveUntil != null && effectiveFrom > effectiveUntil) {
            blockers += "effectiveFrom cannot be later than effectiveUntil."
        }
        if (reviewed && asOfDate != null && effectiveFrom != null && asOfDate < effectiveFrom) {
            blockers += "The reviewed pack is not effective yet."
        }
        if (reviewed && asOfDate != null && effectiveUntil != null && asOfDate > effectiveUntil) {
            blockers += "The reviewed pack is expired."
        }

        val duplicateSourceIds = pack.sources.groupingBy { it.sourceId }.eachCount().filterValues { it > 1 }.keys
        if (duplicateSourceIds.isNotEmpty()) blockers += "Duplicate source IDs: ${duplicateSourceIds.sorted()}."
        val duplicateRuleIds = pack.rules.groupingBy { it.ruleId }.eachCount().filterValues { it > 1 }.keys
        if (duplicateRuleIds.isNotEmpty()) blockers += "Duplicate rule IDs: ${duplicateRuleIds.sorted()}."

        val sourceIds = pack.sources.map { it.sourceId }.toSet()
        pack.rules.forEach { rule ->
            val missing = rule.sourceIds.filterNot(sourceIds::contains)
            if (missing.isNotEmpty()) blockers += "Rule ${rule.ruleId} references missing sources: ${missing.sorted()}."
        }

        val conflicts = pack.rules.groupBy { it.conflictKey() }
            .filterValues { group -> group.map { it.effect }.distinct().size > 1 }
            .keys
        if (conflicts.isNotEmpty()) blockers += "Conflicting deterministic rules: ${conflicts.sorted()}."

        pack.sources.forEach { source ->
            if (source.jurisdiction != "TW") blockers += "Source ${source.sourceId} is outside the TW jurisdiction."
            if (!source.retrievedAt.isIsoDate()) blockers += "Source ${source.sourceId} has an invalid retrieval date."
            if (source.publishedDate != null && !source.publishedDate.isIsoDate()) blockers += "Source ${source.sourceId} has an invalid published date."
            if (source.effectiveFrom != null && !source.effectiveFrom.isIsoDate()) blockers += "Source ${source.sourceId} has an invalid effective date."
            if (source.effectiveUntil != null && !source.effectiveUntil.isIsoDate()) blockers += "Source ${source.sourceId} has an invalid expiry date."
            if (reviewed && source.snapshotSha256?.isSha256() != true) {
                blockers += "Reviewed source ${source.sourceId} requires an archived snapshot SHA-256."
            } else if (!reviewed && source.snapshotSha256 == null) {
                reviewNotes += "Draft source ${source.sourceId} still needs an archived snapshot hash."
            }
        }

        when (pack.status) {
            RulePackStatus.MISSING -> blockers += "A missing rule pack cannot be admitted."
            RulePackStatus.DRAFT -> reviewNotes += "Draft rule packs can be inspected but never execute production decisions."
            RulePackStatus.CLINICALLY_REVIEWED -> {
                if (pack.contentSha256?.isSha256() != true) blockers += "A reviewed pack requires its own content SHA-256."
                if (pack.sources.isEmpty()) blockers += "A reviewed pack requires primary sources."
                if (pack.rules.isEmpty()) blockers += "A reviewed pack requires deterministic rules."
                val missingCases = requiredSafetyCaseIds - pack.testCaseIds
                if (missingCases.isNotEmpty()) blockers += "Missing required safety cases: ${missingCases.sorted()}."
                if (pack.rollbackToVersion.isNullOrBlank() || pack.rollbackToVersion == pack.version) {
                    blockers += "A reviewed pack requires a distinct rollback version."
                }
                validateAttestation(pack, blockers)
            }
        }

        val admission = when {
            blockers.isNotEmpty() -> RulePackAdmission.REJECTED
            reviewed -> RulePackAdmission.ADMITTED
            else -> RulePackAdmission.REVIEW_REQUIRED
        }
        return RulePackValidationResult(
            admission = admission,
            blockers = blockers.distinct(),
            reviewNotes = reviewNotes.distinct(),
        )
    }

    private fun validateAttestation(
        pack: TaiwanRulePackManifest,
        blockers: MutableList<String>,
    ) {
        val attestation = pack.reviewerAttestation
        if (attestation == null) {
            blockers += "A reviewed pack requires a qualified reviewer attestation."
            return
        }
        if (!attestation.signedAtIsoDate.isIsoDate()) blockers += "Reviewer signature date must use YYYY-MM-DD."
        if (!attestation.signatureSha256.isSha256()) blockers += "Reviewer attestation requires a signature SHA-256."
        if (!attestation.userFacingWordingSha256.isSha256()) blockers += "Reviewed user-facing wording requires a SHA-256."
        val missingReview = pack.rules.map { it.ruleId }.toSet() - attestation.reviewedRuleIds
        if (missingReview.isNotEmpty()) blockers += "Reviewer did not attest rules: ${missingReview.sorted()}."
    }
}

@Serializable
data class SupplementDecisionReceipt(
    val receiptId: String,
    val productVariantKey: String,
    val evidenceSha256: String,
    val confirmedAtIsoDate: String,
    val rulePackId: String,
    val rulePackVersion: String,
    val rulePackContentSha256: String,
    val deterministicDecision: SafetyDecision,
    val triggeredRuleIds: List<String>,
    val reasons: List<String>,
    val modelUsedForDecision: Boolean = false,
) {
    init {
        require(receiptId.isNotBlank())
        require(productVariantKey.isNotBlank())
        require(evidenceSha256.isSha256())
        require(confirmedAtIsoDate.isIsoDate())
        require(rulePackId.isNotBlank())
        require(rulePackVersion.isNotBlank())
        require(rulePackContentSha256.isSha256())
        require(!modelUsedForDecision) { "A model may explain a receipt but cannot own the decision." }
    }
}

object SupplementDecisionReceiptFactory {
    fun create(
        receiptId: String,
        product: ProductVariantIdentity,
        evidence: ScanEvidence,
        evaluation: SafetyEvaluation,
        rulePack: TaiwanRulePackManifest,
        triggeredRuleIds: List<String>,
        confirmedAtIsoDate: String,
    ): SupplementDecisionReceipt {
        val validation = TaiwanRulePackAdmissionValidator.validate(rulePack, confirmedAtIsoDate)
        require(validation.admission == RulePackAdmission.ADMITTED) {
            "Decision receipts require an admitted reviewed rule pack: ${validation.blockers.joinToString()}"
        }
        return SupplementDecisionReceipt(
            receiptId = receiptId,
            productVariantKey = product.variantKey,
            evidenceSha256 = evidence.rawTextSha256,
            confirmedAtIsoDate = confirmedAtIsoDate,
            rulePackId = rulePack.packId,
            rulePackVersion = rulePack.version,
            rulePackContentSha256 = requireNotNull(rulePack.contentSha256),
            deterministicDecision = evaluation.decision,
            triggeredRuleIds = triggeredRuleIds.distinct().sorted(),
            reasons = evaluation.reasons.distinct(),
        )
    }
}

private fun String?.isIsoDate(): Boolean = this?.isoDateKey() != null

private fun String.isoDateKey(): Int? {
    if (!isoDatePattern.matches(this)) return null
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

private fun String.isSha256(): Boolean = sha256Pattern.matches(this)
private fun String?.normalizedOcrValue(): String = this.orEmpty()
    .trim()
    .lowercase()
    .replace(Regex("[\\s,，:：;；()（）]+"), "")
