# AI provider key boundary and mandatory notice (Issue #49)

Owner decision `docs/product/mvp-redesign.md` (2026-08-18): the MVP is an information and logging
tool. AI features use OpenAI (ChatGPT) and Anthropic (Claude) and every AI response carries the
medical-risk notice from `legal/DISCLAIMER.md`. This document records the credential boundary and
the type-level enforcement that back those claims.

Source: `shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/explanation/AiProviderContract.kt`
Tests: `shared/src/commonTest/kotlin/dev/ed3c/gymcometrue/explanation/AiProviderContractTest.kt`

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
STATIC   notice literals compared byte-for-byte against legal/DISCLAIMER.md PASS (ad-hoc script)
STATIC   planted-defect control on that comparison                          PASS (turns red)
STATIC   python3 scripts/validate_repository.py                             PASS
STATIC   python3 data/llm-gateway/validate_gateway_corpus.py                PASS (6/6)
SANDBOX  ./gradlew :shared:jvmTest                                          NOT_EXERCISED in this lane
PROD     real OpenAI or Anthropic call, real key, real deployment           ABSENT
```

The byte comparison ran as a throwaway script in the implementation lane, not as a committed
validator: `scripts/validate_*.py` is outside this lane's path lease. Wanted addition, for whoever
owns that file next — extend `validate_llm_boundary()` with:

```python
# concatenate the Kotlin string literals of MedicalRiskNotice.MANDATORY and of the test's
# SSOT_NOTICE_* constants, and require both to equal the "> "-prefixed notice blocks in
# legal/DISCLAIMER.md, so a reworded disclaimer cannot silently diverge from the shipped notice.
```

Until that lands, the notice binding is guarded by two independently typed copies (main source and
test source) plus the Kotlin test that compares them — which catches an edit to either one but not a
simultaneous edit to both.

## External gates (unchanged by this lane)

| Gate | State | Owner |
| --- | --- | --- |
| OpenAI / Anthropic credentials and deployment environment | `ABSENT` | Human Admit |
| Real provider adapter implementation | `ABSENT` | Human Admit |
| Provider adapter for the logged-totals subject | `ABSENT` | follow-up issue |
| Legal review of the shipped notice wording | `HUMAN_ADMIT_REQUIRED` | Human Admit |
| Store review of the AI surface (Apple 1.4.x, Google Play health) | `HUMAN_ADMIT_REQUIRED` | Human Admit |

No vendor model identifier exists in this repository. `AiProviderDescriptor.modelFamily` is an
opaque token supplied at deployment; the tests use `test-family`.
