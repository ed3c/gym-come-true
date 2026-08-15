# Implementation status

**Transition:** empty repository → auditable cross-platform foundation  
**Issue:** #1  
**Branch:** `agent/bootstrap-kmp-fitness-platform`  
**Status:** draft; exact-head hosted CI evidence required before merge

## Implemented

### Shared KMP domain

- evidence-status model;
- English and Traditional Chinese OCR-label parser;
- support for ingredient names such as B12/D3 and comma/decimal formats;
- generic `mcg/mg/g` mass conversion;
- explicit refusal to generically convert IU, volume, and container-count units;
- deterministic safety decisions and reason list;
- daily verified-mass ledger and duplicate ingredient detection across products;
- A 16:00 and B 22:00 daily protocol compiler;
- cross-midnight `dayOffset` ordering;
- immutable LLM explanation boundary with dose/warning override disabled;
- domain tests for parsing, arithmetic, units, blocking, overlap, log-only behavior, rights default-deny, and midnight order.

### Shared Compose interface

- Android/iOS/Web dashboard;
- safety gate;
- platform action callbacks;
- A/B plan selector;
- local schematic muscle activation view;
- protocol timeline and confirmation markers;
- visible non-medical disclaimer.

### Android

- Compose host;
- explicit system-camera capture through `TakePicture`;
- private `FileProvider` scoped to cache;
- bundled ML Kit Chinese text recognition, which also handles Latin text;
- barcode extraction;
- recognized-text SHA-256 and unverified candidate parsing;
- temporary image deletion in success and failure paths;
- notification permission request;
- inexact `AlarmManager` reminder and notification receiver.

### iOS

- SwiftUI host for shared Compose UI;
- one canonical explicit XcodeGen specification: `project.yml`;
- PhotosPicker control wired to local Apple Vision OCR/barcode extraction;
- recognized-text SHA-256;
- visible unverified-evidence summary;
- UserNotifications reminder control;
- camera/photo usage-purpose strings.

### Web

- Compose JS executable;
- Compose Wasm executable;
- Wasm/JS compatibility distribution;
- browser host and responsive full-viewport setup.

### Rights, safety, and delivery

- source registry with `ALLOW/REVIEW/DENY` decisions;
- default-deny media registry;
- first-party exercise seed and first-party schematic provenance;
- secret, hotlink, media, LLM-boundary, registry, seed, toolchain, and documentation validator;
- dependency/asset notices;
- architecture, health-safety, copyright, product strategy, marketing, platform, store, and roadmap documents;
- Linux Android/Web/shared CI and macOS iOS CI.

## Partial or intentionally constrained

| Capability | Foundation behavior | Missing production work |
|---|---|---|
| Supplement recognition | label text/barcode candidates | product identity, table/serving disambiguation, confidence UX, representative-label evaluation |
| Daily supplement ledger | verified mass arithmetic and overlap | chemical-form ontology, reviewed Taiwan limits, clinician workflow |
| Supplement analysis | deterministic evidence and blocking | reviewed Taiwan rule pack, medication data provider, qualified review, incident process |
| LLM | typed explanation payload only | authenticated server gateway, provider, output validator, evals, audit, kill switch |
| Exercise catalog | first-party demo/schema records | production taxonomy, translations, importer, reviewed/commissioned top-50 |
| Exercise media | first-party schematic only | executed licenses, exercise assets, transcode/CDN pipeline, revocation test |
| Muscle visualization | original schematic geometry | reviewed anatomy asset or licensed 3D model, accessibility validation |
| Android reminders | inexact local reminder | reboot/time-zone rescheduling, recurrence, exact-alarm need assessment |
| iOS reminders | local notification UI | recurrence, timezone tests, AlarmKit assessment |
| iOS evidence handoff | native evidence summary | structured Swift/Kotlin state handoff and camera capture |
| Health data | architecture boundary | Health Connect/Apple Health permission, schema, privacy, deletion, tests |
| Subscriptions | product hypothesis | StoreKit/Play Billing/Web entitlement service and server receipt validation |
| Analytics | event design only | consent, SDK/vendor review, privacy manifest, dashboards |

## Not implemented

- diagnosis, medication advice, supplement dose recommendation, or automatic stack optimization;
- clinically reviewed Taiwan dose/interaction rule pack;
- production exercise database importer;
- third-party exercise images, GIFs, video, anatomy SVGs, or 3D models;
- API/CDN hotlinking;
- persistent raw label-photo storage;
- Android exact alarms;
- AlarmKit or challenge-to-dismiss behavior;
- Health Connect or Apple Health;
- login, cloud sync, backend, database, or LLM provider;
- subscriptions, paywall, creator CRM, attribution provider, or production analytics;
- App Store/Play production signing and submission.

## Verification contract

```bash
python3 scripts/validate_repository.py
sh ./gradlew :shared:jvmTest
sh ./gradlew :androidApp:assembleDebug :androidApp:lintDebug
sh ./gradlew :webApp:composeCompatibilityBrowserDistribution

cd iosApp
xcodegen generate --spec project.yml
xcodebuild \
  -project GymComeTrue.xcodeproj \
  -scheme GymComeTrue \
  -sdk iphonesimulator \
  -configuration Debug \
  CODE_SIGNING_ALLOWED=NO \
  ONLY_ACTIVE_ARCH=YES \
  ARCHS=arm64 \
  build
```

Only hosted checks attached to the exact PR head count as merge evidence. A local or narrative claim does not replace them.

## Known review points

1. Confirm the pinned Kotlin/Compose/AGP/Gradle matrix on hosted runners.
2. Confirm Android API 36 compilation, target behavior, and bundled Chinese ML Kit behavior.
3. Confirm XcodeGen/Xcode can locate the embedded static KMP framework.
4. Validate the iOS photo picker and Vision flow on a real device.
5. Review all license descriptions against pinned upstream versions before external release.
6. Replace repository-proprietary licensing only through an explicit owner decision.
