package dev.ed3c.gymcometrue.catalog

import kotlinx.serialization.Serializable

/**
 * Per-field provenance contract (Issue #32).
 *
 * The negative control this file exists to enforce: a repository-level licence never authorizes an
 * individual record or asset. `LICENSE` at the repository root says what the *code* is licensed
 * under; it says nothing about who wrote a given exercise description or who owns a given image.
 * Any record whose grant is [LicenseGrantKind.REPOSITORY_ROOT_LICENSE] is rejected outright.
 */
@Serializable
enum class CatalogField {
    NAME,
    SUMMARY,
    STEPS,
    COMMON_ERRORS,
    TAXONOMY,
    MUSCLE_ENGAGEMENT,
    SAFETY_NOTE,
    MEDIA,
}

/**
 * How the bytes of a field came to exist.
 *
 * [FIRST_PARTY_AGENT_DRAFTED] is deliberately distinct from [FIRST_PARTY_HUMAN_ORIGINAL]: the
 * top-50 catalog text in this repository was drafted by an automated agent for the repository
 * owner and has not been through human editorial review. Collapsing the two would overstate the
 * evidence.
 */
@Serializable
enum class AuthorshipMethod {
    FIRST_PARTY_HUMAN_ORIGINAL,
    FIRST_PARTY_AGENT_DRAFTED,
    LICENSED_THIRD_PARTY,
    SCRAPED_OR_MIRRORED,
    UNKNOWN,
}

@Serializable
enum class LicenseGrantKind {
    NONE,
    REPOSITORY_ROOT_LICENSE,
    FIRST_PARTY_OWNERSHIP,
    EXECUTED_ASSET_SCOPE,
}

/**
 * Content review ladder. These are separate lanes and never collapse into "done":
 * a draft that validates against the schema is still a draft.
 */
@Serializable
enum class ContentReviewState {
    DRAFT,
    EDITORIAL_REVIEWED,
    RIGHTS_REVIEWED,
    ADMITTED,
    REVOKED,
}

@Serializable
data class FieldProvenance(
    val field: CatalogField,
    val authorship: AuthorshipMethod,
    val licenseGrant: LicenseGrantKind,
    val provenanceRecordId: String,
    val reviewState: ContentReviewState,
) {
    init {
        require(provenanceRecordId.isNotBlank()) { "provenanceRecordId must not be blank" }
    }
}

/**
 * Validates a resolved per-field provenance set.
 *
 * `production = true` asks the stronger question "may this field ship?" and requires
 * [ContentReviewState.ADMITTED]; `production = false` asks "is this a coherent draft?".
 */
object FieldProvenanceValidator {
    /**
     * Provenance for each of these is mandatory on every record; a gap is a blocker, never a
     * default. [CatalogField.MEDIA] is excluded on purpose: a record with no media has nothing to
     * hold rights over, and inventing a grant for an absent asset is exactly the fabrication this
     * contract exists to prevent. It becomes required as soon as a record cites media.
     */
    val ALWAYS_REQUIRED: Set<CatalogField> = CatalogField.entries.toSet() - CatalogField.MEDIA

    fun validate(
        provenance: List<FieldProvenance>,
        production: Boolean,
        requiredFields: Set<CatalogField> = ALWAYS_REQUIRED,
    ): List<String> {
        val blockers = mutableListOf<String>()

        val seen = mutableSetOf<CatalogField>()
        provenance.forEach { entry ->
            if (!seen.add(entry.field)) {
                blockers += "Duplicate provenance entry for field ${entry.field}."
            }
            blockers += validateEntry(entry, production)
        }
        (requiredFields - seen).sortedBy { it.name }.forEach { missing ->
            blockers += "Missing provenance for required field $missing."
        }
        (seen - requiredFields).sortedBy { it.name }.forEach { extra ->
            blockers += "Provenance declared for field $extra, which this record does not carry."
        }
        return blockers.distinct()
    }

    private fun validateEntry(entry: FieldProvenance, production: Boolean): List<String> {
        val blockers = mutableListOf<String>()
        val field = entry.field

        when (entry.authorship) {
            AuthorshipMethod.SCRAPED_OR_MIRRORED ->
                blockers += "Field $field is scraped or mirrored text and can never be admitted."
            AuthorshipMethod.UNKNOWN ->
                blockers += "Field $field has UNKNOWN authorship; unknown rights fail closed."
            AuthorshipMethod.LICENSED_THIRD_PARTY ->
                if (entry.licenseGrant != LicenseGrantKind.EXECUTED_ASSET_SCOPE) {
                    blockers += "Field $field is third-party licensed and requires an executed asset scope."
                }
            AuthorshipMethod.FIRST_PARTY_HUMAN_ORIGINAL,
            AuthorshipMethod.FIRST_PARTY_AGENT_DRAFTED,
            ->
                if (entry.licenseGrant != LicenseGrantKind.FIRST_PARTY_OWNERSHIP) {
                    blockers += "Field $field claims first-party authorship without first-party ownership."
                }
        }

        if (entry.licenseGrant == LicenseGrantKind.REPOSITORY_ROOT_LICENSE) {
            blockers += "Field $field cites the repository-root licence; that never authorizes a record."
        }
        if (entry.licenseGrant == LicenseGrantKind.NONE) {
            blockers += "Field $field has no licence grant."
        }
        if (entry.reviewState == ContentReviewState.REVOKED) {
            blockers += "Field $field provenance is revoked."
        }
        if (production) {
            if (entry.reviewState != ContentReviewState.ADMITTED) {
                blockers += "Production requires ADMITTED provenance for field $field; it is ${entry.reviewState}."
            }
            if (entry.authorship == AuthorshipMethod.FIRST_PARTY_AGENT_DRAFTED) {
                blockers += "Agent-drafted field $field requires human editorial acceptance before production."
            }
        }
        return blockers
    }
}
