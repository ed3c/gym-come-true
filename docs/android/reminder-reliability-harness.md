# Reminder reliability harness (Issue #31 / A2)

Issue body's stated transition: `ANDROID_MINIMAL_HEALTH_READS -> ANDROID_DELIVERY_EVIDENCE`.
This delivery reaches `RELIABILITY_HARNESS_CONTRACT_DRAFT`, not
`ANDROID_DELIVERY_EVIDENCE` — see `docs/android/README.md` for why that
distinction matters and is not silently collapsed.

## What "reliability" means here, precisely

Gym Come True's Android reminders use `AlarmManager.setAndAllowWhileIdle`
(inexact, Doze-aware) via `ProtocolReminderScheduler`, never
`setExactAndAllowWhileIdle` or exact-alarm special access. "Reliability" in
this harness means: **when a pending reminder's underlying AlarmManager alarm
is lost or made stale by a system transition, does the app notice and react
correctly** — re-arm it, fire it late-but-honestly, or drop it as stale — not
"does the OS deliver alarms at exactly the requested millisecond." The second
claim would require exact-alarm access this app does not request by default.

## The three transitions this app can actually observe

| Transition | Broadcast | Can this app register a receiver for it? |
|---|---|---|
| Reboot | `android.intent.action.BOOT_COMPLETED` | Yes |
| App update | `android.intent.action.MY_PACKAGE_REPLACED` | Yes |
| Timezone change | `android.intent.action.TIMEZONE_CHANGED` | Yes |
| App data cleared | *(none)* | **No.** Clearing an app's data kills its process and unregisters every receiver until the user next launches the app. There is no broadcast to catch. This is a real, permanent gap — not an oversight — and it stays `ABSENT`; the only mitigation is that `ReminderTransitionReceiver` also reconciles on every `BOOT_COMPLETED`/`MY_PACKAGE_REPLACED`/`TIMEZONE_CHANGED`, so state that survives a data clear (there is none — `SharedPreferences` is wiped too) is a non-issue by construction: a data clear correctly and honestly loses all pending reminders, matching what the user asked for. |

## Reconciliation contract

`androidApp/src/main/kotlin/dev/ed3c/gymcometrue/reminder/ReminderTransitions.kt`
defines `reconcilePendingTriggers(event, pendingTriggersEpochMillis, nowEpochMillis)`
as a pure function with no `Context`, `AlarmManager`, or I/O — the entire
reboot/timezone/package-transition test matrix in
`ReminderTransitionsTest.kt` exercises it directly, deterministically, with
no device or emulator.

For each persisted pending trigger, the outcome is one of:

| Disposition | When | Action taken by `ReminderTransitionReceiver` |
|---|---|---|
| `ALREADY_ARMED_NO_ACTION` | Still in the future; the OS is expected to still have it armed (only true for `TIMEZONE_CHANGED`, since an epoch-based alarm is timezone-invariant) | None |
| `REARM` | Still in the future but the OS likely dropped it (`BOOT_COMPLETED` always; `PACKAGE_REPLACED` conservatively, pending device evidence) | Re-arm via `ProtocolReminderScheduler.armAt` |
| `FIRE_NOW` | Already due, within the staleness window (default 24h) | Arm a near-immediate (5s) trigger instead of silently dropping it |
| `DROP_STALE` | More than 24h overdue | Drop; firing a day-old "confirm evidence" reminder would confuse, not help, the user |

Persisted pending-trigger state lives in `PendingReminderStore`
(`SharedPreferences`), because `AlarmManager` has no query API of its own —
the app must track what it believes is armed. `parsePendingTriggers` drops
any malformed persisted entry instead of crashing (tested in
`PendingReminderStoreParsingTest.kt`).

## Exact-alarm need assessment

`ExactAlarmAssessment.kt` implements the evidence-backed assessment the issue
requires, and nothing more:

- `ProtocolReminderReceiver` records `actualDeliveryTime - intendedTriggerTime`
  into `DeliveryDelayLog` (a bounded, order-preserving rolling log — see
  `DeliveryDelayLogParsingTest.kt` for why a plain `Set<String>` would have
  silently collapsed duplicate delay values and lost recency).
- `assessExactAlarmNeed(observedDelays)` is pure and defaults to `NOT_NEEDED`
  for an empty or mostly-on-time log, and only returns `NEEDS_HUMAN_REVIEW`
  when at least 20% of observed deliveries were 15+ minutes late.
- **`NEEDS_HUMAN_REVIEW` never requests `SCHEDULE_EXACT_ALARM` by itself.**
  There is no code path in this repository that calls
  `AlarmManager.canScheduleExactAlarms()` or requests the permission. A
  `NEEDS_HUMAN_REVIEW` result only unblocks a future, human-designed UI path
  — it is evidence, not authority. This directly satisfies `AGENTS.md`'s
  `HONEST_ALARM_SEMANTICS` and the issue's negative control: "no default
  exact-alarm access."

## Local test matrix (what actually ran)

All of the following are deterministic JVM unit tests with no device,
emulator, or Robolectric dependency — run via `./gradlew :androidApp:test`
(not run by this delivery; see repository lane constraints):

- `ReminderTransitionsTest`: empty-set no-op for every transition; reboot and
  package-replace re-arm a future trigger; timezone change leaves a future
  epoch trigger alone; a recently-elapsed trigger fires late instead of
  vanishing; a >24h-stale trigger is dropped; the exact 24h boundary still
  fires (not drops); a mixed batch reconciles each trigger independently.
- `PendingReminderStoreParsingTest`: malformed persisted entries are dropped,
  not crashed on.
- `DeliveryDelayLogParsingTest`: ordered/duplicate-preserving CSV parsing;
  null/blank input; malformed-entry dropping; bounded append keeps the most
  recent entries.
- `ExactAlarmAssessmentTest`: empty and on-time evidence stay `NOT_NEEDED`;
  frequent lateness (≥20%) flips to `NEEDS_HUMAN_REVIEW`; occasional
  lateness (<20%) stays `NOT_NEEDED`.

## What remains `ABSENT` / `NOT_EXERCISED`

- **Real device/OEM matrix.** Whether `PACKAGE_REPLACED` actually drops
  `AlarmManager` alarms on a given OEM's battery-management stack, whether
  `BOOT_COMPLETED` delivery is delayed by OEM autostart restrictions, and
  actual measured delivery delay distributions are all device-specific and
  unmeasured here. This harness's job is to make sure the app *reacts
  correctly once the transition is observed* — it cannot manufacture
  evidence about which real devices deliver that transition promptly, or at
  all, without a device farm. That evidence is `ABSENT` and stays
  `HUMAN_ADMIT_REQUIRED` (Issue #31: "External gate: device-farm/OEM evidence
  and Play review").
- **Instrumented/Robolectric coverage of `ReminderTransitionReceiver`,
  `PendingReminderStore`, and `DeliveryDelayLog` themselves** (as opposed to
  the pure functions they wrap). `NOT_EXERCISED` — no Robolectric dependency
  exists in this project's catalog, and adding one is outside this lane's
  path lease (`gradle/libs.versions.toml`). The pure/impure split in this
  harness is deliberate so the decision logic is fully covered without it.
- **Play data-safety / store review** for the reminder/notification surface.
  `HUMAN_ADMIT_REQUIRED`.
