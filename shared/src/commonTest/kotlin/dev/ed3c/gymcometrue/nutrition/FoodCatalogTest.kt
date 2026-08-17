package dev.ed3c.gymcometrue.nutrition

import dev.ed3c.gymcometrue.domain.DeterministicSourceTransform
import dev.ed3c.gymcometrue.domain.ExactSourceSelector
import dev.ed3c.gymcometrue.domain.ImmutableSourceArtifact
import dev.ed3c.gymcometrue.domain.MassUnit
import dev.ed3c.gymcometrue.domain.ProductionEvidenceUse
import dev.ed3c.gymcometrue.domain.SourceArtifactKind
import dev.ed3c.gymcometrue.domain.SourceClaimScope
import dev.ed3c.gymcometrue.domain.SourceFieldMapping
import dev.ed3c.gymcometrue.domain.SourceMappingStatus
import dev.ed3c.gymcometrue.domain.SourceSelectorKind
import dev.ed3c.gymcometrue.domain.SourceSnapshotState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FoodCatalogTest {
    private fun hash(character: Char): String = character.toString().repeat(64)

    private fun profile(): FoodNutrientProfile = FoodNutrientProfile(
        energyKcalPer100g = 200.0,
        proteinGPer100g = 10.0,
        fatGPer100g = 5.0,
        carbohydrateGPer100g = 25.0,
        fiberGPer100g = 3.0,
        micronutrientsPer100g = listOf(
            MicronutrientAmount("vitamin_c", 12.0, MassUnit.MG),
            MicronutrientAmount("vitamin_e", 5.0, MassUnit.IU),
        ),
    )

    private fun identity(foodId: String = "rice-cooked-white"): FoodIdentity = FoodIdentity(
        foodId = foodId,
        category = "grain",
        names = listOf(
            LocalizedFoodName(FoodLocale.ZH_TW, "白飯"),
            LocalizedFoodName(FoodLocale.EN, "Cooked white rice"),
        ),
    )

    @Test
    fun foodIdentityRejectsDuplicateLocaleNames() {
        assertFailsWith<IllegalArgumentException> {
            FoodIdentity(
                foodId = "x",
                category = "grain",
                names = listOf(
                    LocalizedFoodName(FoodLocale.EN, "A"),
                    LocalizedFoodName(FoodLocale.EN, "B"),
                ),
            )
        }
    }

    @Test
    fun portionScalerScalesMacrosLinearlyAndDropsNonMassMicronutrients() {
        val amounts = FoodPortionScaler.scale(profile(), grams = 150.0)

        assertEquals(300.0, amounts.energyKcal)
        assertEquals(15.0, amounts.proteinG)
        assertEquals(7.5, amounts.fatG)
        assertEquals(37.5, amounts.carbohydrateG)
        assertEquals(4.5, amounts.fiberG)
        // vitamin_c (MG) scales generically; vitamin_e (IU) has no generic mass conversion and is dropped.
        assertEquals(18.0, amounts.micronutrientsMg["vitamin_c"])
        assertNull(amounts.micronutrientsMg["vitamin_e"])
    }

    @Test
    fun portionScalerRejectsNonPositiveGrams() {
        assertFailsWith<IllegalArgumentException> { FoodPortionScaler.scale(profile(), grams = 0.0) }
    }

    @Test
    fun nutrientAmountsSumMergesMicronutrientKeysAcrossPortions() {
        val a = NutrientAmounts(energyKcal = 100.0, micronutrientsMg = mapOf("sodium" to 50.0))
        val b = NutrientAmounts(energyKcal = 50.0, micronutrientsMg = mapOf("sodium" to 25.0, "calcium" to 10.0))

        val total = NutrientAmounts.sumOf(listOf(a, b))

        assertEquals(150.0, total.energyKcal)
        assertEquals(75.0, total.micronutrientsMg["sodium"])
        assertEquals(10.0, total.micronutrientsMg["calcium"])
    }

    @Test
    fun syntheticEntryWithoutMappingIsTestOnlyNeverAdmitted() {
        val entry = FoodCatalogEntry(
            identity = identity(),
            profile = profile(),
            servings = listOf(FoodServingDefinition("1 bowl / 1 碗", grams = 150.0)),
            sourceId = "synthetic-fixture",
            mappingId = null,
            synthetic = true,
            note = "Repository-authored synthetic value for testing; not a verified nutrition claim.",
        )

        val nonProduction = FoodCatalogAdmissionValidator.validate(entry, emptyMap(), emptyMap(), "2026-08-18", production = false)
        assertEquals(FoodCatalogAdmission.TEST_ONLY, nonProduction.admission)
        assertTrue(nonProduction.blockers.isEmpty())

        val production = FoodCatalogAdmissionValidator.validate(entry, emptyMap(), emptyMap(), "2026-08-18", production = true)
        assertEquals(FoodCatalogAdmission.REJECTED, production.admission)
        assertTrue(production.blockers.any { it.contains("cannot be admitted to production") })
    }

    @Test
    fun nonSyntheticEntryWithoutMappingIsRejectedNoInventedNutrientFacts() {
        val entry = FoodCatalogEntry(
            identity = identity(),
            profile = profile(),
            sourceId = "tfda-food-composition-candidate",
            mappingId = null,
            synthetic = false,
            note = "Candidate value pending an admitted source-field mapping.",
        )

        val result = FoodCatalogAdmissionValidator.validate(entry, emptyMap(), emptyMap(), "2026-08-18", production = false)

        assertEquals(FoodCatalogAdmission.REJECTED, result.admission)
        assertTrue(result.blockers.any { it.contains("requires a source-field mapping") })
    }

    @Test
    fun admittedMappingAndArtifactAllowProductionAdmission() {
        val sha = hash('a')
        val artifact = ImmutableSourceArtifact(
            snapshotId = "reviewed-food-composition-snapshot",
            sourceId = "reviewed-food-composition-source",
            state = SourceSnapshotState.LEGAL_REVIEWED,
            artifactKind = SourceArtifactKind.CSV,
            canonicalUrl = "https://example.test/food-composition",
            retrievalUrl = "https://example.test/food-composition.csv",
            capturedAtIsoDate = "2026-08-15",
            mediaType = "text/csv",
            byteLength = 4096,
            sha256 = sha,
            archiveUri = "repo://fixtures/food-composition.csv#sha256=$sha",
            licenseId = "OGL-TW-1.0",
            attributionText = "Reviewed test publisher",
            redistributable = true,
            legalReviewRef = "legal-review-2026-08-15",
            productionUse = ProductionEvidenceUse.ALLOW,
            note = "Reviewed fixture for the admission-chain test only.",
        )
        val mapping = SourceFieldMapping(
            mappingId = "food-composition-energy-mapping",
            sourceId = artifact.sourceId,
            status = SourceMappingStatus.VERIFIED,
            snapshotId = artifact.snapshotId,
            claimScope = SourceClaimScope.REFERENCE_VALUE,
            selector = ExactSourceSelector(kind = SourceSelectorKind.CSV_COLUMN, locator = "energy_kcal_per_100g"),
            targetField = "profile.energyKcalPer100g",
            transform = DeterministicSourceTransform.PARSE_DECIMAL,
            evidenceExcerptSha256 = hash('b'),
            qualifiedReviewerAttestationSha256 = hash('c'),
            productionUse = ProductionEvidenceUse.ALLOW,
            note = "Reviewed reference-value mapping for the admission-chain test only.",
        )
        val entry = FoodCatalogEntry(
            identity = identity(),
            profile = profile(),
            servings = listOf(FoodServingDefinition("1 bowl / 1 碗", grams = 150.0)),
            sourceId = artifact.sourceId,
            mappingId = mapping.mappingId,
            synthetic = false,
            note = "Backed by an admitted source-field mapping in this test only.",
        )

        val result = FoodCatalogAdmissionValidator.validate(
            entry,
            mapOf(artifact.snapshotId to artifact),
            mapOf(mapping.mappingId to mapping),
            "2026-08-18",
            production = true,
        )

        assertEquals(FoodCatalogAdmission.ADMITTED, result.admission)
        assertTrue(result.blockers.isEmpty())
    }
}
