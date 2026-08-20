# Authority surface drift contract

`AGENTS.md` and `docs/implementation-status.md` are execution authorities, not narrative history. A stale claim in either file can route a later Agent into the wrong implementation, evidence, or admission decision even when product code is correct.

## State transition

```text
MANUALLY_RECONCILED_AUTHORITY
  -> MACHINE_GATED_AUTHORITY_DRAFT
```

The owning offline gate is:

```bash
python3 scripts/validate_authority_surfaces.py
python3 scripts/validate_authority_surfaces.py --self-test
```

## What the gate rejects

The validator fails closed when checked-in authority regresses to already-disproved states, including:

- repository visibility described as private;
- a global claim that hosted Actions never executed;
- Health Connect or HealthKit adapter work described as absent/future-only;
- the pinned Git Town v24.0.0 candidate described as absent or its blocked runtime boundary removed;
- exact-head hosted evidence collapsed across commits;
- Human Admit, legal/clinical/rights, real-device, or runtime-admission boundaries removed.

The self-test plants representative stale claims and requires every mutation to fail.

## Evidence boundary

This is a repository-content consistency gate. It is intentionally network-free and therefore does **not** discover live GitHub state. Live PR heads, repository visibility, workflow conclusions, issue state, and branch ancestry must still be resolved from GitHub before publication and recorded on the exact PR subject.

A PASS proves only that the checked-in authority surfaces preserve the required distinctions encoded by this validator. It does not prove:

```text
CHECKED_IN_AUTHORITY_PASS != LIVE_GITHUB_STATE_PROVEN
HOSTED_CHECK_PASS != MERGE_AUTHORITY
ADAPTER_PRESENT != REAL_DEVICE_VALIDATION
GIT_TOWN_CANDIDATE != GIT_TOWN_RUNTIME_ADMITTED
CODE_OR_TEST != LEGAL_CLINICAL_RIGHTS_APPROVAL
DEBUG_BUILD != RELEASE_SIGNING_ADMISSION
```

## Change law

When an intentional architecture or evidence transition makes one of these assertions obsolete, update the implementation/evidence first, then change the authority surfaces and this validator in the same convergence packet. Never weaken the gate merely to make CI green.

Merge, release promotion, signing admission, legal/clinical/rights acceptance, provider/store credentials, real-device evidence, and destructive rollback remain Human Admit or external gates.
