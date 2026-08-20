# Platform Capability Matrix

This matrix distinguishes checked-in engineering from device/store admission.

```text
ADAPTER_PRESENT != REAL_DEVICE_VALIDATION
BUILD_PASS != STORE_ADMISSION
REMINDER_PRESENT != EXACT_ALARM_GUARANTEE
```

## Current engineering

| Capability | Android | iOS | Web | Remaining evidence |
|---|---|---|---|---|
| Shared KMP domain | Implemented | Implemented | Implemented | release admission |
| Shared Compose dashboard | Implemented | Implemented | Implemented | accessibility/browser/device evaluation |
| Traditional Chinese + Latin OCR | Bundled ML Kit candidate extraction | Apple Vision candidate extraction | Manual/import boundary | representative accuracy + confirmation evidence |
| Barcode | Bundled ML Kit candidate extraction | Apple Vision candidate extraction | Manual/import boundary | formulation/variant confirmation remains evidence-bound |
| Image lifecycle | Temporary app-cache deletion path | Picker/in-memory native path | browser-local boundary | production privacy verification |
| Daily intake arithmetic | Shared | Shared | Shared | information only; no dose/safety claim |
| A/B protocol / meal timetable | Shared deterministic compiler | Shared | Shared | admitted real food/source data where required |
| Reminder | Inexact AlarmManager + persisted reconciliation | UserNotifications scheduling | In-app/browser-specific boundary | measured delivery/device/browser evidence |
| System/exact alarm | Not admitted | AlarmKit not admitted | N/A | policy/usage/device/store evidence |
| Health data | Health Connect availability/permission/read adapters | `NativeHealthReadBridge` / HealthKit read adapter | N/A | real-device, OEM/entitlement, privacy/store evidence |
| Exercise catalog | first-party bilingual DRAFT catalog | Same | Same | editorial/rights admission; third-party media separate |
| Third-party exercise media | None admitted | None admitted | None admitted | exact asset rights + hash + redistribution scope |
| LLM explanation | receipt/provider contract only | Same | Same | live privileged provider deployment + security/privacy review |
| Subscription | Not implemented | Not implemented | Not implemented | business/store implementation and admission |

## Health adapters

### Android

The application declares least-privilege read permissions for body weight and exercise records and has Health Connect availability/permission/read adapter surfaces. No write permission is declared.

This does not prove provider availability on every supported device/OEM, successful permission UX on a real device, Google Play Health apps declaration acceptance, production privacy disclosure correctness, or broader health-data access.

### iOS

`NativeHealthReadBridge` provides a least-privilege read surface and `Info.plist` contains `NSHealthShareUsageDescription`. The adapter grants no Health write authority and does not treat HealthKit as a general read-authorization truth oracle.

This does not prove HealthKit entitlement configuration, real-device authorization, App Store acceptance, privacy review, or production behavior.

## Reminder truth table

### Android

The current scheduler is an inexact reminder. Persisted pending reminders can be reconciled after boot, package replacement, and timezone changes, but that does not make delivery exact. Exact-alarm access requires a qualifying product case, permission/policy handling, user-visible fallback, and measured device evidence.

### iOS

`UNUserNotificationCenter` is a local-notification surface, not an alarm-clock guarantee. AlarmKit is not currently admitted as a product capability. Any future use must preserve system stop controls and separately prove usage-description, device and store behavior.

## OCR choice

Android uses bundled ML Kit Chinese text recognition so first-use OCR does not depend on downloading a model. ML Kit remains a Google SDK governed by its terms and disclosure requirements.

iOS uses Apple Vision for local candidate extraction. OCR/barcode output is always evidence requiring confirmation; it never becomes ingredient, formulation, serving, or dose authority by itself.

## Web delivery

The hosted Web lane builds the Compose compatibility browser distribution. Production still needs browser/device evaluation for WasmGC compatibility, Safari/Firefox/Chromium behavior, accessibility, keyboard navigation, deep links, cache invalidation, reduced motion, and low-memory behavior.

```bash
./gradlew :webApp:composeCompatibilityBrowserDistribution
```

A successful build is not production hosting or browser-notification reliability evidence.
