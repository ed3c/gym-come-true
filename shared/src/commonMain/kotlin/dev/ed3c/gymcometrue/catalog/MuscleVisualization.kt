package dev.ed3c.gymcometrue.catalog

import kotlinx.serialization.Serializable

/**
 * Canonical exercise-muscle to schematic-region binding (Issue #48, V1).
 *
 * The only asset this map may bind to is the first-party schematic already admitted in this
 * repository. Its rights record is `legal/provenance/muscle-map-schematic.json`, which records
 * `anatomically_validated: false` and `diagnostic: false`; nothing here may be presented as an
 * anatomical or clinical illustration.
 *
 * Region ids are the exact `id` attributes inside that SVG. `data/exercise-catalog/validate_catalog.py`
 * re-reads the asset and fails if any id listed here is absent, or if the asset carries a region
 * for a muscle this map calls unrenderable. That check is what stops the mapping and the drawing
 * from drifting apart.
 */
object MuscleRegionMap {
    const val ASSET_PATH: String = "assets/first-party/muscle-map-schematic.svg"

    val regions: List<MuscleRegion> = listOf(
        // Front view.
        region(MuscleGroup.ANTERIOR_DELTOID, BodyView.FRONT, "muscle-front-left-shoulder", "muscle-front-right-shoulder"),
        region(MuscleGroup.PECTORALIS_MAJOR, BodyView.FRONT, "muscle-front-left-chest", "muscle-front-right-chest"),
        region(MuscleGroup.BICEPS_BRACHII, BodyView.FRONT, "muscle-front-left-arm", "muscle-front-right-arm"),
        region(MuscleGroup.ABDOMINALS, BodyView.FRONT, "muscle-front-abdominals"),
        region(MuscleGroup.OBLIQUES, BodyView.FRONT, "muscle-front-left-oblique", "muscle-front-right-oblique"),
        region(MuscleGroup.QUADRICEPS, BodyView.FRONT, "muscle-front-left-quadriceps", "muscle-front-right-quadriceps"),
        region(MuscleGroup.TIBIALIS_ANTERIOR, BodyView.FRONT, "muscle-front-left-lower-leg", "muscle-front-right-lower-leg"),
        // Back view.
        region(MuscleGroup.POSTERIOR_DELTOID, BodyView.BACK, "muscle-back-left-shoulder", "muscle-back-right-shoulder"),
        region(MuscleGroup.TRAPEZIUS, BodyView.BACK, "muscle-back-trapezius"),
        region(MuscleGroup.LATISSIMUS_DORSI, BodyView.BACK, "muscle-back-left-lat", "muscle-back-right-lat"),
        region(MuscleGroup.TRICEPS_BRACHII, BodyView.BACK, "muscle-back-left-triceps", "muscle-back-right-triceps"),
        region(MuscleGroup.ERECTOR_SPINAE, BodyView.BACK, "muscle-back-erectors"),
        region(MuscleGroup.GLUTEUS_MAXIMUS, BodyView.BACK, "muscle-back-left-glute", "muscle-back-right-glute"),
        region(MuscleGroup.HAMSTRINGS, BodyView.BACK, "muscle-back-left-hamstring", "muscle-back-right-hamstring"),
        region(MuscleGroup.GASTROCNEMIUS, BodyView.BACK, "muscle-back-left-calf", "muscle-back-right-calf"),
    )

    private val byMuscle: Map<MuscleGroup, MuscleRegion> = regions.associateBy { it.muscle }

    /**
     * Muscles the v1 asset cannot draw.
     *
     * These are not removed from [MuscleGroup]: an exercise may legitimately load them, and
     * dropping them would silently understate the engagement. They surface as
     * [MuscleVisualizationPlan.unrenderedMuscles] and stay in the spoken summary.
     */
    val unrenderableMuscles: List<MuscleGroup> =
        MuscleGroup.entries.filterNot(byMuscle::containsKey)

    fun isRenderable(muscle: MuscleGroup): Boolean = byMuscle.containsKey(muscle)

    fun regionFor(muscle: MuscleGroup): MuscleRegion? = byMuscle[muscle]

    private fun region(muscle: MuscleGroup, view: BodyView, vararg svgRegionIds: String): MuscleRegion =
        MuscleRegion(muscle = muscle, view = view, svgRegionIds = svgRegionIds.toList())
}

@Serializable
data class MuscleRegion(
    val muscle: MuscleGroup,
    val view: BodyView,
    val svgRegionIds: List<String>,
) {
    init {
        require(svgRegionIds.isNotEmpty()) { "A region binding needs at least one SVG id" }
        require(svgRegionIds.distinct().size == svgRegionIds.size) { "Duplicate SVG region id" }
    }
}

@Serializable
data class RegionHighlight(
    val regionId: String,
    val view: BodyView,
    val muscle: MuscleGroup,
    val intensity: ActivationIntensity,
    /** Deterministic fill opacity; a renderer must not invent its own scale. */
    val opacity: Double,
)

@Serializable
data class MuscleVisualizationPlan(
    val assetPath: String,
    val highlights: List<RegionHighlight>,
    val unrenderedMuscles: List<MuscleGroup>,
    val accessibilitySummary: String,
)

/**
 * Turns a validated engagement list into a renderer-independent draw plan.
 *
 * Compose, SwiftUI, and the web projection all consume this same plan, so the three platforms
 * cannot disagree about which region lights up or how strongly. Ordering is fully determined
 * (view, then muscle name, then region id) so a snapshot test is stable.
 */
object MuscleVisualizationPlanner {
    fun opacityFor(intensity: ActivationIntensity): Double = when (intensity) {
        ActivationIntensity.STABILIZER -> 0.30
        ActivationIntensity.SECONDARY -> 0.60
        ActivationIntensity.PRIMARY -> 0.90
    }

    fun plan(
        engagements: List<MuscleEngagement>,
        locale: CatalogLocale,
    ): MuscleVisualizationPlan {
        val strongest = engagements
            .groupBy { it.muscle }
            .mapValues { (_, entries) -> entries.maxOf { it.intensity.level } }

        val highlights = strongest.entries
            .mapNotNull { (muscle, level) ->
                val region = MuscleRegionMap.regionFor(muscle) ?: return@mapNotNull null
                val intensity = ActivationIntensity.entries.first { it.level == level }
                region.svgRegionIds.map { regionId ->
                    RegionHighlight(
                        regionId = regionId,
                        view = region.view,
                        muscle = muscle,
                        intensity = intensity,
                        opacity = opacityFor(intensity),
                    )
                }
            }
            .flatten()
            .sortedWith(compareBy({ it.view.name }, { it.muscle.name }, { it.regionId }))

        val unrendered = strongest.keys
            .filterNot(MuscleRegionMap::isRenderable)
            .sortedBy { it.name }

        val deduped = strongest.entries
            .map { (muscle, level) ->
                MuscleEngagement(muscle, ActivationIntensity.entries.first { it.level == level })
            }

        return MuscleVisualizationPlan(
            assetPath = MuscleRegionMap.ASSET_PATH,
            highlights = highlights,
            unrenderedMuscles = unrendered,
            accessibilitySummary = ExerciseAccessibility.muscleSummary(deduped, locale),
        )
    }
}
