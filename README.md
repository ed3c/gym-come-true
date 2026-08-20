# Gym Come True

[繁體中文](README.zh-TW.md)

Evidence-first fitness protocol execution for Android, iOS, and Web with Kotlin Multiplatform and Compose Multiplatform.

> **Current truth (2026-08-20):** the repository is public. The previously staged repo-internal chain PRs #55/#57/#61/#63/#65/#67/#69/#71/#73/#75/#77/#79 and convergence PR #81 are merged into `main`. Historical sibling PR #59 is closed and superseded by #81 after a fresh current-main 3/3 hosted run. Hosted GitHub Actions now execute normally. External legal/clinical/editorial/rights/device/store/provider/signing/Git Town runtime admission remains separate.

## Agent read order

Read [AGENTS.md](AGENTS.md), [Implementation status](docs/implementation-status.md), [Issue closure audit](docs/issue-closure-audit.md), [Local Handoff Execution Queue](docs/local-handoff-execution-queue.md), [Architecture](docs/architecture.md), then [Git / Stacked-PR governance](docs/git/README.md) and [Molecular Stack graph](docs/git/STACKED_PRS.md).

Hard evidence laws:

```text
OPEN_ISSUE != ABSENT_IMPLEMENTATION
HOSTED_PASS(commit A) != HOSTED_PASS(commit B)
GITHUB_CHECK_PASS != HUMAN_ADMIT
INFORMATION_OR_LOGGING != SAFETY_VERDICT
ARITHMETIC_RESULT != DOSE_RECOMMENDATION
CONTRACT_CODE != LIVE_PROVIDER_EVIDENCE
ADAPTER_PRESENT != REAL_DEVICE_VALIDATION
DECLARED_PERMISSION != STORE_APPROVAL
DEBUG_SIGNED != RELEASE_SIGNED
SEMANTIC_PAYLOAD_HASHED != REPRODUCIBLE_BUILD_PROVEN
GIT_TOWN_CANDIDATE != GIT_TOWN_RUNTIME_ADMITTED
```

## Product boundary

Gym Come True is an information/logging and deterministic protocol tool, not a diagnostic, dosing, medication-interaction, or clinical decision system. OCR/barcode output starts as a candidate; the user confirms label facts before arithmetic or logging. AI may summarize logged totals/general information with the required notice, but it cannot recommend dosage, invent evidence, diagnose, approve rights, or become decision authority.

## Repository DAG: directory → State Machine → dataflow

```text
capture/platform leaves
  androidApp/ ─┐
  iosApp/     ─┼─> shared/ deterministic domain core ─> shared UI ─> Android/iOS/Web projections
  webApp/     ─┘                     │
                                     ├─> data/ schemas + Draft/default-deny catalogs
                                     ├─> legal/ + assets/ provenance/admission boundaries
                                     └─> scripts/ validators ─> .github/workflows/ hosted evidence

docs/ + docs/git/ ─> Agent/runtime/delivery authority ─> Issues / Local Handoff Queue
```

| Directory | Owning State Machine | Inputs → outputs | Evidence ceiling |
|---|---|---|---|
| `shared/` | `UNVERIFIED -> USER_CONFIRMED -> DETERMINISTIC_RESULT -> LOGGED/RECEIPT` | confirmed supplement/food/workout facts → arithmetic, timetable, UI models | no dose/safety/clinical authority |
| `androidApp/` | `PERMISSION -> CAPTURE -> ML_KIT_CANDIDATE -> CONFIRM -> LOCAL_ACTION` | camera/barcode/OCR + shared commands → reminders and Health Connect reads | adapter/tests != real-device/OEM/store proof |
| `iosApp/` | `PERMISSION -> CAPTURE -> VISION_CANDIDATE -> CONFIRM -> LOCAL_ACTION` | Photos/camera/Vision + shared commands → notifications and HealthKit reads | entitlement/device/store/AlarmKit proof remains external |
| `webApp/` | `BOOTSTRAP -> SHARED_UI_READY -> USER_INPUT -> LOCAL_RESULT` | browser input → shared deterministic result | no native-health parity claim |
| `data/` | `SYNTHETIC_OR_DRAFT -> STRUCTURALLY_VALIDATED -> TEST_ONLY/ADMISSION_PENDING` | catalog/schema/source candidate → validated local records | checked-in fixture cannot self-admit production |
| `legal/` | `UNKNOWN -> REVIEW -> ALLOW/DENY -> REVOKED` | exact source/media terms → admission record | no automatic legal approval |
| `assets/` | `QUARANTINED -> HASHED -> RIGHTS_REVIEWED -> ADMITTED -> REVOKED` | first-party/candidate bytes → immutable admitted asset | third-party rights remain explicit |
| `scripts/` | `INPUT -> VALIDATED -> PASS/FAIL` | repo bytes → deterministic receipts | validator != reviewer |
| `.github/workflows/` | `QUEUED -> RUNNER_ALLOCATED -> EXECUTED -> PASS/FAIL -> ARTIFACT_UPLOADED` | exact checkout → CI evidence + artifact identity receipts | check != Human Admit |
| `docs/` | `OBSERVED -> DOCUMENTED -> MACHINE_GATED -> SUPERSEDED` | live repo/evidence → authority surfaces | prose cannot close external gates |
| `docs/git/` | `WORK_PACKET -> LEASED -> VERIFIED -> PUBLICATION_GATE -> MERGED/HUMAN_ADMIT` | Issue/branch/evals → molecular Stack trace | Git Town runtime remains blocked |

