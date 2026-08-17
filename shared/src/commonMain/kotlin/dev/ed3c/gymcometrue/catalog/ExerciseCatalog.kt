package dev.ed3c.gymcometrue.catalog

import kotlinx.serialization.Serializable

private val slugPattern = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
private val urlLikePattern = Regex("(?i)(https?://|//[a-z0-9-]+\\.[a-z]{2,}|www\\.)")

/** Raw (untrusted) catalog shapes. These mirror `data/exercise-catalog/catalog.v1.json` exactly. */
@Serializable
data class RawMuscleEngagement(
    val muscle: String,
    val intensity: String,
)

@Serializable
data class RawExerciseRecord(
    val id: String,
    val slug: String,
    val name: Map<String, String>,
    val summary: Map<String, String>,
    val movementPattern: String,
    val mechanics: String,
    val force: String,
    val laterality: String,
    val skillLevel: String,
    val equipment: List<String>,
    val muscleEngagement: List<RawMuscleEngagement>,
    val steps: Map<String, List<String>>,
    val commonErrors: Map<String, List<String>>,
    val safetyNoteRef: String,
    val mediaRefs: List<String> = emptyList(),
    val fieldProvenance: List<FieldProvenance>,
)

@Serializable
data class RawExerciseCatalog(
    val schemaVersion: Int,
    val catalogId: String,
    val catalogVersion: String,
    val safetyNotes: Map<String, Map<String, String>>,
    val records: List<RawExerciseRecord>,
)

/**
 * A record that survived validation.
 *
 * The constructor is `internal` on purpose: downstream code cannot hand-build an
 * [ExerciseRecord] from unchecked strings, so "was this validated?" stops being a discipline
 * question and becomes a visibility question. It is deliberately not `@Serializable` — the
 * serialized shape of the catalog is [RawExerciseRecord], which must always be re-validated.
 */
class ExerciseRecord internal constructor(
    val id: String,
    val slug: String,
    val name: Map<CatalogLocale, String>,
    val summary: Map<CatalogLocale, String>,
    val movementPattern: MovementPattern,
    val mechanics: Mechanics,
    val force: ForceVector,
    val laterality: Laterality,
    val skillLevel: SkillLevel,
    val equipment: List<EquipmentClass>,
    val muscleEngagement: List<MuscleEngagement>,
    val steps: Map<CatalogLocale, List<String>>,
    val commonErrors: Map<CatalogLocale, List<String>>,
    val safetyNoteRef: String,
    val mediaRefs: List<String>,
    val provenance: List<FieldProvenance>,
) {
    fun musclesAt(intensity: ActivationIntensity): List<MuscleGroup> =
        muscleEngagement.filter { it.intensity == intensity }.map { it.muscle }

    override fun toString(): String = "ExerciseRecord($id)"
}

@Serializable
enum class CatalogAdmission { REJECTED, DRAFT, REVIEW_REQUIRED, ADMITTED }

data class CatalogValidationResult(
    val admission: CatalogAdmission,
    val records: List<ExerciseRecord>,
    val blockers: List<String>,
    val reviewNotes: List<String>,
)

/**
 * Deterministic screen for wording that would turn an exercise description into a health claim.
 *
 * This is a screen, not a review: passing it means no known claim phrase was found, never that a
 * qualified reviewer accepted the wording. It runs only over user-facing instructional fields
 * (name, summary, steps, common errors) so that safety disclaimers, which legitimately contain
 * words like "medical advice", are not flagged as claims.
 */
object MedicalClaimScreen {
    private val bannedPhrases: List<String> = listOf(
        // English claim verbs and marketing absolutes.
        "cures", "cure ", "treats ", "treatment for", "therapeutic", "diagnose", "diagnosis",
        "prevents injury", "injury-proof", "medically proven", "clinically proven", "guaranteed",
        "burns fat", "detox", "heals", "rehabilitates",
        // Traditional Chinese equivalents.
        "治療", "療效", "治癒", "診斷", "預防受傷", "醫學證實", "臨床證實", "保證",
        "燃脂", "排毒", "痊癒", "矯正骨盆", "根治",
    )

    fun scan(text: String): List<String> {
        val haystack = text.lowercase()
        return bannedPhrases.filter { it.lowercase() in haystack }
    }
}

