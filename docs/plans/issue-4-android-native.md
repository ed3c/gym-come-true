# Issue plan — Android Health Connect and reminder reliability

## Outcome

Add the minimum Health Connect and reminder capabilities needed for protocol execution while handling permission, reboot, time-zone, API, and OEM behavior honestly.

## Acceptance

- Health Connect availability and permission state are explicit.
- Only user-selected records needed by a visible feature are read.
- Recurring reminders survive supported reboot/time-zone/package transitions.
- Exact-alarm special access is requested only after an evidence-backed need assessment.
- Device/API/OEM harness results separate measured reliability from assumptions.
- Play data-safety and permission declarations match runtime behavior.

## Hard limits

- No hidden background health collection.
- No exact-alarm permission as a default shortcut.
- No medical interpretation of health samples.
- No universal-device reliability claim.
