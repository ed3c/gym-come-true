# Android adapters

This directory documents the Android-native adapters that live in `androidApp/`:
Health Connect (Issue #30 / A1) and the local-reminder reliability harness
(Issue #31 / A2). Both are children of the `androidApp/` foundation shipped in
PR #2 (`ANDROID_SHELL`) and both are `DRAFT` — implementation and deterministic
tests exist; device, OEM, and Play Console evidence is `ABSENT` until a human
runs them.

## Mandatory read order

Before changing anything under `androidApp/health/` or `androidApp/reminder/`:

1. root `AGENTS.md` (directory ownership, domain hard invariants — especially
   `HONEST_ALARM_SEMANTICS`);
2. `docs/plans/issue-4-android-native.md` (the original outcome/acceptance
   contract this directory implements a slice of);
3. `docs/platform-capability-matrix.md` "Health adapters" section (the
   cross-platform least-privilege contract Android and iOS both answer to);
4. this file;
5. [health-connect.md](health-connect.md) or
   [reminder-reliability-harness.md](reminder-reliability-harness.md).

## Files

| File | Purpose |
|---|---|
| [health-connect.md](health-connect.md) | Availability/permission-state contract, least-privilege read scope, what remains before a real read is wired to UI |
| [reminder-reliability-harness.md](reminder-reliability-harness.md) | Reboot/timezone/package-transition reconciliation contract, exact-alarm need assessment, device/OEM evidence status |

## State transitions this directory claims

```text
ANDROID_SHELL -> ANDROID_MINIMAL_HEALTH_READS        (Issue #30 / A1, this delivery)
ANDROID_MINIMAL_HEALTH_READS -> ANDROID_DELIVERY_EVIDENCE   (Issue #31 / A2, NOT reached — see below)
```

Issue #31's body frames its transition as
`ANDROID_MINIMAL_HEALTH_READS -> ANDROID_DELIVERY_EVIDENCE`. This delivery adds
the reminder reliability harness *contract* (deterministic reconciliation
logic, persisted pending-trigger tracking, exact-alarm evidence-gathering, and
the local test matrix) but does not reach `ANDROID_DELIVERY_EVIDENCE`, because
that state name implies device/OEM evidence this delivery does not have. Treat
this slice as `RELIABILITY_HARNESS_CONTRACT_DRAFT` until a human runs the
device matrix in [reminder-reliability-harness.md](reminder-reliability-harness.md).

## What is NOT in this directory

- Any shared cross-platform DTO. `docs/architecture.md:372` calls for a
  "future normalized DTO" for health records; that belongs in `shared/src`,
  which this lane does not own. See the two detail docs for the exact shape
  requested from that lane.
- Any UI wiring into `GymComeTrueApp` (the shared Compose entry point). The
  adapters here are callable but not yet invoked from a screen, because doing
  so first needs the shared DTO above.
- Any medical interpretation of health samples, background/periodic Health
  Connect reads, or default request for `SCHEDULE_EXACT_ALARM` — all
  explicitly forbidden by `AGENTS.md` and asserted against in the test
  matrices in both detail docs.
