# Delivery roadmap

## Delivery rule

The roadmap is dependency-ordered. A later issue cannot weaken the evidence, safety, privacy, or rights contract delivered by Issue #1.

```text
#1 Cross-platform foundation
 ├─> #2 Taiwan supplement evidence and reviewed rule pack
 ├─> #3 iOS Vision UI, HealthKit, reminders, and AlarmKit assessment
 ├─> #4 Android Health Connect and alarm/reboot reliability
 ├─> #5 Copyright-clean exercise catalog and licensed media pipeline
 ├─> #6 Private LLM explanation gateway and adversarial evals
 ├─> #7 Subscription, privacy, store, and release operations
 └─> #8 Creator-market validation and launch evidence

#2 + #3 + #4 + #5 + #6 + #7
  -> STORE_RELEASE_CANDIDATE
```

Issue numbers after #1 are reserved by the implementation sequence below and should be reconciled with the actual GitHub issue URLs before merge.

## #1 — Auditable KMP foundation

**State:** implemented on `agent/bootstrap-kmp-fitness-platform`; draft PR and hosted checks pending.

Acceptance:

- shared KMP domain/UI and tests;
- Android app with local OCR/barcode candidate flow and inexact reminder;
- iOS and Web hosts;
- default-deny source/media governance;
- no automatic dosing, client LLM secret, scraped media, Health API, or exact-alarm completion claim;
- exact-head hosted CI evidence.

Rollback: return branch to base SHA `0148e135a4855a700bb666e1181e65611517507c`; no production data migration exists.

## #2 — Taiwan supplement evidence and reviewed rule pack

**Outcome:** convert Taiwan product labels and source documents into a versioned, reviewer-signed deterministic policy pack without introducing dose advice.

Work:

- define product-variant identity and serving schema;
- collect consented representative Traditional Chinese labels;
- measure OCR field accuracy and user correction rate;
- archive primary-source documents with hashes and effective dates;
- implement rule DSL, conflict precedence, expiry, rollback, and decision receipts;
- obtain qualified health/legal review for wording and scope;
- add false-positive, false-negative, ambiguity, medication-context, IU, and regression cases.

Non-goals:

- universal medication interaction engine;
- personal dose recommendation;
- model-generated rules;
- unsupported product authenticity claim.

Gate: no rule becomes `CLINICALLY_REVIEWED` without source, reviewer, version, tests, effective date, and rollback.

## #3 — iOS native evidence, HealthKit, reminders, and AlarmKit assessment

**Outcome:** wire the iOS camera/photo UX to shared evidence, add user-consented health reads where justified, and decide whether AlarmKit is necessary and store-compliant.

Work:

- delete the excluded draft Swift bridge and promote one reviewed native adapter;
- camera/photo picker with local Vision OCR/barcode;
- Swift/Kotlin evidence handoff and confirmation UI;
- UserNotifications recurrence, time-zone, cancellation, and permission state;
- HealthKit capability inventory, minimal data schema, consent, revocation, export/delete;
- AlarmKit proof of concept behind capability detection and honest fallback;
- simulator/device tests and privacy-manifest/store disclosure evidence.

Non-goals:

- guaranteed alarm wording;
- uploading raw photos by default;
- reading all HealthKit categories;
- challenge-to-dismiss behavior without platform and safety review.

## #4 — Android Health Connect and reminder reliability

**Outcome:** add the minimum Health Connect and reminder capabilities needed for protocol execution while handling reboot, time zone, permission, and OEM behavior honestly.

Work:

- Health Connect availability/permission adapter;
- minimal read model for user-selected fitness/recovery records;
- no background health collection without a visible job;
- recurring reminder projection and cancellation;
- boot/time-zone/package-replaced rescheduling receiver;
- exact-alarm need analysis and special-access UX only if justified;
- device/API/OEM harness matrix and delivery-reliability metrics;
- Play data-safety and permission declarations.

Non-goals:

- hidden background monitoring;
- exact-alarm permission as a default shortcut;
- medical interpretation of health samples;
- “works on every Android device” claim.

## #5 — Copyright-clean catalog and licensed media pipeline

