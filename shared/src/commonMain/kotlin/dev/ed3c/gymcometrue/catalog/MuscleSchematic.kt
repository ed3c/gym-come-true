package dev.ed3c.gymcometrue.catalog

/**
 * Drawable geometry for the first-party schematic (Issue #48 rendering).
 *
 * Compose has no SVG parser in `commonMain`, and this repository will not add one or bundle a
 * second drawing. So the shapes below are the *same* geometry as
 * `assets/first-party/muscle-map-schematic.svg`, normalized into the silhouette's own box: every
 * box here is the bounding box of the region with that exact `id` in the asset, expressed as a
 * fraction of the silhouette outline (`0.0` left/top, `1.0` right/bottom).
 *
 * These numbers were not drawn by hand and must not be edited by hand.
 * `data/exercise-catalog/validate_catalog.py` recomputes each box from the asset on every run and
 * fails on any disagreement past `1e-3`, so the drawing cannot drift away from the artwork whose
 * rights record is `legal/provenance/muscle-map-schematic.json`. That record states
 * `anatomically_validated: false` and `diagnostic: false`: this is a schematic locator, not an
 * anatomical illustration.
 *
 * Front and back share one outline in the asset, so one normalized frame serves both views.
 */
object MuscleSchematic {
    /** Width divided by height of the silhouette frame; a renderer that ignores it skews the body. */
    const val ASPECT_RATIO: Double = 0.5077

    /** The silhouette head. It carries no muscle and never highlights; it is what makes it a body. */
    val headBox: SchematicBox = SchematicBox(0.3409, 0.0, 0.6591, 0.1615)

    private val boxes: Map<String, SchematicBox> = mapOf(
        // Front view.
        "muscle-front-left-shoulder" to SchematicBox(0.1629, 0.2058, 0.4205, 0.3135),
        "muscle-front-right-shoulder" to SchematicBox(0.5795, 0.2058, 0.8371, 0.3135),
        "muscle-front-left-chest" to SchematicBox(0.3106, 0.2442, 0.4886, 0.3962),
        "muscle-front-right-chest" to SchematicBox(0.5114, 0.2442, 0.6894, 0.3962),
        "muscle-front-left-arm" to SchematicBox(0.0682, 0.3154, 0.2803, 0.5558),
        "muscle-front-right-arm" to SchematicBox(0.7197, 0.3154, 0.9318, 0.5558),
        "muscle-front-abdominals" to SchematicBox(0.3939, 0.3808, 0.6061, 0.6538),
        "muscle-front-left-oblique" to SchematicBox(0.2879, 0.4, 0.4091, 0.6096),
        "muscle-front-right-oblique" to SchematicBox(0.5909, 0.4, 0.7121, 0.6096),
        "muscle-front-left-quadriceps" to SchematicBox(0.2424, 0.6115, 0.4735, 0.925),
        "muscle-front-right-quadriceps" to SchematicBox(0.5265, 0.6115, 0.7576, 0.925),
        "muscle-front-left-lower-leg" to SchematicBox(0.2311, 0.8923, 0.4091, 0.9923),
        "muscle-front-right-lower-leg" to SchematicBox(0.5909, 0.8923, 0.7689, 0.9923),
        // Back view.
        "muscle-back-left-shoulder" to SchematicBox(0.1629, 0.2058, 0.4205, 0.3135),
        "muscle-back-right-shoulder" to SchematicBox(0.5795, 0.2058, 0.8371, 0.3135),
        "muscle-back-trapezius" to SchematicBox(0.3674, 0.1558, 0.6326, 0.4096),
        "muscle-back-left-lat" to SchematicBox(0.2841, 0.3058, 0.4773, 0.5615),
        "muscle-back-right-lat" to SchematicBox(0.5227, 0.3058, 0.7159, 0.5615),
        "muscle-back-left-triceps" to SchematicBox(0.0682, 0.3154, 0.2803, 0.5558),
        "muscle-back-right-triceps" to SchematicBox(0.7197, 0.3154, 0.9318, 0.5558),
        "muscle-back-erectors" to SchematicBox(0.4205, 0.375, 0.5795, 0.625),
        "muscle-back-left-glute" to SchematicBox(0.2576, 0.5404, 0.4886, 0.7288),
        "muscle-back-right-glute" to SchematicBox(0.5114, 0.5404, 0.7424, 0.7288),
        "muscle-back-left-hamstring" to SchematicBox(0.2462, 0.6827, 0.4659, 0.9173),
        "muscle-back-right-hamstring" to SchematicBox(0.5341, 0.6827, 0.7538, 0.9173),
        "muscle-back-left-calf" to SchematicBox(0.2311, 0.8808, 0.4129, 0.9923),
        "muscle-back-right-calf" to SchematicBox(0.5871, 0.8808, 0.7689, 0.9923),
    )

    /** Exactly the ids [MuscleRegionMap] binds; the test asserts the two sets are equal. */
    val regionIds: Set<String> get() = boxes.keys

    /**
     * `null` for an id this schematic cannot draw. A renderer must skip it rather than invent a
     * placeholder box, because a placeholder would light up a part of the body nothing mapped to.
     */
    fun boxFor(regionId: String): SchematicBox? = boxes[regionId]

    /** Drawable ids for one view, in the map's own order. */
    fun regionIdsFor(view: BodyView): List<String> =
        MuscleRegionMap.regions.filter { it.view == view }.flatMap { it.svgRegionIds }
}

/**
 * A rectangle in silhouette-relative coordinates. Unit-free on purpose: the same box is a `dp`
 * rectangle on Android, a point rectangle on iOS, and a percentage in the web projection.
 */
data class SchematicBox(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double,
) {
    init {
        require(left in 0.0..1.0 && top in 0.0..1.0 && right in 0.0..1.0 && bottom in 0.0..1.0) {
            "A schematic box must stay inside the silhouette: ($left, $top, $right, $bottom)"
        }
        require(left < right && top < bottom) {
            "A schematic box must have positive extent: ($left, $top, $right, $bottom)"
        }
    }

    val width: Double get() = right - left

    val height: Double get() = bottom - top
}
