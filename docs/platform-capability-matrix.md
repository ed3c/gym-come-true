# Platform Capability Matrix

## Foundation

| Capability | Android | iOS | Web |
|---|---|---|---|
| Shared KMP domain | Implemented | Implemented | Implemented |
| Shared Compose dashboard | Implemented | Implemented | Implemented |
| Traditional Chinese + Latin OCR | Bundled ML Kit | Apple Vision | Not implemented |
| Barcode | Bundled ML Kit | Apple Vision | Not implemented |
| Image lifecycle | Temporary cache deleted | Picker/in-memory | N/A |
| Daily intake arithmetic | Shared | Shared | Shared |
| A/B protocol | Shared | Shared | Shared |
| Reminder | Inexact AlarmManager | UserNotifications | Not implemented |
| System alarm | Not implemented | Not implemented | N/A |
| Health data | Boundary only | Boundary only | N/A |
| Local exercise database | Original JSON seed; SQLDelight pending | Same | Same |
| Third-party media | None admitted | None admitted | None admitted |
| LLM | Contract only | Contract only | Contract only |
| Subscription | Not implemented | Not implemented | Not implemented |

## Reminder truth table

### Android

`setAndAllowWhileIdle()` is an inexact alarm. Delivery can move because of Doze, app restrictions, and OEM behavior. Exact alarms require a qualifying core use case, special access/policy handling, user-visible behavior, reboot rescheduling, and separate tests.

### iOS

`UNUserNotificationCenter` is a local notification, not an alarm-clock guarantee.

AlarmKit can present system alarms, but the system provides a stop control. A secondary action may open the app for a post-alarm challenge; it cannot support a truthful “the alarm cannot be stopped before completing push-ups” claim.

Official references:

- https://developer.android.com/develop/background-work/services/alarms/schedule
- https://developer.apple.com/documentation/alarmkit
- https://developer.apple.com/documentation/alarmkit/alarmpresentation/alert-swift.struct

## OCR choice

Android uses the bundled Chinese recognizer so Traditional Chinese and Latin text can be processed without a first-use model download dependency. ML Kit remains a commercial Google SDK governed by its terms and disclosure requirements.

iOS uses Apple Vision to reduce dependencies and keep the vertical slice native. A future camera flow may use VisionKit/DataScanner or a controlled camera picker.

Official reference:

- https://developers.google.com/ml-kit/vision/text-recognition/v2/android

## Web delivery

Compose Multiplatform Web remains less mature than Android/iOS. Production should build a Wasm distribution with JavaScript compatibility fallback and test:

- WasmGC support;
- Safari/Firefox/Chromium behavior;
- accessibility;
- keyboard navigation;
- deep links;
- service worker/cache invalidation;
- reduced-motion and low-memory behavior.

Build:

```bash
./gradlew :webApp:composeCompatibilityBrowserDistribution
```

## Health adapters

First production release should be read-only and limited to data directly needed for a user-visible feature, such as completed workout minutes or body weight when explicitly requested.

- Apple: request each Health data type in context; provide privacy policy and usage descriptions.
- Android: request declared Health Connect permissions in context; support revoke/deletion behavior.
- No health-derived ad targeting or creator attribution.