**Outcome:** ship a useful catalog whose every record and asset has reproducible provenance and revocation behavior.

Work:

- canonical exercise/muscle/equipment taxonomy;
- per-field/per-record source model;
- independent Traditional Chinese and English instruction authoring workflow;
- top-50 requested exercise set before catalog breadth;
- commercial/commissioned asset procurement decision;
- contract evidence store and public-safe references;
- content-addressed originals and deterministic derivatives;
- signed catalog/media manifests;
- build-time hash enforcement, attribution generation, takedown, and kill switch;
- accessibility alternatives for every visual asset.

Non-goals:

- scraped mirror import;
- CDN hotlink;
- claiming repository-wide license resolves each media file;
- 1,000+ exercises before top-50 retention proof.

## #6 — Private LLM explanation gateway and evals

**Outcome:** add optional plain-language explanations while deterministic code remains authoritative.

Work:

- authenticated backend and provider abstraction;
- payload minimization/redaction;
- immutable decision receipt input;
- structured output schema and forbidden-language filter;
- adversarial evals for missing evidence, dosage requests, medication, symptoms, IU, prompt injection, and warning suppression;
- provider/version trace, cost caps, timeouts, fallback, and kill switch;
- no raw label photo or client provider key;
- human review path for flagged output.

Non-goals:

- free-form supplement advisor;
- model-created safety rules;
- diagnosis or dose recommendation;
- model output replacing decision history.

## #7 — Entitlements, privacy, stores, and release operations

**Outcome:** produce a real Android/iOS/Web release candidate with consistent entitlement and privacy behavior.

Work:

- StoreKit, Play Billing, and Web billing entitlement model;
- server receipt validation and restore/refund state;
- feature flags independent of model availability;
- account deletion, data export, retention, regional storage, and consent history;
- privacy policy, terms, health disclaimers, third-party notices, and store forms matching runtime flow;
- crash/analytics vendor review and sensitive-field redaction;
- signing, protected secrets, release provenance, SBOM, vulnerability scanning, rollback, and support runbook;
- accessibility, localization, performance, offline, and upgrade tests.

Non-goals:

- client-only entitlement authority;
- dark-pattern trial or renewal flow;
- collecting health data for advertising;
- production signing material in Git.

## #8 — Creator-market validation and launch evidence

**Outcome:** prove a retained-user acquisition loop using rights-cleared, disclosed, native creator content.

Work:

- 30 problem interviews and seven-day concierge test;
- 12 owned raw creative variants;
- creator selection scorecard and prohibited-claim review;
- staged contracts separating creation, posting, views, raw footage, and paid usage rights;
- creator/store/landing cohort instrumentation;
- verified-protocol activation, paid conversion, refund, day-30 retention, and contribution metrics;
- experiment ledger with stop/revise/repeat/scale decisions;
- rights/disclosure evidence for every published and reused asset.

Non-goals:

- fabricated comments or results;
- hidden sponsorship;
- copying another app's CPM or revenue as a forecast;
- scaling on views or installs without retained contribution.

## Release train

### Foundation 0.1

- Issue #1 only;
- internal/demo distribution;
- original schematic visuals;
- no backend and no health claim.

### Evidence Alpha 0.2

- safe subset of #2, #3, and #4;
- closed cohort;
- local/consented evidence and protocol testing;
- no public supplement-intelligence claim.

### Catalog Beta 0.3

- #5 top-50 rights-clean catalog;
- revocation drill;
- creator alpha content with rights evidence.

### Release Candidate 1.0

- reviewed scope of #2–#7;
- #8 acquisition evidence;
- exact store build, privacy declarations, and support/rollback readiness.

## Definition of done

A feature is done only when all applicable dimensions pass:

```text
code + deterministic tests
+ platform/device evidence
+ source and rights evidence
+ health/safety review
+ privacy and permission behavior
+ observability without sensitive leakage
+ failure and rollback path
+ user-facing wording matching reality
+ exact-head hosted checks
```

A screenshot, model response, vendor marketing page, local-only build, or issue comment cannot replace this evidence set.
