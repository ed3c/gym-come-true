package dev.ed3c.gymcometrue.ui

import dev.ed3c.gymcometrue.catalog.ActivationIntensity
import dev.ed3c.gymcometrue.catalog.BodyView
import dev.ed3c.gymcometrue.catalog.CatalogLocale
import dev.ed3c.gymcometrue.catalog.MuscleLogResolution
import dev.ed3c.gymcometrue.catalog.MuscleLogResolver
import dev.ed3c.gymcometrue.catalog.MuscleSchematic
import dev.ed3c.gymcometrue.domain.TrainingVariant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The muscle map's screen-facing side (Issue #48 rendering).
 *
 * The renderer itself needs a Compose test host this lane does not have, so what is asserted here
 * is what a screen can be built from: the demo day resolves, every region it produces has a shape,
 * and every sentence around the drawing comes from [ProductCopy] rather than from a literal in the
 * renderer — which is what keeps the banned-vocabulary scan able to see them.
 */
class MuscleMapCopyTest {

    @Test
    fun everyDemoSlugResolvesAgainstTheDemoIndex() {
        TrainingVariant.entries.forEach { variant ->
            // A slug typo would land here as UnknownExercises, which is exactly what the screen
            // would show; the cast is the assertion.
            val resolved = SampleTrainingLog.resolve(variant, "zh-TW") as MuscleLogResolution.Resolved

            assertEquals(SampleTrainingLog.loggedSlugs(variant).sorted(), resolved.exerciseSlugs)
            assertTrue(resolved.plan.highlights.isNotEmpty())
            assertTrue(
                resolved.plan.highlights.all { MuscleSchematic.boxFor(it.regionId) != null },
                "$variant plans a region the schematic cannot draw",
            )
        }
    }

    @Test
    fun anUnloggedSlotDrawsNothingRatherThanAnEmptyBody() {
        assertEquals(
            MuscleLogResolution.NoLoggedExercises,
            MuscleLogResolver.resolve(
                index = SampleTrainingLog.engagementBySlug,
                loggedSlugs = emptyList(),
                locale = CatalogLocale.EN,
            ),
        )
    }

    @Test
    fun theDemoDayExercisesTheUnrenderableTextFallback() {
        // SERRATUS_ANTERIOR has no region in the v1 asset. If the screen ever stops reporting it,
        // the picture starts looking complete while the data says otherwise.
        val afternoon = SampleTrainingLog.resolve(TrainingVariant.AFTERNOON_1600, "en")
            as MuscleLogResolution.Resolved

        assertTrue(afternoon.plan.unrenderedMuscles.isNotEmpty())
        assertTrue(afternoon.plan.accessibilitySummary.contains("Serratus anterior"))
    }

    @Test
    fun everyMuscleMapSentenceIsAuthoredInProductCopy() {
        val required = listOf(
            ProductCopy.muscleHeading,
            ProductCopy.muscleNote,
            ProductCopy.muscleInformationNote,
            ProductCopy.muscleLogEmpty,
            ProductCopy.muscleLogUnresolved,
            ProductCopy.muscleUnrenderedNote,
        ) + BodyView.entries.map(ProductCopy::muscleViewLabel) +
            ActivationIntensity.entries.map { ProductCopy.muscleIntensityLabels.getValue(it) }

        required.forEach { text ->
            assertTrue(text in ProductCopy.userFacing, "\"${text.en}\" escapes the language scan")
        }
    }

    @Test
    fun theMapPresentsInformationAndNotARecommendation() {
        assertTrue(ProductCopy.muscleInformationNote.en.contains("your own logged exercises"))
        assertTrue(ProductCopy.muscleInformationNote.en.contains("not a training recommendation"))
        assertTrue(ProductCopy.muscleInformationNote.zhHant.contains("不是訓練建議"))
        assertTrue(ProductCopy.muscleNote.en.contains("first-party"))
    }

    @Test
    fun theCatalogLocaleFollowsTheUiLocaleTag() {
        assertEquals(CatalogLocale.ZH_HANT_TW, catalogLocale("zh-TW"))
        assertEquals(CatalogLocale.ZH_HANT_TW, catalogLocale("zh-Hant-TW"))
        assertEquals(CatalogLocale.EN, catalogLocale("en"))
    }
}
