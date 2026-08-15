# Implementation status

**Transition:** empty repository → auditable cross-platform foundation  
**Issue:** #1  
**Branch:** `agent/bootstrap-kmp-fitness-platform`  
**Base:** `0148e135a4855a700bb666e1181e65611517507c`  
**Status:** draft; hosted CI evidence required before merge

## Implemented

### Shared KMP domain

- evidence-status model;
- OCR label candidate parser;
- generic `mcg/mg/g` display conversion;
- explicit refusal to generically convert IU;
- deterministic safety decisions and reason list;
- A 16:00 and B 22:00 daily protocol compiler;
- cross-midnight `dayOffset` ordering;
- immutable LLM explanation boundary with dose/warning override disabled;
- domain tests for parsing, units, blocking, log-only behavior, and midnight order.

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
- bundled ML Kit text recognition and barcode extraction;
- recognized-text SHA-256 and unverified candidate parsing;
- temporary image deletion in success and failure paths;
- notification permission request;
- inexact `AlarmManager` reminder and notification receiver.

### iOS

- SwiftUI host for shared Compose UI;
- deterministic XcodeGen project specification;
- explicit admitted Swift source list in `project.safe.yml`;
- Vision text/barcode candidate extraction in memory;
- recognized-text SHA-256;
- UserNotifications reminder bridge;
- camera/photo usage-purpose strings.

### Web

- Compose JS executable;
- Compose Wasm executable;
- browser host and responsive full-viewport setup.

### Rights, safety, and delivery

- source registry with `ALLOW/REVIEW/DENY` decisions;
- empty default-deny media registry;
- original media-free exercise schema examples;
- secret, hotlink, media, LLM-boundary, registry, seed, and documentation validator;
- dependency/asset notices;
- architecture, health-safety, copyright, product strategy, marketing, and roadmap documents;
- Linux Android/Web/shared CI and macOS iOS CI.

## Partial or intentionally constrained

| Capability | Foundation behavior | Missing production work |
|---|---|---|
| Supplement recognition | label text/barcode candidates | product identity, serving disambiguation, confidence UX, representative-label evaluation |
| Supplement analysis | deterministic evidence and blocking | reviewed Taiwan rule pack, medication data provider, qualified review, incident process |
| LLM | typed explanation payload only | authenticated server gateway, provider, output validator, evals, audit, kill switch |
| Exercise catalog | three original schema examples | production taxonomy, translations, importer, reviewed/commissioned records |
| Exercise media | none admitted | executed licenses, assets, transcode/CDN pipeline, revocation test |
| Muscle visualization | original schematic geometry | reviewed anatomy asset or licensed 3D model, accessibility validation |
| Android reminders | inexact local reminder | reboot/time-zone rescheduling, recurrence, exact-alarm need assessment |
| iOS reminders | native bridge | UI wiring, recurrence, AlarmKit assessment |
| Health data | architecture boundary | Health Connect/HealthKit permission, schema, privacy, deletion, tests |
| Subscriptions | product hypothesis | StoreKit/Play Billing/Web entitlement service and server receipt validation |
| Analytics | event design only | consent, SDK/vendor review, privacy manifest, dashboards |

## Not implemented

- diagnosis, medication advice, supplement dose recommendation, or automatic stack optimization;
- full exercise database import;
- third-party images, GIFs, video, anatomy SVGs, or 3D models;
- API/CDN hotlinking;
- persistent raw label-photo storage;
- Android exact alarms;
- iOS AlarmKit challenge-to-dismiss behavior;
- Health Connect or HealthKit;
- login, cloud sync, backend, database, or LLM provider;
- subscriptions, paywall, creator CRM, attribution provider, or production analytics;
- App Store/Play production signing and submission.

## Verification contract

```bash
python3 scripts/validate_repository.py
sh ./gradlew :shared:jvmTest
sh ./gradlew :androidApp:assembleDebug :androidApp:lintDebug
sh ./gradlew :webApp:jsBrowserDistribution
sh ./gradlew :webApp:wasmJsBrowserDistribution

cd iosApp
xcodegen generate --spec project.safe.yml
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

1. Confirm Kotlin/Compose/AGP/Gradle compatibility on hosted runners.
2. Confirm the current ML Kit artifact versions and Android API 37 environment.
3. Confirm XcodeGen/Xcode can locate the embedded static KMP framework.
4. Delete the excluded first-draft `NativeCapabilityBridge.swift` after preserving any useful history; only `NativeCapabilityBridgeV2.swift` is admitted by `project.safe.yml`.
5. Review all license descriptions against pinned upstream versions before external release.
6. Replace repository-proprietary licensing only through an explicit owner decision.
