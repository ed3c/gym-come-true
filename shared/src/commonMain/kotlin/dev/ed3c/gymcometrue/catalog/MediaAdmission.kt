package dev.ed3c.gymcometrue.catalog

import kotlinx.serialization.Serializable

private val mediaSha256 = Regex("^[0-9a-f]{64}$")

/** Only content-addressed local storage. A remote scheme is never a storage location here. */
private val contentAddressedStorage = Regex("^(repo|evidence)://[^\\s]+$")

/** Licence evidence is referenced, never embedded: no contract bytes enter this repository. */
private val licenceEvidenceRef = Regex("^(private|repo)://[^\\s]+$")

/**
 * Media intake and admission pipeline (Issue #34, `RIGHTS_CLEAN_TOP50 -> LICENSED_MEDIA_PIPELINE`).
 *
 * This file is the deterministic half of that transition. The other half — executed commercial
 * rights or commissioned first-party artwork — is an external gate and is `ABSENT` in this
 * repository. No record here may be moved past [MediaAdmissionState.HASH_VERIFIED] by code.
 */
@Serializable
enum class MediaKind { STILL_IMAGE, ANIMATION, VIDEO, VECTOR_SCHEMATIC, THREE_D_MODEL }

/**
 * [rank] is declared, not derived from [Enum.ordinal], so reordering the members cannot silently
 * change which evidence a state requires. `SUSPENDED` keeps admitted-grade evidence because it was
 * admitted; `REVOKED` carries none, because a withdrawn asset has no standing at all.
 */
@Serializable
enum class MediaAdmissionState(val rank: Int) {
    INTAKE(0),
    QUARANTINED(1),
    HASH_VERIFIED(2),
    RIGHTS_REVIEWED(3),
    ADMITTED(4),
    SUSPENDED(4),
    REVOKED(0),
}

@Serializable
enum class DerivativeTransform { NONE, RESIZE, CROP, TRANSCODE, FRAME_EXTRACT, LOSSLESS_RECOMPRESS }

@Serializable
data class MediaRightsScope(
    val licenseGrant: LicenseGrantKind,
    val licenseEvidenceRef: String? = null,
    val platforms: Set<String> = emptySet(),
    val territories: Set<String> = emptySet(),
    val termStartIsoDate: String? = null,
    val termEndIsoDate: String? = null,
    val derivativesAllowed: Boolean = false,
    val redistributionAllowed: Boolean = false,
    val attributionRequired: Boolean = false,
    val attributionText: String? = null,
)

@Serializable
data class MediaIntakeRecord(
    val mediaId: String,
    val exerciseId: String?,
    val kind: MediaKind,
    val state: MediaAdmissionState,
    val revocationKey: String,
    val rights: MediaRightsScope,
    val originSha256: String? = null,
    val byteLength: Long? = null,
    /** Only meaningful during [MediaAdmissionState.INTAKE]; a hotlink can never be a served asset. */
    val remoteUrl: String? = null,
    val storageUri: String? = null,
    /** Bilingual accessibility fallback, required before an asset may be admitted. */
    val altText: Map<String, String> = emptyMap(),
    val reviewerAttestationSha256: String? = null,
    val synthetic: Boolean = false,
    val derivedFromSha256: String? = null,
    val derivativeTransform: DerivativeTransform = DerivativeTransform.NONE,
    /** An input manifest can never self-declare admission; see [MediaAdmissionValidator]. */
    val productionAdmitted: Boolean = false,
) {
    init {
        require(mediaId.isNotBlank()) { "mediaId must not be blank" }
        require(revocationKey.isNotBlank()) { "revocationKey must not be blank" }
    }
}

@Serializable
data class MediaAdmissionResult(
    val mediaId: String,
    val effectiveState: MediaAdmissionState,
    val blockers: List<String>,
    val reviewNotes: List<String>,
)

object MediaAdmissionValidator {
    fun validate(
        record: MediaIntakeRecord,
        recordsByOriginSha256: Map<String, MediaIntakeRecord> = emptyMap(),
        asOfIsoDate: String,
    ): MediaAdmissionResult {
        val blockers = mutableListOf<String>()
        val notes = mutableListOf<String>()

        if (record.state == MediaAdmissionState.REVOKED) {
            return MediaAdmissionResult(
                mediaId = record.mediaId,
                effectiveState = MediaAdmissionState.REVOKED,
                blockers = listOf("Media ${record.mediaId} is revoked and cannot be served."),
                reviewNotes = emptyList(),
            )
        }
        if (record.productionAdmitted) {
            blockers += "Media ${record.mediaId} self-declares productionAdmitted; that is never an input."
        }
        if (IsoDate.key(asOfIsoDate) == null) {
            blockers += "asOfIsoDate must be a valid YYYY-MM-DD date."
        }

        validateTransport(record, blockers, notes)
        validateIntegrity(record, blockers, notes)
        validateRights(record, asOfIsoDate, blockers, notes)
        validateDerivative(record, recordsByOriginSha256, blockers)
        validateAccessibility(record, blockers)

        if (record.state == MediaAdmissionState.SUSPENDED) {
            blockers += "Media ${record.mediaId} is suspended pending incident resolution."
        }

        val distinct = blockers.distinct()
        return MediaAdmissionResult(
            mediaId = record.mediaId,
            effectiveState = if (distinct.isEmpty()) record.state else MediaAdmissionState.QUARANTINED,
            blockers = distinct,
            reviewNotes = notes.distinct(),
        )
    }

