package dev.ed3c.gymcometrue.domain

import kotlinx.serialization.Serializable

@Serializable
data class DailyIntakeEntry(
    val id: String,
    val productId: String,
    val ingredient: String,
    val amountPerServing: Double,
    val unit: MassUnit,
    val servingsTaken: Double,
    val evidenceStatus: EvidenceStatus = EvidenceStatus.UNVERIFIED,
) {
    init {
        require(id.isNotBlank())
        require(productId.isNotBlank())
        require(ingredient.isNotBlank())
        require(amountPerServing > 0.0)
        require(servingsTaken > 0.0)
    }
}

@Serializable
data class UnresolvedIntake(
    val entryId: String,
    val ingredient: String,
    val reason: String,
)

@Serializable
data class DailyIntakeSummary(
    val totalMassMgByIngredient: Map<String, Double>,
    val productsByIngredient: Map<String, List<String>>,
    val duplicateIngredientKeys: Set<String>,
    val unresolved: List<UnresolvedIntake>,
    val mayBeComparedWithReviewedLimits: Boolean = false,
)

object DailyIntakeAggregator {
    /**
     * Arithmetic only. This does not establish a safe, effective, or recommended dose.
     * Entries remain unresolved unless their label evidence is reviewed and the unit is
     * generically convertible by mass.
     */
    fun summarize(entries: List<DailyIntakeEntry>): DailyIntakeSummary {
        val resolved = mutableListOf<Triple<String, String, Double>>()
        val unresolved = mutableListOf<UnresolvedIntake>()

        entries.forEach { entry ->
            val key = entry.ingredient.normalizedIngredientKey()
            val massMg = MassUnitConverter.toMilligrams(
                amount = entry.amountPerServing * entry.servingsTaken,
                unit = entry.unit,
            )

            when {
                entry.evidenceStatus != EvidenceStatus.VERIFIED_BY_REVIEWED_SOURCE -> {
                    unresolved += UnresolvedIntake(
                        entryId = entry.id,
                        ingredient = entry.ingredient,
                        reason = "Label or serving evidence is not verified by a reviewed source.",
                    )
                }
                massMg == null -> {
                    unresolved += UnresolvedIntake(
                        entryId = entry.id,
                        ingredient = entry.ingredient,
                        reason = "The unit cannot be converted by a generic mass rule.",
                    )
                }
                else -> resolved += Triple(key, entry.productId, massMg)
            }
        }

        val totals = resolved
            .groupBy({ it.first }, { it.third })
            .mapValues { (_, amounts) -> amounts.sum() }

        val products = entries
            .groupBy { it.ingredient.normalizedIngredientKey() }
            .mapValues { (_, values) -> values.map { it.productId }.distinct().sorted() }

        return DailyIntakeSummary(
            totalMassMgByIngredient = totals,
            productsByIngredient = products,
            duplicateIngredientKeys = products.filterValues { it.size > 1 }.keys,
            unresolved = unresolved,
        )
    }
}
