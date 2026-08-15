package dev.ed3c.gymcometrue.domain

import kotlinx.serialization.Serializable

@Serializable
enum class EvidenceStatus {
    UNVERIFIED,
    USER_CONFIRMED,
    VERIFIED_BY_REVIEWED_SOURCE,
}

@Serializable
enum class MassUnit {
    MCG,
    MG,
    G,
    IU,
    UNKNOWN,
}

@Serializable
data class SupplementFactCandidate(
    val ingredient: String,
    val amount: Double,
    val unit: MassUnit,
    val rawUnit: String,
    val evidenceStatus: EvidenceStatus = EvidenceStatus.UNVERIFIED,
)

@Serializable
data class ScanEvidence(
    val rawTextSha256: String,
    val barcode: String? = null,
    val candidates: List<SupplementFactCandidate>,
    val evidenceStatus: EvidenceStatus = EvidenceStatus.UNVERIFIED,
    val warnings: List<String> = emptyList(),
)

object SupplementLabelParser {
    private val factLine = Regex(
        pattern = """(?im)^\s*([^\n:：]{2,70}?)\s*[:：]?\s*(\d+(?:[.,]\d+)?)\s*(mcg|µg|μg|mg|g|iu)\b""",
    )

    fun parse(rawText: String): List<SupplementFactCandidate> = factLine.findAll(rawText)
        .mapNotNull { match ->
            val name = match.groupValues[1]
                .replace(Regex("\\s+"), " ")
                .trim(' ', '-', '•', '*')
            val amount = match.groupValues[2].replace(',', '.').toDoubleOrNull()
            val rawUnit = match.groupValues[3]
            if (name.length < 2 || amount == null || amount <= 0.0) {
                null
            } else {
                SupplementFactCandidate(
                    ingredient = name,
                    amount = amount,
                    unit = rawUnit.toMassUnit(),
                    rawUnit = rawUnit,
                )
            }
        }
        .distinctBy { "${it.ingredient.lowercase()}|${it.amount}|${it.unit}" }
        .toList()

    private fun String.toMassUnit(): MassUnit = when (lowercase()) {
        "mcg", "µg", "μg" -> MassUnit.MCG
        "mg" -> MassUnit.MG
        "g" -> MassUnit.G
        "iu" -> MassUnit.IU
        else -> MassUnit.UNKNOWN
    }
}

object MassUnitConverter {
    /**
     * Converts only dimensions that are generically equivalent by mass.
     * IU is biological activity and intentionally has no generic conversion.
     */
    fun toMilligrams(amount: Double, unit: MassUnit): Double? = when (unit) {
        MassUnit.MCG -> amount / 1_000.0
        MassUnit.MG -> amount
        MassUnit.G -> amount * 1_000.0
        MassUnit.IU,
        MassUnit.UNKNOWN,
        -> null
    }
}

@Serializable
enum class RulePackStatus {
    MISSING,
    DRAFT,
    CLINICALLY_REVIEWED,
}

@Serializable
data class SafetyContext(
    val medicationUsedWithin72Hours: Boolean = false,
    val adverseSymptomsPresent: Boolean = false,
    val pregnantOrBreastfeeding: Boolean = false,
    val surgeryOrProcedurePlanned: Boolean = false,
    val rulePackStatus: RulePackStatus = RulePackStatus.MISSING,
)

@Serializable
enum class SafetyDecision {
    LOG_ONLY,
    REVIEW_REQUIRED,
    BLOCK_AUTOMATION,
}

@Serializable
data class SafetyEvaluation(
    val decision: SafetyDecision,
    val reasons: List<String>,
    val normalizedMassMg: Map<String, Double> = emptyMap(),
)

