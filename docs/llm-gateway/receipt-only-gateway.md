# Receipt-only explanation gateway

Draft contract for Issues #35, #36 and #37. Every state below is `DRAFT`; nothing here is admitted
for production.

## 1. Why this shape

A supplement explainer is dangerous in exactly one way: the model becomes the thing the user
believes. The defence is not a better prompt. It is removing the model's ability to say anything the
repository did not already write.

Three structural decisions do that:

1. **The request cannot carry evidence.** The only admitted payload is a minimized projection of an
   immutable deterministic receipt: hashes, enums and opaque tokens. There is no field for a label
   photo, OCR text, a product name, a medication list or a question.
2. **The response cannot carry sentences.** A provider returns an `ExplanationPlan` — a list of
   template identifiers drawn from an admitted catalogue. Prose lives in reviewed localization
   resources, not in model output.
3. **The provider is optional by construction.** `DeterministicExplanationPlanner` produces the same
   required plan without any provider. Kill switch, timeout, cost overrun, provider failure and any
   verification failure all resolve to that plan, so no user-visible behaviour depends on a model
   being available or honest.

## 2. What crosses the boundary (#35)

`ReceiptMinimizer.minimize` projects a `SupplementDecisionReceipt` down to
`MinimizedDecisionReceipt`:

| Field | Kind | Note |
| --- | --- | --- |
| `receiptId`, `rulePackId`, `rulePackVersion`, `triggeredRuleIds` | opaque token | `^[A-Za-z0-9][A-Za-z0-9._:-]{0,119}$` — prose cannot ride inside one |
| `receiptSha256`, `productVariantKeySha256`, `evidenceSha256`, `rulePackContentSha256` | SHA-256 | shape-checked only; shared code has no hasher |
| `decision` | enum | the deterministic decision, restated not recomputed |
| `reasonKeys` | enum list | see below |
| `modelUsedForDecision` | boolean | must be false; a true value is rejected |

The product name never crosses. `SupplementDecisionReceipt.productVariantKey` contains brand,
formulation and label revision — often Traditional Chinese free text — so the gateway accepts only
its hash. The caller computes both content hashes, because a hasher is a platform capability and
shared code must not import one. **Content-addressing of the receipt is therefore `ABSENT` inside
this module**: shared code verifies the shape of the hash and that the plan echoes the same hash, not
that the hash matches any bytes. A server-side implementation must compute and compare it.

### Reason keys instead of reason text

`SupplementSafetyEngine` emits English sentences. Forwarding them would let unreviewed wording reach
a model and, through it, a user. `DeterministicReasonKeys` maps each exact deterministic sentence to
an `ExplanationReasonKey`; an unknown sentence produces `UNMAPPED_REASON_TEXT` and the request is
refused. The mapping is exact-match on purpose — when the engine's wording changes, the gateway
fails closed instead of forwarding text nobody reviewed, and
`everyDeterministicReasonHasAnEnumeratedKey` fails in the integrator's build.

### Rejections at the gate

`UNAUTHENTICATED`, `CLIENT_SIDE_EXECUTION_DENIED`, `UNSUPPORTED_INTENT` (dose, diagnosis, rule
authoring, free-form advice), `UNSUPPORTED_LOCALE`, `RAW_IMAGE_PRESENT`, `RAW_OCR_TEXT_PRESENT`,
`FREE_TEXT_CONTEXT_PRESENT`, `MISSING_RECEIPT`, `MALFORMED_HASH`, `NON_TOKEN_FIELD`,
`MODEL_CLAIMED_DECISION`.

Only `ExplanationRequestGate` can construct an `AdmittedExplanationRequest` (internal constructor),
so a provider call site cannot assemble an unchecked request by hand.

### The admitted template catalogue

There is no dose template, no diagnosis template, no interaction template and no
"safe amount" template. A provider cannot request one because the identifier would not exist;
`PLAN_TEMPLATE_NOT_ADMITTED` is returned for anything outside the catalogue. Two independent checks
guard this: `noAdmittedTemplateOffersDoseOrDiagnosis` in Kotlin and
`validate_no_dose_or_diagnosis_template_is_admitted` in the Python validator.

