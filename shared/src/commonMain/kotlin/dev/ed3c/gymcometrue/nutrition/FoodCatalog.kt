package dev.ed3c.gymcometrue.nutrition

import dev.ed3c.gymcometrue.domain.ImmutableSourceArtifact
import dev.ed3c.gymcometrue.domain.MassUnit
import dev.ed3c.gymcometrue.domain.MassUnitConverter
import dev.ed3c.gymcometrue.domain.SourceFieldMapping
import dev.ed3c.gymcometrue.domain.SourceFieldMappingValidator
import dev.ed3c.gymcometrue.domain.SourceLifecycleAdmission
import kotlinx.serialization.Serializable

/**
 * Issue #46 (N1) — copyright-clean Taiwan food/nutrition data layer.
 *
 * This file defines canonical food identity, serving/unit normalization, and macro/micronutrient
 * provenance. It reuses the existing immutable-source-lifecycle machinery in
 * [dev.ed3c.gymcometrue.domain] (`ImmutableSourceArtifact`, `SourceFieldMapping`,
 * `SourceFieldMappingValidator`) rather than building a second source-admission pipeline: a food
 * nutrient reference value is exactly the `SourceClaimScope.REFERENCE_VALUE` case that lifecycle
 * already models.
 *
 * No fixture in this repository is a real nutrition claim. Every non-synthetic catalog entry stays
 * `REVIEW_REQUIRED`/`REJECTED` until an admitted source-field mapping exists; see
 * `docs/nutrition/taiwan-source-evaluation.md` for the (unfinished) real-source evaluation.
 */

@Serializable
enum class FoodLocale { ZH_TW, EN }

@Serializable
data class LocalizedFoodName(
    val locale: FoodLocale,
    val name: String,
) {
    init {
        require(name.isNotBlank()) { "A localized food name cannot be blank." }
    }
}

@Serializable
data class FoodIdentity(
    val foodId: String,
    val category: String,
    val market: String = "TW",
    val names: List<LocalizedFoodName>,
) {
    init {
        require(foodId.isNotBlank()) { "foodId cannot be blank." }
        require(category.isNotBlank()) { "category cannot be blank." }
        require(market.isNotBlank()) { "market cannot be blank." }
        require(names.isNotEmpty()) { "A food identity requires at least one localized name." }
        require(names.map { it.locale }.distinct().size == names.size) {
            "A food identity cannot declare the same locale twice."
        }
    }

    fun nameFor(locale: FoodLocale): String? = names.firstOrNull { it.locale == locale }?.name
}

@Serializable
data class MicronutrientAmount(
    val nutrientKey: String,
    val amount: Double,
    val unit: MassUnit,
) {
    init {
        require(nutrientKey.isNotBlank()) { "nutrientKey cannot be blank." }
        require(amount >= 0.0) { "A micronutrient amount cannot be negative." }
    }
}

/**
 * Always normalized to a per-100g (or per-100ml for liquids, tracked by the caller) reference basis,
 * matching how Taiwan and most food-composition databases publish values.
 */
@Serializable
data class FoodNutrientProfile(
    val energyKcalPer100g: Double,
    val proteinGPer100g: Double,
    val fatGPer100g: Double,
    val carbohydrateGPer100g: Double,
    val fiberGPer100g: Double? = null,
    val micronutrientsPer100g: List<MicronutrientAmount> = emptyList(),
) {
    init {
        require(energyKcalPer100g >= 0.0) { "energyKcalPer100g cannot be negative." }
        require(proteinGPer100g >= 0.0) { "proteinGPer100g cannot be negative." }
        require(fatGPer100g >= 0.0) { "fatGPer100g cannot be negative." }
        require(carbohydrateGPer100g >= 0.0) { "carbohydrateGPer100g cannot be negative." }
        require(fiberGPer100g == null || fiberGPer100g >= 0.0) { "fiberGPer100g cannot be negative." }
    }
}

/** A named serving (e.g. "1 bowl / 1 碗") mapped to grams. `grams == null` means unresolved. */
@Serializable
data class FoodServingDefinition(
    val label: String,
    val grams: Double? = null,
) {
    init {
        require(label.isNotBlank()) { "A serving definition requires a label." }
        require(grams == null || grams > 0.0) { "Serving grams must be positive when known." }
    }
}

@Serializable
data class FoodCatalogEntry(
    val identity: FoodIdentity,
    val profile: FoodNutrientProfile,
    val servings: List<FoodServingDefinition> = emptyList(),
    val sourceId: String,
    val mappingId: String? = null,
    val synthetic: Boolean = false,
    val note: String,
) {
    init {
        require(sourceId.isNotBlank()) { "sourceId cannot be blank." }
        require(note.isNotBlank()) { "note cannot be blank." }
    }
}

