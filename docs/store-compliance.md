# Store and Privacy Compliance Plan

This is an engineering checklist, not legal advice.

## Apple

### Health and medical wording

- Describe the app as fitness planning and evidence organization.
- Do not market it as diagnosis, treatment, medication management, or a dosage calculator.
- Explain that OCR and arithmetic are not recommendations.
- Provide a privacy policy URL and in-app access.
- Disclose exactly which health data types are collected.
- Do not use health/fitness data for targeted advertising or unrelated data mining.

References:

- https://developer.apple.com/app-store/review/guidelines/
- https://developer.apple.com/documentation/healthkit/protecting-user-privacy

### Permissions

Current:

- photo selection is user initiated;
- notifications are requested in context;
- camera usage description exists for the future camera flow.

Future Apple Health:

- request one data type at a time in context;
- read-only first;
- add `NSHealthShareUsageDescription`;
- add write permission only for a proven user benefit;
- never store personal health information in iCloud.

Future AlarmKit:

- add `NSAlarmKitUsageDescription`;
- present it as a system alarm with a system stop control;
- use a secondary “Open” action for an optional challenge;
- do not claim the challenge controls whether the system alarm can stop.

## Google Play

- Complete the Health apps declaration accurately.
- Provide a public privacy policy and in-app link.
- Declare nutrition/fitness features and any later Health Connect access.
- Use prominent disclosure before collecting or transmitting sensitive data.
- Do not imply regulatory approval.
- Keep exact alarm permission out until the core use case and policy eligibility are established.
- Provider keys and privileged business logic remain server-side.

References:

- https://support.google.com/googleplay/android-developer/answer/14738291
- https://support.google.com/googleplay/android-developer/answer/17105854
- https://developer.android.com/health-and-fitness/health-connect

## Data inventory

| Data | Foundation location | Retention | External transfer |
|---|---|---|---|
| Android label photo | app cache | deleted after scan | none |
| iOS selected photo | picker/in-memory | not persisted by bridge | none |
| OCR text | memory | user save not yet implemented | none |
| Barcode | memory | user save not yet implemented | none |
| Protocol selection | memory | not persisted | none |
| Health data | not implemented | N/A | none |
| LLM payload | contract only | N/A | not implemented |
| Analytics | not implemented | N/A | none |

## Production gates

- privacy policy and data deletion;
- consent/permission copy;
- app privacy / data safety forms;
- age rating and audience;
- subscription terms, restore purchases, and account deletion;
- medical-claim review;
- accessibility;
- export controls and regional availability;
- incident response;
- security review;
- evidence for every third-party asset;
- signed Android/iOS release builds and reproducible CI evidence.
