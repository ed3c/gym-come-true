# iOS host

The iOS application is a SwiftUI host for the shared Compose view controller.

## Generate

```bash
xcodegen generate --spec project.safe.yml
open GymComeTrue.xcodeproj
```

`project.safe.yml` is the admitted build contract for this foundation. It compiles only:

- `GymComeTrueApp.swift`
- `ContentView.swift`
- `NativeCapabilityBridgeV2.swift`

The Kotlin framework is built by the Xcode pre-build script with the repository's pinned Gradle launcher.

## Native capabilities

- Vision OCR and barcode extraction run on device and return candidate evidence.
- Local notifications are reminders and do not guarantee alarm-clock delivery.
- AlarmKit, HealthKit, camera UI, and the native-to-KMP evidence bridge remain separate reviewed work items.
- Signing teams, provisioning profiles, and secrets stay local or in protected CI secrets.
