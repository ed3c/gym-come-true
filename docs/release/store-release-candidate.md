# Store release candidate — manifest schema only

> **Issue:** #40 — target transition `ADMITTED_DOMAIN_SLICES -> STORE_RELEASE_CANDIDATE`.
> **State actually reached:** `RELEASE_MANIFEST_SCHEMA_DRAFT`. The issue's transition is **not**
> reached and cannot be reached from this repository.

## Why this slice stops here

Issue #40 is almost entirely external. Signing credentials, store consoles, provider accounts,
measured devices, and trusted hosted checks are all outside the repository, and every one of them is
`ABSENT`. What a repository *can* own is the shape of the convergence: which gates exist, which head
each platform target converges from, and what a manifest is forbidden to claim about itself.

So this slice delivers exactly that and nothing more:

```text
ReleaseManifest      exact convergence head, admitted slice heads, three platform targets,
                     rollback version, and a productionAdmitted field that exists only so a
                     manifest can be rejected for setting it
ReleaseGate          19 named gates, each declared per target
GateEvidenceState    ABSENT | NOT_IMPLEMENTED | NOT_EXERCISED | BLOCKED | DRAFT | VERIFIED | ADMITTED
ReleaseConvergenceChecker.check(manifest, admittedHeadShas)
```

`ReleaseReadiness` has three values — `REJECTED`, `BLOCKED_EXTERNAL_GATES`,
`RELEASE_CANDIDATE_DRAFT` — and deliberately no admitted value. Promotion is a human operation on
evidence this repository does not hold; a state that no code path can emit would be a decorative
state, not a contract.

## Today's honest manifest

`ReleaseConvergenceChecker.absentGateSet()` returns the 19 gates with the evidence state this
repository can actually record, which is `ABSENT` for every one of them. Feeding that into the
checker for all three platforms yields `BLOCKED_EXTERNAL_GATES` with 57 absent gate labels and zero
blockers (`todaysManifestIsBlockedByExternalGates`).

```text
ARTIFACT_BUILD           ABSENT      no release artifact has been produced
CODE_SIGNING             ABSENT      no signing credential exists, and none may enter Git
PROVENANCE_ATTESTATION   ABSENT
SBOM                     ABSENT
VULNERABILITY_SCAN       ABSENT
STORE_LISTING            ABSENT      human admit
ACCESSIBILITY_REVIEW     ABSENT
LOCALIZATION_REVIEW      ABSENT
PERFORMANCE_MEASUREMENT  ABSENT      requires measured devices
OFFLINE_BEHAVIOUR        ABSENT
UPGRADE_MIGRATION        ABSENT
SUPPORT_RUNBOOK          ABSENT
INCIDENT_RUNBOOK         ABSENT
ROLLBACK_RUNBOOK         ABSENT
PROVIDER_VERIFICATION    ABSENT      depends on Issue #38 external gates
PRIVACY_REVIEW           ABSENT      human admit; depends on Issue #39 external gates
CLINICAL_REVIEW          ABSENT      human admit
HOSTED_EXACT_HEAD_CHECK  ABSENT      no hosted check has ever executed on this repository (#45)
HUMAN_SUBMISSION_ADMIT   ABSENT      human admit
```

## What the checker refuses

| Refusal | Test |
| --- | --- |
| A manifest that self-declares `productionAdmitted` | `manifestCannotSelfDeclareProductionAdmission` |
| A target converging from a different head than the manifest (stale parent) | `aTargetConvergingFromAnotherHeadIsRejected` |
| A domain-slice head outside the caller's admitted head set | `anUnadmittedSliceHeadIsRejected` |
| A missing or duplicated platform target | `everyPlatformMustBeDeclaredExactlyOnce` |
| A gate that is simply not declared (absence must be explicit) | `anUndeclaredGateIsRejectedRatherThanAssumedPassing` |
| A `VERIFIED`/`ADMITTED` gate with no evidence reference or hash | `aPassingGateWithoutAHashIsRejected` |
| A human-admit gate marked `ADMITTED` by the manifest | `aHumanAdmitGateCannotBeAdmittedByAManifest` |
| A `DRAFT` gate counted as passing | `aDraftGateIsNotEvidenceOfPassing` |
| A missing or non-distinct rollback version | `aReleaseCandidateRequiresADistinctRollbackVersion` |

Even a manifest with every gate evidenced stops at `RELEASE_CANDIDATE_DRAFT` and still reports its
human-admit gates (`aFullyEvidencedManifestStopsAtReleaseCandidateDraft`).

## What remains

```text
Real artifacts for Android, iOS, and Web                ABSENT
Signing credentials and secure storage for them         ABSENT (never a repository path)
Provenance attestation, SBOM, vulnerability receipts    ABSENT
Store console accounts, listings, and forms             ABSENT
Accessibility, localization, performance, offline,
  and upgrade testing on measured devices               ABSENT
Support, incident, and rollback runbooks                ABSENT
Trusted exact-head hosted checks                        NOT_EXERCISED
Submission, promotion, rollback, and merge              HUMAN_ADMIT_REQUIRED
JSON projection of this schema under data/              ABSENT (outside this slice's path lease)
```

The hashes in `ReleaseConvergenceTest` are repeated-character literals used to exercise the shape
check. They are not, and must never be presented as, real artifact or evidence digests.

## Rollback subject

`ReleaseManifest.kt` and `ReleaseConvergenceTest.kt` at this commit.
