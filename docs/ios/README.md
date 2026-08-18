# iOS native delivery evidence

Scope: Issues #27 (evidence handoff), #28 (least-privilege HealthKit reads), and
#29 (reminder recurrence and AlarmKit assessment).

This document records what was actually produced and, more importantly, what was
not. Nothing here may be read as device evidence, store acceptance, or privacy
review.

## State reached

| Issue | Requested transition | Reached in this lane |
|---|---|---|
| #27 | `IOS_SHELL -> IOS_EVIDENCE_HANDOFF` | `IOS_EVIDENCE_HANDOFF_DRAFT` — contract, seam, and deterministic tests exist; no simulator or device run |
| #28 | `IOS_EVIDENCE_HANDOFF -> IOS_MINIMAL_HEALTH_READS` | `IOS_MINIMAL_HEALTH_READS_DRAFT` — read set, state machine, purge/export contract and tests exist; no HealthKit entitlement, no authorization ever presented |
| #29 | `IOS_MINIMAL_HEALTH_READS -> IOS_DELIVERY_EVIDENCE` | `IOS_REMINDER_CONTRACT_DRAFT` — recurrence, cancellation, denial and capability assessment exist; `IOS_DELIVERY_EVIDENCE` is **not** reached because it requires measured device evidence |

## Ownership

Deterministic decisions live in shared Kotlin and are covered by tests. Swift owns
pixels, permission prompts, Vision, HealthKit, and `UNUserNotificationCenter`, and
nothing else.

```text
shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/health/
  EvidenceHandoff.kt     what native capture may become evidence at all
  HealthReadAccess.kt    which Health types may be requested, and what a read means
  ReminderSchedule.kt    which wall-clock occurrences recur, and what may be claimed

iosApp/GymComeTrue/NativeCapabilityBridge.swift
  the single canonical bridge: Vision, camera, HealthKit, notifications
```

`iosApp/project.yml` and the three canonical Swift files remain the only admitted
build surface. No `project.safe.yml` and no `NativeCapabilityBridgeV2.swift` exist.

## Swift/Kotlin seam

The seam is primitives-in, object-out. Swift passes identifier strings that equal
the Kotlin enum entry names; shared code fails closed on anything it does not
recognise, so drift surfaces as a visible rejection instead of a silent widening.

| Swift call site | Shared entry point | Failure mode |
|---|---|---|
| `EvidenceHandoff.shared.acceptFromNative(...)` | `EvidenceHandoff.acceptFromNative` | unknown source/authorization/retention id → `accepted=false` with a named rejection |
| `ReminderPlanner.shared.planForNative(...)` | `ReminderPlanner.planForNative` | unknown variant/authorization id → empty plan, channel `NONE` |
| `HealthReadPolicy.shared.appleReadIdentifiers(...)` | `HealthReadPolicy.appleReadIdentifiers` | unknown feature id is dropped, never expanded |

## Invariants this lane enforces

- OCR output crosses the seam as `UNVERIFIED` and cannot be raised there. Every
  accepted `ScanEvidence` carries a physical-label confirmation warning.
