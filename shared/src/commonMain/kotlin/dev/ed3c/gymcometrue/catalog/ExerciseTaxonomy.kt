package dev.ed3c.gymcometrue.catalog

import kotlinx.serialization.Serializable

/**
 * Canonical exercise taxonomy (Issue #32, transition `DEMO_CATALOG -> TAXONOMY_CONTRACT`).
 *
 * Every vocabulary here is closed. A raw catalog token that is not present in one of these
 * enumerations is rejected by [ExerciseCatalogValidator]; it is never coerced, defaulted, or
 * silently dropped. The mirrored machine copy of this vocabulary lives in
 * `data/exercise-catalog/taxonomy.v1.json`, and drift between the two is a hard failure of
 * `data/exercise-catalog/validate_catalog.py`.
 */
object CatalogSchema {
    /** Only this schema version is executable. Older or newer inputs fail closed. */
    const val CURRENT_VERSION: Int = 1

    /** Catalog identifiers are lowercase, hyphen separated, and namespaced to this repository. */
    const val ID_PREFIX: String = "gct-"
}

/**
 * Locales the catalog must carry. Deliberately not `@Serializable`: on the wire a locale is the
 * BCP 47 tag used as a JSON object key, and [fromTag] is the only way in.
 */
enum class CatalogLocale(val tag: String) {
    EN("en"),
    ZH_HANT_TW("zh-Hant-TW"),
    ;

    companion object {
        val REQUIRED: List<CatalogLocale> = listOf(CatalogLocale.EN, CatalogLocale.ZH_HANT_TW)

        fun fromTag(tag: String): CatalogLocale? =
            CatalogLocale.entries.firstOrNull { it.tag == tag }
    }
}

/** The two schematic views the first-party muscle map exposes. */
@Serializable
enum class BodyView { FRONT, BACK }

/**
 * Muscle vocabulary.
 *
 * Membership here does not imply the first-party schematic can draw it; see [MuscleRegionMap].
 * A muscle without a region is reported as an explicit unrendered entry rather than being omitted.
 */
@Serializable
enum class MuscleGroup {
    ABDOMINALS,
    ADDUCTORS,
    ANTERIOR_DELTOID,
    BICEPS_BRACHII,
    ERECTOR_SPINAE,
    FOREARM_FLEXORS,
    GASTROCNEMIUS,
    GLUTEUS_MAXIMUS,
    HAMSTRINGS,
    HIP_FLEXORS,
    LATISSIMUS_DORSI,
    OBLIQUES,
    PECTORALIS_MAJOR,
    POSTERIOR_DELTOID,
    QUADRICEPS,
    ROTATOR_CUFF,
    SERRATUS_ANTERIOR,
    TIBIALIS_ANTERIOR,
    TRAPEZIUS,
    TRICEPS_BRACHII,
}

@Serializable
enum class MovementPattern {
    SQUAT,
    HINGE,
    LUNGE,
    HORIZONTAL_PUSH,
    VERTICAL_PUSH,
    HORIZONTAL_PULL,
    VERTICAL_PULL,
    HIP_EXTENSION,
    KNEE_FLEXION,
    KNEE_EXTENSION,
    ANKLE_PLANTARFLEXION,
    SHOULDER_ABDUCTION,
    ELBOW_FLEXION,
    ELBOW_EXTENSION,
    ANTI_EXTENSION,
    ANTI_LATERAL_FLEXION,
    ROTATION,
    CARRY,
}

@Serializable
enum class Mechanics { COMPOUND, ISOLATION, ISOMETRIC }

@Serializable
enum class ForceVector { PUSH, PULL, STATIC }

@Serializable
enum class Laterality { BILATERAL, UNILATERAL, ALTERNATING }

@Serializable
enum class SkillLevel { BEGINNER, INTERMEDIATE, ADVANCED }

@Serializable
enum class EquipmentClass {
    BODYWEIGHT,
    BARBELL,
    DUMBBELL,
    KETTLEBELL,
    MACHINE,
    CABLE_MACHINE,
    FLAT_BENCH,
    INCLINE_BENCH,
    SQUAT_RACK,
    PULL_UP_BAR,
    DIP_BARS,
    STABLE_STEP,
    FLOOR_MAT,
}

/**
 * Editorial movement-classification of how much a muscle is loaded by an exercise.
 *
 * [level] is an ordinal used only for deterministic rendering weight (Issue #48). It is not an
 * EMG measurement, not a percentage of maximal voluntary contraction, and carries no
 * physiological or clinical claim.
 */
@Serializable
enum class ActivationIntensity(val level: Int) {
    STABILIZER(1),
    SECONDARY(2),
    PRIMARY(3),
}

@Serializable
data class MuscleEngagement(
    val muscle: MuscleGroup,
    val intensity: ActivationIntensity,
)

