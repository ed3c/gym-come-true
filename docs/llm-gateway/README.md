# LLM explanation gateway — directory contract

Owner of Issue #35 (L1 receipt-only contract), #36 (L2 provider adapter boundary) and #37
(L3 adversarial eval suite), all under Issue #12. See the
[issue index](../github-issue-index.md) and [plan](../plans/issue-6-llm-gateway.md).

This directory documents a gateway whose entire purpose is to be *unable* to decide anything.
Deterministic code owns conversion, arithmetic, warnings, blocking, admission and lifecycle;
the gateway can only restate an immutable receipt using repository-authored templates.

## Files

| Path | Role |
| --- | --- |
| [`receipt-only-gateway.md`](receipt-only-gateway.md) | The contract, the provider boundary, and the eval suite in detail |
| [`ai-provider-key-boundary.md`](ai-provider-key-boundary.md) | Issue #49: where a provider key may live, the per-provider kill switch, and the type-enforced notice |
| `shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/explanation/AiProviderContract.kt` | `ProviderId`, per-provider descriptor and kill switch, admitted explain subjects, mandatory notice, hash-only audit |
| `shared/src/commonTest/kotlin/dev/ed3c/gymcometrue/explanation/AiProviderContractTest.kt` | Issue #49 tests: dose/diagnosis rejected per provider, kill switch blocks before a request forms, notice always present |
| `shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/explanation/ExplanationGatewayContract.kt` | Minimization, request gate, template catalogue, deterministic planner, plan verifier |
| `shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/explanation/ExplanationProviderBoundary.kt` | Provider interface, policy (kill switch, cost, timeout, credential admission), hash-only audit, service |
| `shared/src/commonTest/kotlin/dev/ed3c/gymcometrue/explanation/ExplanationGatewayContractTest.kt` | Contract tests, including the drift guard against the deterministic engine's reason strings |
| `shared/src/commonTest/kotlin/dev/ed3c/gymcometrue/explanation/AdversarialExplanationEvalTest.kt` | 31-case adversarial harness |
| `data/llm-gateway/adversarial-corpus.v1.json` | Synthetic corpus mirroring the harness |
| `data/llm-gateway/validate_gateway_corpus.py` | Zero-network validator binding corpus, harness and catalogue together |

## Delivered state

| Issue | Target transition | Reached in this lane | Evidence |
| --- | --- | --- | --- |
| #35 | `REVIEWED_TAIWAN_RULE_PACK -> EXPLANATION_GATEWAY_CONTRACT` | `EXPLANATION_GATEWAY_CONTRACT_DRAFT` | Kotlin contract + 10 deterministic tests authored; tests `NOT_EXERCISED` locally |
| #36 | `EXPLANATION_GATEWAY_CONTRACT -> PROVIDER_DRAFT` | `PROVIDER_BOUNDARY_DRAFT` | Interface, policy, audit and fallback authored; provider implementation `ABSENT` |
| #37 | `PROVIDER_DRAFT -> EVALUATED_GATEWAY` | `ADVERSARIAL_CORPUS_DRAFT` | 31-case corpus + harness authored; harness execution `NOT_EXERCISED` |
| #49 | `PROVIDER_BOUNDARY_DRAFT -> NAMED_PROVIDERS_WITH_MANDATORY_NOTICE` | `AI_PROVIDER_CONTRACT_DRAFT` | `ProviderId`, per-provider kill switch, type-enforced notice + 13 tests authored; tests `NOT_EXERCISED` locally |

`EVALUATED_GATEWAY` is **not** reached. An eval suite that has never executed is a written
intention, not evidence. The only checks that actually ran in this lane are the zero-network Python
validators (see below).

## Evidence lanes

```text
STATIC   Kotlin contract, catalogue, corpus, harness sources        PRESENT
STATIC   python3 data/llm-gateway/validate_gateway_corpus.py        PASS (6/6 checks)
STATIC   planted-defect control on that validator                   PASS (6/6 checks turn red)
SANDBOX  ./gradlew :shared:jvmTest                                  NOT_EXERCISED in this lane
PROD     real provider, real model, real deployment                 ABSENT
```

The Kotlin sources compile only in the integrator's build. Nothing in this directory may be read as
proof that the harness passes; it is proof that the harness and its corpus agree and that the
catalogue admits no dose or diagnosis template.

## External gates

| Gate | State | Owner |
| --- | --- | --- |
| Provider credentials and deployment environment | `ABSENT` | Human Admit |
| Independent security / red-team review of the eval suite | `HUMAN_ADMIT_REQUIRED` | Human Admit |
| Legal and privacy review of the minimized payload | `HUMAN_ADMIT_REQUIRED` | Human Admit |
| Clinical review of user-facing template wording | `HUMAN_ADMIT_REQUIRED` | Human Admit |
| Exact model / provider / version record for a real eval run | `ABSENT` | Human Admit |
| Production promotion of any provider | `HUMAN_ADMIT_REQUIRED` | Human Admit |

No credential, endpoint, provider account, vendor model identifier, or review signature exists in
this repository. Two vendors are now *named* — OpenAI (ChatGPT) and Anthropic (Claude), per the
owner decision in `docs/product/mvp-redesign.md` — but naming a vendor admits nothing:
`ProviderCredentialSource.SERVER_INJECTED` stays `PROVIDER_NOT_ADMITTED` until
`GatewayPolicy.serverCredentialAdmitted` is turned on by a human in a deployment this repository
does not describe, and `AiProviderDescriptor` ships with its kill switch engaged and
`credentialAdmitted = false`.

## Verification

```bash
python3 data/llm-gateway/validate_gateway_corpus.py
python3 scripts/validate_repository.py
sh ./gradlew :shared:jvmTest   # integrator only; never run in an implementation lane
```

## Known defect this lane could not fix

`shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/domain/Domain.kt` still exposes
`LlmExplanationBoundary.createPayload`, which forwards a whole `ScanEvidence` — OCR candidate
strings, raw ingredient names and warning text — to a model. That is the exact payload shape #35
exists to forbid. It was not removed here because `scripts/validate_repository.py`
(`validate_llm_boundary`) asserts on that declaration and both files are outside this lane's path
lease. Removing the bootstrap boundary and repointing its one test at `ReceiptMinimizer` needs a
convergence packet that owns `Domain.kt`, `DomainTest.kt` and `scripts/validate_repository.py`
together.
