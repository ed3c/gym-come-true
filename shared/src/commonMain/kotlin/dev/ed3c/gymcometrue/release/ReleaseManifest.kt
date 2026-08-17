package dev.ed3c.gymcometrue.release

import kotlinx.serialization.Serializable

private val sha256Pattern = Regex("^[0-9a-f]{64}$")
private val commitShaPattern = Regex("^[0-9a-f]{40}$")

@Serializable
enum class ReleasePlatform { ANDROID, IOS, WEB }

/**
 * Every gate a store release candidate has to pass. None of them can be satisfied by writing code in
 * this repository, so each one is declared explicitly and defaults to an absent evidence state.
 */
@Serializable
enum class ReleaseGate {
    ARTIFACT_BUILD,
    CODE_SIGNING,
    PROVENANCE_ATTESTATION,
    SBOM,
    VULNERABILITY_SCAN,
    STORE_LISTING,
    ACCESSIBILITY_REVIEW,
    LOCALIZATION_REVIEW,
    PERFORMANCE_MEASUREMENT,
    OFFLINE_BEHAVIOUR,
    UPGRADE_MIGRATION,
    SUPPORT_RUNBOOK,
    INCIDENT_RUNBOOK,
    ROLLBACK_RUNBOOK,
    PROVIDER_VERIFICATION,
    PRIVACY_REVIEW,
    CLINICAL_REVIEW,
    HOSTED_EXACT_HEAD_CHECK,
    HUMAN_SUBMISSION_ADMIT,
}

/** Subset of the repository evidence vocabulary that a release gate can carry. */
@Serializable
enum class GateEvidenceState { ABSENT, NOT_IMPLEMENTED, NOT_EXERCISED, BLOCKED, DRAFT, VERIFIED, ADMITTED }

@Serializable
data class ReleaseGateEvidence(
    val gate: ReleaseGate,
    val state: GateEvidenceState,
    val evidenceRef: String? = null,
    val evidenceSha256: String? = null,
    val humanAdmitRequired: Boolean = false,
    val note: String,
) {
    init { require(note.isNotBlank()) }
}

@Serializable
data class ReleaseTarget(
    val platform: ReleasePlatform,
    val convergenceHeadSha: String,
    val artifactSha256: String? = null,
    val gates: List<ReleaseGateEvidence>,
)

/**
 * A convergence manifest for one store release candidate.
 *
 * [productionAdmitted] exists so a manifest can be rejected for claiming it: an input document can
 * never promote itself, exactly as the Taiwan rule-pack lifecycle refuses a self-declared release.
 */
@Serializable
data class ReleaseManifest(
    val releaseId: String,
    val version: String,
    val convergenceHeadSha: String,
    val admittedSliceHeadShas: List<String>,
    val targets: List<ReleaseTarget>,
    val rollbackToVersion: String? = null,
    val productionAdmitted: Boolean = false,
) {
    init { require(releaseId.isNotBlank() && version.isNotBlank()) }
}

/**
 * States this checker can actually emit. There is deliberately no `ADMITTED` value: promotion is a
 * human operation on evidence this repository does not hold, so a state no code can produce would
 * be a collapsed state rather than a contract.
 */
@Serializable
enum class ReleaseReadiness { REJECTED, BLOCKED_EXTERNAL_GATES, RELEASE_CANDIDATE_DRAFT }

@Serializable
data class ReleaseConvergenceResult(
    val readiness: ReleaseReadiness,
    val blockers: List<String>,
    val absentGates: List<String>,
    val humanAdmitGates: List<String>,
)

object ReleaseConvergenceChecker {
    private val unsatisfiedStates = setOf(
        GateEvidenceState.ABSENT,
        GateEvidenceState.NOT_IMPLEMENTED,
        GateEvidenceState.NOT_EXERCISED,
        GateEvidenceState.BLOCKED,
        GateEvidenceState.DRAFT,
    )

    private val humanAdmitGates = setOf(
        ReleaseGate.STORE_LISTING,
        ReleaseGate.PRIVACY_REVIEW,
        ReleaseGate.CLINICAL_REVIEW,
        ReleaseGate.HUMAN_SUBMISSION_ADMIT,
    )

