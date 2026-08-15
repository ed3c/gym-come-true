# Issue plan — private LLM explanation gateway and evals

## Outcome

Add optional plain-language explanations while deterministic policy remains authoritative.

## Acceptance

- Authenticated server gateway and provider abstraction.
- Minimized structured payload and immutable decision receipt.
- Output schema rejects dose advice, invented evidence, diagnosis, and warning suppression.
- Adversarial evals cover missing fields, IU, medication, symptoms, prompt injection, and provider failure.
- Provider/version trace, cost limits, timeout, fallback, audit, and kill switch.
- No client model-provider secret or raw image upload.

## Hard limits

- No free-form supplement advisor.
- No model-created safety rule.
- No model output replacing the deterministic decision.
