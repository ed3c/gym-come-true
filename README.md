# Gym Come True

[繁體中文](README.zh-TW.md)

Evidence-first fitness protocol execution for Android, iOS, and Web with Kotlin Multiplatform and Compose Multiplatform.

> **Current truth (2026-08-19):** `ed3c/gym-come-true` is public. `main@b1880abe317ac274b59695439c4f9682b8864f6b` contains the merged foundation and domain contracts. Hosted GitHub Actions now execute normally. Active Draft evidence is layered above `main`; green checks prove only their exact heads. The product is not clinically admitted, not store-release admitted, has no licensed third-party exercise-media catalog, and has not admitted a Git Town consumer runtime.

## Read order and evidence vocabulary

Agents must follow [AGENTS.md](AGENTS.md), [Implementation status](docs/implementation-status.md), [Architecture](docs/architecture.md), and [Git / Stacked-PR governance](docs/git/README.md).

| State | Meaning |
|---|---|
| `MERGED` | Code is in `main`; this does not imply external or production admission. |
| `OPEN DRAFT PR` | A reviewable GitHub subject exists and is not merged. |
| `PASS` / `FAIL` | A named command actually executed against the stated subject. |
| `PRE_RUN_BLOCKED` | A historical workflow stopped before runner execution; it is neither code pass nor code fail. |
| `ABSENT` | Required evidence is unavailable. |
| `NOT_IMPLEMENTED` | The capability is intentionally not present. |
| `NOT_EXERCISED` | A subject-bound runtime canary has not run. |
| `HUMAN_ADMIT` | Merge, release, legal/clinical/rights acceptance, signing or destructive production action remains human-owned. |

Hard laws:

```text
HOSTED_PASS(commit A) != HOSTED_PASS(commit B)
GITHUB_CHECK_PASS != HUMAN_ADMIT
ADAPTER_PRESENT != REAL_DEVICE_VALIDATION
GIT_TOWN_CANDIDATE != GIT_TOWN_RUNTIME_ADMITTED
```

## Current delivery graph

Historical foundation/convergence PRs #2, #15, #16, #20 and #22 are merged history.

Current Draft evidence stack:

```text
main@b1880abe...
└── PR #55  DOMAIN_VALIDATORS_OWNED_BY_CI_DRAFT
    ├── PR #57  CURRENT_PUBLIC_REPO_SSOT_DRAFT
    │   └── PR #61  CURRENT_AGENT_RUNTIME_CONTRACT_DRAFT
    │       └── PR #63  MACHINE_GATED_AUTHORITY_DRAFT
    └── PR #59  TRANSPORT_AND_SEMANTIC_IDENTITIES_SEPARATED_DRAFT
```

Exact-head hosted runs #88, #89, #90, #91 and #92 executed successfully on their own PR heads. Older budget-blocked SHAs remain historical `PRE_RUN_BLOCKED` evidence and are not rewritten as green.

## Product thesis

Gym Come True is an information/logging and protocol-execution system, not a free-form supplement advisor.

1. **Evidence-first capture** — ML Kit / Apple Vision OCR and barcode output begins as unverified evidence.
2. **Deterministic arithmetic** — compatible mass units are normalized; unsupported units and missing serving evidence fail closed.
3. **Copyright-clean exercise intelligence** — metadata, rendering, media, models and UGC are separate rights domains.
4. **Nutrition provenance** — checked-in food fixtures are synthetic/default-deny; LLMs cannot invent nutrient facts.
5. **A/B daily protocol** — deterministic 16:00 and 22:00 workout-day schedules support cross-midnight ordering.
6. **Explanation after proof** — an LLM may explain a deterministic receipt; it cannot own dose, diagnosis, regulatory or rights decisions.

## Repository map and State Machine ownership

```text
.
├── shared/                     deterministic domain contracts + shared UI
├── androidApp/                 Android evidence, reminders, Health Connect adapters
├── iosApp/                     Apple evidence, reminders, HealthKit read adapter
├── webApp/                     JS/Wasm projection
├── data/                       synthetic/Draft catalogs and schemas
├── legal/                      source/media/provenance boundaries
├── assets/                     first-party or explicitly admitted assets
├── scripts/                    deterministic validators and local-byte capture
├── docs/                       architecture, implementation and governance SSOT
├── docs/git/                   stacked-worker / Git Town governance
├── .github/workflows/          exact-head hosted verification
└── AGENTS.md                   root execution contract
```

