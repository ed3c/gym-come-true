# Issue plan — iOS native evidence, HealthKit, reminders, and AlarmKit assessment

## Outcome

Wire an explicit camera/photo flow to shared evidence, add only justified HealthKit reads, and evaluate AlarmKit with honest fallback and store-policy evidence.

## Acceptance

- One reviewed Swift bridge replaces all drafts.
- Vision text/barcode candidates reach shared confirmation state.
- Raw images remain local and temporary by default.
- Reminder recurrence, cancellation, time-zone change, and permission denial are tested.
- HealthKit reads are minimal, purpose-bound, revocable, exportable, and deletable.
- AlarmKit is capability-gated and falls back to a reminder without reliability inflation.
- Device/simulator, privacy-manifest, and store disclosure evidence is attached.

## Hard limits

- No guaranteed-alarm claim.
- No default cloud photo upload.
- No broad HealthKit collection.
- No challenge-to-dismiss flow without separate safety and platform review.
