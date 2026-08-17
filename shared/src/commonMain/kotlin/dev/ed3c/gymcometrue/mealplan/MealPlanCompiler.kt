package dev.ed3c.gymcometrue.mealplan

import dev.ed3c.gymcometrue.domain.DailyProtocolCompiler
import dev.ed3c.gymcometrue.domain.ProtocolCategory
import dev.ed3c.gymcometrue.domain.ProtocolEvent
import dev.ed3c.gymcometrue.domain.ProtocolTime
import dev.ed3c.gymcometrue.domain.TrainingVariant
import dev.ed3c.gymcometrue.nutrition.FoodNutrientProfile
import dev.ed3c.gymcometrue.nutrition.FoodPortionScaler
import dev.ed3c.gymcometrue.nutrition.NutrientAmounts
import kotlinx.serialization.Serializable

/**
 * Issue #47 (N2) — deterministic daily meal-plan / timetable compiler.
 *
 * Meal slots and their A/B 16:00 / 22:00 workout-adjacent times are NOT reinvented here: they come
 * straight from the existing [DailyProtocolCompiler] (`MEAL` category events), which already carries
 * the cross-midnight `dayOffset` ordering for the night variant. This compiler only adds food
 * portions, deterministic nutrition arithmetic, and confirmed user overrides on top of that
 * timetable. Reminder/notification delivery is a separate system (see `androidApp`'s
 * `ProtocolReminder`); nothing here schedules a device notification.
 */

@Serializable
data class FoodPortion(
    val id: String,
    val foodId: String,
    val foodDisplayName: String,
    val grams: Double? = null,
    val profile: FoodNutrientProfile? = null,
    /** True only after the caller has run `FoodCatalogAdmissionValidator` and it did not reject. */
    val provenanceAdmitted: Boolean = false,
) {
    init {
        require(id.isNotBlank()) { "id cannot be blank." }
        require(foodId.isNotBlank()) { "foodId cannot be blank." }
        require(foodDisplayName.isNotBlank()) { "foodDisplayName cannot be blank." }
        require(grams == null || grams > 0.0) { "grams must be positive when known." }
    }
}

@Serializable
data class UnresolvedFoodPortion(
    val portionId: String,
    val foodId: String,
    val reason: String,
)

/**
 * A user-authored change to a compiled slot. Per the hard limit "no schedule mutation from
 * unconfirmed OCR/LLM output," [confirmedByUser] gates whether the override is ever applied — an
 * unconfirmed override is rejected, never partially merged.
 */
@Serializable
data class MealSlotOverride(
    val slotId: String,
    val time: ProtocolTime? = null,
    val addPortions: List<FoodPortion> = emptyList(),
    val removePortionIds: Set<String> = emptySet(),
    val confirmedByUser: Boolean = true,
)

@Serializable
data class RejectedOverride(val slotId: String, val reason: String)

@Serializable
data class MealSlot(
    val id: String,
    val time: ProtocolTime,
    val title: String,
    val portions: List<FoodPortion>,
    val resolvedTotals: NutrientAmounts,
    val unresolved: List<UnresolvedFoodPortion>,
    val duplicateFoodIds: Set<String>,
    val requiresConfirmation: Boolean,
    val userOverridden: Boolean,
)

@Serializable
data class DailyMealPlan(
    val variant: TrainingVariant,
    val slots: List<MealSlot>,
    val dailyTotals: NutrientAmounts,
    val rejectedOverrides: List<RejectedOverride>,
)

private class SlotBuildState(var time: ProtocolTime) {
    val portions: MutableList<FoodPortion> = mutableListOf()
    var userOverridden: Boolean = false
}

object MealPlanCompiler {
    fun compile(
        variant: TrainingVariant,
        overrides: List<MealSlotOverride> = emptyList(),
    ): DailyMealPlan {
        val mealEvents = DailyProtocolCompiler.compile(variant).filter { it.category == ProtocolCategory.MEAL }
        val states = mealEvents.associate { it.id to SlotBuildState(time = it.time) }
        val rejected = mutableListOf<RejectedOverride>()

        overrides.forEach { override ->
            val state = states[override.slotId]
            when {
                state == null -> rejected += RejectedOverride(override.slotId, "Unknown meal slot id.")
                !override.confirmedByUser ->
                    rejected += RejectedOverride(override.slotId, "Override was not confirmed by the user.")
                else -> {
                    if (override.removePortionIds.isNotEmpty()) {
                        state.portions.removeAll { it.id in override.removePortionIds }
                    }
                    state.portions += override.addPortions
                    if (override.time != null) state.time = override.time
                    state.userOverridden = true
                }
            }
        }

        val slots = mealEvents
            .map { event -> buildSlot(event, states.getValue(event.id)) }
            .sortedBy { it.time.sortKey }

        return DailyMealPlan(
            variant = variant,
            slots = slots,
            dailyTotals = NutrientAmounts.sumOf(slots.map { it.resolvedTotals }),
            rejectedOverrides = rejected,
        )
    }

