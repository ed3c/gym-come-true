# Gym Come True

[English](README.md)

使用 Kotlin Multiplatform 與 Compose Multiplatform 建立的 evidence-first 健身資訊／記錄與 protocol 執行系統，支援 Android、iOS 與 Web。

> **目前真實狀態（2026-08-20）：** repository 為公開庫。原 staged repo-internal chain PR #55/#57/#61/#63/#65/#67/#69/#71/#73/#75/#77/#79 與 convergence PR #81 已 merge 進 `main`。歷史 sibling PR #59 因 workflow 衝突沒有被強制 merge，而是由 #81 在 current main 上重新驗證後取代。GitHub Actions hosted runners 現在能正常執行。legal/clinical/editorial/rights/device/store/provider/signing/Git Town runtime admission 仍是獨立外部 gate。

## Agent 閱讀順序

依序讀取 [AGENTS.md](AGENTS.md)、[Implementation status](docs/implementation-status.md)、[Issue closure audit](docs/issue-closure-audit.md)、[Local Handoff Execution Queue](docs/local-handoff-execution-queue.md)、[Architecture](docs/architecture.md)、[Git governance](docs/git/README.md) 與 [Molecular Stack graph](docs/git/STACKED_PRS.md)。

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

## 產品邊界

Gym Come True 是 information/logging 與 deterministic protocol tool，不是 diagnosis、dosing、medication-interaction 或 clinical decision system。OCR/barcode 先產生 candidate，使用者確認 label facts 後才進 arithmetic/logging。AI 只能在 mandatory notice 下說明 logged totals / general information，不得推薦劑量、診斷、補造 evidence、核准 rights 或成為 decision authority。

## Repository DAG：目錄 → State Machine → Dataflow

```text
androidApp/ ─┐
iosApp/     ─┼─> shared/ deterministic domain core ─> shared UI ─> Android/iOS/Web
webApp/     ─┘                     │
                                   ├─> data/ schemas + Draft/default-deny catalogs
                                   ├─> legal/ + assets/ provenance/admission
                                   └─> scripts/ validators ─> .github/workflows/ evidence

docs/ + docs/git/ ─> Agent/runtime/delivery authority ─> Issues / Local Handoff Queue
```

| Directory | State Machine | Input → output | Evidence ceiling |
|---|---|---|---|
| `shared/` | `UNVERIFIED -> USER_CONFIRMED -> DETERMINISTIC_RESULT -> LOGGED/RECEIPT` | confirmed facts → arithmetic/timetable/UI | 不具 dose/safety/clinical authority |
| `androidApp/` | `PERMISSION -> CAPTURE -> ML_KIT_CANDIDATE -> CONFIRM -> LOCAL_ACTION` | camera/barcode/OCR → reminder/Health Connect read | adapter != real-device/OEM/store proof |
| `iosApp/` | `PERMISSION -> CAPTURE -> VISION_CANDIDATE -> CONFIRM -> LOCAL_ACTION` | Photos/camera/Vision → notification/HealthKit read | entitlement/device/store/AlarmKit proof 尚未 admit |
| `webApp/` | `BOOTSTRAP -> SHARED_UI_READY -> USER_INPUT -> LOCAL_RESULT` | browser input → shared deterministic result | 不宣稱 native-health parity |
| `data/` | `SYNTHETIC_OR_DRAFT -> VALIDATED -> TEST_ONLY/ADMISSION_PENDING` | catalog/schema/source candidate → local records | fixture 不可自我 production-admit |
| `legal/` | `UNKNOWN -> REVIEW -> ALLOW/DENY -> REVOKED` | exact terms → admission record | 不等於法律核准 |
| `assets/` | `QUARANTINED -> HASHED -> RIGHTS_REVIEWED -> ADMITTED -> REVOKED` | bytes → immutable admitted asset | third-party rights 必須獨立證明 |
| `scripts/` | `INPUT -> VALIDATED -> PASS/FAIL` | repo bytes → deterministic receipts | validator != reviewer |
| `.github/workflows/` | `QUEUED -> RUNNER_ALLOCATED -> EXECUTED -> PASS/FAIL -> ARTIFACT_UPLOADED` | exact checkout → CI evidence | check != Human Admit |
| `docs/` | `OBSERVED -> DOCUMENTED -> MACHINE_GATED -> SUPERSEDED` | live evidence → authority surfaces | 文件不能製造外部 evidence |
| `docs/git/` | `WORK_PACKET -> LEASED -> VERIFIED -> PUBLICATION_GATE -> MERGED/HUMAN_ADMIT` | Issue/branch/evals → molecular trace | Git Town runtime 仍 blocked |

