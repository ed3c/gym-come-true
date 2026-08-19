# GitHub Delivery Issue and Pull Request Index

**Authoritative snapshot:** 2026-08-19  
**Repository:** `ed3c/gym-come-true` — public  
**Live-state rule:** GitHub PR/Issue state and exact commit ancestry outrank historical prose.

This index separates four things that previous snapshots conflated:

```text
MERGED_HISTORY
STAGED_OR_MERGED_ENGINEERING
OPEN_DRAFT_EVIDENCE
HUMAN_OR_EXTERNAL_ADMISSION
```

A branch name or open Issue does not prove absent implementation. A green workflow on one SHA does not prove another SHA.

## Historical stack merged to `main`

| PR | Issue | Exact merged head | Durable engineering state |
|---:|---:|---|---|
| #2 | #1 | `58492815f22af65665172bcf98bfb661639ece92` | `AUDITABLE_CROSS_PLATFORM_FOUNDATION` |
| #15 | #8 | `79f8a65b370806925c32f0a15da88c7c0d7bda36` | `TAIWAN_EVIDENCE_CONTRACT_DRAFT` |
| #16 | #17 | `f58a2feac580ca37bb4d7b3c30e122908bfd6b07` | `TAIWAN_SOURCE_LIFECYCLE_DRAFT` |
| #20 | #19 | `ad065c8ac944f2fb4f9d60e65b008367b1291c43` | `DOCUMENTED_GIT_TOWN_DELIVERY_GRAPH_DRAFT` |
| #22 | #21 | `a70a52cc6e3e2f4107edae2f7bb2034029161568` | pinned Git Town v24.0.0 candidate packet; runtime not admitted |

Older runs on these exact historical heads may remain `PRE_RUN_BLOCKED_BY_ACTIONS_BUDGET`. That history is immutable evidence, but it is not the repository's current hosted-CI state.

## Current merged `main` domain truth

`main@b1880abe317ac274b59695439c4f9682b8864f6b` already contains more engineering than the original 2026-08-16 packet plan implied.

