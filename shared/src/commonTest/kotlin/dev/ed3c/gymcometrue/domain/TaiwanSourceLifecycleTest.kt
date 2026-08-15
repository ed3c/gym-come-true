package dev.ed3c.gymcometrue.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaiwanSourceLifecycleTest {
    private fun hash(character: Char): String = character.toString().repeat(64)

    private fun candidateArtifact(): ImmutableSourceArtifact =
        ImmutableSourceArtifact(
            snapshotId = "candidate-source",
            sourceId = "tfda-candidate",
            state = SourceSnapshotState.CANDIDATE,
            artifactKind = SourceArtifactKind.CSV,
            canonicalUrl = "https://example.test/source",
            retrievalUrl = "https://example.test/source.csv",
            capturedAtIsoDate = "2026-08-15",
            mediaType = "text/csv",
            licenseId = "OGL-TW-1.0",
            attributionText = "Synthetic test publisher",
            redistributable = false,
            productionUse = ProductionEvidenceUse.DENY,
            note = "Candidate-only test record.",
        )

    private fun syntheticArtifact(): ImmutableSourceArtifact {
        val sha = hash('a')
        return ImmutableSourceArtifact(
            snapshotId = "synthetic-snapshot",
            sourceId = "synthetic-source",
            state = SourceSnapshotState.HASH_VERIFIED,
            artifactKind = SourceArtifactKind.TEXT,
            canonicalUrl = "repo://synthetic/source",
            capturedAtIsoDate = "2026-08-15",
            mediaType = "text/plain",
            byteLength = 128,
            sha256 = sha,
            archiveUri = "repo://fixtures/synthetic.txt#sha256=$sha",
            licenseId = "REPOSITORY_SYNTHETIC",
            attributionText = "Repository-authored synthetic fixture",
            redistributable = true,
            synthetic = true,
            productionUse = ProductionEvidenceUse.TEST_ONLY,
            note = "No regulator or product claim.",
        )
    }

    private fun productionArtifact(): ImmutableSourceArtifact {
        val sha = hash('b')
        return ImmutableSourceArtifact(
            snapshotId = "reviewed-source-snapshot",
            sourceId = "reviewed-source",
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
            legalReviewRef = "legal-review-${hash('c')}",
            productionUse = ProductionEvidenceUse.ALLOW,
            note = "In-memory production-contract fixture only.",
        )
    }

    private fun mappingFor(
        artifact: ImmutableSourceArtifact,
        productionUse: ProductionEvidenceUse,
        claimScope: SourceClaimScope = SourceClaimScope.LABEL_SCHEMA,
        reviewerHash: String? = null,
    ): SourceFieldMapping =
        SourceFieldMapping(
            mappingId = "mapping-${artifact.snapshotId}-${claimScope.name}",
            sourceId = artifact.sourceId,
            status = SourceMappingStatus.VERIFIED,
            snapshotId = artifact.snapshotId,
            claimScope = claimScope,
            selector = ExactSourceSelector(
                kind = SourceSelectorKind.TEXT_RANGE,
                locator = "lines",
                lineStart = 1,
                lineEnd = 2,
            ),
            targetField = "label.serving.unit",
            transform = DeterministicSourceTransform.EXACT_COPY,
            evidenceExcerptSha256 = hash('d'),
            qualifiedReviewerAttestationSha256 = reviewerHash,
            productionUse = productionUse,
            note = "Exact test mapping.",
        )

    private fun releaseCandidate(
        mappingId: String,
        modelUsedForDecision: Boolean = false,
        effectiveUntil: String = "2027-08-01",
    ): RulePackReleaseCandidate =
        RulePackReleaseCandidate(
            packId = "tw-supplement-safety",
            version = "1.0.0",
            generatedAtIsoDate = "2026-08-15",
            effectiveFromIsoDate = "2026-08-15",
            effectiveUntilIsoDate = effectiveUntil,
            contentSha256 = hash('e'),
            sourceBundleSha256 = hash('f'),
            testSuiteSha256 = hash('1'),
            reviewerAttestationSha256 = hash('2'),
            userFacingWordingSha256 = hash('3'),
            mappingIds = setOf(mappingId),
            rollbackToVersion = "0.9.0",
            modelUsedForDecision = modelUsedForDecision,
            productionAdmitted = false,
        )

    private fun activationEvents(): List<RulePackLifecycleEvent> =
        listOf(
            RulePackLifecycleEvent(
                sequence = 1,
                action = RulePackLifecycleAction.REVIEW,
                occurredAtIsoDate = "2026-08-15",
                actorSignatureSha256 = hash('4'),
            ),
            RulePackLifecycleEvent(
                sequence = 2,
                action = RulePackLifecycleAction.STAGE,
                occurredAtIsoDate = "2026-08-15",
                actorSignatureSha256 = hash('5'),
            ),
            RulePackLifecycleEvent(
                sequence = 3,
                action = RulePackLifecycleAction.ACTIVATE,
                occurredAtIsoDate = "2026-08-15",
                actorSignatureSha256 = hash('6'),
            ),
        )

    @Test
    fun candidateWithoutHashRequiresReview() {
        val result = ImmutableSourceArtifactValidator.validate(
            artifact = candidateArtifact(),
            asOfIsoDate = "2026-08-15",
            production = false,
        )

        assertEquals(SourceLifecycleAdmission.REVIEW_REQUIRED, result.admission)
        assertTrue(result.blockers.isEmpty())
        assertTrue(result.reviewNotes.any { it.contains("candidate", ignoreCase = true) })
    }

    @Test
    fun syntheticHashVerifiedArtifactIsTestOnly() {
        val result = ImmutableSourceArtifactValidator.validate(
            artifact = syntheticArtifact(),
            asOfIsoDate = "2026-08-15",
            production = false,
        )

        assertEquals(SourceLifecycleAdmission.TEST_ONLY, result.admission)
        assertTrue(result.blockers.isEmpty())
    }

    @Test
    fun productionArtifactRequiresLegalReviewAndNonSyntheticEvidence() {
        val invalid = syntheticArtifact().copy(
            productionUse = ProductionEvidenceUse.ALLOW,
            state = SourceSnapshotState.HASH_VERIFIED,
        )

        val result = ImmutableSourceArtifactValidator.validate(
            artifact = invalid,
            asOfIsoDate = "2026-08-15",
            production = true,
        )

        assertEquals(SourceLifecycleAdmission.REJECTED, result.admission)
        assertTrue(result.blockers.any { it.contains("synthetic", ignoreCase = true) })
        assertTrue(result.blockers.any { it.contains("LEGAL_REVIEWED") || it.contains("legally reviewed") })
    }

    @Test
    fun verifiedMappingRejectsUnknownSnapshot() {
        val mapping = mappingFor(
            artifact = syntheticArtifact(),
            productionUse = ProductionEvidenceUse.TEST_ONLY,
        ).copy(snapshotId = "missing-snapshot")

        val result = SourceFieldMappingValidator.validate(
            mapping = mapping,
            artifactsById = emptyMap(),
            asOfIsoDate = "2026-08-15",
            production = false,
        )

        assertEquals(SourceLifecycleAdmission.REJECTED, result.admission)
        assertTrue(result.blockers.any { it.contains("unknown snapshot") })
    }

    @Test
    fun pdfSelectorRequiresPageAndLineRange() {
        val artifact = syntheticArtifact()
        val mapping = mappingFor(
            artifact = artifact,
            productionUse = ProductionEvidenceUse.TEST_ONLY,
        ).copy(
            selector = ExactSourceSelector(
                kind = SourceSelectorKind.PDF_PAGE_LINE,
                locator = "supplement-label-requirements",
                pageNumber = 1,
            ),
        )

        val result = SourceFieldMappingValidator.validate(
            mapping = mapping,
            artifactsById = mapOf(artifact.snapshotId to artifact),
            asOfIsoDate = "2026-08-15",
            production = false,
        )

        assertEquals(SourceLifecycleAdmission.REJECTED, result.admission)
        assertTrue(result.blockers.any { it.contains("pageNumber, lineStart, and lineEnd") })
    }

    @Test
    fun referenceValueMappingRequiresQualifiedReviewInProduction() {
        val artifact = productionArtifact()
        val mapping = mappingFor(
            artifact = artifact,
            productionUse = ProductionEvidenceUse.ALLOW,
            claimScope = SourceClaimScope.REFERENCE_VALUE,
            reviewerHash = null,
        )

        val result = SourceFieldMappingValidator.validate(
            mapping = mapping,
            artifactsById = mapOf(artifact.snapshotId to artifact),
            asOfIsoDate = "2026-08-15",
            production = true,
        )

        assertEquals(SourceLifecycleAdmission.REJECTED, result.admission)
        assertTrue(result.blockers.any { it.contains("qualified reviewer") })
    }

    @Test
    fun lifecycleRejectsSkippingReviewAndStage() {
        val artifact = productionArtifact()
        val mapping = mappingFor(
            artifact = artifact,
            productionUse = ProductionEvidenceUse.ALLOW,
        )
        val candidate = releaseCandidate(mapping.mappingId)
        val result = RulePackLifecycleResolver.resolve(
            candidate = candidate,
            events = listOf(
                RulePackLifecycleEvent(
                    sequence = 1,
                    action = RulePackLifecycleAction.ACTIVATE,
                    occurredAtIsoDate = "2026-08-15",
                    actorSignatureSha256 = hash('7'),
                ),
            ),
            mappingsById = mapOf(mapping.mappingId to mapping),
            artifactsById = mapOf(artifact.snapshotId to artifact),
            asOfIsoDate = "2026-08-15",
            production = true,
        )

        assertEquals(RulePackReleaseState.DRAFT, result.finalState)
        assertFalse(result.productionAdmitted)
        assertTrue(result.blockers.any { it.contains("ACTIVATE requires STAGED") })
    }

    @Test
    fun completeReviewedLifecycleCanReachActive() {
        val artifact = productionArtifact()
        val mapping = mappingFor(
            artifact = artifact,
            productionUse = ProductionEvidenceUse.ALLOW,
        )
        val candidate = releaseCandidate(mapping.mappingId)

        val result = RulePackLifecycleResolver.resolve(
            candidate = candidate,
            events = activationEvents(),
            mappingsById = mapOf(mapping.mappingId to mapping),
            artifactsById = mapOf(artifact.snapshotId to artifact),
            asOfIsoDate = "2026-08-15",
            production = true,
        )

        assertEquals(RulePackReleaseState.ACTIVE, result.finalState)
        assertTrue(result.blockers.isEmpty())
        assertTrue(result.productionAdmitted)
        assertEquals(listOf(1, 2, 3), result.appliedSequences)
    }

    @Test
    fun revocationLocksReleaseOutOfProduction() {
        val artifact = productionArtifact()
        val mapping = mappingFor(
            artifact = artifact,
            productionUse = ProductionEvidenceUse.ALLOW,
        )
        val events = activationEvents() + RulePackLifecycleEvent(
            sequence = 4,
            action = RulePackLifecycleAction.REVOKE,
            occurredAtIsoDate = "2026-08-16",
            actorSignatureSha256 = hash('8'),
            reasonCode = "SOURCE_WITHDRAWN",
            incidentId = "INC-2026-001",
        )

        val result = RulePackLifecycleResolver.resolve(
            candidate = releaseCandidate(mapping.mappingId),
            events = events,
            mappingsById = mapOf(mapping.mappingId to mapping),
            artifactsById = mapOf(artifact.snapshotId to artifact),
            asOfIsoDate = "2026-08-16",
            production = true,
        )

        assertEquals(RulePackReleaseState.REVOKED, result.finalState)
        assertFalse(result.productionAdmitted)
        assertTrue(result.blockers.any { it.contains("final ACTIVE state") })
    }

    @Test
    fun rollbackRequiresExactTargetVersion() {
        val artifact = productionArtifact()
        val mapping = mappingFor(
            artifact = artifact,
            productionUse = ProductionEvidenceUse.ALLOW,
        )
        val events = activationEvents() + RulePackLifecycleEvent(
            sequence = 4,
            action = RulePackLifecycleAction.ROLLBACK,
            occurredAtIsoDate = "2026-08-16",
            actorSignatureSha256 = hash('9'),
            reasonCode = "INCIDENT_ROLLBACK",
            incidentId = "INC-2026-002",
            targetVersion = "0.8.0",
        )

        val result = RulePackLifecycleResolver.resolve(
            candidate = releaseCandidate(mapping.mappingId),
            events = events,
            mappingsById = mapOf(mapping.mappingId to mapping),
            artifactsById = mapOf(artifact.snapshotId to artifact),
            asOfIsoDate = "2026-08-16",
            production = true,
        )

        assertEquals(RulePackReleaseState.ACTIVE, result.finalState)
        assertFalse(result.productionAdmitted)
        assertTrue(result.blockers.any { it.contains("rollbackToVersion") })
        assertEquals(listOf(1, 2, 3), result.appliedSequences)
    }

    @Test
    fun modelOwnedReleaseDecisionIsRejected() {
        val artifact = productionArtifact()
        val mapping = mappingFor(
            artifact = artifact,
            productionUse = ProductionEvidenceUse.ALLOW,
        )

        val result = RulePackLifecycleResolver.resolve(
            candidate = releaseCandidate(
                mappingId = mapping.mappingId,
                modelUsedForDecision = true,
            ),
            events = activationEvents(),
            mappingsById = mapOf(mapping.mappingId to mapping),
            artifactsById = mapOf(artifact.snapshotId to artifact),
            asOfIsoDate = "2026-08-15",
            production = true,
        )

        assertFalse(result.productionAdmitted)
        assertTrue(result.blockers.any { it.contains("model", ignoreCase = true) })
    }

    @Test
    fun activeReleaseExpiresAtTheEndOfItsWindow() {
        val artifact = productionArtifact()
        val mapping = mappingFor(
            artifact = artifact,
            productionUse = ProductionEvidenceUse.ALLOW,
        )

        val result = RulePackLifecycleResolver.resolve(
            candidate = releaseCandidate(
                mappingId = mapping.mappingId,
                effectiveUntil = "2026-08-20",
            ),
            events = activationEvents(),
            mappingsById = mapOf(mapping.mappingId to mapping),
            artifactsById = mapOf(artifact.snapshotId to artifact),
            asOfIsoDate = "2026-08-21",
            production = true,
        )

        assertEquals(RulePackReleaseState.EXPIRED, result.finalState)
        assertFalse(result.productionAdmitted)
        assertTrue(result.blockers.any { it.contains("final ACTIVE state") })
    }
}
