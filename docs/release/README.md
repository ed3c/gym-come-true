# Entitlement, privacy lifecycle, and store release candidate

> **Status:** Draft engineering contract for Issues #38, #39, and #40.
> Nothing in this directory admits a provider account, a signed artifact, a store submission, a
> privacy or legal review, or production access for any real person.

This directory owns the deterministic contracts for three sibling slices that all end at the same
place: an external party we do not control has to produce evidence before anything ships.

| Document | Issue | Transition reached |
| --- | --- | --- |
| [entitlement-contract.md](entitlement-contract.md) | #38 | `NO_ENTITLEMENT -> VERIFIED_ENTITLEMENT_DRAFT` |
| [privacy-lifecycle.md](privacy-lifecycle.md) | #39 | `VERIFIED_ENTITLEMENT_DRAFT -> ACCOUNT_DATA_LIFECYCLE_DRAFT` |
| [store-release-candidate.md](store-release-candidate.md) | #40 | `NOT_STARTED -> RELEASE_MANIFEST_SCHEMA_DRAFT` (not the issue's target state) |

## Hard laws

```text
CLIENT_ASSERTION       != VERIFIED_ENTITLEMENT
WEBHOOK_ARRIVAL        != VERIFIED_ENTITLEMENT
PAID_PRICE             != ACCESS
UI_HIDDEN              != DELETED
ANONYMISED             != DELETED
CONSENTED              != LAWFUL_FOR_ANY_PURPOSE
MANIFEST_CLAIM         != SIGNED_ARTIFACT
GREEN_DETERMINISTIC_TEST != STORE_RELEASE_READINESS
```

## External gates for this whole directory

Every row below is `ABSENT` in this repository and needs a human or an external party. No agent may
move any of them.

```text
Apple / Google / web merchant provider accounts        ABSENT
Server-side provider verification endpoint             NOT_IMPLEMENTED
Provider design and security review                    HUMAN_ADMIT_REQUIRED
Privacy, legal, and cross-border storage review        HUMAN_ADMIT_REQUIRED
Data-protection representative / DPIA sign-off         ABSENT
Real account data, real consent from real humans       ABSENT
Signing credentials and provenance attestation         ABSENT
SBOM and vulnerability scan receipts                   ABSENT
Store console listings, forms, and submission          ABSENT
Measured devices for performance and offline claims    ABSENT
Trusted exact-head hosted checks                       NOT_EXERCISED
Merge, promotion, rollback                             HUMAN_ADMIT_REQUIRED
```

## What actually exists here

Deterministic Kotlin contracts and their negative controls:

```text
shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/
├── entitlement/Entitlement.kt        # receipts, replay-safe ledger, access projection
├── privacy/PrivacyLifecycle.kt       # inventory, consent history, deletion, export
└── release/ReleaseManifest.kt        # release-gate manifest and convergence checker

shared/src/commonTest/kotlin/dev/ed3c/gymcometrue/
├── entitlement/EntitlementContractTest.kt
├── privacy/PrivacyLifecycleTest.kt
└── release/ReleaseConvergenceTest.kt
```

These are in-memory contracts. There is no persistence, no network client, no provider SDK, and no
secret anywhere in this slice — shared code cannot import any of them (`AGENTS.md`, Directory
ownership).