/** Nutrient amounts at a resolved gram quantity (a scaled portion, or a summed total). */
@Serializable
data class NutrientAmounts(
    val energyKcal: Double = 0.0,
    val proteinG: Double = 0.0,
    val fatG: Double = 0.0,
    val carbohydrateG: Double = 0.0,
    val fiberG: Double = 0.0,
    val micronutrientsMg: Map<String, Double> = emptyMap(),
) {
    operator fun plus(other: NutrientAmounts): NutrientAmounts = NutrientAmounts(
        energyKcal = energyKcal + other.energyKcal,
        proteinG = proteinG + other.proteinG,
        fatG = fatG + other.fatG,
        carbohydrateG = carbohydrateG + other.carbohydrateG,
        fiberG = fiberG + other.fiberG,
        micronutrientsMg = (micronutrientsMg.keys + other.micronutrientsMg.keys)
            .associateWith { key -> (micronutrientsMg[key] ?: 0.0) + (other.micronutrientsMg[key] ?: 0.0) },
    )

    companion object {
        val ZERO = NutrientAmounts()

        fun sumOf(amounts: List<NutrientAmounts>): NutrientAmounts = amounts.fold(ZERO, NutrientAmounts::plus)
    }
}

/**
 * Scales a per-100g profile to an exact gram quantity. Micronutrients that cannot use the generic
 * mcg/mg/g mass conversion (IU, unrecognized units) are dropped rather than guessed, matching
 * `REVIEWED_HEALTH_RULES_ONLY`.
 */
object FoodPortionScaler {
    fun scale(profile: FoodNutrientProfile, grams: Double): NutrientAmounts {
        require(grams > 0.0) { "grams must be positive." }
        val factor = grams / 100.0
        val micronutrientsMg = profile.micronutrientsPer100g
            .mapNotNull { entry ->
                MassUnitConverter.toMilligrams(entry.amount, entry.unit)?.let { entry.nutrientKey to it * factor }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.sum() }

        return NutrientAmounts(
            energyKcal = profile.energyKcalPer100g * factor,
            proteinG = profile.proteinGPer100g * factor,
            fatG = profile.fatGPer100g * factor,
            carbohydrateG = profile.carbohydrateGPer100g * factor,
            fiberG = (profile.fiberGPer100g ?: 0.0) * factor,
            micronutrientsMg = micronutrientsMg,
        )
    }
}

@Serializable
enum class FoodCatalogAdmission { REJECTED, REVIEW_REQUIRED, TEST_ONLY, ADMITTED }

@Serializable
data class FoodCatalogValidationResult(
    val admission: FoodCatalogAdmission,
    val blockers: List<String>,
    val reviewNotes: List<String>,
)

/**
 * Default-deny admission for a catalog entry. A real (non-synthetic) entry can never be admitted
 * without an admitted `SourceFieldMapping` binding it back to an immutable, hashed, legally
 * reviewed source artifact — reusing `SourceFieldMappingValidator` rather than re-deriving that
 * chain here.
 */
object FoodCatalogAdmissionValidator {
    fun validate(
        entry: FoodCatalogEntry,
        artifactsById: Map<String, ImmutableSourceArtifact>,
        mappingsById: Map<String, SourceFieldMapping>,
        asOfIsoDate: String,
        production: Boolean,
    ): FoodCatalogValidationResult {
        val blockers = mutableListOf<String>()
        val notes = mutableListOf<String>()

        if (entry.identity.nameFor(FoodLocale.ZH_TW) == null || entry.identity.nameFor(FoodLocale.EN) == null) {
            notes += "Bilingual (zh-TW/en) naming is incomplete for ${entry.identity.foodId}."
        }
        if (entry.profile.micronutrientsPer100g.any { it.unit == MassUnit.IU || it.unit == MassUnit.UNKNOWN }) {
            notes += "IU or unrecognized micronutrient units on ${entry.identity.foodId} stay excluded from arithmetic totals."
        }
        if (entry.servings.isEmpty() || entry.servings.all { it.grams == null }) {
            notes += "No serving on ${entry.identity.foodId} resolves to grams; portion arithmetic stays unresolved until one does."
        }

        val mapping = entry.mappingId?.let(mappingsById::get)
        if (entry.mappingId != null && mapping == null) {
            blockers += "Catalog entry ${entry.identity.foodId} references an unknown source-field mapping ${entry.mappingId}."
        }
        val mappingResult = mapping?.let {
            SourceFieldMappingValidator.validate(it, artifactsById, asOfIsoDate, production)
        }
        mappingResult?.let {
            blockers += it.blockers
            notes += it.reviewNotes
        }

        if (entry.synthetic && production) {
            blockers += "Synthetic catalog entry ${entry.identity.foodId} cannot be admitted to production."
        }
        if (!entry.synthetic && mapping == null) {
            blockers += "Non-synthetic catalog entry ${entry.identity.foodId} requires a source-field mapping; no LLM-created nutrient facts."
        }
        if (production && mappingResult?.admission != SourceLifecycleAdmission.ADMITTED) {
            blockers += "Production catalog entry ${entry.identity.foodId} requires an admitted source-field mapping."
        }

        val admission = when {
            blockers.isNotEmpty() -> FoodCatalogAdmission.REJECTED
            production -> FoodCatalogAdmission.ADMITTED
            entry.synthetic -> FoodCatalogAdmission.TEST_ONLY
            mappingResult?.admission == SourceLifecycleAdmission.ADMITTED -> FoodCatalogAdmission.ADMITTED
            else -> FoodCatalogAdmission.REVIEW_REQUIRED
        }
        return FoodCatalogValidationResult(admission, blockers.distinct(), notes.distinct())
    }
}
