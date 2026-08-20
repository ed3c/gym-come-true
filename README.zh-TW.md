# Gym Come True

[English](README.md)

使用 Kotlin Multiplatform 與 Compose Multiplatform 建立的 evidence-first 健身 protocol 執行系統，支援 Android、iOS 與 Web。

> **目前真實狀態（2026-08-19）：** `ed3c/gym-come-true` 是公開 repository。`main@b1880abe317ac274b59695439c4f9682b8864f6b` 已包含 foundation 與主要 domain contracts；GitHub Actions hosted runners 現在能正常執行。`main` 之上的 Draft PR evidence 只能證明各自 exact head。產品尚未取得 clinical、store release、第三方 exercise-media 或 Git Town consumer runtime admission。

## 閱讀順序與 Evidence Vocabulary

Agent 必須依序閱讀 [AGENTS.md](AGENTS.md)、[Implementation status](docs/implementation-status.md)、[Architecture](docs/architecture.md) 與 [Git / Stacked-PR governance](docs/git/README.md)。

| State | 精確含義 |
|---|---|
| `MERGED` | 程式已進 `main`；不代表 external 或 production admission。 |
| `OPEN DRAFT PR` | GitHub 上已有 review subject，但尚未 merge。 |
| `PASS` / `FAIL` | 指定 command 確實對指定 subject 執行。 |
| `PRE_RUN_BLOCKED` | 歷史 workflow 在 runner 執行前停止；不是 code pass 或 code fail。 |
| `ABSENT` | 所需 evidence 不存在。 |
| `NOT_IMPLEMENTED` | 能力尚未實作。 |
| `NOT_EXERCISED` | subject-bound runtime canary 尚未執行。 |
| `HUMAN_ADMIT` | merge、release、legal/clinical/rights、signing 或 destructive production 操作仍由 human 決定。 |

硬規則：

```text
HOSTED_PASS(commit A) != HOSTED_PASS(commit B)
GITHUB_CHECK_PASS != HUMAN_ADMIT
ADAPTER_PRESENT != REAL_DEVICE_VALIDATION
GIT_TOWN_CANDIDATE != GIT_TOWN_RUNTIME_ADMITTED
```

## Current Delivery Graph

歷史 foundation/convergence PR #2、#15、#16、#20、#22 已 merge。

目前 Draft evidence stack：

```text
main@b1880abe...
└── PR #55  DOMAIN_VALIDATORS_OWNED_BY_CI_DRAFT
    ├── PR #57  CURRENT_PUBLIC_REPO_SSOT_DRAFT
    │   └── PR #61  CURRENT_AGENT_RUNTIME_CONTRACT_DRAFT
    │       └── PR #63  MACHINE_GATED_AUTHORITY_DRAFT
    └── PR #59  TRANSPORT_AND_SEMANTIC_IDENTITIES_SEPARATED_DRAFT
```

Hosted runs #88、#89、#90、#91、#92 都已在各自 exact PR head 執行成功。更早被 Actions budget 擋在 runner allocation 前的 SHA 仍保留為歷史 `PRE_RUN_BLOCKED` evidence，不會被後來 green run 改寫。

## 產品定位

Gym Come True 是 information/logging 與 protocol-execution 系統，不是自由生成補充品建議的 chatbot。

1. **Evidence-first capture**：ML Kit／Apple Vision OCR 與 barcode 結果先是 unverified evidence。
2. **Deterministic arithmetic**：只對相容 mass units 做通用換算；unsupported unit 與缺 serving evidence fail closed。
3. **Copyright-clean exercise intelligence**：metadata、rendering、media、model、UGC 分開治理。
4. **Nutrition provenance**：repo 內 food fixtures 是 synthetic/default-deny；LLM 不得創造 nutrient facts。
5. **A/B daily protocol**：deterministic 16:00／22:00 訓練日 schedule 支援跨午夜排序。
6. **Proof before explanation**：LLM 只能解釋 deterministic receipt，不能擁有 dose、diagnosis、regulatory 或 rights decision。

## Repository Map 與 State Machine 分工

```text
.
├── shared/                     deterministic domain contracts + shared UI
├── androidApp/                 Android evidence、reminders、Health Connect adapters
├── iosApp/                     Apple evidence、reminders、HealthKit read adapter
├── webApp/                     JS/Wasm projection
├── data/                       synthetic/Draft catalogs 與 schemas
├── legal/                      source/media/provenance boundaries
├── assets/                     first-party 或明確 admitted assets
├── scripts/                    deterministic validators 與 local-byte capture
├── docs/                       architecture、implementation、governance SSOT
├── docs/git/                   stacked-worker / Git Town governance
├── .github/workflows/          exact-head hosted verification
└── AGENTS.md                   root execution contract
```

