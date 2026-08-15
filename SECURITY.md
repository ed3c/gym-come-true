# Security Policy

## Reporting

Report security, privacy, medical-safety, licensing, or secret-exposure issues privately to the repository owner. Do not place real user health data, supplement labels, access tokens, signing material, or proprietary contracts in a public issue.

Include:

- affected commit and platform;
- reproduction steps using synthetic data;
- expected and actual behavior;
- whether user evidence or a secret may have left the device;
- a minimal rollback or containment suggestion.

## High-priority classes

- client-exposed provider/API keys;
- raw label images retained or uploaded without consent;
- health data used for advertising or unrelated analytics;
- OCR promoted to verified truth;
- LLM output bypassing deterministic warnings;
- unsafe generic unit conversion;
- duplicate ingredient totals omitted or miscomputed;
- unlicensed media shipped or hotlinked;
- reminder UI claiming guaranteed or coercive alarm behavior;
- signing credentials or private keys in git history.

## Response principles

1. Contain data flow or disable the affected capability.
2. Preserve a minimal audit trail without retaining sensitive payloads.
3. Revoke exposed credentials.
4. Correct user-facing claims.
5. Add a regression test and update the relevant registry.
6. Notify affected users when required by law or platform policy.

## Supported versions

Only the latest commit on the default branch after CI admission is considered supported. Draft branches are development evidence and must not be distributed as production builds.