## 端到端 Dataflows

```text
Supplement / Body Hacker
capture -> OCR/barcode candidate -> user confirmation -> compatible-mass arithmetic
-> logged total -> A/B timetable -> reminder -> optional general-information AI explanation

Nutrition
synthetic/admitted food record -> provenance/serving validation -> deterministic arithmetic
-> meal slots -> A/B timetable -> editable reminders

Exercise/media
first-party metadata/candidate asset -> quarantine -> exact provenance/rights review
-> immutable hash -> admitted package OR deny/revoke

Health
platform permission -> least-privilege read adapter -> normalized shared observation -> user-visible log

Artifact evidence
build -> transport hash -> payload enumeration -> semantic payload hash -> JSON receipt -> hosted upload
```

## 真實問題 Closure Matrix

| 問題 | 已實作 | Closure |
|---|---|---|
| KMP Android/iOS/Web | shared JVM、Android debug/lint、Web distribution、iOS simulator framework/host | **repo-internal closed**；store/signing/device release 外部 |
| ML Kit / Apple Vision supplement capture | on-device candidate extraction + confirmation boundary | engineering present；real-device/consented accuracy corpus open |
| supplement totals/timetable/reminders | deterministic mass arithmetic、A/B schedule、local reminders | **repo-internal closed**；不提供 dosing/safety verdict |
| exercise metadata | taxonomy + first-party bilingual 50-record `DRAFT` + validator | #32/#33 editorial/rights open |
| exercise media / muscle visualization | first-party schematic + default-deny media governance | licensed media/anatomical review open |
| nutrition / meal planning | synthetic catalog + validator + deterministic compiler | #46 source/license、#47 admitted-record dependency open |
| AI analysis | OpenAI/Anthropic descriptors、notice、logged-totals/general-information、fallback | #35 provider/security/privacy open |
| Health Connect / HealthKit | least-privilege read adapters | device/OEM/entitlement/store open |
| exact alarm / AlarmKit | reminder fallback contracts | **not admitted** |
| artifact identity | PR #81 / run #128 transport-vs-semantic receipts | **repo-internal closed**；release signing/reproducibility/attestation 外部 |
| Git Town worker | v24.0.0 candidate metadata/verifier/harness | runtime/config/sync/publication canaries `NOT_EXERCISED` |

## Molecular Stack PR Trace

```text
main
└─ #55 X2 domain validators
   ├─ #57 X3 implementation SSOT
   │  └─ #61 X5 AGENTS runtime contract
   │     └─ #63 X6 authority gate
   │        └─ #65 X7 bilingual README authority
   │           └─ #67 X8 delivery machine SSOT
   │              └─ #69 X9 roadmap/Git routing
   │                 └─ #71 X10 implementation SSOT
   │                    └─ #73 X11 architecture/platform authority
   │                       └─ #75 X12 product/safety authority
   │                          └─ #77 X13 product implementation SSOT
   │                             └─ #79 X14 machine delivery graph
   └─ #59 X4 historical sibling evidence（closed/unmerged after conflict）
      └─ semantics replayed by #81 X15 current-main artifact-identity convergence
```

每個 historical hosted run 只證明自己的 exact head。#59 沒有 force merge；衝突依 semantic-conflict-stop 規則改由 fresh convergence 處理。

## Git Town Boundary

Canonical method：[`skills-shared/skills/git-town-stacked-pr-worker`](https://github.com/ed3c/skills-shared/tree/main/skills/git-town-stacked-pr-worker)。

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

只剩需要 local devices/accounts/credentials/legal/rights review 的工作。詳見 [Local Handoff Execution Queue](docs/local-handoff-execution-queue.md)。Open Issues #32/#33/#35/#46/#47 是 acceptance queues，不代表 implementation absent。

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

Repository 自行創作的程式碼與文件採 **Apache License 2.0**。Third-party dependencies/assets 保留自己的授權；Apache-2.0 不會授予 third-party media、official-data redistribution、medical、trademark、store 或 release 權利。