| Directory | State Machine / responsibility | 目前 evidence ceiling |
|---|---|---|
| `shared/` | `UNVERIFIED -> USER_CONFIRMED -> DETERMINISTIC_RESULT -> RECEIPT` | 不提供 personalized safe-dose 或 clinical authority。 |
| `androidApp/` | permission/capture -> ML Kit candidate -> confirmation；reminder 與 least-privilege Health Connect adapters | Adapter/tests 已存在；real-device/OEM/privacy/store evidence 分開處理。 |
| `iosApp/` | picker/camera -> Vision candidate -> confirmation；UserNotifications 與 `NativeHealthReadBridge` | HealthKit read surface 已存在；entitlement/device/store evidence 分開處理。 |
| `webApp/` | `BOOTSTRAP -> SHARED_UI_READY -> USER_INPUT -> LOCAL_RESULT` | 不宣稱 native-health parity。 |
| `data/` | `SYNTHETIC_OR_DRAFT -> STRUCTURALLY_VALIDATED -> TEST_ONLY` | Exercise content 仍是 Draft；nutrition source candidates 仍為 `CANDIDATE + DENY`。 |
| `legal/` | `UNKNOWN -> REVIEW -> ALLOW/DENY -> REVOKED` | 權利不明 source/media 不得自行 admission。 |
| `assets/` | `QUARANTINED -> HASHED -> RIGHTS_REVIEWED -> ADMITTED -> REVOKED` | 除非存在 exact admission record，目前只使用 first-party schematic assets。 |
| `scripts/` | `INPUT -> VALIDATED -> PASS/FAIL` | Validator 不是 legal/clinical reviewer。 |
| `docs/` | `OBSERVED -> DOCUMENTED -> REVIEWED -> SUPERSEDED` | Staged PR #63 已 machine-gate authority drift。 |
| `.github/workflows/` | `QUEUED -> RUNNER_ALLOCATED -> EXECUTED -> PASS/FAIL` | Hosted runs 現在可執行；歷史 pre-run block 仍保留歷史身分。 |
| `docs/git/` | `TASK_PACKET -> LEASED -> VERIFIED -> PUBLICATION_GATE -> HUMAN_ADMIT` | Git Town consumer runtime 仍 denied。 |

## 端到端 Data Flows

### Supplement / Body Hacker Ledger

```text
明確 capture
-> on-device OCR / barcode
-> UNVERIFIED candidate
-> 實體 label confirmation
-> compatible-mass arithmetic
-> deterministic LOG / REVIEW / BLOCK receipt
-> A/B protocol compiler
-> Android / iOS / Web timeline
-> platform reminder
-> optional receipt-only explanation
```

### Taiwan Regulatory Evidence

```text
Mutable MOHW/TFDA reference
-> CANDIDATE + DENY
-> approved local bytes
-> SHA-256 / content address
-> legal/reuse review
-> exact mapping
-> qualified review
-> DRAFT -> REVIEWED -> STAGED -> ACTIVE
-> SUSPENDED / EXPIRED / REVOKED / ROLLED_BACK
```

`HASH_VERIFIED != LEGAL_REVIEWED != CLINICALLY_REVIEWED`。

### Exercise / Media Rights

```text
First-party metadata 或 candidate asset
-> quarantine
-> exact rights evidence
-> immutable hash
-> scope/territory/term/derivative review
-> ALLOW
-> deterministic package
-> takedown / revocation
```

Publicly reachable 不代表可商業再散布；vendor CDN hotlink 不是 admission path。

### Nutrition / Meal Plan

```text
Synthetic 或 admitted food record
-> provenance + serving/unit validation
-> deterministic nutrition arithmetic
-> user-selected targets/preferences
-> meal slots + A/B workout-day timetable
-> editable reminder commands
```

不得輸出 disease-treatment diet、medical calorie target 或 LLM-created nutrient facts。

### Worker / Stacked PR

```text
Work packet
-> branch/path lease
-> bounded edit
-> fixed evals + negative controls
-> exact-head publication
-> remote ancestry/check evidence
-> HUMAN_ADMIT for merge/promotion
```

## Current Capability Truth

