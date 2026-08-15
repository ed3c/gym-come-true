package dev.ed3c.gymcometrue.domain

import kotlinx.serialization.Serializable

private val sourceSha256 = Regex("^[0-9a-f]{64}$")
private val sourceHttps = Regex("^https://[^\\s]+$")
private val sourceArchive = Regex("^(repo|evidence|s3|gs|az|oci|ipfs|file)://[^\\s]+$")

@Serializable
enum class SourceArtifactKind { PDF, CSV, JSON, XML, HTML, ZIP, TEXT }

@Serializable
enum class SourceSnapshotState { CANDIDATE, CAPTURED, HASH_VERIFIED, LEGAL_REVIEWED, REVOKED }

@Serializable
enum class ProductionEvidenceUse { DENY, TEST_ONLY, ALLOW }

@Serializable
enum class SourceLifecycleAdmission { REJECTED, REVIEW_REQUIRED, TEST_ONLY, ADMITTED }

@Serializable
data class ImmutableSourceArtifact(
    val snapshotId: String,
    val sourceId: String,
    val state: SourceSnapshotState,
    val artifactKind: SourceArtifactKind,
    val jurisdiction: String = "TW",
    val canonicalUrl: String,
    val retrievalUrl: String? = null,
    val capturedAtIsoDate: String,
    val sourceModifiedAtIsoDate: String? = null,
    val effectiveFromIsoDate: String? = null,
    val effectiveUntilIsoDate: String? = null,
    val mediaType: String,
    val byteLength: Long? = null,
    val sha256: String? = null,
    val archiveUri: String? = null,
    val licenseId: String,
    val attributionText: String,
    val redistributable: Boolean,
    val synthetic: Boolean = false,
    val legalReviewRef: String? = null,
    val productionUse: ProductionEvidenceUse = ProductionEvidenceUse.DENY,
    val modelGenerated: Boolean = false,
    val note: String,
) {
    init {
        require(snapshotId.isNotBlank() && sourceId.isNotBlank())
        require(canonicalUrl.isNotBlank() && mediaType.isNotBlank())
        require(licenseId.isNotBlank() && attributionText.isNotBlank() && note.isNotBlank())
        require(byteLength == null || byteLength > 0)
    }
}

@Serializable
data class SourceArtifactValidationResult(
    val admission: SourceLifecycleAdmission,
    val blockers: List<String>,
    val reviewNotes: List<String>,
)