    private fun buildSlot(event: ProtocolEvent, state: SlotBuildState): MealSlot {
        val resolved = mutableListOf<NutrientAmounts>()
        val unresolved = mutableListOf<UnresolvedFoodPortion>()

        state.portions.forEach { portion ->
            val grams = portion.grams
            val profile = portion.profile
            when {
                grams == null ->
                    unresolved += UnresolvedFoodPortion(portion.id, portion.foodId, "Missing serving size in grams.")
                profile == null ->
                    unresolved += UnresolvedFoodPortion(portion.id, portion.foodId, "No nutrient profile is attached to this food record.")
                !portion.provenanceAdmitted ->
                    unresolved += UnresolvedFoodPortion(portion.id, portion.foodId, "Food record provenance is not admitted.")
                else -> resolved += FoodPortionScaler.scale(profile, grams)
            }
        }

        val duplicateFoodIds = state.portions.groupingBy { it.foodId }.eachCount().filterValues { it > 1 }.keys

        return MealSlot(
            id = event.id,
            time = state.time,
            title = event.title,
            portions = state.portions.toList(),
            resolvedTotals = NutrientAmounts.sumOf(resolved),
            unresolved = unresolved,
            duplicateFoodIds = duplicateFoodIds,
            requiresConfirmation = event.requiresConfirmation || unresolved.isNotEmpty(),
            userOverridden = state.userOverridden,
        )
    }
}

@Serializable
data class DailyNutritionTarget(
    val maxEnergyKcal: Double? = null,
    val minProteinG: Double? = null,
) {
    init {
        require(maxEnergyKcal == null || maxEnergyKcal > 0.0) { "maxEnergyKcal must be positive when set." }
        require(minProteinG == null || minProteinG >= 0.0) { "minProteinG cannot be negative." }
    }
}

@Serializable
data class ConstraintEvaluation(
    val satisfiable: Boolean,
    val violations: List<String>,
)

/**
 * Pure arithmetic comparison against an explicit target. This is not a medical/therapeutic
 * assessment: it only reports whether the compiled plan's resolved totals meet the caller-supplied
 * numbers, and it refuses to claim satisfiability while food records remain unresolved.
 */
object MealPlanConstraintEvaluator {
    fun evaluate(plan: DailyMealPlan, target: DailyNutritionTarget): ConstraintEvaluation {
        val violations = mutableListOf<String>()

        if (target.maxEnergyKcal != null && plan.dailyTotals.energyKcal > target.maxEnergyKcal) {
            violations += "Daily energy ${plan.dailyTotals.energyKcal} kcal exceeds the target maximum of ${target.maxEnergyKcal} kcal."
        }
        if (target.minProteinG != null && plan.dailyTotals.proteinG < target.minProteinG) {
            violations += "Daily protein ${plan.dailyTotals.proteinG} g is below the target minimum of ${target.minProteinG} g."
        }
        if (plan.slots.any { it.unresolved.isNotEmpty() }) {
            violations += "Constraint evaluation is incomplete while unresolved food portions remain."
        }

        return ConstraintEvaluation(satisfiable = violations.isEmpty(), violations = violations)
    }
}

/**
 * LLM_EXPLANATION_ONLY boundary for a compiled plan, aligned with
 * `dev.ed3c.gymcometrue.explanation.ExplanationGatewayContract`. A model may narrate this
 * payload; it may not invent nutrient facts, medical restrictions, or personalized
 * therapeutic targets.
 */
@Serializable
data class MealPlanExplanationPayload(
    val plan: DailyMealPlan,
    val purpose: String = "Explain a deterministically compiled meal plan in plain language",
    val mayInventNutrientFacts: Boolean = false,
    val mayInventMedicalRestrictions: Boolean = false,
    val mayInventTherapeuticTargets: Boolean = false,
    val instructions: List<String> = listOf(
        "Do not invent nutrient facts beyond what the resolved food records report.",
        "Do not state or imply a personalized medical or therapeutic target.",
        "Repeat unresolved and rejected-override reasons instead of filling them in.",
        "Reminder scheduling is a separate system; this payload does not trigger a notification.",
    ),
)

object MealPlanExplanationBoundary {
    fun createPayload(plan: DailyMealPlan): MealPlanExplanationPayload = MealPlanExplanationPayload(plan = plan)
}