/**
 * Catalog validation (Issues #32 and #33).
 *
 * Negative controls this validator owns:
 * - duplicate exercise id or slug is rejected;
 * - any taxonomy token outside the closed vocabulary is rejected, never coerced;
 * - a repository-level licence never authorizes a record (see [FieldProvenanceValidator]);
 * - a media reference is rejected unless the media id is in the admitted set the caller supplies;
 * - any URL-shaped string inside catalog text is rejected, which is what a hotlink looks like.
 */
object ExerciseCatalogValidator {
    fun validate(
        catalog: RawExerciseCatalog,
        admittedMediaIds: Set<String> = emptySet(),
        production: Boolean = false,
    ): CatalogValidationResult {
        val blockers = mutableListOf<String>()
        val notes = mutableListOf<String>()

        if (catalog.schemaVersion != CatalogSchema.CURRENT_VERSION) {
            blockers += "Unsupported catalog schemaVersion ${catalog.schemaVersion}; " +
                "only ${CatalogSchema.CURRENT_VERSION} is executable."
        }
        if (catalog.catalogId.isBlank() || catalog.catalogVersion.isBlank()) {
            blockers += "Catalog requires a non-blank catalogId and catalogVersion."
        }
        if (catalog.records.isEmpty()) {
            blockers += "Catalog contains no records."
        }

        catalog.safetyNotes.forEach { (ref, localized) ->
            if (ref.isBlank()) blockers += "Safety note key must not be blank."
            localizedText("safetyNotes[$ref]", localized, blockers)
        }

        val seenIds = mutableSetOf<String>()
        val seenSlugs = mutableSetOf<String>()
        val records = mutableListOf<ExerciseRecord>()

        catalog.records.forEach { raw ->
            if (!seenIds.add(raw.id)) blockers += "Duplicate exercise id ${raw.id}."
            if (!seenSlugs.add(raw.slug)) blockers += "Duplicate exercise slug ${raw.slug}."
            val record = validateRecord(raw, catalog.safetyNotes.keys, admittedMediaIds, production, blockers, notes)
            if (record != null) records += record
        }

        if (MuscleLabels.isComplete().not()) {
            blockers += "Muscle label table is incomplete for a required locale."
        }

        val distinct = blockers.distinct()
        val admission = when {
            distinct.isNotEmpty() -> CatalogAdmission.REJECTED
            production -> CatalogAdmission.ADMITTED
            else -> CatalogAdmission.DRAFT
        }
        return CatalogValidationResult(
            admission = admission,
            records = if (distinct.isEmpty()) records else emptyList(),
            blockers = distinct,
            reviewNotes = notes.distinct(),
        )
    }

