# Health and supplement safety contract

## Authority and product boundary

The owner-admitted MVP decision in [`product/mvp-redesign.md`](product/mvp-redesign.md) and the user-facing wording SSOT in [`../legal/DISCLAIMER.md`](../legal/DISCLAIMER.md) define the shipped product boundary.

Gym Come True is an **information and logging tool**. It may organize user-authored supplements, meals, workouts and reminders; perform honest arithmetic over confirmed data; and explain recorded information. It does not diagnose, treat, prescribe, recommend doses, or render a medical/safety verdict.

```text
INFORMATION_OR_LOGGING != SAFETY_VERDICT
ARITHMETIC_RESULT != DOSE_RECOMMENDATION
REVIEWED_RULE_PACK_CONTRACT_PRESENT != MVP_RULE_PACK_REQUIRED
MODEL_EXPLANATION != MEDICAL_AUTHORITY
```

If a future owner decision reintroduces individualized safety verdicts, medication-interaction decisions, dosing recommendations, or other regulated decision support, that is a product reclassification. The dormant reviewed-rule-pack/source-lifecycle contracts may then become relevant again, but they are not a current MVP release prerequisite.

## Permitted MVP behavior

The application may:

- extract candidate label text and barcodes on device;
- let the user compare candidates with a physical label and confirm or correct them;
- normalize compatible mass units (`mcg`, `mg`, `g`) for display, totals and duplicate-product overlap;
- preserve unresolved units or serving evidence instead of inventing a conversion;
- organize user-authored meal, training, recovery and reminder events;
- read explicitly requested, least-privilege health records through platform adapters where the feature is enabled;
- send minimized, allowed data to an admitted AI provider boundary for general-information explanation, with the mandatory medical-risk notice.

The application must not:

- diagnose a condition or symptom;
- state that a supplement, food, workout, combination or amount is safe for a person;
- infer missing formulation, serving size or ingredient data;
- apply a generic conversion to IU, CFU, activity units, proprietary blends, tablets, capsules, scoops or servings without verified dimensional evidence;
- recommend starting, stopping, increasing, decreasing or combining a supplement or medication;
- encode the user's historical medication-related pause rules as universal medical rules;
- let a model override deterministic evidence boundaries or hide uncertainty;
- claim continuous monitoring, emergency detection or guaranteed reminder/alarm delivery.

## Evidence levels

| Level | Meaning | MVP use |
|---|---|---|
| `UNVERIFIED` | OCR, barcode, import or free-form user entry | display/correction only |
| `USER_CONFIRMED` | user compared a field with the current physical label | personal log and arithmetic where dimensions match |
| `SOURCE_REVIEWED` | an exact source/mapping has been reviewed under its stated scope | reference metadata only unless a future admitted feature explicitly consumes it |

A barcode does not prove formulation, serving size, authenticity, expiry, market variant, or that the scanned bottle matches a database record.

## Unit and supplement arithmetic

Generic conversion is limited to the same mass dimension:

```text
1 g  = 1,000 mg
1 mg = 1,000 mcg
```

The engine may use these conversions for display and arithmetic. It must not derive a personalized dose, safe range, treatment recommendation or medication interaction from the result.

Unresolved evidence remains unresolved. In particular, IU and other activity units are not generically converted into mass.

## User-authored timetable

The user's A/B training schedule, foods, supplement names, product servings and timing are represented as user-authored plan data, not validated health guidance.

```text
User input
  -> local candidate/log record
  -> optional physical-label confirmation
  -> honest arithmetic where dimensions are compatible
  -> user-visible timetable/reminder
  -> optional general-information AI explanation
```

No step creates a prescription or a safety verdict. The application does not silently alter the user's amount based on OCR or an LLM.

## Dormant reviewed-rule-pack engineering

The repository contains source-lineage, reviewed-rule-pack, decision-receipt and related validators created before the 2026-08-18 MVP repositioning. They remain tested engineering and provenance history.