| Directory | State Machine / responsibility | Current evidence ceiling |
|---|---|---|
| `shared/` | `UNVERIFIED -> USER_CONFIRMED -> DETERMINISTIC_RESULT -> RECEIPT` | No personalized safe-dose or clinical authority. |
| `androidApp/` | permission/capture -> ML Kit candidate -> confirmation; reminder and least-privilege Health Connect adapters | Adapter/tests exist; real-device/OEM/privacy/store evidence remains separate. |
| `iosApp/` | picker/camera -> Vision candidate -> confirmation; UserNotifications and `NativeHealthReadBridge` | HealthKit read surface exists; entitlement/device/store evidence remains separate. |
| `webApp/` | `BOOTSTRAP -> SHARED_UI_READY -> USER_INPUT -> LOCAL_RESULT` | No native-health parity claim. |
| `data/` | `SYNTHETIC_OR_DRAFT -> STRUCTURALLY_VALIDATED -> TEST_ONLY` | Exercise content remains Draft; nutrition source candidates remain `CANDIDATE + DENY`. |
| `legal/` | `UNKNOWN -> REVIEW -> ALLOW/DENY -> REVOKED` | No unknown-rights media/source can self-admit. |
| `assets/` | `QUARANTINED -> HASHED -> RIGHTS_REVIEWED -> ADMITTED -> REVOKED` | First-party schematic assets only unless an exact admission record exists. |
| `scripts/` | `INPUT -> VALIDATED -> PASS/FAIL` | Validators are not legal/clinical reviewers. |
| `docs/` | `OBSERVED -> DOCUMENTED -> REVIEWED -> SUPERSEDED` | Authority drift is machine-gated on staged PR #63. |
| `.github/workflows/` | `QUEUED -> RUNNER_ALLOCATED -> EXECUTED -> PASS/FAIL` | Current hosted runs execute; historical pre-run blocks remain historical evidence. |
| `docs/git/` | `TASK_PACKET -> LEASED -> VERIFIED -> PUBLICATION_GATE -> HUMAN_ADMIT` | Git Town consumer runtime remains denied. |

## End-to-end data flows

### Supplement / Body Hacker ledger

```text
Explicit capture
-> on-device OCR / barcode
-> UNVERIFIED candidate
-> physical-label confirmation
-> compatible-mass arithmetic
-> deterministic LOG / REVIEW / BLOCK receipt
-> A/B protocol compiler
-> Android / iOS / Web timeline
-> platform reminder
-> optional receipt-only explanation
```

### Taiwan regulatory evidence

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

`HASH_VERIFIED != LEGAL_REVIEWED != CLINICALLY_REVIEWED`.

### Exercise / media rights

```text
First-party metadata or candidate asset
-> quarantine
-> exact rights evidence
-> immutable hash
-> scope/territory/term/derivative review
-> ALLOW
-> deterministic package
-> takedown / revocation
```

Publicly reachable media is not automatically redistributable. Vendor CDN hotlinking is not an admission path.

### Nutrition / meal plan

```text
Synthetic or admitted food record
-> provenance + serving/unit validation
-> deterministic nutrition arithmetic
-> user-selected targets/preferences
-> meal slots + A/B workout-day timetable
-> editable reminder commands
```

No disease-treatment diet, medical calorie target or LLM-created nutrient fact is admitted.

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

## Current capability truth

| Capability | Current state | Not yet proven/admitted |
|---|---|---|
| Android OCR/barcode | Bundled ML Kit candidate extraction | Representative consented corpus and real-device accuracy evidence |
| iOS OCR/barcode | Apple Vision candidate extraction | Representative consented corpus and real-device accuracy evidence |
| Supplement arithmetic | Shared deterministic mass arithmetic | Personalized safe dose / medication compatibility |
| Exercise catalog | 50-record first-party bilingual Draft + deterministic validator | Editorial/rights acceptance and licensed third-party media |
| Muscle visualization | First-party schematic/local mapping | Anatomical/medical validation beyond declared schematic scope |
| Nutrition | Synthetic bilingual catalog + deterministic admission validator + meal-plan compiler | Real Taiwan source/version/reuse-rights admission |
| Android health | Health Connect availability/permission/read adapters + tests | Real-device/OEM/privacy/store evidence |
| iOS health | HealthKit least-privilege read adapter | Entitlement/user authorization/device/store evidence |
| Reminders | Android local reminders + iOS UserNotifications | Universal delivery, exact-alarm or AlarmKit reliability guarantees |
| LLM explanation | Receipt-only decision-preserving contract | Security/privacy Human Admit and live provider/deployment evidence |
| Artifact identity | Staged transport-vs-semantic identity receipts in PR #59 | Release reproducibility / signing / supply-chain attestation |
| Git Town | Pinned v24.0.0 candidate metadata and canary harness | Consumer config, binary execution, live sync/publication canaries, runtime admission |

## Git Town boundary

Canonical method: [`skills-shared/skills/git-town-stacked-pr-worker`](https://github.com/ed3c/skills-shared/tree/main/skills/git-town-stacked-pr-worker).

Current state:

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

Git Town may own branch hierarchy/synchronization only after runtime admission. It never proves product correctness, legal/clinical acceptance, merge readiness or release readiness.

## Validation

Policy/convergence commands on the current staged lineage include:

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

The hosted macOS lane also links the Kotlin iOS simulator framework, generates the canonical XcodeGen project and builds the unsigned simulator host.

## Remaining external / Human Admit gates

Repository code cannot manufacture:

- consented real Traditional Chinese label corpus and withdrawal/deletion operations;
- exact MOHW/TFDA bytes, reuse approval, qualified Taiwan review and production rule activation;
- real Taiwan food-composition source/version/reuse-rights mappings;
- exercise editorial/rights acceptance and licensed/commissioned media;
- real-device Health Connect/HealthKit/reminder evidence;
- security/privacy approval and production provider/store credentials;
- App Store / Google Play signing, listings, declarations and release-console operations;
- Git Town runtime admission and live consumer canaries;
- merge/release promotion.

## Document index

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

Repository-authored code and documentation are licensed under the **Apache License 2.0**; see [LICENSE](LICENSE). Third-party dependencies and assets retain their own terms; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Apache-2.0 does not grant rights to third-party media, official-source redistribution, trademarks, medical approval or store/release authorization.
