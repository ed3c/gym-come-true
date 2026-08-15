# Issue plan — entitlements, privacy, stores, and release operations

## Outcome

Produce a real Android/iOS/Web release candidate with consistent entitlement, privacy, signing, and rollback behavior.

## Acceptance

- StoreKit, Play Billing, and Web entitlement model with server validation.
- Restore, refund, cancellation, and offline grace behavior.
- Account export/delete, retention, consent history, and regional storage.
- Store forms, privacy policy, terms, and notices match runtime data flow.
- Sensitive-field redaction, SBOM, vulnerability checks, release provenance, support, incident, and rollback runbooks.
- Accessibility, localization, performance, offline, and upgrade tests.

## Hard limits

- No client-only entitlement authority.
- No dark-pattern trial/renewal flow.
- No health-data advertising.
- No production signing secret in Git.