    private fun validateTransport(
        record: MediaIntakeRecord,
        blockers: MutableList<String>,
        notes: MutableList<String>,
    ) {
        if (record.remoteUrl != null && record.state.rank > MediaAdmissionState.INTAKE.rank) {
            blockers += "Media ${record.mediaId} still carries a remote URL past intake; served media is never hotlinked."
        }
        if (record.state.rank >= MediaAdmissionState.HASH_VERIFIED.rank) {
            val storage = record.storageUri
            val sha = record.originSha256
            if (storage == null || !contentAddressedStorage.matches(storage)) {
                blockers += "Media ${record.mediaId} requires a content-addressed repo:// or evidence:// storage URI."
            } else if (sha != null && !storage.contains(sha)) {
                blockers += "Media ${record.mediaId} storage URI is not addressed by its own hash."
            }
        }
        if (record.state == MediaAdmissionState.INTAKE) {
            notes += "Media ${record.mediaId} is at intake; it must be quarantined and hashed before any use."
        }
    }

    private fun validateIntegrity(
        record: MediaIntakeRecord,
        blockers: MutableList<String>,
        notes: MutableList<String>,
    ) {
        if (record.state.rank < MediaAdmissionState.HASH_VERIFIED.rank) {
            notes += "Media ${record.mediaId} is not hash verified."
            return
        }
        val sha = record.originSha256
        if (sha == null || !mediaSha256.matches(sha)) {
            blockers += "Media ${record.mediaId} requires a lowercase 64-hex SHA-256 to be hash verified."
        }
        val byteLength = record.byteLength
        if (byteLength == null || byteLength <= 0) {
            blockers += "Media ${record.mediaId} requires a positive byte length to be hash verified."
        }
    }

    private fun validateRights(
        record: MediaIntakeRecord,
        asOfIsoDate: String,
        blockers: MutableList<String>,
        notes: MutableList<String>,
    ) {
        val rights = record.rights
        if (rights.licenseGrant == LicenseGrantKind.REPOSITORY_ROOT_LICENSE) {
            blockers += "Media ${record.mediaId} cites the repository-root licence; that never authorizes an asset."
        }
        if (record.state.rank < MediaAdmissionState.RIGHTS_REVIEWED.rank) {
            notes += "Media ${record.mediaId} has no rights review; hash verification is not a licence."
            return
        }
        if (rights.licenseGrant !in setOf(LicenseGrantKind.EXECUTED_ASSET_SCOPE, LicenseGrantKind.FIRST_PARTY_OWNERSHIP)) {
            blockers += "Media ${record.mediaId} needs an executed asset scope or first-party ownership."
        }
        val ref = rights.licenseEvidenceRef
        if (ref == null || !licenceEvidenceRef.matches(ref)) {
            blockers += "Media ${record.mediaId} needs a private:// or repo:// licence evidence reference."
        }
        if (rights.platforms.isEmpty()) blockers += "Media ${record.mediaId} needs an explicit platform scope."
        if (rights.territories.isEmpty()) blockers += "Media ${record.mediaId} needs an explicit territory scope."
        if (rights.attributionRequired && rights.attributionText.isNullOrBlank()) {
            blockers += "Media ${record.mediaId} requires attribution but carries no attribution text."
        }
        val attestation = record.reviewerAttestationSha256
        if (attestation == null || !mediaSha256.matches(attestation)) {
            blockers += "Media ${record.mediaId} needs a reviewer attestation hash to be rights reviewed."
        }

        val start = IsoDate.key(rights.termStartIsoDate)
        val end = IsoDate.key(rights.termEndIsoDate)
        if (rights.termStartIsoDate != null && start == null) blockers += "Media ${record.mediaId} termStartIsoDate is invalid."
        if (rights.termEndIsoDate != null && end == null) blockers += "Media ${record.mediaId} termEndIsoDate is invalid."
        if (start != null && end != null && start > end) blockers += "Media ${record.mediaId} term window is inverted."

        if (record.state == MediaAdmissionState.ADMITTED) {
            if (start == null || end == null) {
                blockers += "Media ${record.mediaId} requires a bounded rights term before admission."
            }
            val asOf = IsoDate.key(asOfIsoDate)
            if (asOf != null && start != null && asOf < start) blockers += "Media ${record.mediaId} term has not started."
            if (asOf != null && end != null && asOf > end) blockers += "Media ${record.mediaId} term has expired."
        }
    }

