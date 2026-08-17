package dev.ed3c.gymcometrue.release

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReleaseConvergenceTest {
    private val head = "a".repeat(40)
    private val sliceHead = "b".repeat(40)
    private val admittedHeads = setOf(head, sliceHead)

    private fun evidenceHash(character: Char): String = character.toString().repeat(64)

    private fun target(
        platform: ReleasePlatform,
        convergenceHeadSha: String = head,
        gates: List<ReleaseGateEvidence> = ReleaseConvergenceChecker.absentGateSet(),
        artifactSha256: String? = null,
    ) = ReleaseTarget(platform, convergenceHeadSha, artifactSha256, gates)

    private fun manifest(
        targets: List<ReleaseTarget> = ReleasePlatform.entries.map { target(it) },
        admittedSliceHeadShas: List<String> = listOf(sliceHead),
        rollbackToVersion: String? = "0.9.0",
        productionAdmitted: Boolean = false,
        convergenceHeadSha: String = head,
    ) = ReleaseManifest(
        releaseId = "store-release-candidate-1",
        version = "1.0.0",
        convergenceHeadSha = convergenceHeadSha,
        admittedSliceHeadShas = admittedSliceHeadShas,
        targets = targets,
        rollbackToVersion = rollbackToVersion,
        productionAdmitted = productionAdmitted,
    )

    private fun satisfiedGates(): List<ReleaseGateEvidence> = ReleaseConvergenceChecker.absentGateSet().map {
        it.copy(
            state = if (it.humanAdmitRequired) GateEvidenceState.VERIFIED else GateEvidenceState.ADMITTED,
            evidenceRef = "receipt/${it.gate}",
            evidenceSha256 = evidenceHash('c'),
        )
    }

    @Test
    fun todaysManifestIsBlockedByExternalGates() {
        val result = ReleaseConvergenceChecker.check(manifest(), admittedHeads)
        assertEquals(ReleaseReadiness.BLOCKED_EXTERNAL_GATES, result.readiness)
        assertTrue(result.blockers.isEmpty(), result.blockers.toString())
        assertEquals(ReleasePlatform.entries.size * ReleaseGate.entries.size, result.absentGates.size)
        listOf("CODE_SIGNING", "STORE_LISTING", "PROVIDER_VERIFICATION", "HOSTED_EXACT_HEAD_CHECK").forEach { gate ->
            assertTrue(result.absentGates.any { it.contains(gate) && it.endsWith("ABSENT") }, "missing $gate")
        }
        assertTrue(result.humanAdmitGates.any { it.contains("HUMAN_SUBMISSION_ADMIT") })
    }

    @Test
    fun manifestCannotSelfDeclareProductionAdmission() {
        val result = ReleaseConvergenceChecker.check(manifest(productionAdmitted = true), admittedHeads)
        assertEquals(ReleaseReadiness.REJECTED, result.readiness)
        assertTrue(result.blockers.any { it.contains("cannot self-declare production admission") })
    }

    @Test
    fun aTargetConvergingFromAnotherHeadIsRejected() {
        val targets = listOf(
            target(ReleasePlatform.ANDROID),
            target(ReleasePlatform.IOS, convergenceHeadSha = "c".repeat(40)),
            target(ReleasePlatform.WEB),
        )
        val result = ReleaseConvergenceChecker.check(manifest(targets = targets), admittedHeads)
        assertEquals(ReleaseReadiness.REJECTED, result.readiness)
        assertTrue(result.blockers.any { it.contains("stale parent") })
    }

    @Test
    fun anUnadmittedSliceHeadIsRejected() {
        val result = ReleaseConvergenceChecker.check(
            manifest(admittedSliceHeadShas = listOf("d".repeat(40))),
            admittedHeads,
        )
        assertEquals(ReleaseReadiness.REJECTED, result.readiness)
        assertTrue(result.blockers.any { it.contains("not in the admitted head set") })
    }

    @Test
    fun everyPlatformMustBeDeclaredExactlyOnce() {
        val missing = ReleaseConvergenceChecker.check(
            manifest(targets = listOf(target(ReleasePlatform.ANDROID), target(ReleasePlatform.IOS))),
            admittedHeads,
        )
        assertEquals(ReleaseReadiness.REJECTED, missing.readiness)
        assertTrue(missing.blockers.any { it.contains("declares no WEB target") })

        val duplicated = ReleaseConvergenceChecker.check(
            manifest(targets = ReleasePlatform.entries.map { target(it) } + target(ReleasePlatform.WEB)),
            admittedHeads,
        )
        assertEquals(ReleaseReadiness.REJECTED, duplicated.readiness)
        assertTrue(duplicated.blockers.any { it.contains("declares WEB more than once") })
    }

    @Test
    fun anUndeclaredGateIsRejectedRatherThanAssumedPassing() {
        val partial = ReleaseConvergenceChecker.absentGateSet().filter { it.gate != ReleaseGate.SBOM }
        val result = ReleaseConvergenceChecker.check(
            manifest(
                targets = listOf(
                    target(ReleasePlatform.ANDROID, gates = partial),
                    target(ReleasePlatform.IOS),
                    target(ReleasePlatform.WEB),
                ),
            ),
            admittedHeads,
        )
        assertEquals(ReleaseReadiness.REJECTED, result.readiness)
        assertTrue(result.blockers.any { it.contains("does not declare gate SBOM") })
    }

    @Test
    fun aPassingGateWithoutAHashIsRejected() {
        val unhashed = satisfiedGates().map {
            if (it.gate == ReleaseGate.VULNERABILITY_SCAN) it.copy(evidenceSha256 = null) else it
        }
        val result = ReleaseConvergenceChecker.check(
            manifest(
                targets = listOf(
                    target(ReleasePlatform.ANDROID, gates = unhashed),
                    target(ReleasePlatform.IOS, gates = satisfiedGates()),
                    target(ReleasePlatform.WEB, gates = satisfiedGates()),
                ),
            ),
            admittedHeads,
        )
        assertEquals(ReleaseReadiness.REJECTED, result.readiness)
        assertTrue(result.blockers.any { it.contains("without an evidence hash") })
    }

    @Test
    fun aHumanAdmitGateCannotBeAdmittedByAManifest() {
        val selfAdmitted = satisfiedGates().map {
            if (it.gate == ReleaseGate.HUMAN_SUBMISSION_ADMIT) it.copy(state = GateEvidenceState.ADMITTED) else it
        }
        val result = ReleaseConvergenceChecker.check(
            manifest(
                targets = listOf(
                    target(ReleasePlatform.ANDROID, gates = selfAdmitted),
                    target(ReleasePlatform.IOS, gates = satisfiedGates()),
                    target(ReleasePlatform.WEB, gates = satisfiedGates()),
                ),
            ),
            admittedHeads,
        )
        assertEquals(ReleaseReadiness.REJECTED, result.readiness)
        assertTrue(result.blockers.any { it.contains("cannot be admitted by a manifest") })
    }

    @Test
    fun aDraftGateIsNotEvidenceOfPassing() {
        val drafted = satisfiedGates().map {
            if (it.gate == ReleaseGate.ROLLBACK_RUNBOOK) {
                it.copy(state = GateEvidenceState.DRAFT, evidenceRef = null, evidenceSha256 = null)
            } else {
                it
            }
        }
        val result = ReleaseConvergenceChecker.check(
            manifest(
                targets = listOf(
                    target(ReleasePlatform.ANDROID, gates = drafted),
                    target(ReleasePlatform.IOS, gates = satisfiedGates()),
                    target(ReleasePlatform.WEB, gates = satisfiedGates()),
                ),
            ),
            admittedHeads,
        )
        assertEquals(ReleaseReadiness.BLOCKED_EXTERNAL_GATES, result.readiness)
        assertEquals(listOf("ANDROID:ROLLBACK_RUNBOOK=DRAFT"), result.absentGates)
    }

    @Test
    fun aFullyEvidencedManifestStopsAtReleaseCandidateDraft() {
        val result = ReleaseConvergenceChecker.check(
            manifest(targets = ReleasePlatform.entries.map { target(it, gates = satisfiedGates()) }),
            admittedHeads,
        )
        assertEquals(ReleaseReadiness.RELEASE_CANDIDATE_DRAFT, result.readiness)
        assertTrue(result.blockers.isEmpty())
        assertTrue(result.absentGates.isEmpty())
        assertEquals(
            ReleasePlatform.entries.size * 4,
            result.humanAdmitGates.size,
            "human admit gates must survive a fully evidenced manifest",
        )
    }

    @Test
    fun aReleaseCandidateRequiresADistinctRollbackVersion() {
        val absent = ReleaseConvergenceChecker.check(manifest(rollbackToVersion = null), admittedHeads)
        assertEquals(ReleaseReadiness.REJECTED, absent.readiness)
        val same = ReleaseConvergenceChecker.check(manifest(rollbackToVersion = "1.0.0"), admittedHeads)
        assertEquals(ReleaseReadiness.REJECTED, same.readiness)
        assertTrue(same.blockers.any { it.contains("distinct rollback version") })
    }
}