    /**
     * @param admittedHeadShas the exact heads a caller has independently admitted. A manifest that
     * names any other head is converging from a stale or unadmitted parent.
     */
    fun check(
        manifest: ReleaseManifest,
        admittedHeadShas: Set<String>,
    ): ReleaseConvergenceResult {
        val blockers = mutableListOf<String>()
        val absent = mutableListOf<String>()
        val humanAdmit = mutableListOf<String>()

        if (manifest.productionAdmitted) {
            blockers += "A release manifest cannot self-declare production admission."
        }
        if (!commitShaPattern.matches(manifest.convergenceHeadSha)) {
            blockers += "convergenceHeadSha must be an exact 40-character commit SHA."
        }
        val rollback = manifest.rollbackToVersion
        if (rollback.isNullOrBlank() || rollback == manifest.version) {
            blockers += "A release candidate requires a distinct rollback version."
        }
        if (manifest.admittedSliceHeadShas.isEmpty()) {
            blockers += "A release candidate requires at least one admitted domain-slice head."
        }
        manifest.admittedSliceHeadShas.distinct().sorted().forEach { sha ->
            if (!commitShaPattern.matches(sha)) {
                blockers += "Domain-slice head $sha is not an exact 40-character commit SHA."
            } else if (sha !in admittedHeadShas) {
                blockers += "Domain-slice head $sha is not in the admitted head set."
            }
        }

        val platforms = manifest.targets.map { it.platform }
        ReleasePlatform.entries.forEach { platform ->
            when (platforms.count { it == platform }) {
                1 -> Unit
                0 -> blockers += "Release manifest declares no $platform target."
                else -> blockers += "Release manifest declares $platform more than once."
            }
        }

        manifest.targets.sortedBy { it.platform.name }.forEach { target ->
            if (target.convergenceHeadSha != manifest.convergenceHeadSha) {
                blockers += "${target.platform} converges from a different head than the manifest; " +
                    "a stale parent cannot be released."
            }
            val artifactSha = target.artifactSha256
            if (artifactSha != null && !sha256Pattern.matches(artifactSha)) {
                blockers += "${target.platform} artifact SHA-256 is malformed."
            }

            val declared = target.gates.map { it.gate }
            declared.groupBy { it }.filterValues { it.size > 1 }.keys.sortedBy { it.name }.forEach {
                blockers += "${target.platform} declares gate $it more than once."
            }
            ReleaseGate.entries.forEach { gate ->
                if (gate !in declared) {
                    blockers += "${target.platform} does not declare gate $gate; an absent gate must be explicit."
                }
            }

            target.gates.sortedBy { it.gate.name }.forEach { evidence ->
                val label = "${target.platform}:${evidence.gate}=${evidence.state}"
                if (evidence.humanAdmitRequired) {
                    humanAdmit += "${target.platform}:${evidence.gate}"
                    if (evidence.state == GateEvidenceState.ADMITTED) {
                        blockers += "${target.platform}:${evidence.gate} is a human-admit gate and " +
                            "cannot be admitted by a manifest."
                    }
                }
                if (evidence.state in unsatisfiedStates) {
                    absent += label
                } else {
                    if (evidence.evidenceRef.isNullOrBlank()) {
                        blockers += "${target.platform}:${evidence.gate} claims ${evidence.state} without an evidence reference."
                    }
                    if (!sha256Pattern.matches(evidence.evidenceSha256.orEmpty())) {
                        blockers += "${target.platform}:${evidence.gate} claims ${evidence.state} without an evidence hash."
                    }
                }
            }
        }

        val distinct = blockers.distinct()
        val readiness = when {
            distinct.isNotEmpty() -> ReleaseReadiness.REJECTED
            absent.isNotEmpty() -> ReleaseReadiness.BLOCKED_EXTERNAL_GATES
            else -> ReleaseReadiness.RELEASE_CANDIDATE_DRAFT
        }
        return ReleaseConvergenceResult(readiness, distinct, absent.distinct(), humanAdmit.distinct())
    }

    /**
     * The gate set every target must declare, with the evidence state this repository can honestly
     * record today: nothing has been built, signed, scanned, submitted, or reviewed.
     */
    fun absentGateSet(): List<ReleaseGateEvidence> = ReleaseGate.entries.map { gate ->
        ReleaseGateEvidence(
            gate = gate,
            state = GateEvidenceState.ABSENT,
            humanAdmitRequired = gate in humanAdmitGates,
            note = "No evidence exists in this repository for $gate.",
        )
    }
}
