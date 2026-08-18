package dev.ed3c.gymcometrue.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What a renderer is allowed to draw (Issue #48 rendering).
 *
 * The plan-level ordering and opacity rules are asserted in [ExerciseCatalogContractTest]; this
 * file covers the three ways the drawing could silently lie — geometry that does not cover the
 * mapping, an intensity outside the closed scale, and a logged exercise nobody can resolve.
 */
class MuscleVisualizationRenderingTest {

    private val demoIndex: Map<String, List<MuscleEngagement>> = mapOf(
        "bodyweight-squat" to listOf(
            MuscleEngagement(MuscleGroup.QUADRICEPS, ActivationIntensity.PRIMARY),
            MuscleEngagement(MuscleGroup.GLUTEUS_MAXIMUS, ActivationIntensity.PRIMARY),
            MuscleEngagement(MuscleGroup.ABDOMINALS, ActivationIntensity.STABILIZER),
        ),
        "glute-bridge" to listOf(
            MuscleEngagement(MuscleGroup.GLUTEUS_MAXIMUS, ActivationIntensity.PRIMARY),
            MuscleEngagement(MuscleGroup.HAMSTRINGS, ActivationIntensity.SECONDARY),
        ),
        "wall-push-up" to listOf(
            MuscleEngagement(MuscleGroup.PECTORALIS_MAJOR, ActivationIntensity.PRIMARY),
            MuscleEngagement(MuscleGroup.SERRATUS_ANTERIOR, ActivationIntensity.STABILIZER),
        ),
    )

    // ------------------------------------------------------------------ mapping and geometry

    @Test
    fun everyMappedRegionHasGeometryAndEveryShapeIsMapped() {
        val mapped = MuscleRegionMap.regions.flatMap { it.svgRegionIds }

        // Both directions: a mapping with no shape draws nothing, and a shape nobody maps is a
        // part of the body that can never light up. Either one is invisible at runtime.
        assertEquals(mapped.toSet(), MuscleSchematic.regionIds)
        assertEquals(mapped.size, mapped.toSet().size, "A region id is bound to two muscles")
        assertEquals(mapped.size, MuscleSchematic.regionIds.size)
    }

    @Test
    fun regionTotalsAccountForEveryMuscleInTheVocabulary() {
        val renderable = MuscleRegionMap.regions.map { it.muscle }

        assertEquals(renderable.size, renderable.toSet().size, "A muscle claims two regions")
        assertEquals(
            MuscleGroup.entries.size,
            renderable.size + MuscleRegionMap.unrenderableMuscles.size,
        )
        assertTrue(MuscleRegionMap.unrenderableMuscles.none(MuscleRegionMap::isRenderable))
    }

    @Test
    fun eachViewDrawsItsOwnRegionsAndNothingElse() {
        val front = MuscleSchematic.regionIdsFor(BodyView.FRONT)
        val back = MuscleSchematic.regionIdsFor(BodyView.BACK)

        assertTrue(front.all { it.startsWith("muscle-front-") })
        assertTrue(back.all { it.startsWith("muscle-back-") })
        assertEquals(MuscleSchematic.regionIds, (front + back).toSet())
    }

    @Test
    fun everyShapeStaysInsideTheSilhouette() {
        // The box constructor rejects the out-of-frame case; this proves the shipped table passed
        // it rather than that the requirement exists.
        (MuscleSchematic.regionIds.mapNotNull(MuscleSchematic::boxFor) + MuscleSchematic.headBox)
            .forEach { box ->
                assertTrue(box.left in 0.0..1.0 && box.right in 0.0..1.0, "x outside frame: $box")
                assertTrue(box.top in 0.0..1.0 && box.bottom in 0.0..1.0, "y outside frame: $box")
                assertTrue(box.width > 0.0 && box.height > 0.0, "empty shape: $box")
            }
        assertFailsWith<IllegalArgumentException> { SchematicBox(0.2, 0.1, 0.2, 0.4) }
        assertFailsWith<IllegalArgumentException> { SchematicBox(0.2, 0.1, 1.4, 0.4) }
        assertNull(MuscleSchematic.boxFor("muscle-front-does-not-exist"))
    }

    // ------------------------------------------------------------------ intensity clamp

    @Test
    fun opacityOutsideTheClosedScaleIsRejected() {
        listOf(1.4, 0.0, -0.2, 0.95).forEach { outOfScale ->
            assertFailsWith<IllegalArgumentException>("opacity $outOfScale was accepted") {
                RegionHighlight(
                    regionId = "muscle-front-abdominals",
                    view = BodyView.FRONT,
                    muscle = MuscleGroup.ABDOMINALS,
                    intensity = ActivationIntensity.PRIMARY,
                    opacity = outOfScale,
                )
            }
        }
    }

    @Test
    fun repeatingOneMuscleAcrossADayNeverDarkensPastItsClass() {
        val heavyDay = (1..6).flatMap { demoIndex.getValue("bodyweight-squat") }
        val plan = MuscleVisualizationPlanner.plan(heavyDay, CatalogLocale.EN)

        assertTrue(plan.highlights.isNotEmpty())
        plan.highlights.forEach { highlight ->
            assertEquals(
                MuscleVisualizationPlanner.opacityFor(highlight.intensity),
                highlight.opacity,
                1e-9,
            )
            assertTrue(
                highlight.opacity <= MuscleVisualizationPlanner.MAX_OPACITY,
                "${highlight.regionId} accumulated past the strongest class",
            )
            assertTrue(highlight.opacity >= MuscleVisualizationPlanner.MIN_OPACITY)
        }
        assertEquals(
            MuscleVisualizationPlanner.plan(demoIndex.getValue("bodyweight-squat"), CatalogLocale.EN),
            plan,
        )
    }

    // ------------------------------------------------------------------ logged data

    @Test
    fun anUnknownSlugFailsClosedAndDrawsNothing() {
        val resolution = MuscleLogResolver.resolve(
            index = demoIndex,
            loggedSlugs = listOf("bodyweight-squat", "kettlebell-swing"),
            locale = CatalogLocale.EN,
        )

        // Not "draw the part that resolved": a day drawn from half its entries looks complete.
        assertEquals(
            MuscleLogResolution.UnknownExercises(listOf("kettlebell-swing")),
            resolution,
        )
        assertTrue(resolution !is MuscleLogResolution.Resolved)
    }

    @Test
    fun anEmptyLogIsItsOwnStateRatherThanAnEmptyBody() {
        assertEquals(
            MuscleLogResolution.NoLoggedExercises,
            MuscleLogResolver.resolve(demoIndex, emptyList(), CatalogLocale.EN),
        )
    }

    @Test
    fun aDayUnionsItsExercisesAndKeepsTheStrongestClassPerMuscle() {
        val day = MuscleLogResolver.resolve(
            index = demoIndex,
            loggedSlugs = listOf("glute-bridge", "bodyweight-squat", "glute-bridge"),
            locale = CatalogLocale.EN,
        )

        val resolved = day as MuscleLogResolution.Resolved
        assertEquals(listOf("bodyweight-squat", "glute-bridge"), resolved.exerciseSlugs)

        val byMuscle = resolved.plan.highlights.associate { it.muscle to it.intensity }
        assertEquals(ActivationIntensity.PRIMARY, byMuscle[MuscleGroup.GLUTEUS_MAXIMUS])
        assertEquals(ActivationIntensity.PRIMARY, byMuscle[MuscleGroup.QUADRICEPS])
        assertEquals(ActivationIntensity.SECONDARY, byMuscle[MuscleGroup.HAMSTRINGS])
        assertEquals(ActivationIntensity.STABILIZER, byMuscle[MuscleGroup.ABDOMINALS])
        assertTrue(resolved.plan.highlights.all { MuscleSchematic.boxFor(it.regionId) != null })
    }

    @Test
    fun aSingleExerciseUsesTheSameResolutionAsAWholeDay() {
        val single = MuscleLogResolver.resolve(demoIndex, listOf("wall-push-up"), CatalogLocale.EN)

        val resolved = single as MuscleLogResolution.Resolved
        assertEquals(listOf("wall-push-up"), resolved.exerciseSlugs)
        assertEquals(
            MuscleVisualizationPlanner.plan(demoIndex.getValue("wall-push-up"), CatalogLocale.EN),
            resolved.plan,
        )
        // SERRATUS_ANTERIOR has no shape, so it must survive as text rather than disappear.
        assertEquals(listOf(MuscleGroup.SERRATUS_ANTERIOR), resolved.plan.unrenderedMuscles)
        assertTrue(resolved.plan.accessibilitySummary.contains("Serratus anterior"))
    }

    @Test
    fun theIndexIsKeyedByTheValidatedSlug() {
        val validated = ExerciseCatalogValidator.validate(
            RawExerciseCatalog(
                schemaVersion = CatalogSchema.CURRENT_VERSION,
                catalogId = "muscle-map-test",
                catalogVersion = "1.0.0-test",
                safetyNotes = mapOf(
                    "general" to mapOf("en" to "Stop for pain.", "zh-Hant-TW" to "疼痛時停止。"),
                ),
                records = listOf(rawGluteBridge()),
            ),
        )

        assertEquals(emptyList(), validated.blockers)
        assertEquals(
            setOf("glute-bridge"),
            MuscleLogResolver.index(validated.records).keys,
        )
    }

    private fun rawGluteBridge(): RawExerciseRecord = RawExerciseRecord(
        id = "gct-glute-bridge",
        slug = "glute-bridge",
        name = mapOf("en" to "Glute Bridge", "zh-Hant-TW" to "臀橋"),
        summary = mapOf(
            "en" to "A hip extension performed on the floor.",
            "zh-Hant-TW" to "在地面完成的髖伸展動作。",
        ),
        movementPattern = "HIP_EXTENSION",
        mechanics = "COMPOUND",
        force = "PUSH",
        laterality = "BILATERAL",
        skillLevel = "BEGINNER",
        equipment = listOf("BODYWEIGHT", "FLOOR_MAT"),
        muscleEngagement = listOf(
            RawMuscleEngagement("GLUTEUS_MAXIMUS", "PRIMARY"),
            RawMuscleEngagement("HAMSTRINGS", "SECONDARY"),
        ),
        steps = mapOf(
            "en" to listOf("Lie on a stable surface.", "Press through the feet.", "Lower slowly."),
            "zh-Hant-TW" to listOf("躺在穩定表面。", "踩地抬起髖部。", "慢慢下降。"),
        ),
        commonErrors = mapOf(
            "en" to listOf("Moving too quickly."),
            "zh-Hant-TW" to listOf("動作速度過快。"),
        ),
        safetyNoteRef = "general",
        fieldProvenance = FieldProvenanceValidator.ALWAYS_REQUIRED.map { field ->
            FieldProvenance(
                field = field,
                authorship = AuthorshipMethod.FIRST_PARTY_AGENT_DRAFTED,
                licenseGrant = LicenseGrantKind.FIRST_PARTY_OWNERSHIP,
                provenanceRecordId = "prov-test-record",
                reviewState = ContentReviewState.DRAFT,
            )
        },
    )
}