| Capability | 目前狀態 | 尚未證明／admit |
|---|---|---|
| Android OCR/barcode | Bundled ML Kit candidate extraction | Representative consented corpus 與 real-device accuracy evidence |
| iOS OCR/barcode | Apple Vision candidate extraction | Representative consented corpus 與 real-device accuracy evidence |
| Supplement arithmetic | Shared deterministic mass arithmetic | Personalized safe dose／medication compatibility |
| Exercise catalog | 50-record first-party bilingual Draft + deterministic validator | Editorial/rights acceptance 與 licensed third-party media |
| Muscle visualization | First-party schematic/local mapping | 超出 declared schematic scope 的 anatomical/medical validation |
| Nutrition | Synthetic bilingual catalog + deterministic admission validator + meal-plan compiler | Real Taiwan source/version/reuse-rights admission |
| Android health | Health Connect availability/permission/read adapters + tests | Real-device/OEM/privacy/store evidence |
| iOS health | HealthKit least-privilege read adapter | Entitlement/user authorization/device/store evidence |
| Reminders | Android local reminders + iOS UserNotifications | Universal delivery、exact-alarm 或 AlarmKit reliability guarantee |
| LLM explanation | Receipt-only decision-preserving contract | Security/privacy Human Admit 與 live provider/deployment evidence |
| Artifact identity | PR #59 staged transport-vs-semantic receipts | Release reproducibility/signing/supply-chain attestation |
| Git Town | Pinned v24.0.0 candidate metadata + canary harness | Consumer config、binary execution、live sync/publication canaries、runtime admission |

## Git Town Boundary

Canonical method：[`skills-shared/skills/git-town-stacked-pr-worker`](https://github.com/ed3c/skills-shared/tree/main/skills/git-town-stacked-pr-worker)。

目前狀態：

```yaml
candidate: v24.0.0
candidate_metadata: VERIFIED
runtime: CANDIDATE_METADATA_VERIFIED_RUNTIME_BLOCKED
consumer_config: NOT_IMPLEMENTED
binary_execution_in_consumer: NOT_EXERCISED
sync_canary: NOT_EXERCISED
publication_canary: NOT_EXERCISED
background_sync: DISABLED
production_use: DENY
merge_ship_promotion: HUMAN_ADMIT
```

Git Town 只有在 runtime admission 後才能擁有 branch hierarchy/synchronization；它永遠不能證明 product correctness、legal/clinical acceptance、merge readiness 或 release readiness。

## Validation

目前 staged lineage 的 policy/convergence commands 包含：

```bash
python3 scripts/validate_repository.py
python3 scripts/validate_taiwan_rule_pack.py
python3 scripts/validate_taiwan_source_lifecycle.py
python3 scripts/validate_taiwan_source_hardening.py
python3 scripts/validate_stacked_delivery.py --self-test
python3 data/exercise-catalog/validate_catalog.py
python3 data/exercise-catalog/validate_catalog.py --selftest
python3 scripts/validate_nutrition_catalog.py
python3 scripts/validate_nutrition_catalog.py --self-test
python3 scripts/validate_authority_surfaces.py
python3 scripts/validate_authority_surfaces.py --self-test
sh ./gradlew :shared:jvmTest
sh ./gradlew :androidApp:assembleDebug :androidApp:lintDebug
sh ./gradlew :webApp:composeCompatibilityBrowserDistribution
```

Hosted macOS lane 另外會 link Kotlin iOS simulator framework、產生 canonical XcodeGen project，並 build unsigned simulator host。

## Remaining External / Human Admit Gates

Repository code 無法自行製造：

- 真實 consented Traditional Chinese label corpus 與 withdrawal/deletion operations；
- exact MOHW/TFDA bytes、reuse approval、qualified Taiwan review 與 production rule activation；
- real Taiwan food-composition source/version/reuse-rights mappings；
- exercise editorial/rights acceptance 與 licensed/commissioned media；
- real-device Health Connect/HealthKit/reminder evidence；
- security/privacy approval 與 production provider/store credentials；
- App Store／Google Play signing、listing、declaration 與 release-console operations；
- Git Town runtime admission 與 live consumer canaries；
- merge/release promotion。

## 文件索引

- [Implementation status](docs/implementation-status.md)
- [Architecture](docs/architecture.md)
- [Roadmap](docs/roadmap.md)
- [GitHub Issue / PR index](docs/github-issue-index.md)
- [Git / Stacked-PR governance](docs/git/README.md)
- [Molecular Stack graph](docs/git/STACKED_PRS.md)
- [Git Town admission](docs/git/GIT_TOWN_ADMISSION.md)
- [Authority surface contract](docs/authority-surface-contract.md)
- [Copyright and data governance](docs/copyright-and-data-governance.md)
- [Health and supplement safety](docs/health-safety.md)

## License

Repository 自行創作的程式碼與文件採用 **Apache License 2.0**，詳見 [LICENSE](LICENSE)。Third-party dependencies 與 assets 保留各自授權，詳見 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。Apache-2.0 不授予 third-party media、official-source redistribution、trademark、medical approval 或 store/release authorization。