object ImmutableSourceArtifactValidator {
    fun validate(
        artifact: ImmutableSourceArtifact,
        asOfIsoDate: String,
        production: Boolean,
    ): SourceArtifactValidationResult {
        val blockers = mutableListOf<String>()
        val notes = mutableListOf<String>()

        if (artifact.jurisdiction != "TW") blockers += "Taiwan source evidence must use jurisdiction TW."
        checkDate("capturedAtIsoDate", artifact.capturedAtIsoDate, blockers)
        checkOptionalDate("sourceModifiedAtIsoDate", artifact.sourceModifiedAtIsoDate, blockers)
        checkOptionalDate("effectiveFromIsoDate", artifact.effectiveFromIsoDate, blockers)
        checkOptionalDate("effectiveUntilIsoDate", artifact.effectiveUntilIsoDate, blockers)
        checkDate("validation date", asOfIsoDate, blockers)

        val start = artifact.effectiveFromIsoDate.dateKey()
        val end = artifact.effectiveUntilIsoDate.dateKey()
        val asOf = asOfIsoDate.dateKey()
        if (start != null && end != null && start > end) blockers += "Source effective window is inverted."
        if (asOf != null && start != null && asOf < start) notes += "Source is not effective yet."
        if (asOf != null && end != null && asOf > end) blockers += "Source evidence is outside its effective window."

        if (artifact.modelGenerated) blockers += "A source artifact cannot be model generated."
        if (!artifact.synthetic && !sourceHttps.matches(artifact.canonicalUrl)) {
            blockers += "A non-synthetic source requires an HTTPS canonical URL."
        }
        if (artifact.retrievalUrl != null && !sourceHttps.matches(artifact.retrievalUrl)) {
            blockers += "retrievalUrl must use HTTPS."
        }

        val hashVerified = artifact.state in setOf(
            SourceSnapshotState.HASH_VERIFIED,
            SourceSnapshotState.LEGAL_REVIEWED,
        )
        if (hashVerified) {
            if (!artifact.sha256.isSha256()) blockers += "Hash-verified evidence requires lowercase SHA-256."
            if (artifact.byteLength == null || artifact.byteLength <= 0) {
                blockers += "Hash-verified evidence requires positive byte length."
            }
            if (!artifact.archiveUri.isContentAddressed(artifact.sha256)) {
                blockers += "Hash-verified evidence requires a content-addressed archive URI."
            }
        } else {
            notes += "Source is not hash verified."
        }

        when (artifact.state) {
            SourceSnapshotState.CANDIDATE -> notes += "Live source metadata is only a candidate."
            SourceSnapshotState.CAPTURED -> notes += "Captured source still requires hash verification."
            SourceSnapshotState.HASH_VERIFIED -> notes += "Hash verification does not replace legal review."
            SourceSnapshotState.LEGAL_REVIEWED ->
                if (artifact.legalReviewRef.isNullOrBlank()) blockers += "LEGAL_REVIEWED requires legalReviewRef."
            SourceSnapshotState.REVOKED -> blockers += "Revoked source evidence cannot be used."
        }

        when (artifact.productionUse) {
            ProductionEvidenceUse.DENY -> Unit
            ProductionEvidenceUse.TEST_ONLY -> {
                if (!artifact.synthetic) blockers += "TEST_ONLY evidence must be repository-authored synthetic evidence."
                if (!hashVerified) blockers += "TEST_ONLY evidence must be hash verified."
            }
            ProductionEvidenceUse.ALLOW -> {
                if (artifact.synthetic) blockers += "Synthetic evidence cannot be admitted to production."
                if (artifact.state != SourceSnapshotState.LEGAL_REVIEWED) {
                    blockers += "Production evidence must be legally reviewed."
                }
            }
        }
        if (production) {
            if (artifact.productionUse != ProductionEvidenceUse.ALLOW) blockers += "Production requires productionUse=ALLOW."
            if (artifact.synthetic) blockers += "Production rejects synthetic evidence."
            if (artifact.state != SourceSnapshotState.LEGAL_REVIEWED) {
                blockers += "Production requires LEGAL_REVIEWED evidence."
            }
        }

        val admission = when {
            blockers.isNotEmpty() -> SourceLifecycleAdmission.REJECTED
            production -> SourceLifecycleAdmission.ADMITTED
            artifact.productionUse == ProductionEvidenceUse.TEST_ONLY -> SourceLifecycleAdmission.TEST_ONLY
            artifact.productionUse == ProductionEvidenceUse.ALLOW -> SourceLifecycleAdmission.ADMITTED
            else -> SourceLifecycleAdmission.REVIEW_REQUIRED
        }
        return SourceArtifactValidationResult(admission, blockers.distinct(), notes.distinct())
    }
}

@Serializable
enum class SourceMappingStatus { DRAFT, VERIFIED, REVOKED }

@Serializable
enum class SourceSelectorKind { JSON_POINTER, CSV_COLUMN, XML_XPATH, PDF_PAGE_LINE, HTML_SELECTOR, TEXT_RANGE }

@Serializable
data class ExactSourceSelector(
    val kind: SourceSelectorKind,
    val locator: String,
    val pageNumber: Int? = null,
    val lineStart: Int? = null,
    val lineEnd: Int? = null,
) {
    init {
        require(locator.isNotBlank())
        require(pageNumber == null || pageNumber > 0)
        require(lineStart == null || lineStart > 0)
        require(lineEnd == null || lineEnd > 0)
    }
}

@Serializable
enum class SourceClaimScope {
    LABEL_SCHEMA, PRODUCT_IDENTITY, BUSINESS_IDENTITY, REGULATORY_TEXT,
    FOOD_ADDITIVE_CATEGORY, REFERENCE_VALUE, TOLERANCE_RANGE,
}

@Serializable
enum class DeterministicSourceTransform {
    EXACT_COPY, TRIM_WHITESPACE, PARSE_ISO_DATE, PARSE_DECIMAL, NORMALIZE_UNIT, IDENTITY_MATCH,
}

