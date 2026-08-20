# Implementation status

**Authoritative snapshot:** 2026-08-19  
**Repository:** `ed3c/gym-come-true` — public, immutable repository ID `1334805292`  
**Merged `main` at snapshot:** `b1880abe317ac274b59695439c4f9682b8864f6b`  
**Current staged parent:** Draft PR #55, `agent/converge-domain-validation@1338b6fd2a1007cf06e24aca3a6a4bd07f9b7fa5`  
**This SSOT packet:** Issue #56, docs-only child of PR #55

This file separates merged repository truth, staged-but-unmerged evidence, and external Human Admit gates. A green check on one SHA never retroactively proves an older SHA, and code/tests never imply legal, clinical, rights, device, store, provider, or production admission.

## Historical stack already merged

| PR | Merged state | Current meaning |
|---:|---|---|
| #2 | `AUDITABLE_CROSS_PLATFORM_FOUNDATION` | KMP Android/iOS/Web foundation with default-deny health/evidence/media boundaries |
| #15 | `TAIWAN_EVIDENCE_CONTRACT_DRAFT` | Product/corpus identity, OCR metrics, rule-pack admission and decision receipts |
| #16 | `TAIWAN_SOURCE_LIFECYCLE_DRAFT` | Immutable source, exact mapping, release/revoke/rollback contracts |
| #20 | `DOCUMENTED_GIT_TOWN_DELIVERY_GRAPH_DRAFT` | README/AGENTS/architecture/roadmap and repository-owned delivery governance |

These PRs are merged history, not active Draft PRs.

## Current staged convergence

Draft PR #55 is one commit ahead of `main` and changes only the exercise/nutrition owning-oracle layer:

```text
main@b1880abe...
└── PR #55 agent/converge-domain-validation@1338b6fd...
    MERGED_DOMAIN_LANES_WITH_EVIDENCE_GAPS
      -> DOMAIN_VALIDATORS_OWNED_BY_CI_DRAFT
```

PR #55 adds `scripts/validate_nutrition_catalog.py`, wires both exercise and nutrition baseline/self-tests into `policy-and-provenance`, and updates the two data-lane READMEs. It does not alter Kotlin domain semantics or admit external evidence.

Exact-head GitHub Actions `verify` run #88 (`32250370996`) completed successfully on `1338b6fd2a1007cf06e24aca3a6a4bd07f9b7fa5`:

- `policy-and-provenance`: PASS, including exercise baseline/self-test and nutrition baseline/10-mutation self-test;
- `android-web-domain`: PASS, including shared JVM tests, Android debug/lint, Web compatibility distribution, and uploaded artifacts;
- `ios-framework-and-host`: PASS, including Kotlin simulator framework, canonical XcodeGen generation, and unsigned simulator host build.

This evidence proves PR #55's exact head only. PR #55 remains Draft; merge is Human Admit.

## Current domain truth

| Domain | Merged/staged engineering state | Still not admitted |
|---|---|---|
| Supplement evidence | OCR/barcode candidates, confirmed-mass arithmetic, duplicate detection, deterministic `LOG/REVIEW/BLOCK`, A/B timetable, decision receipts | personalized dose/diagnosis, medication compatibility, clinically reviewed Taiwan rule pack |
| Taiwan source lifecycle | immutable-source, exact-mapping, release/revoke/rollback contracts and local-byte capture boundary | real official source bytes, exact reuse approval, legal review, qualified clinical review, production activation |
| Exercise taxonomy/catalog | canonical taxonomy and 50-record first-party bilingual `DRAFT` catalog are merged; deterministic gate exists; PR #55 gives it CI ownership | editorial/rights acceptance, licensed third-party media, production promotion |
| Nutrition/meal plan | `FoodCatalogAdmissionValidator`, bilingual synthetic catalog, `CANDIDATE + DENY` source fixtures, and deterministic meal-plan compiler are merged; PR #55 adds the repository-level nutrition oracle | real Taiwan food-composition source/version/license approval, exact mappings, production food records |
| Explanation gateway | receipt-only, decision-preserving explanation contract is merged | security/privacy Human Admit, live provider/secret/deployment evidence, production traffic |
| Android health | Health Connect availability/permission adapter surfaces and tests exist | real-device/OEM evidence, production disclosure/privacy evidence, any claim of universal availability |
| iOS health | `HKHealthStore` bridge plus shared read-access decision logic exists | HealthKit entitlement/device authorization evidence, store disclosure, real-device validation |
| Reminders | Android local reminder and iOS notification/recurrence contracts exist | exact-alarm/AlarmKit product admission, OEM/device reliability guarantees |
| Git Town | v24.0.0 candidate metadata, static verifier and disposable canary harness exist | runtime admission, consumer `.git-town.toml`, live consumer sync/publication canaries, legal/supply-chain approval |

## Evidence boundaries that remain hard laws

```text
OCR_CANDIDATE != CONFIRMED_FACT
HASH_VERIFIED != LEGAL_REVIEWED
LEGAL_REVIEWED != CLINICALLY_REVIEWED
DRAFT_CONTENT != RIGHTS_ADMITTED_CONTENT
CONTRACT_CODE != LIVE_PROVIDER_EVIDENCE
ADAPTER_PRESENT != REAL_DEVICE_VALIDATION
GITHUB_CHECK_PASS != HUMAN_ADMIT
GIT_TOWN_CANDIDATE != GIT_TOWN_RUNTIME_ADMITTED
```

