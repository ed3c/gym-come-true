package dev.ed3c.gymcometrue.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaiwanSourceLifecycleHardeningTest {
    private fun hash(character: Char): String = character.toString().repeat(64)

    private fun productionArtifact(): ImmutableSourceArtifact {
        val sha = hash('a')
        return ImmutableSourceArtifact(
            snapshotId = "reviewed-source-snapshot-hardening",
            sourceId = "reviewed-source-hardening",
            state = SourceSnapshotState.LEGAL_REVIEWED,
            artifactKind = SourceArtifactKind.PDF,
            canonicalUrl = "https://example.test/regulator/source",
            retrievalUrl = "https://example.test/regulator/source.pdf",
            capturedAtIsoDate = "2026-08-15",
            sourceModifiedAtIsoDate = "2026-08-01",
            effectiveFromIsoDate = "2026-08-01",
            effectiveUntilIsoDate = "2027-08-01",
            mediaType = "application/pdf",
            byteLength = 2048,
            sha256 = sha,
            archiveUri = "evidence://tw/sources/$sha",
            licenseId = "LEGAL-REVIEWED-TEST",
            attributionText = "Qualified synthetic test publisher",
            redistributable = false,
            synthetic = false,
            legalReviewRef = "legal-review-${hash('b')}",
            productionUse = ProductionEvidenceUse.ALLOW,
            note = "In-memory contract fixture only.",
        )
    }

    private fun productionMapping(
        artifact: ImmutableSourceArtifact,
        status: SourceMappingStatus = SourceMappingStatus.VERIFIED,
        sourceId: String = artifact.sourceId,
        productionUse: ProductionEvidenceUse = ProductionEvidenceUse.ALLOW,
    ): SourceFieldMapping =
        SourceFieldMapping(
            mappingId = "hardening-${artifact.snapshotId}",
            sourceId = sourceId,
            status = status,
            snapshotId = artifact.snapshotId,
            claimScope = SourceClaimScope.LABEL_SCHEMA,
            selector = ExactSourceSelector(
                kind = SourceSelectorKind.TEXT_RANGE,
                locator = "lines",
                lineStart = 1,
                lineEnd = 2,
            ),
            targetField = "label.serving.definition",
            transform = DeterministicSourceTransform.EXACT_COPY,
            evidenceExcerptSha256 = hash('c'),
            productionUse = productionUse,
            note = "Exact hardening mapping.",
        )

    private fun releaseCandidate(
        mappingId: String,
        productionAdmitted: Boolean = false,
    ): RulePackReleaseCandidate =
        RulePackReleaseCandidate(
            packId = "tw-supplement-safety-hardening",
            version = "1.0.0",
            generatedAtIsoDate = "2026-08-15",
            effectiveFromIsoDate = "2026-08-15",
            effectiveUntilIsoDate = "2026-08-31",
            contentSha256 = hash('d'),
            sourceBundleSha256 = hash('e'),
            testSuiteSha256 = hash('f'),
            reviewerAttestationSha256 = hash('1'),
            userFacingWordingSha256 = hash('2'),
            mappingIds = setOf(mappingId),
            rollbackToVersion = "0.9.0",
            modelUsedForDecision = false,
            productionAdmitted = productionAdmitted,
        )

    private fun activationEvents(
        activationDate: String = "2026-08-15",
    ): List<RulePackLifecycleEvent> =
        listOf(
            RulePackLifecycleEvent(
                sequence = 1,
                action = RulePackLifecycleAction.REVIEW,
                occurredAtIsoDate = "2026-08-15",
                actorSignatureSha256 = hash('3'),
            ),
            RulePackLifecycleEvent(
                sequence = 2,
                action = RulePackLifecycleAction.STAGE,
                occurredAtIsoDate = "2026-08-15",
                actorSignatureSha256 = hash('4'),
            ),
            RulePackLifecycleEvent(
                sequence = 3,
                action = RulePackLifecycleAction.ACTIVATE,
                occurredAtIsoDate = activationDate,
                actorSignatureSha256 = hash('5'),
            ),
        )

    @Test
    fun archiveUriMustContainTheArtifactHash() {
        val artifact = productionArtifact().copy(
            archiveUri = "evidence://tw/sources/${hash('9')}",
        )

        val result = ImmutableSourceArtifactValidator.validate(
            artifact = artifact,
            asOfIsoDate = "2026-08-15",
            production = true,
        )

        assertEquals(SourceLifecycleAdmission.REJECTED, result.admission)
        assertTrue(result.blockers.any { it.contains("content-addressed archive URI") })
    }

    @Test
    fun exactMappingMustMatchTheSnapshotSourceIdentity() {
        val artifact = productionArtifact()
        val mapping = productionMapping(
            artifact = artifact,
            sourceId = "different-source",
        )

        val result = SourceFieldMappingValidator.validate(
            mapping = mapping,
            artifactsById = mapOf(artifact.snapshotId to artifact),
            asOfIsoDate = "2026-08-15",
            production = true,
        )

        assertEquals(SourceLifecycleAdmission.REJECTED, result.admission)
        assertTrue(result.blockers.any { it.contains("sourceId does not match") })
    }

    @Test
    fun testOnlyMappingCannotRemainDraft() {
        val artifact = productionArtifact().copy(
            state = SourceSnapshotState.HASH_VERIFIED,
            synthetic = true,
            legalReviewRef = null,
            productionUse = ProductionEvidenceUse.TEST_ONLY,
            canonicalUrl = "repo://synthetic/source",
        )
        val mapping = productionMapping(
            artifact = artifact,
            status = SourceMappingStatus.DRAFT,
            productionUse = ProductionEvidenceUse.TEST_ONLY,
        )

        val result = SourceFieldMappingValidator.validate(
            mapping = mapping,
            artifactsById = mapOf(artifact.snapshotId to artifact),
            asOfIsoDate = "2026-08-15",
            production = false,
        )

        assertEquals(SourceLifecycleAdmission.REJECTED, result.admission)
        assertTrue(result.blockers.any { it.contains("must be VERIFIED") })
    }

    @Test
    fun futureLifecycleEventIsNotApplied() {
        val artifact = productionArtifact()
        val mapping = productionMapping(artifact)
        val result = RulePackLifecycleResolver.resolve(
            candidate = releaseCandidate(mapping.mappingId),
            events = activationEvents(activationDate = "2026-08-16"),
            mappingsById = mapOf(mapping.mappingId to mapping),
            artifactsById = mapOf(artifact.snapshotId to artifact),
            asOfIsoDate = "2026-08-15",
            production = true,
        )

        assertEquals(RulePackReleaseState.STAGED, result.finalState)
        assertEquals(listOf(1, 2), result.appliedSequences)
        assertFalse(result.productionAdmitted)
        assertTrue(result.blockers.any { it.contains("after the as-of date") })
    }

    @Test
    fun validRevocationCanRollBackToTheDeclaredVersion() {
        val artifact = productionArtifact()
        val mapping = productionMapping(artifact)
        val events = activationEvents() + listOf(
            RulePackLifecycleEvent(
                sequence = 4,
                action = RulePackLifecycleAction.REVOKE,
                occurredAtIsoDate = "2026-08-16",
                actorSignatureSha256 = hash('6'),
                reasonCode = "SOURCE_WITHDRAWN",
                incidentId = "INC-2026-008B-01",
            ),
            RulePackLifecycleEvent(
                sequence = 5,
                action = RulePackLifecycleAction.ROLLBACK,
                occurredAtIsoDate = "2026-08-16",
                actorSignatureSha256 = hash('7'),
                reasonCode = "INCIDENT_ROLLBACK",
                incidentId = "INC-2026-008B-01",
                targetVersion = "0.9.0",
            ),
        )

        val result = RulePackLifecycleResolver.resolve(
            candidate = releaseCandidate(mapping.mappingId),
            events = events,
            mappingsById = mapOf(mapping.mappingId to mapping),
            artifactsById = mapOf(artifact.snapshotId to artifact),
            asOfIsoDate = "2026-08-16",
            production = false,
        )

        assertEquals(RulePackReleaseState.ROLLED_BACK, result.finalState)
        assertTrue(result.blockers.isEmpty())
        assertFalse(result.productionAdmitted)
        assertEquals(listOf(1, 2, 3, 4, 5), result.appliedSequences)
    }

    @Test
    fun manifestCannotSelfDeclareProductionAdmission() {
        val artifact = productionArtifact()
        val mapping = productionMapping(artifact)
        val result = RulePackLifecycleResolver.resolve(
            candidate = releaseCandidate(
                mappingId = mapping.mappingId,
                productionAdmitted = true,
            ),
            events = activationEvents(),
            mappingsById = mapOf(mapping.mappingId to mapping),
            artifactsById = mapOf(artifact.snapshotId to artifact),
            asOfIsoDate = "2026-08-15",
            production = true,
        )

        assertEquals(RulePackReleaseState.ACTIVE, result.finalState)
        assertFalse(result.productionAdmitted)
        assertTrue(result.blockers.any { it.contains("self-declare") })
    }

    @Test
    fun lifecycleSequencesMustBeUniqueAndContiguous() {
        val artifact = productionArtifact()
        val mapping = productionMapping(artifact)
        val events = listOf(
            RulePackLifecycleEvent(
                sequence = 1,
                action = RulePackLifecycleAction.REVIEW,
                occurredAtIsoDate = "2026-08-15",
                actorSignatureSha256 = hash('3'),
            ),
            RulePackLifecycleEvent(
                sequence = 3,
                action = RulePackLifecycleAction.STAGE,
                occurredAtIsoDate = "2026-08-15",
                actorSignatureSha256 = hash('4'),
            ),
            RulePackLifecycleEvent(
                sequence = 3,
                action = RulePackLifecycleAction.ACTIVATE,
                occurredAtIsoDate = "2026-08-15",
                actorSignatureSha256 = hash('5'),
            ),
        )

        val result = RulePackLifecycleResolver.resolve(
            candidate = releaseCandidate(mapping.mappingId),
            events = events,
            mappingsById = mapOf(mapping.mappingId to mapping),
            artifactsById = mapOf(artifact.snapshotId to artifact),
            asOfIsoDate = "2026-08-15",
            production = false,
        )

        assertTrue(result.blockers.any { it.contains("unique") })
        assertTrue(result.blockers.any { it.contains("contiguous") })
    }
}