| Domain / Issues | Engineering present on `main` | Still Human/external gated |
|---|---|---|
| Taiwan evidence/source (#8, #17/#18) | product/corpus identity, OCR metrics, immutable-source/mapping/release contracts | real official bytes, reuse/legal review, qualified review, production activation |
| Exercise (#32/#33) | canonical taxonomy, first-party bilingual 50-record `DRAFT` catalog, deterministic validator | editorial/rights admission, licensed third-party media |
| Nutrition (#46/#47) | schema/admission contracts, synthetic/default-deny catalog, deterministic meal-plan compiler | real source/version/license review, exact admitted mappings |
| Explanation (#35, #49/#51) | receipt-only decision-preserving contract and provider-boundary engineering | live provider credentials/deployment, security/privacy admission |
| Android health (#10/#30/#31) | Health Connect availability/permission adapter surfaces and tests | real-device/OEM/privacy/store evidence |
| iOS health (#9/#27-#29) | HealthKit bridge/read-policy surfaces plus reminder contracts | entitlement, real-device, privacy/store and AlarmKit evidence |
| Product surface (#50/#52/#53) | information-only/disclaimer and hardening work represented in current `main` state | release/store/signing and external review |
| Git Town (#21/#23) | pinned v24.0.0 candidate metadata, verifier, disposable canary harness, machine delivery contract | executable/runtime admission, consumer config/sync/publication canaries, supply-chain/legal approval |

`ENGINEERING_PRESENT != PRODUCTION_ADMITTED`.

## Active Draft evidence graph

The current public Draft stack is:

```text
main@b1880abe...
└── PR #55  agent/converge-domain-validation@1338b6fd...
    ├── PR #57  agent/reconcile-implementation-status@58e4fc14...
    │   └── PR #61  agent/reconcile-agent-runtime-contract@7a59f6b8...
    │       └── PR #63  agent/gate-authority-drift@0c76c714...
    │           └── PR #65  agent/reconcile-readme-authority@30468077...
    │               └── X8 / Issue #66  delivery-machine SSOT convergence
    └── PR #59  agent/artifact-identity-receipts@036951d5...
```

| PR | Issue | Transition | Exact-head hosted evidence | Admission |
|---:|---:|---|---|---|
| #55 | #54 | `MERGED_DOMAIN_LANES_WITH_EVIDENCE_GAPS -> DOMAIN_VALIDATORS_OWNED_BY_CI_DRAFT` | run #88: 3/3 PASS | Draft; merge Human Admit |
| #57 | #56 | `STALE_IMPLEMENTATION_SNAPSHOT -> CURRENT_PUBLIC_REPO_SSOT_DRAFT` | run #89: 3/3 PASS | Draft; merge Human Admit |
| #59 | #58 | `HOSTED_BUILD_ARTIFACTS_WITH_AMBIGUOUS_HASH_SEMANTICS -> TRANSPORT_AND_SEMANTIC_IDENTITIES_SEPARATED_DRAFT` | run #90: 3/3 PASS | Draft; signing/provenance/merge Human Admit |
| #61 | #60 | `STALE_AGENT_AUTHORITY_SURFACE -> CURRENT_AGENT_RUNTIME_CONTRACT_DRAFT` | run #91: 3/3 PASS | Draft; merge Human Admit |
| #63 | #62 | `MANUALLY_RECONCILED_AUTHORITY -> MACHINE_GATED_AUTHORITY_DRAFT` | run #92: 3/3 PASS | Draft; merge Human Admit |
| #65 | #64 | `MACHINE_GATED_AGENT_STATUS_AUTHORITY -> README_AUTHORITY_RECONCILED_AND_GATED_DRAFT` | run #96: 3/3 PASS | Draft; merge Human Admit |

X8 / Issue #66 is the next serial convergence packet under PR #65. Its exact PR number and final head belong to its own PR metadata after publication.

## Open Issue semantics

Open Issue means “some acceptance remains,” not “nothing exists.”

Current engineering-complete-but-admission-open examples include #32/#33/#35/#46/#47. Their remaining gates are recorded in `docs/implementation-status.md`. External or Human-owned work such as real-source licensing, clinical/legal review, real-device validation, provider/store credentials, signing, release promotion, and Git Town runtime admission must remain open until evidence actually exists.

Issue #45's old Actions-capacity problem is historical. Current Draft heads #55/#57/#59/#61/#63/#65 have executed hosted workflows successfully. Do not rewrite the old blocked receipts as PASS.

## Stacked delivery authority

Human narrative and machine projection are a pair:

- `docs/git/STACKED_PRS.md`
- `docs/git/stacked-delivery-manifest.json`
- `scripts/validate_stacked_delivery.py --self-test`

The manifest SHA-binds the exact `STACKED_PRS.md` bytes. If the narrative, graph, status vocabulary, or Git Town runtime boundary changes, update the pair atomically.

## Evidence laws

```text
HISTORICAL_PRE_RUN_BLOCKED != CURRENT_ACTIONS_STATE
OPEN_ISSUE != ABSENT_IMPLEMENTATION
MERGED_ENGINEERING != EXTERNAL_ADMISSION
OPEN_DRAFT_PR != MERGED_TO_MAIN
HOSTED_PASS(commit A) != HOSTED_PASS(commit B)
GITHUB_CHECK_PASS != HUMAN_ADMIT
GIT_TOWN_CANDIDATE != GIT_TOWN_RUNTIME_ADMITTED
```

## Human / external gates that documentation cannot close

- legal, clinical, editorial and rights acceptance;
- real MOHW/TFDA or nutrition-source bytes and exact reuse terms;
- real-device/OEM/entitlement/store evidence;
- provider credentials, live deployment and independent security/privacy review;
- release signing, store submission, promotion and destructive rollback;
- Git Town executable/runtime/consumer-config admission and publication authority.

See `docs/implementation-status.md`, `docs/git/GIT_TOWN_ADMISSION.md`, and `docs/git/STACKED_PRS.md` for the corresponding engineering boundaries.
