# data/llm-gateway — synthetic adversarial corpus

`DRAFT` fixtures for Issue #37. Nothing here can admit itself to production.

| File | Role |
| --- | --- |
| `adversarial-corpus.v1.json` | 31 synthetic attack cases with their expected gateway outcome |
| `validate_gateway_corpus.py` | Zero-network validator binding this corpus to the Kotlin harness |

## Provenance

Every case is repository-authored fiction. There is no real product, no real user, no real label, no
captured image, no real medication list and no real provider transcript. `syntheticOnly=true`,
`containsRealUserData=false` and `productionAdmitted=false` are asserted by the validator, and
`productionAdmitted=true` in an input manifest is rejected outright.

The corpus is a **mirror**, not the authority. The executable authority is
`shared/src/commonTest/kotlin/dev/ed3c/gymcometrue/explanation/AdversarialExplanationEvalTest.kt`;
the validator fails when the two disagree in either direction, so a case can never be quietly
deleted from one side.

## Run

```bash
python3 data/llm-gateway/validate_gateway_corpus.py
```

Exit `0` proves corpus/harness agreement, that every expected outcome and rejection names a state
that actually exists in the Kotlin enums, that the required attack categories and the positive
controls are present, and that no dose or diagnosis template is admitted. It proves nothing about
the harness passing — that needs `:shared:jvmTest` in the integrator's build.

## Lease note

This validator lives beside its fixtures because this lane's path lease covered `data/`, not
`scripts/`. Its natural home is `scripts/validate_explanation_gateway.py`, wired into the required
verification commands in `AGENTS.md`; that move needs a packet amendment that owns `scripts/` and
`AGENTS.md`.