@Serializable
data class SourceFieldMapping(
    val mappingId: String,
    val sourceId: String,
    val status: SourceMappingStatus,
    val snapshotId: String?,
    val claimScope: SourceClaimScope,
    val selector: ExactSourceSelector,
    val targetField: String,
    val transform: DeterministicSourceTransform,
    val evidenceExcerptSha256: String? = null,
    val qualifiedReviewerAttestationSha256: String? = null,
    val productionUse: ProductionEvidenceUse = ProductionEvidenceUse.DENY,
    val modelGenerated: Boolean = false,
    val note: String,
) {
    init {
        require(mappingId.isNotBlank() && sourceId.isNotBlank())
        require(targetField.isNotBlank() && note.isNotBlank())
    }
}

@Serializable
data class SourceMappingValidationResult(
    val admission: SourceLifecycleAdmission,
    val blockers: List<String>,
    val reviewNotes: List<String>,
)

object SourceFieldMappingValidator {
    private val qualifiedReviewScopes = setOf(
        SourceClaimScope.REGULATORY_TEXT,
        SourceClaimScope.REFERENCE_VALUE,
        SourceClaimScope.TOLERANCE_RANGE,
    )

    fun validate(
        mapping: SourceFieldMapping,
        artifactsById: Map<String, ImmutableSourceArtifact>,
        asOfIsoDate: String,
        production: Boolean,
    ): SourceMappingValidationResult {
        val blockers = mutableListOf<String>()
        val notes = mutableListOf<String>()
        if (mapping.modelGenerated) blockers += "A model cannot create an exact source-field mapping."
        if (mapping.status == SourceMappingStatus.REVOKED) blockers += "Revoked mapping cannot be used."
        if (mapping.status == SourceMappingStatus.DRAFT) notes += "Draft mapping is inspectable but not executable."
        if (mapping.status == SourceMappingStatus.VERIFIED && mapping.snapshotId.isNullOrBlank()) {
            blockers += "Verified mapping requires snapshotId."
        }
        if (mapping.status == SourceMappingStatus.VERIFIED && !mapping.evidenceExcerptSha256.isSha256()) {
            blockers += "Verified mapping requires evidence excerpt SHA-256."
        }
        validateSelector(mapping.selector, blockers)

        val artifact = mapping.snapshotId?.let(artifactsById::get)
        if (mapping.snapshotId != null && artifact == null) blockers += "Mapping references an unknown snapshot."
        if (artifact != null) {
            if (artifact.sourceId != mapping.sourceId) {
                blockers += "Mapping sourceId does not match the referenced snapshot."
            }
            val result = ImmutableSourceArtifactValidator.validate(artifact, asOfIsoDate, production)
            blockers += result.blockers
            notes += result.reviewNotes
            if (mapping.productionUse == ProductionEvidenceUse.ALLOW &&
                result.admission != SourceLifecycleAdmission.ADMITTED
            ) blockers += "Production mapping requires an admitted production source artifact."
            if (mapping.productionUse == ProductionEvidenceUse.TEST_ONLY &&
                result.admission != SourceLifecycleAdmission.TEST_ONLY
            ) blockers += "TEST_ONLY mapping requires a TEST_ONLY source artifact."
        }

        if (mapping.claimScope in qualifiedReviewScopes &&
            !mapping.qualifiedReviewerAttestationSha256.isSha256()
        ) {
            if (production) blockers += "This claim scope requires qualified reviewer attestation."
            else notes += "This claim scope still requires qualified reviewer attestation."
        }
        if (mapping.productionUse != ProductionEvidenceUse.DENY &&
            mapping.status != SourceMappingStatus.VERIFIED
        ) blockers += "TEST_ONLY or production mapping must be VERIFIED."
        if (production) {
            if (mapping.productionUse != ProductionEvidenceUse.ALLOW) blockers += "Production mapping requires ALLOW."
            if (mapping.status != SourceMappingStatus.VERIFIED) blockers += "Production mapping requires VERIFIED."
        }

        val admission = when {
            blockers.isNotEmpty() -> SourceLifecycleAdmission.REJECTED
            production -> SourceLifecycleAdmission.ADMITTED
            mapping.productionUse == ProductionEvidenceUse.TEST_ONLY -> SourceLifecycleAdmission.TEST_ONLY
            mapping.productionUse == ProductionEvidenceUse.ALLOW -> SourceLifecycleAdmission.ADMITTED
            else -> SourceLifecycleAdmission.REVIEW_REQUIRED
        }
        return SourceMappingValidationResult(admission, blockers.distinct(), notes.distinct())
    }

