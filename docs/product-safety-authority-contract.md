# MVP product / safety authority contract

This contract prevents the owner-admitted MVP product boundary from drifting back into a clinical/safety-verdict product through stale documentation.

## Authority order

```text
repository-owner Human Admit
  -> docs/product/mvp-redesign.md
  -> legal/DISCLAIMER.md
  -> docs/health-safety.md
  -> implementation code/tests
  -> exact-head hosted evidence
```

`docs/product/mvp-redesign.md` records the product decision. `legal/DISCLAIMER.md` owns the user-facing notice wording. `docs/health-safety.md` owns engineering behavior inside that product boundary.

The offline gate is:

```bash
python3 scripts/validate_product_safety_authority.py
python3 scripts/validate_product_safety_authority.py --self-test
```

## Current product invariants

```text
INFORMATION_OR_LOGGING != SAFETY_VERDICT
ARITHMETIC_RESULT != DOSE_RECOMMENDATION
REVIEWED_RULE_PACK_CONTRACT_PRESENT != MVP_RULE_PACK_REQUIRED
MODEL_EXPLANATION != MEDICAL_AUTHORITY
DISCLAIMER_PRESENT != LEGAL_APPROVAL
NO_SAFETY_VERDICT_MVP != NO_REGULATORY_OBLIGATIONS
```

The repository may retain dormant source-lineage, reviewed-rule-pack and decision-receipt engineering. Their presence is provenance/history, not current user-facing decision authority and not an MVP release prerequisite while the product renders no safety verdicts.

## Fail-closed regressions

The gate rejects, among other things:

- reclassifying the MVP as medical/safety decision support without a new owner decision;
- making a reviewed Taiwan clinical rule pack a current MVP release prerequisite;
- treating arithmetic as a dose recommendation;
- treating AI explanation as medical authority;
- claiming medication-interaction lookup that the MVP does not implement;
- promoting platform adapter presence into real-device/store proof;
- treating the disclaimer as legal approval;
- removing the independent legal/privacy/device/rights/signing/release gates.

The self-test plants 12 contradictory states and requires every mutation to fail.

## External boundaries

This repository-content gate does not provide legal advice, clinical review, store approval, real-device evidence, provider credentials, security/privacy review, signing, release promotion or merge authority.

If a future owner decision introduces diagnosis, dosing, medication-interaction decisions, individualized safety verdicts or other regulated decision support, update the product authority first and re-evaluate legal/clinical/store requirements before changing the engineering contract. Do not weaken this validator simply to make CI green.