    private fun validateRecord(
        raw: RawExerciseRecord,
        safetyNoteRefs: Set<String>,
        admittedMediaIds: Set<String>,
        production: Boolean,
        blockers: MutableList<String>,
        notes: MutableList<String>,
    ): ExerciseRecord? {
        val before = blockers.size
        val where = raw.id.ifBlank { "<blank id>" }

        if (!slugPattern.matches(raw.slug)) {
            blockers += "Record $where has a slug that is not lowercase-hyphen: ${raw.slug}."
        }
        if (raw.id != CatalogSchema.ID_PREFIX + raw.slug) {
            blockers += "Record $where must use id ${CatalogSchema.ID_PREFIX}${raw.slug}."
        }

        val name = localizedText("$where.name", raw.name, blockers)
        val summary = localizedText("$where.summary", raw.summary, blockers)
        val steps = localizedList("$where.steps", raw.steps, 3, 8, blockers)
        val commonErrors = localizedList("$where.commonErrors", raw.commonErrors, 1, 5, blockers)

        listOfNotNull(name, summary).forEach { screenText(where, it, blockers) }
        listOfNotNull(steps, commonErrors).forEach { byLocale ->
            byLocale.forEach { (locale, lines) ->
                screenText(where, mapOf(locale to lines.joinToString(" ")), blockers)
            }
        }

        val pattern = resolve("$where.movementPattern", raw.movementPattern, blockers, Taxonomy::movementPattern)
        val mechanics = resolve("$where.mechanics", raw.mechanics, blockers, Taxonomy::mechanics)
        val force = resolve("$where.force", raw.force, blockers, Taxonomy::force)
        val laterality = resolve("$where.laterality", raw.laterality, blockers, Taxonomy::laterality)
        val skillLevel = resolve("$where.skillLevel", raw.skillLevel, blockers, Taxonomy::skillLevel)

        if (raw.equipment.isEmpty()) blockers += "Record $where declares no equipment class."
        if (raw.equipment.distinct().size != raw.equipment.size) {
            blockers += "Record $where repeats an equipment class."
        }
        val equipment = raw.equipment.mapNotNull { resolve("$where.equipment", it, blockers, Taxonomy::equipment) }

        val engagement = mutableListOf<MuscleEngagement>()
        val seenMuscles = mutableSetOf<String>()
        raw.muscleEngagement.forEach { entry ->
            if (!seenMuscles.add(entry.muscle)) {
                blockers += "Record $where repeats muscle ${entry.muscle}."
            }
            val muscle = resolve("$where.muscleEngagement.muscle", entry.muscle, blockers, Taxonomy::muscle)
            val intensity = resolve("$where.muscleEngagement.intensity", entry.intensity, blockers, Taxonomy::intensity)
            if (muscle != null && intensity != null) engagement += MuscleEngagement(muscle, intensity)
        }
        if (engagement.none { it.intensity == ActivationIntensity.PRIMARY }) {
            blockers += "Record $where declares no PRIMARY muscle."
        }
        engagement.map { it.muscle }.filterNot(MuscleRegionMap::isRenderable).distinct().forEach {
            notes += "Muscle $it has no region in the first-party schematic and renders as text only."
        }

        if (raw.safetyNoteRef !in safetyNoteRefs) {
            blockers += "Record $where references unknown safety note ${raw.safetyNoteRef}."
        }

        raw.mediaRefs.forEach { mediaId ->
            if (mediaId !in admittedMediaIds) {
                blockers += "Record $where references media $mediaId that is not admitted."
            }
        }
        if (raw.mediaRefs.isEmpty()) {
            notes += "Record $where has no admitted media; the accessible text fallback is the only surface."
        }

        val requiredProvenance = if (raw.mediaRefs.isEmpty()) {
            FieldProvenanceValidator.ALWAYS_REQUIRED
        } else {
            FieldProvenanceValidator.ALWAYS_REQUIRED + CatalogField.MEDIA
        }
        blockers += FieldProvenanceValidator.validate(raw.fieldProvenance, production, requiredProvenance)
            .map { "Record $where: $it" }

        if (blockers.size != before) return null
        return ExerciseRecord(
            id = raw.id,
            slug = raw.slug,
            name = name!!,
            summary = summary!!,
            movementPattern = pattern!!,
            mechanics = mechanics!!,
            force = force!!,
            laterality = laterality!!,
            skillLevel = skillLevel!!,
            equipment = equipment,
            muscleEngagement = engagement,
            steps = steps!!,
            commonErrors = commonErrors!!,
            safetyNoteRef = raw.safetyNoteRef,
            mediaRefs = raw.mediaRefs,
            provenance = raw.fieldProvenance,
        )
    }

    private fun <T> resolve(
        label: String,
        raw: String,
        blockers: MutableList<String>,
        lookup: (String) -> T?,
    ): T? {
        val resolved = lookup(raw)
        if (resolved == null) blockers += "Unknown taxonomy token '$raw' for $label."
        return resolved
    }

    private fun screenText(
        where: String,
        localized: Map<CatalogLocale, String>,
        blockers: MutableList<String>,
    ) {
        localized.forEach { (locale, text) ->
            MedicalClaimScreen.scan(text).forEach { phrase ->
                blockers += "Record $where ${locale.tag} text contains claim phrase '$phrase'."
            }
            if (urlLikePattern.containsMatchIn(text)) {
                blockers += "Record $where ${locale.tag} text contains a URL; catalog text carries no links."
            }
        }
    }