No LLM may invent nutrient facts, recommend a supplement dose, override a deterministic warning, sign a rule pack, approve rights, or promote a source/media record.

## Open issues and actual remaining gates

| Issue | Engineering state | Remaining gate |
|---:|---|---|
| #32 | `TAXONOMY_CONTRACT` present on `main`; owning validator staged in PR #55 | taxonomy/rights Human Admit |
| #33 | first-party bilingual top-50 `DRAFT` present; owning validator staged in PR #55 | editorial/rights Human Admit |
| #35 | `EXPLANATION_GATEWAY_CONTRACT_DRAFT` present | security/privacy Human Admit |
| #46 | schema/admission contract and synthetic/default-deny catalog present; repository validator staged in PR #55 | real source/version/reuse-rights review and admitted mappings |
| #47 | deterministic meal-plan compiler and tests present; shared JVM job passed on PR #55 | parent #46 must provide admitted real food records |
| #54 | convergence implementation is PR #55 and exact-head hosted checks are green | merge PR #55 (Human Admit) |
| #56 | this documentation reconciliation packet | exact-head child verification and merge (Human Admit) |

Issue #45 is closed: Actions capacity is currently sufficient for the active Draft stack, as demonstrated by run #88. Older budget-blocked runs remain historical `PRE_RUN_BLOCKED` evidence and are not rewritten as green.

## Directory-level implementation highlights

### `shared/`

- supplement parsing and compatible mass normalization (`mcg/mg/g` only);
- verified daily arithmetic, duplicate detection, deterministic safety decisions;
- A 16:00 / B 22:00 protocol with cross-midnight ordering;
- Taiwan evidence/source lifecycle and decision-receipt contracts;
- exercise taxonomy/catalog contracts and muscle mapping;
- nutrition catalog admission and deterministic meal-plan compiler;
- shared Health/evidence/reminder decision contracts;
- explanation boundary that cannot own the safety decision.

### `androidApp/`

- Compose host, system-camera capture, private cache `FileProvider`;
- bundled ML Kit Chinese OCR/barcode candidates and temporary-file deletion;
- notification permission/reminder surfaces;
- Health Connect availability/permission integration surfaces.

Real device/OEM reliability and production privacy/store evidence remain absent.

### `iosApp/`

- SwiftUI/Compose host with canonical `iosApp/project.yml`;
- PhotosPicker / Vision OCR+barcode evidence path;
- notification scheduling bridge;
- `HKHealthStore` integration surface and shared Health read policy.

Entitlements, real-user authorization, AlarmKit admission, signing and real-device/store evidence remain separate gates.

### `webApp/`

- Kotlin/Wasm + Kotlin/JS compatibility distribution;
- shared UI/runtime contract.

Production hosting, browser-notification reliability and native health parity are not implied.

### `data/`, `legal/`, `assets/`

- synthetic/default-deny Taiwan evidence and nutrition fixtures;
- first-party exercise catalog and schematic muscle asset;
- default-deny source/media governance.

No unreviewed third-party media, scraped nutrition database, official source byte, or vendor CDN hotlink is production-admitted.

## Git Town / Worker state

`docs/git/GIT_TOWN_ADMISSION.md` is authoritative for runtime admission. Current state is:

```yaml
state: CANDIDATE_METADATA_VERIFIED_RUNTIME_BLOCKED
runtime_admitted: false
consumer_config_admitted: false
archive_materialized_in_current_environment: false
binary_executed_in_current_environment: false
live_canaries_exercised: false
background_sync_enabled: false
publication_enabled: false
production_use: DENY
```

The repository contains the candidate packet, verifier and disposable canary harness. `.git-town.toml` remains `NOT_IMPLEMENTED`; no consumer sync, publication, merge, ship or rollback authority is delegated to Git Town.

## Verification contract

On PR #55 the following owning lanes have exact-head hosted PASS:

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

sh ./gradlew :shared:jvmTest
sh ./gradlew :androidApp:assembleDebug :androidApp:lintDebug
sh ./gradlew :webApp:composeCompatibilityBrowserDistribution
```

The macOS job also linked the Kotlin simulator framework, generated the canonical Xcode project and built the unsigned simulator host. Re-run exact-head checks after any code/evidence/workflow change; do not reuse run #88 after the head moves.

## Not production-admitted

The repository does not currently establish:

- diagnosis, medication advice or personalized supplement dose recommendations;
- a clinically reviewed Taiwan dose/interaction rule pack;
- a real consented production label corpus in Git;
- an admitted Taiwan food-composition dataset or medical diet target;
- licensed third-party exercise media;
- Health Connect/HealthKit real-device production validation;
- Android exact-alarm or AlarmKit delivery guarantees;
- live LLM/store/provider credentials or production authority;
- App Store / Google Play signing and submission;
- Git Town consumer runtime admission;
- any external legal, clinical, editorial, rights or security approval inferred solely from code/tests.

## Next admitted work

1. Human review/merge PR #55 after reading the exact-head run #88 receipt.
2. Keep #32/#33/#35/#46 open until their explicit Human Admit/external gates are satisfied.
3. Treat #47 as staged code blocked on admitted real food records from #46.
4. Complete Issue #56 only as this docs-only child; it must not modify parent code/evidence.
5. Continue future work from the dependency graph in `docs/roadmap.md`, `docs/github-issue-index.md`, and `docs/git/STACKED_PRS.md` without reopening already-merged implementations.