    private fun validateSelector(selector: ExactSourceSelector, blockers: MutableList<String>) {
        when (selector.kind) {
            SourceSelectorKind.JSON_POINTER ->
                if (!selector.locator.startsWith("/")) blockers += "JSON pointer must start with /."
            SourceSelectorKind.XML_XPATH ->
                if (!selector.locator.startsWith("/")) blockers += "XPath must start with /."
            SourceSelectorKind.PDF_PAGE_LINE -> checkRange(selector, true, blockers)
            SourceSelectorKind.TEXT_RANGE -> checkRange(selector, false, blockers)
            SourceSelectorKind.CSV_COLUMN, SourceSelectorKind.HTML_SELECTOR -> Unit
        }
    }

    private fun checkRange(
        selector: ExactSourceSelector,
        requirePage: Boolean,
        blockers: MutableList<String>,
    ) {
        if ((requirePage && selector.pageNumber == null) ||
            selector.lineStart == null || selector.lineEnd == null
        ) {
            blockers += if (requirePage) {
                "PDF mapping requires pageNumber, lineStart, and lineEnd."
            } else {
                "Text mapping requires lineStart and lineEnd."
            }
        } else if (selector.lineStart > selector.lineEnd) {
            blockers += "Source lineStart cannot be later than lineEnd."
        }
    }
}

@Serializable
enum class RulePackReleaseState { DRAFT, REVIEWED, STAGED, ACTIVE, SUSPENDED, REVOKED, EXPIRED, ROLLED_BACK }

@Serializable
enum class RulePackLifecycleAction { REVIEW, STAGE, ACTIVATE, SUSPEND, RESUME, REVOKE, EXPIRE, ROLLBACK }

@Serializable
data class RulePackReleaseCandidate(
    val packId: String,
    val version: String,
    val generatedAtIsoDate: String,
    val effectiveFromIsoDate: String?,
    val effectiveUntilIsoDate: String?,
    val contentSha256: String?,
    val sourceBundleSha256: String?,
    val testSuiteSha256: String?,
    val reviewerAttestationSha256: String?,
    val userFacingWordingSha256: String?,
    val mappingIds: Set<String>,
    val rollbackToVersion: String?,
    val modelUsedForDecision: Boolean = false,
    val productionAdmitted: Boolean = false,
) {
    init { require(packId.isNotBlank() && version.isNotBlank()) }
}

@Serializable
data class RulePackLifecycleEvent(
    val sequence: Int,
    val action: RulePackLifecycleAction,
    val occurredAtIsoDate: String,
    val actorSignatureSha256: String,
    val reasonCode: String? = null,
    val incidentId: String? = null,
    val targetVersion: String? = null,
) {
    init { require(sequence > 0) }
}

@Serializable
data class RulePackLifecycleResult(
    val finalState: RulePackReleaseState,
    val blockers: List<String>,
    val appliedSequences: List<Int>,
    val productionAdmitted: Boolean,
)

object RulePackLifecycleResolver {
    fun resolve(
        candidate: RulePackReleaseCandidate,
        events: List<RulePackLifecycleEvent>,
        mappingsById: Map<String, SourceFieldMapping>,
        artifactsById: Map<String, ImmutableSourceArtifact>,
        asOfIsoDate: String,
        production: Boolean,
    ): RulePackLifecycleResult {
        val blockers = mutableListOf<String>()
        val applied = mutableListOf<Int>()
        var state = RulePackReleaseState.DRAFT

        validateCandidate(candidate, asOfIsoDate, production, blockers)
        if (candidate.modelUsedForDecision) blockers += "A model cannot own the release decision."
        if (candidate.productionAdmitted) blockers += "Input manifest cannot self-declare production admission."

        val ordered = events.sortedBy { it.sequence }
        val sequences = ordered.map { it.sequence }
        if (sequences.distinct().size != sequences.size) blockers += "Lifecycle event sequences must be unique."
        if (sequences.isNotEmpty() && sequences != (1..sequences.size).toList()) {
            blockers += "Lifecycle event sequences must be contiguous from 1."
        }

        candidate.mappingIds.forEach { id ->
            val mapping = mappingsById[id]
            if (mapping == null) blockers += "Release candidate references unknown mapping $id."
            else {
                val result = SourceFieldMappingValidator.validate(mapping, artifactsById, asOfIsoDate, production)
                blockers += result.blockers
                if (production && result.admission != SourceLifecycleAdmission.ADMITTED) {
                    blockers += "Mapping $id is not admitted for production."
                }
            }
        }

        val asOf = asOfIsoDate.dateKey()
        var previousDate: Int? = null
        for (event in ordered) {
            val eventDate = event.occurredAtIsoDate.dateKey()
            var valid = true
            if (eventDate == null) { blockers += "Event ${event.sequence} has an invalid date."; valid = false }
            if (!event.actorSignatureSha256.isSha256()) {
                blockers += "Event ${event.sequence} requires actor signature SHA-256."; valid = false
            }
            if (eventDate != null && previousDate != null && eventDate < previousDate) {
                blockers += "Lifecycle event dates must be non-decreasing."; valid = false
            }
            if (eventDate != null && asOf != null && eventDate > asOf) {
                blockers += "Event ${event.sequence} occurs after the as-of date."; valid = false
            }
            if (eventDate != null) previousDate = eventDate
            if (!valid) continue
            transition(state, event, candidate, blockers)?.let {
                state = it
                applied += event.sequence
            }
        }

        val end = candidate.effectiveUntilIsoDate.dateKey()
        if (state == RulePackReleaseState.ACTIVE && asOf != null && end != null && asOf > end) {
            state = RulePackReleaseState.EXPIRED
        }
        if (production && state != RulePackReleaseState.ACTIVE) {
            blockers += "Production resolution requires final ACTIVE state."
        }
        val distinct = blockers.distinct()
        return RulePackLifecycleResult(
            finalState = state,
            blockers = distinct,
            appliedSequences = applied,
            productionAdmitted = production && state == RulePackReleaseState.ACTIVE && distinct.isEmpty(),
        )
    }