object SupplementSafetyEngine {
    fun evaluate(
        evidence: ScanEvidence,
        context: SafetyContext,
    ): SafetyEvaluation {
        val reasons = mutableListOf<String>()

        if (evidence.candidates.isEmpty()) {
            reasons += "No ingredient-and-amount pair was confirmed from the label."
        }
        if (context.adverseSymptomsPresent) {
            reasons += "Adverse symptoms require immediate human assessment; automation is blocked."
        }
        if (context.pregnantOrBreastfeeding) {
            reasons += "Pregnancy or breastfeeding requires qualified professional review."
        }
        if (context.surgeryOrProcedurePlanned) {
            reasons += "A planned procedure can change supplement risk and requires professional review."
        }
        if (context.medicationUsedWithin72Hours) {
            reasons += "Recent medication use can create interactions that this generic engine cannot assess."
        }
        if (evidence.evidenceStatus != EvidenceStatus.VERIFIED_BY_REVIEWED_SOURCE ||
            evidence.candidates.any { it.evidenceStatus != EvidenceStatus.VERIFIED_BY_REVIEWED_SOURCE }
        ) {
            reasons += "OCR and user-entered label facts remain unverified evidence."
        }
        if (evidence.candidates.any { it.unit == MassUnit.IU || it.unit == MassUnit.UNKNOWN }) {
            reasons += "IU or unknown units cannot use a generic mass conversion."
        }
        if (context.rulePackStatus != RulePackStatus.CLINICALLY_REVIEWED) {
            reasons += "No clinically reviewed regional rule pack is active."
        }

        val hardBlock = context.adverseSymptomsPresent ||
            context.pregnantOrBreastfeeding ||
            context.surgeryOrProcedurePlanned ||
            context.medicationUsedWithin72Hours ||
            evidence.candidates.any { it.unit == MassUnit.IU || it.unit == MassUnit.UNKNOWN }

        val decision = when {
            hardBlock -> SafetyDecision.BLOCK_AUTOMATION
            reasons.isNotEmpty() -> SafetyDecision.REVIEW_REQUIRED
            else -> SafetyDecision.LOG_ONLY
        }

        val normalized = evidence.candidates.mapNotNull { candidate ->
            MassUnitConverter.toMilligrams(candidate.amount, candidate.unit)
                ?.let { candidate.ingredient to it }
        }.toMap()

        return SafetyEvaluation(
            decision = decision,
            reasons = reasons.distinct(),
            normalizedMassMg = normalized,
        )
    }
}

@Serializable
enum class TrainingVariant {
    AFTERNOON_1600,
    NIGHT_2200,
}

@Serializable
data class ProtocolTime(
    val hour: Int,
    val minute: Int,
    val dayOffset: Int = 0,
) {
    init {
        require(hour in 0..23)
        require(minute in 0..59)
        require(dayOffset >= 0)
    }

    val sortKey: Int
        get() = dayOffset * 24 * 60 + hour * 60 + minute

    override fun toString(): String = buildString {
        if (dayOffset > 0) append("+").append(dayOffset).append("d ")
        append(hour.toString().padStart(2, '0'))
        append(":")
        append(minute.toString().padStart(2, '0'))
    }
}

@Serializable
enum class ProtocolEventKind {
    MEAL,
    SUPPLEMENT_LOG,
    WORKOUT,
    HYDRATION,
    RECOVERY,
    CHECKPOINT,
}

@Serializable
data class ProtocolEvent(
    val id: String,
    val time: ProtocolTime,
    val kind: ProtocolEventKind,
    val title: String,
    val detail: String,
    val reminderEligible: Boolean = true,
)

object DailyProtocolCompiler {
    fun compile(variant: TrainingVariant): List<ProtocolEvent> = when (variant) {
        TrainingVariant.AFTERNOON_1600 -> afternoonPlan()
        TrainingVariant.NIGHT_2200 -> nightPlan()
    }.sortedBy { it.time.sortKey }

    private fun sharedMorning() = listOf(
        ProtocolEvent(
            id = "breakfast",
            time = ProtocolTime(8, 0),
            kind = ProtocolEventKind.MEAL,
            title = "Breakfast / 早餐",
            detail = "Log the meal and only confirmed supplement facts. The app does not prescribe a dose.",
        ),
        ProtocolEvent(
            id = "lunch",
            time = ProtocolTime(12, 0),
            kind = ProtocolEventKind.MEAL,
            title = "Lunch / 午餐",
            detail = "Protein + carbohydrate + vegetables; confirm any supplement label before logging.",
        ),
        ProtocolEvent(
            id = "hydration",
            time = ProtocolTime(14, 0),
            kind = ProtocolEventKind.HYDRATION,
            title = "Hydration checkpoint / 補水",
            detail = "Use your own clinician- or coach-reviewed hydration target.",
        ),
    )

