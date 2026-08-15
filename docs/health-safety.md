# Health and supplement safety contract

## Product boundary

Gym Come True is a planning, evidence-capture, and logging product. It is not a medical device, clinician, pharmacist, dietitian, emergency service, or source of an individualized supplement dose.

The application may:

- extract candidate text and barcodes locally;
- help a user compare candidates with a physical label;
- normalize compatible mass units for display and duplicate detection;
- organize user-authored meal, training, recovery, and reminder events;
- apply reviewed deterministic rules that produce `LOG_ONLY`, `REVIEW_REQUIRED`, or `BLOCK_AUTOMATION`;
- explain the reason for a deterministic result in plain language.

The application must not:

- diagnose a condition or symptom;
- state that a supplement, food, workout, or dose is safe for a person;
- infer missing formulation or serving information;
- convert IU or another activity unit with a generic formula;
- recommend starting, stopping, increasing, decreasing, or combining a supplement or medication;
- replace medication instructions or professional care;
- hide a blocking rule because a model produced reassuring text.

`LOG_ONLY` means “the evidence can be recorded under the active policy,” not “clinically approved.”

## Evidence levels

| Level | Meaning | Permitted use |
|---|---|---|
| `UNVERIFIED` | OCR, barcode, import, or free-form user entry | Display and correction only |
| `USER_CONFIRMED` | User compared the field with the current physical label | Personal log; no product/rule authority |
| `VERIFIED_BY_REVIEWED_SOURCE` | Product variant and rule source were independently reviewed and versioned | Deterministic rule evaluation under the reviewed scope |

A barcode does not prove formulation, serving size, authenticity, expiry, country variant, or that the scanned bottle matches a database record.

## Units

Generic conversion is limited to the same physical dimension:

```text
1 g   = 1,000 mg
1 mg  = 1,000 mcg
```

The engine implements those display conversions only. It deliberately returns no generic conversion for:

- IU;
- CFU;
- enzyme activity units;
- proprietary blend totals;
- drops, scoops, tablets, capsules, or “servings” without verified mass per unit;
- ingredient-specific chemical equivalents.

Even a valid mass conversion is not a dose recommendation.

## User-provided A/B timetable

The source request contains a personal draft with a morning meal, lunch, 16:00 or 22:00 training variants, dinner, sleep recovery, foods, supplement names, amounts, and medication-related pause rules.

The implementation treats this as **Draft 0 / user-authored protocol**, not as validated health guidance:

- Food and training time blocks can be represented as ordinary plan events.
- Product names, servings, scoops, and amounts enter as unverified evidence.
- The “medication within three days” statements are not encoded as universal medical rules.
- A medication, planned procedure, pregnancy/breastfeeding state, unusual symptom, unclear label, IU value, or conflicting product blocks automation and asks for qualified review.
- The app never changes the draft amount based on OCR or an LLM.
- A future clinician-reviewed plan can reference the original draft while preserving who changed what, when, and why.

### Safe transformation

```text
User text
  -> time blocks and user-stated items
  -> each supplement field marked UNVERIFIED
  -> physical-label confirmation
  -> medication / symptom / procedure context gate
  -> reviewed regional rule-pack lookup
  -> log-only, review-required, or block receipt
  -> optional plain-language explanation
```

No step creates a prescription.

## Deterministic safety receipt

A production decision receipt should be immutable and include:

```json
{
  "decisionId": "uuid",
  "createdAt": "instant",
  "decision": "REVIEW_REQUIRED",
  "evidenceIds": ["scan-evidence-version"],
  "productVariantId": null,
  "rulePack": {
    "id": "tw-supplement-policy",
    "version": "not-active-in-foundation",
    "status": "MISSING"
  },
  "contextFlags": ["MEDICATION_CONTEXT_UNRESOLVED"],
  "reasons": ["A qualified interaction review is required"],
  "normalizedDisplayValues": {},
  "mayRecommendDose": false
}
```

