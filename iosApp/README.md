# iOS host

The iOS application is a SwiftUI host for the shared Compose view controller and
the canonical native capability bridge.

## Generate

```bash
xcodegen generate --spec project.yml
open GymComeTrue.xcodeproj
```

`project.yml` is the only admitted build contract. It compiles exactly:

- `GymComeTrueApp.swift`
- `ContentView.swift`
- `NativeCapabilityBridge.swift`

`project.safe.yml` and `NativeCapabilityBridgeV2.swift` are prohibited shadow
surfaces and must not reappear; `scripts/validate_repository.py` fails closed on
both.

The Kotlin framework is built by the Xcode pre-build script with the repository's
pinned Gradle launcher.

## Native capabilities

- Vision OCR and barcode extraction run on device and return candidate evidence.
- Raw pixels stay in memory for the length of one Vision request and are never
  written to disk; `PhotosPicker` runs out of process, so no photo-library
  permission is requested.
- Shared `EvidenceHandoff` decides whether native output may become evidence.
  Everything it accepts stays `UNVERIFIED`.
- HealthKit reads are limited to the set shared `HealthReadPolicy` returns for the
  features the user switched on. iOS never discloses read authorization, so an
  empty result is reported as ambiguous, never as "no data".
- Local notifications are reminders. Recurrence uses wall-clock components from
  shared `ReminderPlanner`, so time-zone changes and reboots are handled by the
  system. AlarmKit is not linked and no alarm guarantee is made.
- Signing teams, provisioning profiles, HealthKit entitlements, and secrets stay
  local or in protected CI secrets.

State, evidence lanes, and the external gates that remain `ABSENT` are recorded in
[`docs/ios/README.md`](../docs/ios/README.md).
