# Store and Privacy Compliance Plan

This is an engineering checklist, not legal advice. Checked-in permission/adapter surfaces are not evidence that Apple or Google has accepted the app, disclosures, entitlements, declarations, or release.

```text
DECLARED_PERMISSION != STORE_APPROVAL
ADAPTER_PRESENT != REAL_DEVICE_VALIDATION
DEBUG_BUILD != RELEASE_SIGNING_ADMISSION
```

## Apple

### Current checked-in engineering

- photo selection is user initiated;
- notifications are requested in context;
- camera/photo usage descriptions exist;
- `NSHealthShareUsageDescription` exists;
- `NativeHealthReadBridge` provides a least-privilege HealthKit read surface;
- no Health write authority is claimed;
- AlarmKit is not admitted as a current product capability.

These bytes do not prove HealthKit entitlement configuration, real-device authorization, App Privacy answers, privacy-policy completeness, App Store review acceptance, or production data handling.

### Health and medical wording

- Describe the product as fitness planning, information, evidence organization, and logging.
- Do not market it as diagnosis, treatment, medication management, or a dosage calculator.
- Explain that OCR and arithmetic are not recommendations.
- Disclose exactly which health data types a released feature reads and why.
- Do not use health/fitness data for targeted advertising or unrelated profiling.
- Keep Health reads user-visible, least-privilege, and contextual.

### AlarmKit boundary

A future AlarmKit packet must separately establish `NSAlarmKitUsageDescription`, supported OS/device behavior, system stop semantics, fallback, timezone behavior and App Store wording. Do not claim a challenge prevents the system alarm from being stopped.

## Google Play / Android

### Current checked-in engineering

- Android declares `READ_WEIGHT` and `READ_EXERCISE` only for the current least-privilege Health Connect read scope;
- Health Connect availability/permission/read adapters exist;
- no Health Connect write permission is declared;
- notifications and reboot/package/timezone reminder reconciliation surfaces exist;
- no exact-alarm permission is admitted.

These declarations do not prove provider availability on all devices/OEMs, real-device permission UX, Google Play Health apps declaration acceptance, Data safety accuracy, privacy review, or production release admission.

### Store requirements still external

- complete the Health apps declaration from actual released behavior;
- provide a public privacy policy and in-app access;
- use prominent disclosure where sensitive data collection/transmission requires it;
- keep exact alarm access out until the core use case and policy eligibility are established;
- do not imply regulatory approval;
- keep provider keys and privileged business logic outside clients and source control.

## Data-flow inventory

This table describes current code paths, not a production privacy attestation.

| Data | Current engineering path | Default retention / transfer | Still required before production |
|---|---|---|---|
| Android label photo | app cache -> on-device ML Kit | temporary deletion path; no provider transfer in current adapter | device/privacy verification |
| iOS selected photo | picker/in-memory -> Vision | not persisted by native bridge by default | device/privacy verification |
| OCR/barcode candidate | local candidate -> user confirmation | must stay out of general analytics | retention/export/delete policy if persisted |
| Protocol/meal plan | shared deterministic state | user-visible local/product state | persistence/account policy if added |
| Android health reads | Health Connect adapter | least-privilege feature read; no ad use authorized | real-device/OEM/store/privacy evidence |
| iOS health reads | `NativeHealthReadBridge` | least-privilege feature read; no Health write authority | entitlement/device/store/privacy evidence |
| LLM payload | receipt/provider contract | no client provider secret; live deployment not proven | security/privacy/provider admission |
| Analytics | structural-event policy only | sensitive raw OCR/health data prohibited | production analytics inventory/consent |

## Production gates

Repository code or CI cannot self-close these gates:

- privacy policy, operator identity, retention/export/deletion behavior;
- consent/permission copy and jurisdiction-specific review;
- Apple App Privacy and Google Play Data safety / Health apps declarations;
- HealthKit entitlement and real-device authorization evidence;
- Health Connect real-device/OEM/provider behavior;
- age rating and audience;
- subscription terms, restore purchases, account deletion, and billing implementation if enabled;
- medical-claim and safety wording review;
- accessibility and supported-device/browser evaluation;
- incident response and independent security/privacy review;
- exact third-party asset/source rights;
- signing identities, store records, signed release builds and submission;
- release promotion and rollback authority.

`GITHUB_CHECK_PASS != HUMAN_ADMIT`.
