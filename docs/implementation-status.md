# Implementation status

**Authoritative routing snapshot:** 2026-08-20  
**Repository:** `ed3c/gym-come-true` — public, immutable repository ID `1334805292`  
**Merged `main` at snapshot:** `b1880abe317ac274b59695439c4f9682b8864f6b`  
**Current top Draft evidence:** PR #69, `agent/reconcile-roadmap-git-entrypoint@c9837970086ea0a522bda35a435ee1acc89e4ff2`  
**This reconciliation packet:** Issue #70, serial documentation/authority child of PR #69

This file separates merged repository truth, staged-but-unmerged engineering/evidence, and Human/external admission. Read live GitHub state before dispatch: this checked-in snapshot is routing authority for repository content, not network discovery.

```text
OPEN_ISSUE != ABSENT_IMPLEMENTATION
HOSTED_PASS(commit A) != HOSTED_PASS(commit B)
GITHUB_CHECK_PASS != HUMAN_ADMIT
MERGED_ENGINEERING != PRODUCTION_ADMISSION
```

## Merged engineering baseline

Historical delivery PRs #2, #15, #16, #20, and #22 are merged history, not active Draft PRs. They established the KMP Android/iOS/Web foundation, Taiwan evidence/source lifecycle contracts, repository delivery governance, and the pinned Git Town candidate packet.

`main` also already contains engineering for exercise taxonomy/catalog, nutrition/meal planning, receipt-only explanations, Health Connect/HealthKit adapter surfaces, reminders, and other MVP slices. Open domain Issues must not be redispatched solely because they remain open.

## Current staged evidence graph

The branch-level source of truth remains `docs/git/STACKED_PRS.md` plus its SHA-bound machine manifest. At this snapshot:

```text
main@b1880abe...
└── PR #55  DOMAIN_VALIDATORS_OWNED_BY_CI_DRAFT
    ├── PR #57  CURRENT_PUBLIC_REPO_SSOT_DRAFT
    │   └── PR #61  CURRENT_AGENT_RUNTIME_CONTRACT_DRAFT
    │       └── PR #63  MACHINE_GATED_AUTHORITY_DRAFT
    │           └── PR #65  README_AUTHORITY_RECONCILED_AND_GATED_DRAFT
    │               └── PR #67  LIVE_DELIVERY_GRAPH_RECONCILED_DRAFT
    │                   └── PR #69  SECONDARY_ROUTING_SURFACES_RECONCILED_DRAFT
    │                       └── Issue #70 / X10  current implementation-SSOT reconciliation
    └── PR #59  TRANSPORT_AND_SEMANTIC_IDENTITIES_SEPARATED_DRAFT
```

Every Draft head owns separate exact-head hosted evidence. Parent green does not prove a child SHA. All listed PRs remain unmerged at this snapshot; merge remains Human Admit.

## Exact-head hosted evidence already established

The current Draft chain has hosted proof on its own exact heads, including:

- PR #55 / run #88 — exercise/nutrition owning validators plus Android/Web/iOS lanes PASS;
- PR #57 / run #89 — implementation-status reconciliation PASS;
- PR #59 / run #90 — transport vs semantic artifact identity PASS;
- PR #61 / run #91 — Agent runtime authority reconciliation PASS;
- PR #63 / run #92 — authority drift machine gate PASS;
- PR #65 / run #96 — bilingual README authority reconciliation PASS;
- PR #67 / run #100 — delivery index + machine Stack SSOT reconciliation PASS;
- PR #69 / run #102 — roadmap/Git-entrypoint reconciliation and six-surface authority gate PASS.

These receipts are historical evidence for those exact heads only. Older Actions runs that ended before runner allocation remain historical `PRE_RUN_BLOCKED` evidence; current hosted execution does not rewrite them.

## Current domain truth and remaining admission