    private fun validateDerivative(
        record: MediaIntakeRecord,
        recordsByOriginSha256: Map<String, MediaIntakeRecord>,
        blockers: MutableList<String>,
    ) {
        val parentSha = record.derivedFromSha256
        if (parentSha == null) {
            if (record.derivativeTransform != DerivativeTransform.NONE) {
                blockers += "Media ${record.mediaId} declares a transform without a parent hash."
            }
            return
        }
        if (record.derivativeTransform == DerivativeTransform.NONE) {
            blockers += "Media ${record.mediaId} declares a parent hash without a deterministic transform."
        }
        if (!mediaSha256.matches(parentSha)) {
            blockers += "Media ${record.mediaId} parent hash is not a lowercase 64-hex SHA-256."
            return
        }
        if (record.originSha256 == parentSha) {
            blockers += "Media ${record.mediaId} cannot be its own derivative."
        }
        val parent = recordsByOriginSha256[parentSha]
        if (parent == null) {
            blockers += "Media ${record.mediaId} derives from an unknown parent hash."
            return
        }
        if (parent.state != MediaAdmissionState.ADMITTED) {
            blockers += "Media ${record.mediaId} derives from ${parent.mediaId}, which is not admitted."
        }
        if (!parent.rights.derivativesAllowed) {
            blockers += "Media ${record.mediaId} derives from ${parent.mediaId}, whose licence forbids derivatives."
        }
        if (!parent.rights.platforms.containsAll(record.rights.platforms)) {
            blockers += "Media ${record.mediaId} claims platforms beyond its parent's scope."
        }
        if (!parent.rights.territories.containsAll(record.rights.territories)) {
            blockers += "Media ${record.mediaId} claims territories beyond its parent's scope."
        }
        if (record.rights.redistributionAllowed && !parent.rights.redistributionAllowed) {
            blockers += "Media ${record.mediaId} claims redistribution its parent does not grant."
        }
        if (record.revocationKey != parent.revocationKey) {
            blockers += "Media ${record.mediaId} must inherit its parent's revocation key so takedown reaches it."
        }
    }

    private fun validateAccessibility(record: MediaIntakeRecord, blockers: MutableList<String>) {
        if (record.state != MediaAdmissionState.ADMITTED) return
        CatalogLocale.REQUIRED.forEach { locale ->
            if (record.altText[locale.tag].isNullOrBlank()) {
                blockers += "Media ${record.mediaId} needs ${locale.tag} alternative text before admission."
            }
        }
    }
}

@Serializable
data class TakedownResult(
    val records: List<MediaIntakeRecord>,
    val revokedMediaIds: List<String>,
    /** True when no record carried the key: an absent target is reported, never treated as success. */
    val keyNotFound: Boolean,
)

/**
 * Takedown and kill switch.
 *
 * Revocation is transitive over the derivative graph: pulling an original also pulls every
 * derivative made from it, because a derivative of a withdrawn asset is still that asset.
 */
object MediaTakedown {
    fun apply(records: List<MediaIntakeRecord>, revocationKey: String): TakedownResult {
        val directHits = records.filter { it.revocationKey == revocationKey }
        if (directHits.isEmpty()) {
            return TakedownResult(records, emptyList(), keyNotFound = true)
        }

        val revokedHashes = directHits.mapNotNull { it.originSha256 }.toMutableSet()
        val revokedIds = directHits.map { it.mediaId }.toMutableSet()
        var changed = true
        while (changed) {
            changed = false
            records.forEach { record ->
                val parent = record.derivedFromSha256
                if (record.mediaId !in revokedIds && parent != null && parent in revokedHashes) {
                    revokedIds += record.mediaId
                    record.originSha256?.let { revokedHashes += it }
                    changed = true
                }
            }
        }

        return TakedownResult(
            records = records.map {
                if (it.mediaId in revokedIds) it.copy(state = MediaAdmissionState.REVOKED) else it
            },
            revokedMediaIds = revokedIds.sorted(),
            keyNotFound = false,
        )
    }
}

/** The only surface allowed to answer "may this exercise show media right now?". */
object MediaAdmissionLedger {
    fun admittedMediaIds(records: List<MediaIntakeRecord>, asOfIsoDate: String): Set<String> {
        val byOrigin = records.mapNotNull { record -> record.originSha256?.let { it to record } }.toMap()
        return records
            .filter { it.state == MediaAdmissionState.ADMITTED }
            .filter { MediaAdmissionValidator.validate(it, byOrigin, asOfIsoDate).blockers.isEmpty() }
            .map { it.mediaId }
            .toSet()
    }
}

/** Calendar-only date handling; the catalog package has no clock and no time zone. */
internal object IsoDate {
    fun key(value: String?): Int? {
        if (value == null) return null
        val parts = value.split("-")
        if (parts.size != 3 || parts[0].length != 4 || parts[1].length != 2 || parts[2].length != 2) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null
        if (month !in 1..12) return null
        val maxDay = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            else -> if (year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)) 29 else 28
        }
        if (day !in 1..maxDay) return null
        return year * 10_000 + month * 100 + day
    }
}