    private fun validateCandidate(
        candidate: RulePackReleaseCandidate,
        asOf: String,
        production: Boolean,
        blockers: MutableList<String>,
    ) {
        checkDate("generatedAtIsoDate", candidate.generatedAtIsoDate, blockers)
        checkDate("asOfIsoDate", asOf, blockers)
        checkOptionalDate("effectiveFromIsoDate", candidate.effectiveFromIsoDate, blockers)
        checkOptionalDate("effectiveUntilIsoDate", candidate.effectiveUntilIsoDate, blockers)
        val start = candidate.effectiveFromIsoDate.dateKey()
        val end = candidate.effectiveUntilIsoDate.dateKey()
        if (start != null && end != null && start > end) blockers += "Release effective window is inverted."
        if (production && (start == null || end == null)) blockers += "Production requires a bounded effective window."
        if (candidate.rollbackToVersion.isNullOrBlank() || candidate.rollbackToVersion == candidate.version) {
            blockers += "Release candidate requires a distinct rollback version."
        }
        if (candidate.mappingIds.isEmpty()) blockers += "Release candidate requires exact source mappings."
    }

    private fun transition(
        state: RulePackReleaseState,
        event: RulePackLifecycleEvent,
        candidate: RulePackReleaseCandidate,
        blockers: MutableList<String>,
    ): RulePackReleaseState? {
        fun hasIncident(): Boolean {
            var valid = true
            if (event.reasonCode.isNullOrBlank()) { blockers += "Event ${event.sequence} requires reasonCode."; valid = false }
            if (event.incidentId.isNullOrBlank()) { blockers += "Event ${event.sequence} requires incidentId."; valid = false }
            return valid
        }
        return when (event.action) {
            RulePackLifecycleAction.REVIEW ->
                if (state != RulePackReleaseState.DRAFT) invalid("REVIEW requires DRAFT state.", blockers)
                else if (hasReleaseEvidence(candidate, blockers)) RulePackReleaseState.REVIEWED else null
            RulePackLifecycleAction.STAGE ->
                if (state == RulePackReleaseState.REVIEWED) RulePackReleaseState.STAGED
                else invalid("STAGE requires REVIEWED state.", blockers)
            RulePackLifecycleAction.ACTIVATE -> {
                if (state != RulePackReleaseState.STAGED) invalid("ACTIVATE requires STAGED state.", blockers)
                else {
                    val eventDate = event.occurredAtIsoDate.dateKey()
                    val start = candidate.effectiveFromIsoDate.dateKey()
                    val end = candidate.effectiveUntilIsoDate.dateKey()
                    var valid = true
                    if (eventDate != null && start != null && eventDate < start) {
                        blockers += "Activation is before effectiveFromIsoDate."; valid = false
                    }
                    if (eventDate != null && end != null && eventDate > end) {
                        blockers += "Activation is after effectiveUntilIsoDate."; valid = false
                    }
                    if (valid) RulePackReleaseState.ACTIVE else null
                }
            }
            RulePackLifecycleAction.SUSPEND ->
                if (state == RulePackReleaseState.ACTIVE && hasIncident()) RulePackReleaseState.SUSPENDED
                else if (state != RulePackReleaseState.ACTIVE) invalid("SUSPEND requires ACTIVE state.", blockers) else null
            RulePackLifecycleAction.RESUME ->
                if (state == RulePackReleaseState.SUSPENDED && hasIncident()) RulePackReleaseState.ACTIVE
                else if (state != RulePackReleaseState.SUSPENDED) invalid("RESUME requires SUSPENDED state.", blockers) else null
            RulePackLifecycleAction.REVOKE ->
                if (state in setOf(RulePackReleaseState.REVIEWED, RulePackReleaseState.STAGED,
                        RulePackReleaseState.ACTIVE, RulePackReleaseState.SUSPENDED) && hasIncident()
                ) RulePackReleaseState.REVOKED
                else if (state !in setOf(RulePackReleaseState.REVIEWED, RulePackReleaseState.STAGED,
                        RulePackReleaseState.ACTIVE, RulePackReleaseState.SUSPENDED)
                ) invalid("REVOKE requires REVIEWED, STAGED, ACTIVE, or SUSPENDED state.", blockers) else null
            RulePackLifecycleAction.EXPIRE ->
                if (state in setOf(RulePackReleaseState.ACTIVE, RulePackReleaseState.SUSPENDED)) {
                    RulePackReleaseState.EXPIRED
                } else invalid("EXPIRE requires ACTIVE or SUSPENDED state.", blockers)
            RulePackLifecycleAction.ROLLBACK -> {
                if (state !in setOf(RulePackReleaseState.ACTIVE, RulePackReleaseState.SUSPENDED,
                        RulePackReleaseState.REVOKED, RulePackReleaseState.EXPIRED)
                ) invalid("ROLLBACK requires ACTIVE, SUSPENDED, REVOKED, or EXPIRED state.", blockers)
                else {
                    var valid = hasIncident()
                    if (event.targetVersion != candidate.rollbackToVersion) {
                        blockers += "Rollback target must equal rollbackToVersion."; valid = false
                    }
                    if (valid) RulePackReleaseState.ROLLED_BACK else null
                }
            }
        }
    }

