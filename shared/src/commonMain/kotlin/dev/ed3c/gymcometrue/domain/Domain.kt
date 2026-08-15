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
        pattern = """(?im)^\s*([^\d\n:：]{2,70}?)\s*[:：]?\s*(\d+(?:[.,]\d+)?)\s*(mcg|µg|μg|mg|g|iu)\b""",
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
enum class ProtocolCategory {
    MEAL,
    SUPPLEMENT_CHECKPOINT,
    TRAINING,
    HYDRATION,
    RECOVERY,
    SLEEP,
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

    val sortKey: Int = dayOffset * 24 * 60 + hour * 60 + minute

    fun display(): String = buildString {
        if (dayOffset > 0) append("+").append(dayOffset).append("d ")
        append(hour.toString().padStart(2, '0'))
        append(":")
        append(minute.toString().padStart(2, '0'))
    }
}

@Serializable
data class ProtocolEvent(
    val id: String,
    val time: ProtocolTime,
    val title: String,
    val category: ProtocolCategory,
    val note: String,
    val requiresConfirmation: Boolean = false,
)

object DailyProtocolCompiler {
    fun compile(variant: TrainingVariant): List<ProtocolEvent> = when (variant) {
        TrainingVariant.AFTERNOON_1600 -> afternoonPlan()
        TrainingVariant.NIGHT_2200 -> nightPlan()
    }.sortedBy { it.time.sortKey }

    private fun sharedMorning(): List<ProtocolEvent> = listOf(
        ProtocolEvent(
            id = "morning-meal",
            time = ProtocolTime(8, 0),
            title = "Morning nutrition base",
            category = ProtocolCategory.MEAL,
            note = "Oats, unsweetened soy milk, nuts, and other foods are logged as foods—not treatment claims.",
        ),
        ProtocolEvent(
            id = "morning-evidence",
            time = ProtocolTime(8, 15),
            title = "Confirm supplement label evidence",
            category = ProtocolCategory.SUPPLEMENT_CHECKPOINT,
            note = "Review product, serving size, unit, duplicate ingredients, medication context, and expiry. No automatic dose change.",
            requiresConfirmation = true,
        ),
        ProtocolEvent(
            id = "hydration-1",
            time = ProtocolTime(10, 30),
            title = "Hydration checkpoint",
            category = ProtocolCategory.HYDRATION,
            note = "Use a personal hydration target reviewed for climate, activity, and health conditions.",
        ),
        ProtocolEvent(
            id = "lunch",
            time = ProtocolTime(12, 0),
            title = "Lunch",
            category = ProtocolCategory.MEAL,
            note = "A protein source, carbohydrate source, vegetables, and a tolerated dressing. Record symptoms rather than hiding them.",
        ),
    )

    private fun sharedDinner(): List<ProtocolEvent> = listOf(
        ProtocolEvent(
            id = "dinner",
            time = ProtocolTime(19, 15),
            title = "Dinner and recovery meal",
            category = ProtocolCategory.MEAL,
            note = "Prioritize adequate energy and protein. Mineral or supplement timing remains an unverified user protocol until reviewed.",
        ),
        ProtocolEvent(
            id = "evening-safety",
            time = ProtocolTime(20, 0),
            title = "Evening safety check",
            category = ProtocolCategory.SUPPLEMENT_CHECKPOINT,
            note = "Pause automation when medication, unusual symptoms, a procedure, or conflicting labels are present.",
            requiresConfirmation = true,
        ),
    )

    private fun afternoonPlan(): List<ProtocolEvent> = sharedMorning() + listOf(
        ProtocolEvent(
            id = "a-pre-workout",
            time = ProtocolTime(15, 30),
            title = "Pre-workout readiness",
            category = ProtocolCategory.MEAL,
            note = "Use a tolerated light snack when needed; check hydration and training readiness.",
        ),
        ProtocolEvent(
            id = "a-training",
            time = ProtocolTime(16, 0),
            title = "Strength training",
            category = ProtocolCategory.TRAINING,
            note = "Run today's progressive plan with technique and pain stop-rules.",
        ),
        ProtocolEvent(
            id = "a-post-workout",
            time = ProtocolTime(17, 30),
            title = "Post-workout recovery",
            category = ProtocolCategory.RECOVERY,
            note = "Log food, fluid, effort, and recovery. Supplements remain evidence entries, not automatic prescriptions.",
        ),
    ) + sharedDinner() + listOf(
        ProtocolEvent(
            id = "a-sleep",
            time = ProtocolTime(23, 30),
            title = "Sleep preparation",
            category = ProtocolCategory.SLEEP,
            note = "Reduce stimulation, review tomorrow's plan, and record any adverse response.",
        ),
    )

    private fun nightPlan(): List<ProtocolEvent> = sharedMorning() + listOf(
        ProtocolEvent(
            id = "b-afternoon-snack",
            time = ProtocolTime(16, 0),
            title = "Afternoon snack",
            category = ProtocolCategory.MEAL,
            note = "Choose a familiar food that supports the later session without replacing dinner.",
        ),
    ) + sharedDinner() + listOf(
        ProtocolEvent(
            id = "b-pre-workout",
            time = ProtocolTime(21, 30),
            title = "Late-session readiness",
            category = ProtocolCategory.RECOVERY,
            note = "Check fatigue, hydration, stimulant exposure, and whether a late workout will impair sleep.",
            requiresConfirmation = true,
        ),
        ProtocolEvent(
            id = "b-training",
            time = ProtocolTime(22, 0),
            title = "Strength training",
            category = ProtocolCategory.TRAINING,
            note = "Use the lower-noise late-session plan and stop for pain, dizziness, or unusual symptoms.",
        ),
        ProtocolEvent(
            id = "b-post-workout",
            time = ProtocolTime(23, 30),
            title = "Late recovery meal",
            category = ProtocolCategory.MEAL,
            note = "Use a tolerated recovery meal; avoid turning OCR output into an automatic dose instruction.",
        ),
        ProtocolEvent(
            id = "b-sleep",
            time = ProtocolTime(0, 15, dayOffset = 1),
            title = "Sleep transition",
            category = ProtocolCategory.SLEEP,
            note = "The compiler preserves next-day ordering rather than sorting 00:15 before the 22:00 session.",
        ),
    )
}

@Serializable
data class MuscleActivation(
    val muscle: String,
    val intensity: Int,
) {
    init {
        require(intensity in 0..10)
    }
}

@Serializable
data class ExplanationPayload(
    val evidence: ScanEvidence,
    val evaluation: SafetyEvaluation,
    val purpose: String = "Explain deterministic results in plain language",
    val mayRecommendDose: Boolean = false,
    val mayOverrideWarnings: Boolean = false,
    val instructions: List<String> = listOf(
        "Do not infer missing ingredients or serving sizes.",
        "Do not calculate or recommend a dose.",
        "Do not diagnose, treat, or claim medical safety.",
        "Repeat blocking reasons and direct the user to qualified review when required.",
    ),
)

object LlmExplanationBoundary {
    fun createPayload(
        evidence: ScanEvidence,
        evaluation: SafetyEvaluation,
    ): ExplanationPayload = ExplanationPayload(
        evidence = evidence.copy(rawTextSha256 = evidence.rawTextSha256.take(64)),
        evaluation = evaluation,
    )
}