## End-to-end product dataflows

```text
Supplement / Body Hacker
capture -> OCR/barcode candidate -> user confirmation -> compatible-mass arithmetic
-> logged total -> A/B daily timetable -> reminder command -> optional general-information AI explanation

Nutrition
synthetic/admitted food record -> provenance + serving validation -> deterministic nutrition arithmetic
-> meal slots -> A/B workout timetable -> editable reminders

Exercise/media
first-party metadata/candidate asset -> quarantine -> exact provenance/rights review
-> immutable hash -> admitted package OR deny/revoke

Health
platform permission -> least-privilege read adapter -> normalized shared observation -> user-visible log

Artifact evidence
build -> transport hash -> payload enumeration -> semantic payload hash -> JSON receipt -> hosted upload
```

## Real-problem closure matrix

| Requested problem | Repository engineering | Closure state |
|---|---|---|
| KMP Android/iOS/Web build | shared JVM, Android debug/lint, Web distribution, iOS simulator framework/host repeatedly hosted-proven | **repo-internal closed**; store/signing/device release external |
| ML Kit / Apple Vision supplement capture | on-device candidate extraction + user-confirmation boundary | **engineering present**; real consented accuracy corpus/device evidence open |
| supplement totals/timetable/reminders | deterministic compatible-mass arithmetic, A/B schedule, local reminder contracts | **repo-internal closed**; no dosing/safety claim |
| exercise metadata | canonical taxonomy + first-party bilingual 50-record `DRAFT` + validator | engineering present; Issue #32/#33 editorial/rights admission open |
| exercise media / muscle visualization | first-party schematic/local mapping and default-deny media governance | licensed/commissioned media + anatomy validation open |
| nutrition + meal planning | synthetic/default-deny catalog + validator + deterministic compiler | engineering present; Issue #46 real source/license and #47 admitted-record dependency open |
| AI analysis | OpenAI/Anthropic descriptors, mandatory notice, logged-totals/general-information boundary, deterministic fallback | contract present; Issue #35 live provider/security/privacy open |
| Health Connect / HealthKit | least-privilege read adapters | engineering present; real-device/OEM/entitlement/store evidence open |
| exact alarm / AlarmKit | reminder fallback contracts only | **not admitted**; do not claim system-alarm reliability |
| artifact identity | transport-vs-semantic receipt tooling merged by PR #81, run #128 | **repo-internal closed**; release signing/reproducibility/attestation external |
| Git Town worker | v24.0.0 candidate metadata/verifier/harness | runtime/config/sync/publication canaries `NOT_EXERCISED` |

## Molecular Stack PR trace

The implementation/governance chain is now merged:

```text
main
└─ #55 X2  domain validators / CI ownership
   ├─ #57 X3  implementation SSOT
   │  └─ #61 X5  AGENTS runtime contract
   │     └─ #63 X6  authority machine gate
   │        └─ #65 X7  bilingual README authority
   │           └─ #67 X8  delivery machine SSOT
   │              └─ #69 X9  roadmap/Git routing
   │                 └─ #71 X10 implementation SSOT current chain
   │                    └─ #73 X11 architecture/platform authority
   │                       └─ #75 X12 product/safety authority
   │                          └─ #77 X13 product implementation SSOT
   │                             └─ #79 X14 current machine delivery graph
   └─ #59 X4 historical sibling evidence (closed/unmerged after conflict)
      └─ semantics replayed onto current main by #81 X15 artifact-identity convergence
```

Every historical hosted run remains evidence for its exact head only. #59 was not force-merged: its workflow conflict was handled by a fresh current-main convergence packet, preserving semantic-conflict-stop behavior.

## Git Town boundary

Canonical method: [`skills-shared/skills/git-town-stacked-pr-worker`](https://github.com/ed3c/skills-shared/tree/main/skills/git-town-stacked-pr-worker).

```yaml
candidate: v24.0.0
candidate_metadata: VERIFIED
runtime: CANDIDATE_METADATA_VERIFIED_RUNTIME_BLOCKED
consumer_config: NOT_IMPLEMENTED
sync_canary: NOT_EXERCISED
publication_canary: NOT_EXERCISED
background_sync: DISABLED
production_use: DENY
```

## Local Handoff

Only work that requires local devices/accounts/credentials/legal or rights review remains in the handoff queue. See [docs/local-handoff-execution-queue.md](docs/local-handoff-execution-queue.md). Open domain Issues #32/#33/#35/#46/#47 are acceptance queues, not proof that implementation is absent.

## Validation

```bash
python3 scripts/validate_repository.py
python3 scripts/validate_stacked_delivery.py --self-test
python3 data/exercise-catalog/validate_catalog.py --selftest
python3 scripts/validate_nutrition_catalog.py --self-test
python3 scripts/validate_authority_surfaces.py --self-test
python3 scripts/validate_product_safety_authority.py --self-test
python3 scripts/validate_artifact_identity.py self-test
sh ./gradlew :shared:jvmTest
sh ./gradlew :androidApp:assembleDebug :androidApp:lintDebug
sh ./gradlew :webApp:composeCompatibilityBrowserDistribution
```

## License

Repository-authored code and documentation are licensed under the **Apache License 2.0**. Third-party dependencies/assets retain their own terms; Apache-2.0 does not grant third-party media, official-data redistribution, medical, trademark, store, or release rights.