/** Bilingual display labels for the muscle vocabulary; mirrored in `taxonomy.v1.json`. */
object MuscleLabels {
    private val labels: Map<MuscleGroup, Map<CatalogLocale, String>> = mapOf(
        MuscleGroup.ABDOMINALS to bilingual("Abdominals", "腹直肌"),
        MuscleGroup.ADDUCTORS to bilingual("Adductors", "內收肌群"),
        MuscleGroup.ANTERIOR_DELTOID to bilingual("Front deltoid", "前三角肌"),
        MuscleGroup.BICEPS_BRACHII to bilingual("Biceps", "肱二頭肌"),
        MuscleGroup.ERECTOR_SPINAE to bilingual("Spinal erectors", "豎脊肌"),
        MuscleGroup.FOREARM_FLEXORS to bilingual("Forearm flexors", "前臂屈肌群"),
        MuscleGroup.GASTROCNEMIUS to bilingual("Calf", "腓腸肌"),
        MuscleGroup.GLUTEUS_MAXIMUS to bilingual("Glutes", "臀大肌"),
        MuscleGroup.HAMSTRINGS to bilingual("Hamstrings", "腿後肌群"),
        MuscleGroup.HIP_FLEXORS to bilingual("Hip flexors", "髖屈肌群"),
        MuscleGroup.LATISSIMUS_DORSI to bilingual("Lats", "闊背肌"),
        MuscleGroup.OBLIQUES to bilingual("Obliques", "腹斜肌"),
        MuscleGroup.PECTORALIS_MAJOR to bilingual("Chest", "胸大肌"),
        MuscleGroup.POSTERIOR_DELTOID to bilingual("Rear deltoid", "後三角肌"),
        MuscleGroup.QUADRICEPS to bilingual("Quadriceps", "股四頭肌"),
        MuscleGroup.ROTATOR_CUFF to bilingual("Rotator cuff", "旋轉肌群"),
        MuscleGroup.SERRATUS_ANTERIOR to bilingual("Serratus anterior", "前鋸肌"),
        MuscleGroup.TIBIALIS_ANTERIOR to bilingual("Shin", "脛前肌"),
        MuscleGroup.TRAPEZIUS to bilingual("Trapezius", "斜方肌"),
        MuscleGroup.TRICEPS_BRACHII to bilingual("Triceps", "肱三頭肌"),
    )

    private fun bilingual(en: String, zh: String): Map<CatalogLocale, String> =
        mapOf(CatalogLocale.EN to en, CatalogLocale.ZH_HANT_TW to zh)

    /** Every muscle has a label in every required locale; [MuscleGroup.name] is never user facing. */
    fun label(muscle: MuscleGroup, locale: CatalogLocale): String =
        labels.getValue(muscle).getValue(locale)

    fun isComplete(): Boolean = MuscleGroup.entries.all { muscle ->
        val entry = labels[muscle]
        entry != null && CatalogLocale.REQUIRED.all { !entry[it].isNullOrBlank() }
    }
}

/**
 * Raw-token resolution.
 *
 * Catalog files are untrusted string input. Resolution returns `null` for an unknown token so the
 * caller must decide; there is no fallback member and no "OTHER" bucket.
 */
object Taxonomy {
    private val musclesByName = MuscleGroup.entries.associateBy { it.name }
    private val patternsByName = MovementPattern.entries.associateBy { it.name }
    private val mechanicsByName = Mechanics.entries.associateBy { it.name }
    private val forcesByName = ForceVector.entries.associateBy { it.name }
    private val lateralitiesByName = Laterality.entries.associateBy { it.name }
    private val skillLevelsByName = SkillLevel.entries.associateBy { it.name }
    private val equipmentByName = EquipmentClass.entries.associateBy { it.name }
    private val intensitiesByName = ActivationIntensity.entries.associateBy { it.name }

    fun muscle(raw: String): MuscleGroup? = musclesByName[raw]

    fun movementPattern(raw: String): MovementPattern? = patternsByName[raw]

    fun mechanics(raw: String): Mechanics? = mechanicsByName[raw]

    fun force(raw: String): ForceVector? = forcesByName[raw]

    fun laterality(raw: String): Laterality? = lateralitiesByName[raw]

    fun skillLevel(raw: String): SkillLevel? = skillLevelsByName[raw]

    fun equipment(raw: String): EquipmentClass? = equipmentByName[raw]

    fun intensity(raw: String): ActivationIntensity? = intensitiesByName[raw]

    /** The exact vocabulary snapshot that `taxonomy.v1.json` must reproduce token for token. */
    fun vocabulary(): Map<String, List<String>> = mapOf(
        "muscle" to MuscleGroup.entries.map { it.name },
        "movementPattern" to MovementPattern.entries.map { it.name },
        "mechanics" to Mechanics.entries.map { it.name },
        "force" to ForceVector.entries.map { it.name },
        "laterality" to Laterality.entries.map { it.name },
        "skillLevel" to SkillLevel.entries.map { it.name },
        "equipment" to EquipmentClass.entries.map { it.name },
        "intensity" to ActivationIntensity.entries.map { it.name },
    )
}