- The digest is produced on device by CryptoKit over the same trimmed text the
  shared seam reads. Shared code recomputes SHA-256 (via
  `org.kotlincrypto.hash:sha2`, Issue #53) and rejects with `DIGEST_MISMATCH`
  if it disagrees with the declared value; a malformed shape is rejected first
  with `DIGEST_SHAPE_INVALID`.
- `RawPixelRetention.PERSISTED_LOCAL` is rejected. Retention needs consent,
  encryption, expiry, deletion, withdrawal, hashes, and provenance; none exist.
- `PhotosPicker` runs out of process, so no photo-library permission is requested;
  the explicit pick is the authorization. Only the camera path has a permission
  state, and a denied or restricted camera produces no evidence.
- HealthKit reads are derived from enabled features only.
  `ACTIVE_ENERGY_BURNED` exists in the type list precisely as a negative control:
  no feature justifies it, and a test asserts it never enters the request set.
- iOS never discloses read authorization. After the sheet is answered the state is
  `REQUEST_PRESENTED_OUTCOME_UNKNOWABLE`, and an empty query is
  `EMPTY_INDISTINGUISHABLE` — never rendered as "no data" and never as "denied".
- Reminder recurrence is expressed as wall-clock components
  (`weekday/hour/minute`) consumed by `UNCalendarNotificationTrigger`, so the
  system re-resolves them across time-zone changes, DST transitions, and reboots.
  The previous fixed `UNTimeIntervalNotificationTrigger` could not and is gone.
- No delivery guarantee. `ReminderPlan.guaranteedDelivery` is `false`,
  `AlarmKitAssessment.systemStopControlsRetained` is `true`,
  `challengeToDismissAdmitted` is `false`, and a test asserts the capability claim
  string contains no form of the word "guarantee".
- Health data is never uploaded and carries no medical interpretation.

## Deterministic tests

`shared/src/commonTest/kotlin/dev/ed3c/gymcometrue/health/`

- `EvidenceHandoffTest` — unverified status preserved, denied/restricted capture
  rejected, persisted pixels rejected, malformed or absent digest rejected, a
  well-shaped but wrong digest rejected as `DIGEST_MISMATCH`, recomputation
  checked against the known `sha256("abc")` vector, barcode-only capture
  accepted, unknown native identifiers fail closed, native seam equals the
  typed contract.
- `HealthReadAccessTest` — least-privilege request set, unjustified type never
  requested, unknown feature id ignored, empty read never reported as no-data or
  denial, display gate, per-feature purge, full purge on last feature, export.
- `ReminderScheduleTest` — Apple weekday mapping, weekly recurrence order,
  denied/undetermined authorization schedules nothing, provisional is quiet,
  cancellation removes exactly the matching ids, empty weekday selection, an
  after-midnight event recurring on the following weekday, DST warning, the 64
  pending-request ceiling, native seam equality, alarm honesty.

Status: **written, `NOT_EXERCISED` in this lane.** This lane is forbidden from
running any JVM build, so `sh ./gradlew :shared:jvmTest` has not executed on this
code. A serial integrator must run it before any of the above counts as passing.

## Verification actually performed

```bash
# clean, exit 0
xcrun swiftc -parse iosApp/GymComeTrue/*.swift

# clean, exit 0, against iPhoneSimulator26.4.sdk (Vision, HealthKit,
# AVFoundation, UserNotifications, SwiftUI, UIKit all resolved)
xcrun --sdk iphonesimulator swiftc -typecheck \
  -target arm64-apple-ios18.0-simulator \
  iosApp/GymComeTrue/NativeCapabilityBridge.swift

# ContentView typechecked against a scratch stub of the Kotlin export surface,
# plus a planted-defect control (one wrong argument label -> exit 1)

xcodegen generate --spec iosApp/project.yml   # succeeds; Info.plist digest unchanged
plutil -lint iosApp/GymComeTrue/Info.plist    # OK
python3 scripts/validate_repository.py        # all checks PASS
```

`xcodebuild` was **not** run. `project.yml` runs `:shared:embedAndSignAppleFrameworkForXcode`
as a pre-build script, so an iOS build is a Gradle build, which this lane may not
start. The Swift/Kotlin call sites are therefore verified statically against a
stub, not against the real generated framework header.

### Defect found and fixed on the documented build path

`project.yml` declared `info: path: GymComeTrue/Info.plist`, which makes XcodeGen
*generate* that file. Running the documented `xcodegen generate` command deleted
the reviewed `Info.plist` — every usage description with it — and replaced it with
a spec-derived stub. The target now references the checked-in plist through
`INFOPLIST_FILE` instead, so generation cannot clobber it. Verified by digest:
`8e7cd2d8…` before and after a full `xcodegen generate`.

## External gates — none of these were obtained

| Gate | State |
|---|---|
| HealthKit entitlement and provisioning profile | `ABSENT` — `HUMAN_ADMIT_REQUIRED` |
| Health authorization presented to a real person | `ABSENT` — no consent exists |
| Real-device reminder delivery measurement (locked, low power, reboot, time-zone change) | `ABSENT` — `NOT_EXERCISED` |
| Simulator run of the app | `NOT_EXERCISED` |
| AlarmKit framework, `NSAlarmKitUsageDescription`, and its review | `NOT_IMPLEMENTED` |
| App Store privacy manifest and Health disclosure review | `ABSENT` — `HUMAN_ADMIT_REQUIRED` |
| Apple privacy/legal review of the usage descriptions | `ABSENT` — `HUMAN_ADMIT_REQUIRED` |
| Signing identity, team, store account | `ABSENT` |
| `sh ./gradlew :shared:jvmTest` on this code | `NOT_EXERCISED` in this lane |

## Known ceilings

- The shared `HealthReadStateMachine` is the tested owner of display gating, but
  the iOS UI currently renders the bridge's own honest sentence; binding the
  Kotlin state object to SwiftUI is not implemented.
- `UIImagePickerController` is used for camera capture because it needs no extra
  dependency. `VisionKit`/`DataScannerViewController` would give live guidance and
  is the obvious upgrade once there is device evidence to tune against.
- Reminder content is derived from protocol events that require confirmation. It
  never contains a dose, a threshold, or a health instruction.
