# AGENTS.md — Gym Come True execution contract

This repository builds an evidence-first fitness protocol product for Android, iOS, and Web. Agents must preserve the safety, rights, privacy, and platform boundaries below even when a task asks for faster delivery.

## Current state

```text
EMPTY_REPOSITORY
  -> AUDITABLE_CROSS_PLATFORM_FOUNDATION   # current transition / Issue #1
  -> REVIEWED_TAIWAN_RULE_PACK
  -> LICENSED_EXERCISE_CATALOG
  -> NATIVE_HEALTH_AND_ALARM_INTEGRATION
  -> PRIVATE_LLM_EXPLANATION_GATEWAY
  -> STORE_RELEASE_CANDIDATE
```

Do not claim a later state merely because an interface, stub, prompt, or sample record exists.

## Hard invariants

### OCR_IS_EVIDENCE_NOT_TRUTH

- OCR and barcode results begin as `UNVERIFIED`.
- Preserve the physical-label confirmation step.
- Never infer a missing serving size, ingredient, unit, product identity, or daily amount.
- Raw label images are temporary by default and must be deleted after local processing unless the user explicitly opts into encrypted storage.

### MEDIA_DEFAULT_DENY

- Publicly reachable is not the same as redistributable.
- Do not add images, GIFs, video, SVG anatomy maps, 3D models, scraped IDs, or CDN links unless `legal/media-registry.json` contains an `ALLOW` record with a rights reference and SHA-256.
- Do not hotlink ExerciseDB or another vendor CDN.
- Keep metadata, media, rendering code, and user-generated uploads as separate rights domains.

### LLM_EXPLANATION_ONLY

- Deterministic code owns unit conversion, warnings, blocking decisions, and protocol state.
- A model may explain a structured result; it may not calculate or recommend dosage, diagnose, suppress a warning, or fill missing evidence.
- The client must not call a model provider directly. Future model access goes through a server-side policy gateway with minimized payloads and audit logs.

### NO_CLIENT_PROVIDER_SECRETS

- Never commit API keys, signing material, service-account credentials, store secrets, or private health-rule packs.
- Mobile and web artifacts must be safe to inspect and reverse engineer.
- Use protected CI/store secret systems only after the corresponding delivery issue explicitly permits them.

### REVIEWED_HEALTH_RULES_ONLY

- Generic mass conversion is limited to `mcg/µg/μg`, `mg`, and `g`.
- `IU`, activity units, proprietary blends, medication interactions, pregnancy, procedures, and symptoms fail closed.
- A Taiwan or other regional rule pack is not production-ready until the source, reviewer, version, effective date, and rollback are recorded.

### HONEST_ALARM_SEMANTICS

- Android `set`/`setAndAllowWhileIdle` and iOS local notifications are reminders, not guaranteed alarms.
- Exact-alarm special access and AlarmKit require their own permission, review, fallback, and store-policy work.
- Never market background delivery as 100% reliable without measured evidence and platform qualification.

## Module ownership

```text
shared/
  domain models, deterministic safety, protocol compiler, tests, shared Compose UI

androidApp/
  Android permissions, system camera hand-off, ML Kit, temporary files, notifications,
  future Health Connect and exact-alarm adapters

iosApp/
  SwiftUI host, Vision evidence adapter, UserNotifications,
  future HealthKit and AlarmKit adapters

webApp/
  JS/Wasm host and browser-safe features; no native-health parity claim

legal/ + data/
  provenance and admission truth; build must fail closed

docs/
  product, legal, safety, marketing, and delivery decisions
```

Shared code must not import Android, Apple, browser, store, or model-provider APIs.

## Canonical iOS source set

`iosApp/project.safe.yml` is the admitted iOS build specification for this foundation. It explicitly lists the Swift files that CI compiles. New Swift files do not enter the build merely by being placed in the directory; add them to the safe spec and provide validation evidence.

## Required commands

```bash
python3 scripts/validate_repository.py
sh ./gradlew :shared:jvmTest
sh ./gradlew :androidApp:assembleDebug :androidApp:lintDebug
sh ./gradlew :webApp:jsBrowserDistribution
sh ./gradlew :webApp:wasmJsBrowserDistribution
```

On macOS with XcodeGen:

```bash
cd iosApp
xcodegen generate --spec project.safe.yml
xcodebuild \
  -project GymComeTrue.xcodeproj \
  -scheme GymComeTrue \
  -sdk iphonesimulator \
  -configuration Debug \
  CODE_SIGNING_ALLOWED=NO \
  build
```

A documentation-only change may skip platform builds only when it cannot alter executable policy, source admission, store claims, or generated build inputs.

## Change protocol

1. Identify one state transition and its rollback.
2. Define allowed paths before modifying files.
3. Add deterministic tests before adding an LLM prompt.
4. Add source and media records before importing data or assets.
5. Keep platform permissions behind explicit user actions.
6. Record what was actually validated; separate local checks from hosted CI evidence.
7. Open a draft PR until all exact-head required checks pass.

## Prohibited shortcuts

- No WebView shell presented as native KMP completion.
- No scraped production catalog.
- No remote media hotlink.
- No direct client-to-LLM provider key.
- No unreviewed supplement threshold or interaction table.
- No automatic schedule change based on OCR alone.
- No fabricated clinical, copyright, reliability, revenue, download, or conversion claim.
- No switching repository visibility, transferring ownership, weakening branch protection, or changing licensing without explicit owner action.

## Review questions

Every PR must answer:

- What evidence changed from unknown to known?
- Which deterministic invariant protects the user if OCR or a model is wrong?
- Which rights record permits every new asset and field?
- What data leaves the device, and why is it necessary?
- What happens when permission, network, model, store, or platform API access fails?
- Which capability is still not implemented?