The receipt is the authority passed to an explanation model. The model output is not written back as a decision.

## Regional rule-pack admission

A production Taiwan rule pack needs all of the following:

- explicit jurisdiction and product category;
- primary source document, issuing body, publication/effective date, and retrieved copy hash;
- exact field-to-rule mapping;
- unit dimension and ingredient identity rules;
- exclusions, ambiguity behavior, and conflict precedence;
- qualified reviewer identity and review date;
- automated tests for allowed, edge, ambiguous, and blocked cases;
- version, rollout percentage, expiry/re-review date, rollback target, and incident owner;
- user-facing wording reviewed separately from machine rules.

Web articles, model memories, marketing pages, forum posts, and product labels alone cannot become global safety rules.

## Medication and clinical context

The app stores only the minimum context needed for a decision. A first release should prefer coarse flags such as “medication context requires review” rather than sending full medication lists to analytics or an LLM.

When a user chooses to maintain a medication list, the design must add:

- explicit consent and purpose;
- local encryption and OS-protected keys;
- source and last-confirmed time for each entry;
- export/delete controls;
- no ad targeting or unrelated profiling;
- a reviewed interaction-data provider contract;
- conflict handling when professional instructions differ from a generic rule;
- emergency and adverse-event messaging reviewed for each store jurisdiction.

The foundation does not implement medication interaction lookup.

## Symptoms and emergency behavior

The UI must not attempt to triage an emergency with a general-purpose model. When the user reports a serious or rapidly worsening symptom, the safe behavior is to stop protocol automation and direct the user to local emergency or qualified medical help using jurisdiction-reviewed language.

Do not create a false promise that the app monitors the user continuously or can detect all dangerous reactions.

## Training safety

Workout planning follows the same evidence hierarchy:

- an exercise record describes a movement; it does not prove suitability;
- pain, dizziness, loss of balance, unusual shortness of breath, or technique failure must stop automatic progression;
- a camera pose estimate is a coaching signal, not a medical or injury diagnosis;
- workload progression needs recent completion, effort, recovery, and explicit user confirmation;
- late-night training recommendations must account for the user's sleep response rather than assume a universal result.

Future computer-vision form analysis needs separate accuracy tests across body type, mobility, camera angle, clothing, lighting, assistive devices, and exercise variation.

## LLM contract

A model request contains only structured, minimized data and these immutable flags:

```json
{
  "purpose": "Explain deterministic results in plain language",
  "mayRecommendDose": false,
  "mayOverrideWarnings": false,
  "instructions": [
    "Do not infer missing label fields",
    "Do not calculate or recommend a dose",
    "Do not diagnose or claim medical safety",
    "Repeat blocking reasons and qualified-review guidance"
  ]
}
```

The gateway must reject output that:

- introduces an ingredient or amount absent from evidence;
- changes the deterministic decision;
- uses imperative dosing language;
- states that a combination is safe;
- omits a blocking reason;
- contains a medical diagnosis.

A release needs schema validation, adversarial tests, provider outage fallback, model/version traceability, and a kill switch.

## Privacy defaults

- Process label images on device when platform support exists.
- Delete temporary image files immediately after extraction.
- Store evidence hashes and confirmed structured fields, not raw photos, by default.
- Keep raw OCR text out of general analytics and crash reports.
- Do not send barcode or product identity to a model unless needed and consented.
- Separate account, health, billing, marketing, and diagnostic purposes.
- Make export and deletion available before collecting sensitive long-term history.

## Release gate

Supplement intelligence cannot move from foundation to production until:

1. the regional rule pack is source-anchored and reviewed;
2. medication and symptom behavior is reviewed;
3. OCR accuracy and confirmation completion are measured on representative labels;
4. model output is constrained and independently evaluated;
5. privacy/store disclosures match real data flow;
6. there is a support, incident, rollback, and user-notification process.
