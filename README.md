# Gym Come True

[繁體中文](README.zh-TW.md)

Evidence-first fitness planning for Android, iOS, and Web, built with Kotlin Multiplatform and Compose Multiplatform.

> **Status:** auditable foundation + Taiwan evidence-contract draft. This repository is not a medical device and does not provide diagnosis, treatment, medication advice, or automatic supplement dosing.

## Delivery stack

```text
PR #2  AUDITABLE_CROSS_PLATFORM_FOUNDATION
  └─ PR #15  TAIWAN_EVIDENCE_CONTRACT_DRAFT
       └─ Issue #8  REVIEWED_TAIWAN_RULE_PACK                 # not complete

Follow-up issues:
#9  iOS native evidence / Apple Health / reminders / AlarmKit
#10 Android Health Connect / reminder reliability
#11 Copyright-clean exercise catalog / licensed media
#12 Private LLM explanation gateway / adversarial evals
#13 Entitlements / privacy / stores / release operations
#14 Creator-market validation / launch evidence
```

PR #15 is stacked on the exact current PR #2 head. Its evidence commit is preserved through an explicit merge/relock rather than a force reset. Both PRs remain Draft until exact-head hosted validation executes successfully.

## Product thesis

Most fitness apps optimize for workout logging or generic AI chat. Gym Come True takes a narrower position:

1. **Copyright-clean exercise intelligence** — exercise metadata, media, and anatomy assets are admitted only through a provenance registry. Unknown rights fail closed.
2. **Taiwan and Asian-label evidence capture** — on-device OCR and barcode scanning produce unverified evidence candidates, not facts.
3. **Deterministic protocol safety** — compatible mass units can be normalized by code; IU conversion, drug interactions, and dose recommendations are blocked or sent to human review.
4. **Daily Body Hacker ledger** — verified mass arithmetic can be aggregated and overlapping ingredients can be surfaced across products without interpreting a safe or recommended dose.
5. **A/B daily protocol execution** — the same plan supports a 16:00 workout day and a 22:00 workout day, including cross-midnight ordering, reminders, meals, recovery, and confirmation checkpoints.
6. **Proof before advice** — an LLM may explain verified rule-engine output. It may not invent ingredients, calculate dosage, override a warning, or act as the safety authority.

## What is implemented

### Foundation — PR #2

- Shared Kotlin domain models, Traditional Chinese/English supplement-label parser, unit normalization, daily intake arithmetic, duplicate-ingredient detection, safety evaluation, A/B schedule compiler, and tests.
- Shared Compose dashboard and a locally rendered muscle-activation view without third-party anatomy media.
- Android shell with system-camera capture, bundled Google ML Kit Chinese/Latin text recognition and barcode scanning, temporary-file deletion, and inexact local reminders.
- iOS SwiftUI/Compose shell with native PhotosPicker, Apple Vision OCR/barcode extraction, and a local-notification reminder control.
- Web JS and Wasm entry points plus a compatibility distribution.
- Default-deny exercise source and media registries, first-party provenance records, policy validation, and CI contracts.

### Taiwan evidence contract — PR #15

- Product-variant identity across market, barcode, internal product ID, formulation, and label revision.
- Consent-aware corpus admission with default no-image retention and fail-closed withdrawal/unknown states.
- Field-level OCR observations that separate first-pass accuracy from correction completion.
- Default-deny MOHW/TFDA source registry candidates.
- Deterministic Taiwan rule-pack admission gates, source/reviewer coverage, safety-case requirements, rollback identity, and decision receipts.
- Synthetic Traditional Chinese fixtures and JSON Schema transport contracts.

This slice is an engineering admission contract only. It does not contain a production Taiwan rule pack, real consented label corpus, personalized safe-dose thresholds, medication-interaction conclusions, or a qualified reviewer attestation.

## Repository map

```text
.
├── androidApp/                 # Android application and device adapters
├── iosApp/                     # SwiftUI host, Vision/notification bridge, XcodeGen spec
├── shared/                     # KMP domain, rules, tests, and Compose UI
├── webApp/                     # JS/Wasm browser application
├── assets/                     # First-party assets with provenance
├── data/                       # Original/example data and Taiwan synthetic fixtures
├── legal/                      # Source, media, provenance, Taiwan source registries
├── docs/                       # Architecture, compliance, strategy, safety, roadmap
├── scripts/                    # Repository and rule-pack policy verification
├── .github/workflows/          # Hosted build evidence
├── AGENTS.md                   # Agent execution contract and hard laws
└── THIRD_PARTY_NOTICES.md      # Dependency and asset obligations
```

## Architecture

```mermaid
flowchart LR
    Camera[Camera / Photo] --> OCR[Platform OCR + Barcode]
    OCR --> Evidence[Unverified ScanEvidence]
    Evidence --> Confirm[User confirmation]
    Confirm --> Ledger[Daily intake ledger]
    Ledger --> Rules[Deterministic Safety Engine]
    Rules -->|log / review / block| Protocol[Daily Protocol Compiler]
    Protocol --> UI[Shared Compose UI]
    Protocol --> Reminder[Platform Reminder Adapter]
    Rules --> Receipt[Versioned decision receipt]
    Receipt --> SafePayload[Minimized structured payload]
    SafePayload --> Gateway[Future server-side LLM gateway]
    Gateway --> Explain[Explanation only]

    TaiwanSource[MOHW / TFDA candidate] --> SourceRegistry[Default-deny source registry]
    SourceRegistry --> Snapshot[Future archived snapshot + SHA-256]
    Snapshot --> Review[Future qualified review]
    Review --> Pack[Reviewed rule-pack admission]
    Pack --> Rules

    Source[Exercise source] --> Registry[Source + media registry]
    Registry -->|ALLOW with evidence| Import[Build-time importer]
    Registry -->|REVIEW / DENY| Quarantine[No production shipment]
    Import --> ExerciseDB[Local exercise database]
    ExerciseDB --> UI
```