    private fun afternoonPlan() = sharedMorning() + listOf(
        ProtocolEvent(
            id = "a-preworkout",
            time = ProtocolTime(15, 30),
            kind = ProtocolEventKind.CHECKPOINT,
            title = "Pre-workout checkpoint / 訓練前確認",
            detail = "Confirm readiness and any label evidence; no automatic supplement recommendation.",
        ),
        ProtocolEvent(
            id = "a-training",
            time = ProtocolTime(16, 0),
            kind = ProtocolEventKind.WORKOUT,
            title = "Workout / 訓練",
            detail = "Follow the selected workout session and record completion.",
        ),
        ProtocolEvent(
            id = "a-recovery",
            time = ProtocolTime(17, 30),
            kind = ProtocolEventKind.RECOVERY,
            title = "Recovery / 恢復",
            detail = "Log recovery food and hydration without automated dose advice.",
        ),
        ProtocolEvent(
            id = "a-dinner",
            time = ProtocolTime(19, 30),
            kind = ProtocolEventKind.MEAL,
            title = "Dinner / 晚餐",
            detail = "Complete the meal and supplement log from confirmed evidence only.",
        ),
        ProtocolEvent(
            id = "a-sleep",
            time = ProtocolTime(23, 30),
            kind = ProtocolEventKind.RECOVERY,
            title = "Wind down / 睡前修復",
            detail = "Recovery reminder; do not treat it as medical advice.",
        ),
    )

    private fun nightPlan() = sharedMorning() + listOf(
        ProtocolEvent(
            id = "b-snack",
            time = ProtocolTime(16, 0),
            kind = ProtocolEventKind.MEAL,
            title = "Afternoon snack / 下午點心",
            detail = "Record the snack and hydration checkpoint.",
        ),
        ProtocolEvent(
            id = "b-dinner",
            time = ProtocolTime(19, 30),
            kind = ProtocolEventKind.MEAL,
            title = "Dinner / 晚餐",
            detail = "Finish the main meal before the late workout window.",
        ),
        ProtocolEvent(
            id = "b-preworkout",
            time = ProtocolTime(21, 30),
            kind = ProtocolEventKind.CHECKPOINT,
            title = "Pre-workout checkpoint / 訓練前確認",
            detail = "Confirm readiness; uncertain supplement evidence remains blocked from automation.",
        ),
        ProtocolEvent(
            id = "b-training",
            time = ProtocolTime(22, 0),
            kind = ProtocolEventKind.WORKOUT,
            title = "Workout / 訓練",
            detail = "Follow the selected workout session and record completion.",
        ),
        ProtocolEvent(
            id = "b-recovery",
            time = ProtocolTime(23, 30),
            kind = ProtocolEventKind.RECOVERY,
            title = "Post-workout recovery / 訓練後恢復",
            detail = "Log food and hydration. No dose is inferred from OCR or a model.",
        ),
        ProtocolEvent(
            id = "b-sleep",
            time = ProtocolTime(0, 30, dayOffset = 1),
            kind = ProtocolEventKind.RECOVERY,
            title = "Wind down / 睡前修復",
            detail = "Cross-midnight recovery checkpoint.",
        ),
    )
}

@Serializable
data class LlmExplanationPayload(
    val confirmedFacts: List<String>,
    val safetyDecision: SafetyDecision,
    val reasons: List<String>,
    val mayRecommendDose: Boolean = false,
    val mayOverrideWarnings: Boolean = false,
)

object LlmExplanationBoundary {
    fun createPayload(
        evidence: ScanEvidence,
        evaluation: SafetyEvaluation,
    ): LlmExplanationPayload = LlmExplanationPayload(
        confirmedFacts = evidence.candidates
            .filter { it.evidenceStatus == EvidenceStatus.VERIFIED_BY_REVIEWED_SOURCE }
            .map { "${it.ingredient}: ${it.amount} ${it.rawUnit}" },
        safetyDecision = evaluation.decision,
        reasons = evaluation.reasons,
    )
}