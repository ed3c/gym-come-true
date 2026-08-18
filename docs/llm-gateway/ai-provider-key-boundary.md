# AI provider key boundary and mandatory notice (Issue #49, deepened by Issue #51)

Owner decision `docs/product/mvp-redesign.md` (2026-08-18): the MVP is an information and logging
tool. AI features use OpenAI (ChatGPT) and Anthropic (Claude) and every AI response carries the
medical-risk notice from `legal/DISCLAIMER.md`. This document records the credential boundary and
the type-level enforcement that back those claims.

Source: `shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/explanation/AiProviderContract.kt`
Source: `shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/explanation/ExplanationProviderBoundary.kt`
Tests: `shared/src/commonTest/kotlin/dev/ed3c/gymcometrue/explanation/AiProviderContractTest.kt`
Validator: `scripts/validate_repository.py::validate_llm_boundary`

## Where a key may live

`ProviderEndpointClass` names the only two admitted placements. There is no third.

| Endpoint class | Key holder | Reaches a client build? | Reaches Git? |
| --- | --- | --- | --- |
| `SERVER_SIDE_RELAY` | a relay this repository does not describe | no | no |
| `USER_SUPPLIED_RUNTIME_KEY` | the user, typed at runtime, session-lifetime only | held in memory, never persisted here | no |

Consequences encoded in code rather than in this prose:

- No credential is a parameter, a constant, a resource, or a build input of the shared module.
  `AiProviderDescriptor` carries `credentialAdmitted: Boolean` — the fact that a human admitted a
  key somewhere else, never the key.
- `credentialAdmitted` defaults to `false` and `killSwitchEngaged` defaults to `true`, so a
  half-configured deployment blocks instead of dialling out.
- `data/llm-gateway/validate_gateway_corpus.py` fails the build if `apiKey`, `api_key`,
  `Authorization`, `Bearer `, `http://` or `https://` appear in the gateway contract or the provider
  boundary. See the wanted extension below.

## Per-provider kill switch

`AiProviderDescriptor.killSwitchEngaged` is per provider, not global: disabling OpenAI leaves
Anthropic serving and vice versa. `AiExplanationService` evaluates it (together with
`credentialAdmitted`) **before** any request object forms, so an engaged switch cannot leak a
payload. `AiExplainOutcome.request` is `null` on every blocked path, and the tests use a
call-counting provider double to assert the provider was never reached — not merely that it
answered no.

The pre-existing global `GatewayPolicy.killSwitchEngaged` stays where it is; the per-provider switch
is an additional gate in front of it, not a replacement.

## Admitted explain subjects

An `AiExplainRequest` can only carry an `ExplainSubject`, and `ExplainSubject` has exactly two
implementations, both with internal constructors:

1. `ExplainSubject.GatewayReceipt` wraps an `AdmittedExplanationRequest`, which only
   `ExplanationRequestGate` can produce. Dose, diagnosis, rule-authoring and free-form intents,
   raw images, OCR text and free-text context are rejected there, unchanged by this lane.
2. `ExplainSubject.LoggedTotals` wraps `MinimizedLoggedTotals`: a SHA-256 of the user's own daily
   summary plus opaque ingredient keys and counts. Free-text ingredient names fail the token check
   and are rejected, not silently forwarded — shared code has no hasher, so the pseudonymous keys
   are supplied by the caller that does.

The logging surface serves `tpl.logged.*` templates only. It never restates a `SafetyDecision`,
because the MVP app renders no safety verdicts at all.

## Provider deepening: an admitted provider may serve logged totals (Issue #51)

`AiExplanationService.explainLoggedTotals` was deterministic-only. It now takes an optional
`LoggedTotalsProvider`, and the widening is deliberately narrow:

| Property | How it is held |
| --- | --- |
| The provider is reached only when the subject gate admits | its input is an `ExplainSubject.LoggedTotals`, whose constructor is internal; the service builds one only after the kill-switch, credential, caller, locale, hash and token checks have all passed |
| The kill-switch and credential gates are unchanged | both paths call the one `providerBlocker(policy, descriptor)` in `ExplanationProviderBoundary.kt`; the per-provider switch in front of it is untouched |
| The `GatewayRejection` ladder is unchanged | `LoggedTotalsPlanVerifier` reuses existing rungs (`PLAN_RECEIPT_MISMATCH`, `PLAN_TEMPLATE_NOT_ADMITTED`, `PLAN_INVENTED_REASON`, `PLAN_MISSING_DISCLAIMER`, `PLAN_SUPPRESSED_WARNING`, `UNSUPPORTED_LOCALE`); no enum member was added, removed or reordered |
| A served plan carries no dose, diagnosis or clearance template | the verifier admits only `AdmittedLoggedTotalsTemplates.all` plus the disclaimer — the decision templates are not in that set, so a provider cannot restate a safety verdict on a surface that renders none |
| Every served response carries the notice | the response type takes `MedicalRiskNotice.MANDATORY` as a property initializer, and every failure path degrades to the deterministic plan rather than to nothing |