The shared module owns business state and deterministic decisions. Platform modules own permissions, camera/OCR, Health APIs, reminders, and store-specific behavior. No API key or privileged health rule is stored in a mobile or web client.

## Safety contract

- OCR output always starts as `UNVERIFIED`.
- Only `mcg/µg/μg`, `mg`, and `g` use generic mass conversion.
- `IU`, volume, capsule/tablet count, and other activity or container units are never converted without ingredient-specific, reviewed rules.
- Daily totals are arithmetic observations, not safety limits or recommendations.
- Medication use, pregnancy, surgery, adverse symptoms, missing label evidence, or conflicting products require professional review.
- The application never turns a user-entered supplement schedule into a medical recommendation.
- Raw label images are temporary by default; production corpus retention requires explicit consent, encryption, expiry, withdrawal support, and provenance.
- LLM output is explanatory and non-authoritative; deterministic warnings cannot be suppressed by model text.
- A Taiwan rule pack is not production-admitted merely because its schema validates; archived source evidence and qualified review are separate hard gates.

See [docs/health-safety.md](docs/health-safety.md) and [docs/taiwan-supplement-evidence.md](docs/taiwan-supplement-evidence.md).

## Copyright and data admission

No third-party exercise image, GIF, video, SVG body map, scraped dataset, or remote CDN URL is shipped merely because it appears in a public repository. Every production asset must have an `ALLOW` record with a source, scope, license evidence, immutable hash, and review date. Current visual assets are first-party schematic material; no third-party exercise media is admitted.

See [docs/copyright-and-data-governance.md](docs/copyright-and-data-governance.md) and `legal/*.json`.

## Local development

Prerequisites:

- JDK 21
- Gradle **9.5.0** available as `gradle`
- Android SDK Platform 36
- Xcode and XcodeGen for the iOS host

The checked-in `gradlew` is a thin, fail-fast launcher that delegates to an installed Gradle 9.5.0. It does not silently download executable code.

```bash
python3 scripts/validate_repository.py
python3 scripts/validate_taiwan_rule_pack.py
./gradlew :shared:jvmTest
./gradlew :androidApp:assembleDebug :androidApp:lintDebug
./gradlew :webApp:composeCompatibilityBrowserDistribution
```

Run browser development builds:

```bash
./gradlew :webApp:jsBrowserDevelopmentRun
# or
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

Generate and open the iOS host:

```bash
cd iosApp
xcodegen generate --spec project.yml
open GymComeTrue.xcodeproj
```

Set Apple signing locally; do not commit signing credentials.

## Honest capability matrix

| Capability | Android | iOS | Web | Current state |
|---|---:|---:|---:|---|
| Shared dashboard and schedule | Yes | Yes | Yes | Foundation |
| OCR label extraction | Bundled Chinese/Latin ML Kit | Vision via photo picker | Manual/import later | Candidate evidence only |
| Barcode extraction | ML Kit | Vision | Planned | Not a product truth source |
| Daily intake arithmetic | Shared | Shared | Shared | No safety-limit interpretation |
| Taiwan rule-pack admission contract | Shared | Shared | Shared | Draft contract; no production pack |
| Local reminders | Inexact AlarmManager | UserNotifications | Browser notification later | No exact-delivery guarantee |
| Health data | Adapter boundary | Adapter boundary | Not applicable | Not implemented |
| System alarm challenge | Future exact-alarm review | Future AlarmKit review | Not applicable | No coercive/undismissable promise |
| LLM explanation | Contract only | Contract only | Contract only | Server gateway not implemented |
| Licensed third-party exercise media | None | None | None | Default deny |

## Product and launch documents

- [Architecture and data flow](docs/architecture.md)
- [Health and supplement safety](docs/health-safety.md)
- [Taiwan supplement evidence contract](docs/taiwan-supplement-evidence.md)
- [Copyright and source governance](docs/copyright-and-data-governance.md)
- [Blue-ocean product strategy](docs/product-strategy.md)
- [90-day marketing plan and UGC material](docs/marketing-plan.md)
- [Delivery roadmap and issue stack](docs/roadmap.md)
- [Implementation status](docs/implementation-status.md)

## Delivery state machine

```text
EMPTY_REPOSITORY
  -> AUDITABLE_CROSS_PLATFORM_FOUNDATION       # PR #2
  -> TAIWAN_EVIDENCE_CONTRACT_DRAFT            # PR #15
  -> REVIEWED_TAIWAN_RULE_PACK                  # Issue #8, blocked on real evidence/review
  -> LICENSED_EXERCISE_CATALOG                  # Issue #11
  -> NATIVE_HEALTH_AND_ALARM_INTEGRATION        # Issues #9 / #10
  -> PRIVATE_LLM_EXPLANATION_GATEWAY            # Issue #12
  -> STORE_RELEASE_CANDIDATE                     # Issues #13 / #14
```

Each transition requires code, policy, provenance, privacy review, and exact-head hosted build evidence. Later phases must not weaken earlier safety or rights guarantees.

## Hosted evidence status

PR #2 and PR #15 remain Draft. The latest exact-head PR #15 workflow was prevented from allocating a runner by the repository's GitHub Actions budget. A pre-run infrastructure failure is neither a passing build nor evidence of a product-code failure. Do not promote either PR based on stale or partial runs.

## License

Application code is currently proprietary and all rights are reserved. Third-party dependencies retain their own licenses; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). No third-party exercise media license is granted by this repository.