    private fun localizedText(
        label: String,
        value: Map<String, String>,
        blockers: MutableList<String>,
    ): Map<CatalogLocale, String>? {
        val resolved = mutableMapOf<CatalogLocale, String>()
        value.forEach { (tag, text) ->
            val locale = CatalogLocale.fromTag(tag)
            if (locale == null) {
                blockers += "$label uses unsupported locale tag '$tag'."
            } else if (text.isBlank()) {
                blockers += "$label is blank for ${locale.tag}."
            } else {
                resolved[locale] = text
            }
        }
        val missing = CatalogLocale.REQUIRED.filterNot(resolved::containsKey)
        missing.forEach { blockers += "$label is missing locale ${it.tag}." }
        return if (missing.isEmpty() && resolved.size == CatalogLocale.REQUIRED.size) resolved else null
    }

    private fun localizedList(
        label: String,
        value: Map<String, List<String>>,
        minSize: Int,
        maxSize: Int,
        blockers: MutableList<String>,
    ): Map<CatalogLocale, List<String>>? {
        val resolved = mutableMapOf<CatalogLocale, List<String>>()
        value.forEach { (tag, lines) ->
            val locale = CatalogLocale.fromTag(tag)
            when {
                locale == null -> blockers += "$label uses unsupported locale tag '$tag'."
                lines.size !in minSize..maxSize ->
                    blockers += "$label for ${locale.tag} must hold $minSize..$maxSize entries, found ${lines.size}."
                lines.any(String::isBlank) -> blockers += "$label for ${locale.tag} contains a blank entry."
                else -> resolved[locale] = lines
            }
        }
        val missing = CatalogLocale.REQUIRED.filterNot(resolved::containsKey)
        missing.forEach { blockers += "$label is missing locale ${it.tag}." }
        if (missing.isNotEmpty()) return null
        val sizes = resolved.values.map { it.size }.distinct()
        if (sizes.size != 1) {
            blockers += "$label has a different entry count per locale: $sizes."
            return null
        }
        return resolved
    }
}

/**
 * Deterministic accessibility text for a validated record (Issues #33 and #48).
 *
 * Derived rather than hand-authored so the spoken description can never drift from the muscle
 * mapping that the visualization draws.
 */
object ExerciseAccessibility {
    fun label(record: ExerciseRecord, locale: CatalogLocale): String {
        val head = record.name.getValue(locale)
        val sections = sections(record.muscleEngagement, locale)
        return (listOf(head) + sections).joinToString(sentenceSeparator(locale)) + terminator(locale)
    }

    /** Spoken fallback for the muscle map, including muscles the schematic cannot draw. */
    fun muscleSummary(engagements: List<MuscleEngagement>, locale: CatalogLocale): String {
        val sections = sections(engagements, locale)
        return if (sections.isEmpty()) "" else sections.joinToString(sentenceSeparator(locale)) + terminator(locale)
    }

    private fun sections(
        engagements: List<MuscleEngagement>,
        locale: CatalogLocale,
    ): List<String> = ActivationIntensity.entries
        .sortedByDescending { it.level }
        .mapNotNull { intensity ->
            val muscles = engagements.filter { it.intensity == intensity }.map { it.muscle }
            if (muscles.isEmpty()) {
                null
            } else {
                val names = muscles
                    .map { MuscleLabels.label(it, locale) }
                    .sorted()
                    .joinToString(separator(locale))
                "${heading(intensity, locale)}$names"
            }
        }

    private fun heading(intensity: ActivationIntensity, locale: CatalogLocale): String =
        when (locale) {
            CatalogLocale.EN -> when (intensity) {
                ActivationIntensity.PRIMARY -> "primary muscles: "
                ActivationIntensity.SECONDARY -> "supporting muscles: "
                ActivationIntensity.STABILIZER -> "stabilizing muscles: "
            }
            CatalogLocale.ZH_HANT_TW -> when (intensity) {
                ActivationIntensity.PRIMARY -> "主要肌群："
                ActivationIntensity.SECONDARY -> "協同肌群："
                ActivationIntensity.STABILIZER -> "穩定肌群："
            }
        }

    private fun separator(locale: CatalogLocale): String =
        if (locale == CatalogLocale.EN) ", " else "、"

    private fun sentenceSeparator(locale: CatalogLocale): String =
        if (locale == CatalogLocale.EN) ". " else "。"

    private fun terminator(locale: CatalogLocale): String =
        if (locale == CatalogLocale.EN) "." else "。"
}