The provider's entire degree of freedom is which admitted next step (`tpl.logged.next-step.*`) to
surface, and each one is verified against the deterministic counts: proposing "review the
duplicate" when the day has no duplicate is an invented observation and is rejected with
`PLAN_INVENTED_REASON`, exactly as an invented reason key is on the receipt path.

The receipt path is unchanged apart from that shared blocker extraction. `PROVIDER_IMPLEMENTATION`
stays `ABSENT` for both subjects: shared code holds the interface, the gate and the verifier.

## Type-enforced medical-risk notice

`MedicalRiskNotice` has a private constructor and a single instance, `MedicalRiskNotice.MANDATORY`,
whose bytes are the zh-Hant and en notice from `legal/DISCLAIMER.md`. `AiExplanationResponse` takes
the notice as a property initializer rather than a constructor parameter, so:

- a response without a notice has no representation,
- a response with weakened, translated, or empty wording has no representation,
- the only code that can build a response at all is `AiExplanationService`, whose constructor is
  internal to the shared module.

"Attempting to build a notice-less response fails to compile" is not expressible as a passing test,
so the enforcement is structural (private constructor + single instance + non-parameter property)
and the tests assert the observable half: every served response — deterministic fallback, accepted
model plan, and logged-totals summary, for both providers — carries `MANDATORY`, byte-identical to
the SSOT strings. One test plants a provider that strips the disclaimer template and asserts the
served response still carries both the notice and the template.

## Evidence lanes

```text
STATIC   Kotlin contract + tests authored                                   PRESENT
STATIC   notice byte binding inside validate_llm_boundary()                 PASS (committed)
STATIC   planted-defect control on that binding                             PASS (turns red, twice)
STATIC   python3 scripts/validate_repository.py                             PASS (8/8)
STATIC   python3 data/llm-gateway/validate_gateway_corpus.py                PASS (6/6)
SANDBOX  ./gradlew :shared:jvmTest                                          NOT_EXERCISED in this lane
PROD     real OpenAI or Anthropic call, real key, real deployment           ABSENT
```

### Notice byte binding (Issue #51) — extraction spec

The wanted addition from the Issue #49 lane has landed inside
`scripts/validate_repository.py::validate_llm_boundary`. What it does, and why in that shape:

1. Split `legal/DISCLAIMER.md` at `## AI medical-risk notice` and stop at the next `## ` heading, so
   the first-run acknowledgement blocks below it cannot be mistaken for the AI notice.
2. Inside that section, take the `> `-prefixed lines after the `zh-Hant:` and `en:` labels and strip
   the marker. zh-Hant wraps mid-sentence, so its lines join with **no** separator; en wraps on word
   boundaries, so its lines join with a single space.
3. Concatenate the Kotlin string literals of `MedicalRiskNotice.MANDATORY` (main source) and of
   `SSOT_NOTICE_ZH_HANT` / `SSOT_NOTICE_EN` (test source), and require all four to equal the two
   unwrapped blocks.

Every anchor is a `require(...)`: a rename or a reformat that makes an anchor unfindable fails
closed rather than silently checking nothing. Escaped Kotlin literals are refused for the same
reason — this check unescapes nothing, so it must never pretend to.

The binding closes the gap the previous lane recorded: the two independently typed Kotlin copies
plus the Kotlin test catch an edit to either copy, but not a simultaneous edit to both, and neither
catches a reworded `legal/DISCLAIMER.md`. The validator is a third, independent arrival that reads
the SSOT itself.

Planted-defect controls (both reverted, `legal/DISCLAIMER.md` restored to
`sha256 1e41f5656b593db4562b94d0b2c4d5373f7c5cfa946a1a5c9b8aa6d6cd3934bd`):

| Planted defect | Validator exit | Message |
| --- | --- | --- |
| dropped `Consult a qualified healthcare professional…` from the en block of `legal/DISCLAIMER.md` | `1` | `FAIL MedicalRiskNotice.MANDATORY en notice diverged from the legal/DISCLAIMER.md SSOT` |
| dropped `相關決定與後果由使用者自行負責。` from the test's `SSOT_NOTICE_ZH_HANT` | `1` | `FAIL AiProviderContractTest SSOT constants zh-Hant notice diverged from the legal/DISCLAIMER.md SSOT` |

The second control exists because the first one short-circuits on the main source: without it, the
test-source arm of the comparison would be an untested claim.

## External gates (unchanged by this lane)

| Gate | State | Owner |
| --- | --- | --- |
| OpenAI / Anthropic credentials and deployment environment | `ABSENT` | Human Admit |
| Real provider adapter implementation (both subjects) | `ABSENT` | Human Admit |
| Provider adapter *interface* for the logged-totals subject | `PRESENT` (Issue #51) | — |
| Legal review of the shipped notice wording | `HUMAN_ADMIT_REQUIRED` | Human Admit |
| Store review of the AI surface (Apple 1.4.x, Google Play health) | `HUMAN_ADMIT_REQUIRED` | Human Admit |

No vendor model identifier exists in this repository. `AiProviderDescriptor.modelFamily` is an
opaque token supplied at deployment; the tests use `test-family`.