| Domain | Engineering present | Still not admitted |
|---|---|---|
| Supplement evidence | OCR/barcode candidates, confirmed-mass arithmetic, duplicate detection, deterministic `LOG/REVIEW/BLOCK`, A/B timetable, decision receipts | personalized dose/diagnosis, medication compatibility, clinically reviewed Taiwan rule pack |
| Taiwan source lifecycle | immutable-source, exact-mapping, release/revoke/rollback contracts and local-byte capture boundary | real official source bytes, reuse approval, legal review, qualified clinical review, production activation |
| Exercise taxonomy/catalog | canonical taxonomy, 50-record first-party bilingual `DRAFT` catalog, deterministic validator | editorial/rights acceptance, licensed third-party media, production promotion |
| Nutrition/meal plan | admission schema, bilingual synthetic/default-deny catalog, deterministic meal-plan compiler, repository validator | real Taiwan food-composition source/version/license approval, admitted mappings/records |
| Explanation gateway | receipt-only, decision-preserving explanation contract | security/privacy review, provider/secret/deployment evidence, production traffic |
| Android health | Health Connect availability/permission/read adapters and tests | real-device/OEM evidence, production privacy/store disclosure |
| iOS health | `NativeHealthReadBridge`, `HKHealthStore` bridge and shared read policy | entitlement/device authorization, store disclosure, real-device evidence |
| Reminders | Android local reminder and iOS notification/recurrence contracts | exact-alarm/AlarmKit product admission and OEM/device reliability |
| Git Town | v24.0.0 pinned candidate metadata, verifier and disposable canary harness | consumer `.git-town.toml`, live sync/publication canaries, runtime/legal/supply-chain admission |

## Open Issues: engineering state versus actual blocker

| Issue | Current engineering meaning | Remaining gate |
|---:|---|---|
| #32 | `TAXONOMY_CONTRACT` already present | taxonomy/rights Human Admit |
| #33 | first-party bilingual top-50 `DRAFT` already present | editorial/rights Human Admit |
| #35 | receipt-only explanation gateway contract already present | security/privacy Human Admit |
| #46 | schema/admission contract and synthetic/default-deny catalog already present | real source/version/reuse-rights review and admitted mappings |
| #47 | deterministic meal-plan compiler/tests already present | admitted real food records from #46 |
| #54 | engineering child is PR #55 with hosted proof | Human merge/admit PR #55 |
| #56/#60/#62/#64/#66/#68 | implemented by Draft PRs #57/#61/#63/#65/#67/#69 respectively | Human merge of their exact heads |
| #70 | this bounded implementation-status reconciliation packet | fresh exact-head hosted proof, then Human merge |

Issue #45 is closed. Actions capacity is currently sufficient for the active Draft chain; historical budget-blocked SHAs remain `PRE_RUN_BLOCKED` evidence only.

## Hard evidence boundaries

```text
OCR_CANDIDATE != CONFIRMED_FACT
HASH_VERIFIED != LEGAL_REVIEWED
LEGAL_REVIEWED != CLINICALLY_REVIEWED
DRAFT_CONTENT != RIGHTS_ADMITTED_CONTENT
CONTRACT_CODE != LIVE_PROVIDER_EVIDENCE
ADAPTER_PRESENT != REAL_DEVICE_VALIDATION
DEBUG_SIGNED != RELEASE_SIGNED
GIT_TOWN_CANDIDATE != GIT_TOWN_RUNTIME_ADMITTED
```

No LLM, repository test, GitHub check, or documentation packet may sign a rule pack, approve rights, invent nutrient facts, recommend a supplement dose, grant device/store/provider authority, admit Git Town runtime, or promote a release.

## Git Town / Worker state

`docs/git/GIT_TOWN_ADMISSION.md` remains authoritative for runtime admission:

```yaml
state: CANDIDATE_METADATA_VERIFIED_RUNTIME_BLOCKED
candidate: v24.0.0
runtime_admitted: false
consumer_config_admitted: false
live_canaries_exercised: false
background_sync_enabled: false
publication_enabled: false
production_use: DENY
```

`.git-town.toml` remains `NOT_IMPLEMENTED`. Candidate metadata is not runtime admission.

## Verification and next dispatch

Repository-owned validation includes the six-surface authority gate, stacked-delivery machine self-tests, domain validators, shared JVM tests, Android debug/lint, Web compatibility distribution, and the iOS simulator-host build. Any moved head requires fresh exact-head hosted evidence.

Next dispatch rules:

1. do not reopen already-present domain engineering because an umbrella/admission Issue is open;
2. prefer bounded internal repair only when Shadow Architect finds a concrete evidence, authority, graph, or executable correctness delta;
3. keep #32/#33/#35/#46/#47 open at their explicit Human/external gates;
4. do not manufacture legal/clinical/editorial/rights/device/store/provider/signing/release/Git Town runtime evidence;
5. release convergence Issue #44 may consume only Human-selected admitted heads and cannot repair domain semantics;
6. use `docs/github-issue-index.md`, `docs/git/STACKED_PRS.md`, and live GitHub state before creating another packet.