For the current MVP:

- they do not authorize a medical or safety claim;
- a reviewed Taiwan clinical rule pack is **not** required merely to ship information/logging, arithmetic, reminders or general-information AI;
- their presence must not cause the UI to label a product, combination or amount medically safe;
- they may be removed later if they create maintenance cost;
- reactivating them as user-facing decision authority requires a new owner product decision plus appropriate legal/clinical/store review and fresh tests.

## Medication, symptoms and emergencies

The MVP does not implement medication-interaction lookup and does not use a general-purpose model to triage emergencies.

If users record medication, symptom, pregnancy, procedure or similar context, the application may preserve that as user-provided information under an admitted privacy design. It must not infer a medical action from it.

Serious or rapidly worsening symptoms require jurisdiction-appropriate user-facing guidance reviewed outside repository code. Do not promise continuous monitoring or detection of dangerous reactions.

## Training safety

Exercise metadata describes movements; it does not prove suitability for an individual. Pose or form signals, if added, are coaching signals rather than diagnosis. Pain, dizziness, loss of balance, unusual shortness of breath or other concerning conditions must not trigger an automated medical conclusion.

Future computer-vision coaching requires separate accuracy, bias, accessibility and real-device evidence.

## AI contract

The owner-selected provider direction is OpenAI (ChatGPT) and Anthropic (Claude), through a server-side or user-key provider boundary that keeps repository/client secrets absent.

Every AI response must carry the medical-risk notice whose wording SSOT is [`../legal/DISCLAIMER.md`](../legal/DISCLAIMER.md). The response is general information only.

The AI boundary must reject or suppress behavior that:

- invents an ingredient or amount absent from the supplied record;
- calculates or recommends a dose;
- diagnoses or claims medical safety;
- overrides explicit uncertainty or evidence status;
- omits the mandatory risk notice.

Provider deployment, credentials, security review, privacy review, outage behavior, model/version observability and kill-switch operation require separate evidence.

## Health-data boundary

Android Health Connect and iOS HealthKit adapters are least-privilege read surfaces. Their checked-in presence does not prove real-device behavior, entitlement/store approval, OEM/provider availability, privacy disclosure completeness or release admission.

```text
ADAPTER_PRESENT != REAL_DEVICE_VALIDATION
DECLARED_PERMISSION != STORE_APPROVAL
```

## Privacy defaults

- process label images on device when platform support exists;
- delete temporary image files after extraction unless the user explicitly chooses an admitted retention flow;
- keep raw OCR text, images, barcodes and health samples out of general analytics/crash telemetry;
- minimize any provider payload and do not put provider/store/signing secrets in the client or repository;
- separate account, health, billing, marketing and diagnostics purposes;
- implement export/deletion and store disclosures before long-term sensitive-history collection is admitted.

## Current MVP release gates

The information/logging MVP still requires its real shipped behavior to satisfy the applicable engineering and external gates, including:

1. OCR/user-confirmation behavior measured on representative labels if the feature ships;
2. AI notice enforcement, constrained provider boundary, adversarial evaluation, privacy/security review and kill switch if AI ships;
3. real-device/OEM/entitlement evidence for enabled Health Connect/HealthKit features;
4. privacy policy, retention/export/deletion behavior and accurate Apple/Google disclosures;
5. rights evidence for every third-party source/media asset used in the released product;
6. accessibility, supported-device/browser checks, incident handling and support process;
7. signed release builds, store records, submission/review evidence, promotion and rollback authority.

A clinically reviewed Taiwan rule pack is not a current MVP gate because the MVP does not render safety verdicts. If that product boundary changes, the required gates must be reassessed before implementation or release.

```text
GITHUB_CHECK_PASS != HUMAN_ADMIT
DISCLAIMER_PRESENT != LEGAL_APPROVAL
NO_SAFETY_VERDICT_MVP != NO_REGULATORY_OBLIGATIONS
```