    private fun hasReleaseEvidence(
        candidate: RulePackReleaseCandidate,
        blockers: MutableList<String>,
    ): Boolean {
        val before = blockers.size
        if (!candidate.contentSha256.isSha256()) blockers += "Reviewed release requires content SHA-256."
        if (!candidate.sourceBundleSha256.isSha256()) blockers += "Reviewed release requires source-bundle SHA-256."
        if (!candidate.testSuiteSha256.isSha256()) blockers += "Reviewed release requires test-suite SHA-256."
        if (!candidate.reviewerAttestationSha256.isSha256()) blockers += "Reviewed release requires reviewer attestation SHA-256."
        if (!candidate.userFacingWordingSha256.isSha256()) blockers += "Reviewed release requires wording SHA-256."
        return blockers.size == before
    }
}

private fun invalid(message: String, blockers: MutableList<String>): RulePackReleaseState? {
    blockers += message
    return null
}

private fun checkDate(label: String, value: String, blockers: MutableList<String>) {
    if (value.dateKey() == null) blockers += "$label must be a valid YYYY-MM-DD date."
}

private fun checkOptionalDate(label: String, value: String?, blockers: MutableList<String>) {
    if (value != null && value.dateKey() == null) blockers += "$label must be a valid YYYY-MM-DD date."
}

private fun String?.isSha256(): Boolean = this != null && sourceSha256.matches(this)

private fun String?.isContentAddressed(expectedSha256: String?): Boolean =
    this != null && expectedSha256 != null && sourceArchive.matches(this) && contains(expectedSha256)

private fun String?.dateKey(): Int? {
    if (this == null) return null
    val parts = split("-")
    if (parts.size != 3 || parts[0].length != 4 || parts[1].length != 2 || parts[2].length != 2) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    if (month !in 1..12) return null
    val maxDay = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)) 29 else 28
        else -> return null
    }
    if (day !in 1..maxDay) return null
    return year * 10_000 + month * 100 + day
}