The provider's entire freedom is choosing among `optionalTemplates` (next-step prompts) and ordering.
That is deliberate: an explanation layer that can add content is an advice layer.

### Plan verification

`ExplanationPlanVerifier` rejects a plan that mutates the receipt identity
(`PLAN_RECEIPT_MISMATCH`), restates a different decision (`PLAN_DECISION_MUTATED`), uses an
unadmitted template (`PLAN_TEMPLATE_NOT_ADMITTED`), covers a reason the receipt never carried
(`PLAN_INVENTED_REASON`), drops a warning the receipt did carry (`PLAN_SUPPRESSED_WARNING`), or omits
the not-medical-advice footer (`PLAN_MISSING_DISCLAIMER`).

## 3. Provider adapter boundary (#36)

`PROVIDER_IMPLEMENTATION = ABSENT`. Shared code holds only the `ExplanationProvider` interface, the
policy that decides whether a provider may be consulted, verification of what it returns, and the
fallback. The single implementation that exists anywhere in this repository is the in-memory fake in
the eval harness, declared `NONE_LOCAL_DETERMINISTIC`.

- **No secrets, no endpoints.** `ProviderDescriptor` carries provider, model and version tokens for
  the audit trail. There is no credential parameter anywhere in the API; the Python validator fails
  if `apiKey`, `Authorization`, `Bearer ` or an `http(s)://` literal ever appears in either shared
  source file.
- **Credential admission is a human gate.** `SERVER_INJECTED` providers return
  `PROVIDER_NOT_ADMITTED` until `GatewayPolicy.serverCredentialAdmitted` is set by a deployment this
  repository does not own.
- **Kill switch is deterministic.** `killSwitchEngaged` short-circuits before any provider call and
  is recorded in the audit record.
- **Cost and timeout are deterministic.** `costUnits > maxCostUnitsPerRequest` and
  `latencyMs > timeoutMs` both fall back; the provider cannot self-report a budget.
- **Audit is hash-only.** `GatewayAuditRecord` has no field capable of carrying label text, a product
  name, a symptom description or model prose: hashes, enums, tokens and numbers only. The caller's
  identity is a session pseudonym.

Outcomes are `REQUEST_REJECTED` (no plan served), `DETERMINISTIC_FALLBACK` (the deterministic plan
served, rejection codes recorded) and `MODEL_PLAN_ACCEPTED` (a verified provider plan served).

## 4. Adversarial eval suite (#37)

31 cases in `data/llm-gateway/adversarial-corpus.v1.json`, mirrored one-for-one by the Kotlin
harness. Categories: `MISSING_EVIDENCE`, `RAW_INPUT`, `MEDICATION_SYMPTOM`, `DOSE_REQUEST`,
`DIAGNOSIS_REQUEST`, `DECISION_AUTHORITY`, `PROMPT_INJECTION`, `IU`, `INVENTED_EVIDENCE`,
`WARNING_SUPPRESSION`, `PROVIDER_FAILURE`, `POSITIVE_CONTROL`.

Every case asserts the outcome kind and the expected rejection code. Every served plan additionally
asserts, regardless of the case: only admitted templates, the deterministic decision unchanged, the
disclaimer present, and the IU warning still present. Two positive controls exist so the suite cannot
pass by rejecting everything, and `aPlantedDoseTemplateIsProvablyProducedAndProvablyDropped` shows
the planted unsafe output really is produced by the fake provider and really is absent from what the
gateway serves.

### Honest limits of this suite

- It has **never executed**. It is authored Kotlin; `:shared:jvmTest` belongs to the integrator.
- The subject under eval is deterministic repository code plus a scripted fake. No real model,
  provider or version has been exercised, so no model-behaviour claim can be derived from it.
- Prompt-injection coverage is structural (injection cannot occupy a token field or a template
  identifier). It says nothing about a real model's behaviour on a real prompt, because no prompt
  template has been authored yet.
- Red-team review, security review and production promotion remain `HUMAN_ADMIT_REQUIRED`.

## 5. Rollback

Immutable rollback subject: the exact commit that introduced
`shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/explanation/`. Reverting that directory,
`docs/llm-gateway/` and `data/llm-gateway/` removes the gateway entirely; no other module imports it
yet, and no deterministic decision path depends on it.
