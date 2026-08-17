package dev.ed3c.gymcometrue.mealplan

import dev.ed3c.gymcometrue.domain.MassUnit
import dev.ed3c.gymcometrue.domain.ProtocolTime
import dev.ed3c.gymcometrue.domain.TrainingVariant
import dev.ed3c.gymcometrue.nutrition.FoodNutrientProfile
import dev.ed3c.gymcometrue.nutrition.MicronutrientAmount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MealPlanCompilerTest {
    private fun profile(): FoodNutrientProfile = FoodNutrientProfile(
        energyKcalPer100g = 200.0,
        proteinGPer100g = 20.0,
        fatGPer100g = 5.0,
        carbohydrateGPer100g = 10.0,
        micronutrientsPer100g = listOf(
            MicronutrientAmount("sodium", 100.0, MassUnit.MG),
            MicronutrientAmount("vitamin_e", 5.0, MassUnit.IU),
        ),
    )

    private fun portion(id: String, foodId: String, grams: Double? = 150.0): FoodPortion = FoodPortion(
        id = id,
        foodId = foodId,
        foodDisplayName = foodId,
        grams = grams,
        profile = profile(),
        provenanceAdmitted = true,
    )

    @Test
    fun aVariantSlotsFollowTheExistingProtocolCompilerTimetable() {
        val plan = MealPlanCompiler.compile(TrainingVariant.AFTERNOON_1600)

        assertEquals(listOf("morning-meal", "lunch", "a-pre-workout", "dinner"), plan.slots.map { it.id })
        assertEquals(listOf(8 * 60, 12 * 60, 15 * 60 + 30, 19 * 60 + 15), plan.slots.map { it.time.sortKey })
    }

    @Test
    fun missingServingSizeLeavesPortionUnresolvedInsteadOfSilentlyDropped() {
        val plan = MealPlanCompiler.compile(
            TrainingVariant.AFTERNOON_1600,
            overrides = listOf(
                MealSlotOverride(
                    slotId = "lunch",
                    addPortions = listOf(portion("p1", "chicken-breast", grams = null)),
                ),
            ),
        )

        val lunch = plan.slots.single { it.id == "lunch" }
        assertEquals(1, lunch.unresolved.size)
        assertEquals("p1", lunch.unresolved.single().portionId)
        assertTrue(lunch.unresolved.single().reason.contains("serving size"))
        assertEquals(0.0, lunch.resolvedTotals.energyKcal)
        assertTrue(lunch.requiresConfirmation)
    }

    @Test
    fun unitMismatchExcludesOnlyThatMicronutrientButMacrosStillResolve() {
        val plan = MealPlanCompiler.compile(
            TrainingVariant.AFTERNOON_1600,
            overrides = listOf(MealSlotOverride(slotId = "lunch", addPortions = listOf(portion("p1", "chicken-breast")))),
        )

        val lunch = plan.slots.single { it.id == "lunch" }
        assertTrue(lunch.unresolved.isEmpty())
        assertEquals(300.0, lunch.resolvedTotals.energyKcal) // 200 kcal/100g * 1.5
        assertEquals(150.0, lunch.resolvedTotals.micronutrientsMg["sodium"]) // 100 mg/100g * 1.5
        assertNull(lunch.resolvedTotals.micronutrientsMg["vitamin_e"]) // IU has no generic mass conversion
    }

    @Test
    fun duplicateFoodsAreReportedAndStillSummedNotMerged() {
        val plan = MealPlanCompiler.compile(
            TrainingVariant.AFTERNOON_1600,
            overrides = listOf(
                MealSlotOverride(
                    slotId = "lunch",
                    addPortions = listOf(portion("p1", "rice"), portion("p2", "rice")),
                ),
            ),
        )

        val lunch = plan.slots.single { it.id == "lunch" }
        assertEquals(setOf("rice"), lunch.duplicateFoodIds)
        assertEquals(600.0, lunch.resolvedTotals.energyKcal) // both 300 kcal portions counted
    }

    @Test
    fun confirmedOverrideCanCrossMidnightAndSortsAfterTheLateSession() {
        val plan = MealPlanCompiler.compile(
            TrainingVariant.NIGHT_2200,
            overrides = listOf(
                MealSlotOverride(slotId = "b-post-workout", time = ProtocolTime(0, 30, dayOffset = 1)),
            ),
        )

        // b-post-workout (pushed past midnight) must sort after same-day 22:00 training and 19:15 dinner,
        // not before 08:00 breakfast because "00:30" looks earlier in clock time.
        assertEquals("b-post-workout", plan.slots.last().id)
        assertTrue(plan.slots.last().time.sortKey > plan.slots.first { it.id == "dinner" }.time.sortKey)
        assertEquals("+1d 00:30", plan.slots.last().time.display())
        assertTrue(plan.slots.last().userOverridden)
    }

    @Test
    fun unconfirmedOverrideIsRejectedAndNeverMutatesTheSchedule() {
        val plan = MealPlanCompiler.compile(
            TrainingVariant.AFTERNOON_1600,
            overrides = listOf(
                MealSlotOverride(slotId = "lunch", time = ProtocolTime(13, 0), confirmedByUser = false),
            ),
        )

        val lunch = plan.slots.single { it.id == "lunch" }
        assertEquals(12 * 60, lunch.time.sortKey) // unchanged from the compiled default
        assertFalse(lunch.userOverridden)
        assertEquals(1, plan.rejectedOverrides.size)
        assertTrue(plan.rejectedOverrides.single().reason.contains("not confirmed"))
    }

    @Test
    fun unknownSlotOverrideIsRejectedNotSilentlyIgnored() {
        val plan = MealPlanCompiler.compile(
            TrainingVariant.AFTERNOON_1600,
            overrides = listOf(MealSlotOverride(slotId = "second-breakfast", addPortions = listOf(portion("p1", "toast")))),
        )

        assertEquals(1, plan.rejectedOverrides.size)
        assertEquals("second-breakfast", plan.rejectedOverrides.single().slotId)
        assertTrue(plan.rejectedOverrides.single().reason.contains("Unknown meal slot"))
    }

    @Test
    fun laterConfirmedOverrideCanRemoveAnEarlierAddedPortion() {
        val plan = MealPlanCompiler.compile(
            TrainingVariant.AFTERNOON_1600,
            overrides = listOf(
                MealSlotOverride(slotId = "lunch", addPortions = listOf(portion("p1", "rice"), portion("p2", "chicken"))),
                MealSlotOverride(slotId = "lunch", removePortionIds = setOf("p1")),
            ),
        )

        val lunch = plan.slots.single { it.id == "lunch" }
        assertEquals(listOf("p2"), lunch.portions.map { it.id })
        assertEquals(300.0, lunch.resolvedTotals.energyKcal)
    }

    @Test
    fun plainDailyPlanWithNoPortionsIsTriviallySatisfiable() {
        val plan = MealPlanCompiler.compile(TrainingVariant.AFTERNOON_1600)

        val evaluation = MealPlanConstraintEvaluator.evaluate(plan, DailyNutritionTarget())

        assertTrue(evaluation.satisfiable)
        assertTrue(evaluation.violations.isEmpty())
    }

    @Test
    fun impossibleConstraintsAreReportedNotSilentlyAccepted() {
        val plan = MealPlanCompiler.compile(
            TrainingVariant.AFTERNOON_1600,
            overrides = listOf(MealSlotOverride(slotId = "lunch", addPortions = listOf(portion("p1", "chicken-breast")))),
        )
        // lunch resolves to 300 kcal / 30 g protein; ask for an impossible combination of a lower
        // energy ceiling and a higher protein floor than the plan can actually deliver.
        val evaluation = MealPlanConstraintEvaluator.evaluate(
            plan,
            DailyNutritionTarget(maxEnergyKcal = 100.0, minProteinG = 999.0),
        )

        assertFalse(evaluation.satisfiable)
        assertEquals(2, evaluation.violations.size)
        assertTrue(evaluation.violations.any { it.contains("exceeds the target maximum") })
        assertTrue(evaluation.violations.any { it.contains("below the target minimum") })
    }

    @Test
    fun unresolvedPortionsBlockConstraintSatisfiabilityClaim() {
        val plan = MealPlanCompiler.compile(
            TrainingVariant.AFTERNOON_1600,
            overrides = listOf(MealSlotOverride(slotId = "lunch", addPortions = listOf(portion("p1", "chicken-breast", grams = null)))),
        )

        val evaluation = MealPlanConstraintEvaluator.evaluate(plan, DailyNutritionTarget())

        assertFalse(evaluation.satisfiable)
        assertTrue(evaluation.violations.any { it.contains("unresolved food portions") })
    }

    @Test
    fun explanationBoundaryNeverClaimsInventionRights() {
        val plan = MealPlanCompiler.compile(TrainingVariant.AFTERNOON_1600)
        val payload = MealPlanExplanationBoundary.createPayload(plan)

        assertFalse(payload.mayInventNutrientFacts)
        assertFalse(payload.mayInventMedicalRestrictions)
        assertFalse(payload.mayInventTherapeuticTargets)
        assertEquals(plan, payload.plan)
    }
}
