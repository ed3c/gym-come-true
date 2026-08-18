# Health Connect adapter (Issue #30 / A1)

State transition: `ANDROID_SHELL -> ANDROID_MINIMAL_HEALTH_READS`.

## What this delivers

| Capability | Status | Source |
|---|---|---|
| Availability classification (`AVAILABLE` / `NOT_INSTALLED` / `UPDATE_REQUIRED`) | `VERIFIED` (deterministic unit test) | `androidApp/src/main/kotlin/dev/ed3c/gymcometrue/health/HealthConnectAvailability.kt` |
| Least-privilege read permission scope (body weight, completed exercise minutes only) | `VERIFIED` (deterministic unit test) | `androidApp/src/main/kotlin/dev/ed3c/gymcometrue/health/HealthConnectPermissions.kt` |
| Permission-state classification, incl. revocation | `VERIFIED` (deterministic unit test) | same file |
| Manifest declarations (`READ_WEIGHT`, `READ_EXERCISE`, provider `<queries>`) | `DRAFT` | `androidApp/src/main/AndroidManifest.xml` |
| Actual on-device read of weight/exercise records wired to a visible feature | `ABSENT` | not implemented — see "What remains" |
| Play Console Data Safety form | `ABSENT` / `HUMAN_ADMIT_REQUIRED` | external gate: Play Console account |
| Real-device/OEM permission-flow evidence | `ABSENT` / `NOT_EXERCISED` | external gate: physical or emulator device run |

## Why weight and exercise minutes, and nothing else

`docs/platform-capability-matrix.md` "Health adapters" states the least-privilege
bar directly: *"First production release should be read-only and limited to
data directly needed for a user-visible feature, such as completed workout
minutes or body weight when explicitly requested."* No other Health Connect
record type is requested by this adapter. Extending the scope (steps, heart
rate, sleep, nutrition, …) requires a named, shipped, visible feature that
needs it — not a speculative future one — and a corresponding update to
`minimalHealthConnectReadPermissions`, the manifest `<uses-permission>` list,
and this table.

## Availability contract

`classifyHealthConnectSdkStatus(sdkStatus: Int)` is a pure function over
`HealthConnectClient.getSdkStatus()`'s return value. It fails closed: any
status this app does not explicitly recognize as `SDK_AVAILABLE` or
`SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED` is treated as `NOT_INSTALLED`,
never silently treated as available. `currentHealthConnectAvailability(context)`
is the thin, side-effecting wrapper — call it from the UI in response to an
explicit user action or screen entry, never from a background job.

## Permission-state contract

`classifyHealthConnectPermissionState(granted, requested)` is pure and always
recomputed from a live query (`PermissionController.getGrantedPermissions()`),
never cached. This is what makes revocation honest: if the user revokes a
permission from system settings between two screen visits, the next call
reflects `NONE_GRANTED` or `PARTIALLY_GRANTED` — there is no stale in-memory
"granted" flag anywhere in this adapter for the OS to disagree with.

`healthConnectPermissionRequestContract()` exposes
`PermissionController.createRequestPermissionResultContract()` for a caller to
wire into `rememberLauncherForActivityResult` from an explicit,
user-initiated "connect health data" action. This adapter never calls it on
its own initiative — there is no code path that requests a Health Connect
permission without a direct user tap driving it.

## No hidden background collection

There is no `WorkManager`, `JobScheduler`, or periodic-broadcast wiring
anywhere in this adapter. Reads, when they exist, are call-and-return
functions triggered by explicit UI action; nothing polls Health Connect on a
schedule. This is a design property enforced by omission — the harness for
proving its absence is: `grep -r "WorkManager\|JobScheduler\|PeriodicWork" androidApp/src/main/kotlin/dev/ed3c/gymcometrue/health/` returns nothing.

## No medical interpretation

The adapter returns raw record values (kilograms, minutes) with no scoring,
BMI calculation, trend analysis, or health claim. That boundary is shared
with the rest of the app's `LLM_EXPLANATION_ONLY` and
`REVIEWED_HEALTH_RULES_ONLY` invariants in `AGENTS.md`; this adapter adds no
exception to them.

## What remains (explicitly deferred, not silently dropped)

1. **Shared cross-platform read DTO.** `docs/architecture.md:372` lists
   "Health records | future normalized DTO | Health Connect planned |
   HealthKit planned | user import only". That DTO belongs in `shared/src`,
   which this lane (`android-health`) does not own — its path lease is
   `androidApp/**` and `docs/android/`. The shape this adapter needs from
   that DTO, once it exists:

   ```text
   sealed BodyWeightSample(kilograms: Double, recordedAt: Instant, sourcePlatform: enum)
   sealed CompletedWorkoutSample(minutes: Long, start: Instant, end: Instant, sourcePlatform: enum)
   ```

   Until that DTO exists, this adapter intentionally does not ship an
   Android-only competing shape for actual record reads — doing so would
   create a second contract the shared/iOS lane would then have to reconcile
   against instead of designing cleanly.
2. **Wiring a "connect health data" UI action.** `MainActivity.kt` and the
   shared `GymComeTrueApp` composable currently have no screen that would
   consume a health read. Adding one is scoped to whichever lane owns that
   feature's shared UI contract, not this adapter.
3. **`ViewPermissionUsageActivity` privacy-rationale activity.** Health
   Connect requires an app targeting Android 14+ that requests health
   permissions to declare a rationale activity
   (`androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE`). Not added here to
   keep this slice to the availability/permission-state contract; required
   before Play submission.
4. **Play Console Data Safety form and privacy review.** External gate;
   `HUMAN_ADMIT_REQUIRED`. No claim of Play policy compliance is made by this
   delivery.
5. **Real-device/emulator evidence** that `getSdkStatus`, the permission
   request contract, and revocation actually behave as documented on a real
   Health Connect provider. `NOT_EXERCISED` — this lane never runs Gradle or
   a device/emulator (see repository lane constraints); a later integration
   pass or CI run owns that evidence.

## Dependency note

`androidx.health.connect:connect-client` is declared directly in
`androidApp/build.gradle.kts` (pinned `1.1.0`, reviewed and bumped from
`1.1.0-alpha07` in Issue #53) rather than through `gradle/libs.versions.toml`,
because the version catalog is outside this lane's path lease. The integrator
should promote it into the catalog alongside the project's other Android
dependencies.

**Version rationale (2026-08-18).** `dl.google.com/dl/android/maven2/androidx/
health/connect/connect-client/maven-metadata.xml` lists `1.1.0` as a released,
non-alpha version (the next release track, `1.2.0`, is still `alpha05`).
`1.1.0` stable ships with no changes since its `1.1.0-rc03` release, and none
of the intermediate alpha/beta/rc changes touch the APIs this adapter calls
(`HealthPermission.getReadPermission`, `WeightRecord`, `ExerciseSessionRecord`,
`PermissionController.createRequestPermissionResultContract`,
`HealthConnectClient.getSdkStatus`, `SDK_AVAILABLE`,
`SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED`) — the one relevant change
(`Metadata`/`Device` constructors becoming factory-based) affects only code
that constructs those types, which this adapter does not. `minSdk 24` was not
introduced until `1.2.0-alpha05`, so it does not constrain this project's
`minSdk 26`. Net effect of the bump: same API surface, first stable release
instead of a two-year-old alpha.
