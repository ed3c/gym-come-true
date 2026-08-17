# Server-verified cross-store entitlement contract

> **Issue:** #38 — `NO_ENTITLEMENT -> VERIFIED_ENTITLEMENT_DRAFT`.
> **Status:** Draft. No provider account, verification server, or paying account exists.

## Decision

Access is a deterministic projection of an append-only ledger of server-verified provider events.
Nothing the client says participates in that projection.

```text
Apple / Google / web merchant
        │  (provider event)
        ▼
Our server verifies against the provider directly
        │  ProviderVerificationReceipt(channel = SERVER_TO_PROVIDER,
        │      serverVerificationSignatureSha256, providerPayloadSha256)
        ▼
Append-only ledger keyed by providerEventId  ──►  replay is idempotent
        │
        ▼
EntitlementProjector.project(account, ledger, asOf, production)
        ▼
EntitlementSnapshot(access = DENIED | GRACE | ENTITLED)
```

## What can never grant access

`EntitlementAssertionChannel` names each assertion source, and only `SERVER_TO_PROVIDER` is ever
admissible. Each of the following is covered by a test that asserts `DENIED`:

| Signal | Channel | Test |
| --- | --- | --- |
| Client purchase callback | `CLIENT_PURCHASE_CALLBACK` | `clientAssertedChannelsNeverGrantAccess` |
| Local boolean / cached flag | `CLIENT_LOCAL_CACHE` | same |
| Paywall UI state | `PAYWALL_UI_STATE` | same |
| A webhook that merely arrived | `WEBHOOK_ARRIVAL_UNVERIFIED` | same |
| Price and currency | any | `priceAndCurrencyCannotChangeAccess` |
| Sandbox verification in production | any | `sandboxReceiptCannotGrantProductionAccess` |
| A receipt for a different account | any | `anotherAccountsReceiptCannotGrantAccess` |
| Missing verification signature or payload hash | `SERVER_TO_PROVIDER` | `missingServerVerificationEvidenceDeniesAccess` |

The verification hashes are `null` in this repository. An absent hash is a blocker, not a default
allow, so the contract as committed grants nothing to anyone.

## Replay safety

`EntitlementEventLedger.append` is keyed on `providerEventId`:

```text
new id                     -> APPENDED
same id, identical content -> DUPLICATE_IGNORED   (ledger unchanged, projection unchanged)
same id, different content -> CONFLICTING_REPLAY  (ledger unchanged, conflict reported)
```

A ledger that already contains two rows with one id fails closed (`duplicatedEventIdsFailClosed`)
instead of picking a winner. Projection sorts by `(occurredAtIsoDate, providerEventId)`, so arrival
order cannot change the result (`projectionIsIndependentOfLedgerOrder`).

## Grant and revoke asymmetry

Granting requires an admitted receipt. Revoking does not: a refund, revocation, or expiry applies
even when its receipt is unverified, because the failure direction of a bad revocation is loss of
access, not unpaid access (`refundRevokesAccessEvenWithoutAnAdmittedReceipt`). Restoring access
after a refund needs its own server verification (`restoreAfterRefundRequiresItsOwnServerVerification`).

Auto-renew cancellation keeps the paid term and lets it expire on schedule. Grace is its own access
state rather than a longer entitlement, and it expires like any other grant.

## Evidence boundary

```text
Deterministic projection and its tests          THIS COMMIT (draft)
Provider verification server                    NOT_IMPLEMENTED
Apple / Google / web merchant accounts          ABSENT
Real provider receipts, signatures, payloads    ABSENT
Provider design and security review             HUMAN_ADMIT_REQUIRED
Persistence, transport, and key management      NOT_IMPLEMENTED
```

No provider secret exists in this slice, and none may: shared code cannot import platform, store,
provider, secret-management, or network APIs.

## Rollback subject

The immutable rollback subject is this commit's `Entitlement.kt` plus `EntitlementContractTest.kt`.
Reverting both returns the product to `NO_ENTITLEMENT`, which denies everyone — the same behaviour
the contract has today.
