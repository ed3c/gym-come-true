# Account export, deletion, retention, and consent history

> **Issue:** #39 — `VERIFIED_ENTITLEMENT_DRAFT -> ACCOUNT_DATA_LIFECYCLE_DRAFT`.
> **Status:** Draft. No real account data, no real consent, and no privacy or legal review exists.

## Decision

The privacy inventory is the subject every other privacy operation is checked against. A data flow
that is not inventoried cannot be exported, retained, or erased, so "we forgot that store" becomes a
blocker instead of a silent survival of personal data.

```text
PrivacyInventoryEntry(category, purpose, store, region, retention, legal basis, export treatment)
        │
        ├── PrivacyInventoryValidator ── purpose bans, legal basis, retention, region, export shape
        ├── AccountExportValidator    ── coverage of every inventoried record
        └── AccountDeletionValidator  ── one erasure receipt per inventoried store
                    ▲
        ConsentHistoryResolver (append-only; withdrawal is a new event)
```

## Negative controls

| Law | Encoding | Test |
| --- | --- | --- |
| Deletion cannot be UI-only | `ErasureMethod.UI_HIDE` is never erasure | `deletionCannotBeUiOnly` |
| Deletion must reach every store | one execution per inventoried `(entryId, store)` | `deletionMustReachEveryInventoriedStore` |
| Anonymisation is not deletion | allowed only for `DIAGNOSTIC_TELEMETRY` | `anonymisationIsNotErasureForHealthData` |
| Erasure needs a receipt | `receiptRef` required, dated at or after the request | `erasureWithoutAReceiptIsNotComplete` |
| Health data is never advertising data | hard blocker, consent cannot unlock it | `healthDataCanNeverBeUsedForAdvertising` |
| Health data is never training data | hard blocker | `healthDataCanNeverBeUsedForModelTraining` |
| Health data never enters analytics | hard blocker on `ANALYTICS_PIPELINE` | `healthDataCannotEnterTheAnalyticsPipeline` |
| Retention needs an explicit basis | `LegalBasis.ABSENT` is a blocker | `retainedRecordRequiresAnExplicitLegalBasis` |
| Indefinite retention needs a legal obligation plus an exemption reference | | `indefiniteRetentionRequiresALegalObligation` |
| Withdrawal stops processing | consent-gated purposes require `GRANTED` | `withdrawnConsentStopsConsentGatedProcessing` |
| A broken consent ledger stops processing | unresolved history is treated as no consent | `unresolvedConsentHistoryStopsConsentGatedProcessing` |
| A UI-only category has no record of truth | inventory blocker | `aCategoryHeldOnlyAsAUiProjectionHasNoRecordOfTruth` |
| Export must cover the account | every non-excluded record | `exportMustCoverEveryInventoriedRecord` |
| Health data cannot be excluded from export | inventory blocker | `healthDataCannotBeExcludedFromTheExport` |
| Provider ledger rows are redacted in export | raw payloads can carry provider credentials | `providerLedgerRecordsMustBeRedactedInExport` |

Health-derived categories are `HEALTH_MEASUREMENT`, `SUPPLEMENT_INTAKE`, `LABEL_SCAN_IMAGE`,
`OCR_TEXT`, and `PROTOCOL_SCHEDULE`. A protocol schedule is health data: it reveals a supplement and
training routine even without a measurement attached.

## Deletion outcomes

```text
REJECTED                        no account-holder confirmation
INCOMPLETE                      a store, method, receipt, or date failed
COMPLETE_WITH_RETAINED_RECORDS  every erasable record erased; retained rows listed with their
                                legal obligation and exemption reference
COMPLETE                        every inventoried record erased
```

A retained record must carry `LegalBasis.LEGAL_OBLIGATION` and an explicit
`retentionExemptionRef`. "We keep it because it is useful" is not representable.

## Consent history

Consent events are append-only, sequenced contiguously from 1, and non-decreasing in time.
Withdrawal is a new event; the grant that preceded it is never edited away, so the history can be
replayed for any past date. A gap in the sequence is a lost record and blocks consent-gated
processing rather than defaulting to the last known grant.

## Evidence boundary

```text
Deterministic contracts and their tests     THIS COMMIT (draft)
Runtime data-flow audit against the code    NOT_EXERCISED
Real inventory of a running system          ABSENT
Privacy / legal / storage review            HUMAN_ADMIT_REQUIRED
Cross-border transfer assessment            ABSENT
Deletion executed against a real store      ABSENT
Consent collected from a real person        ABSENT
```

The inventory fixtures in the tests are synthetic. They describe the flows this product intends to
have; they are not a survey of a deployed system, and calling them one would be the exact defect
Issue #39 names ("privacy inventory that matches runtime data flow").

## Rollback subject

`PrivacyLifecycle.kt` and `PrivacyLifecycleTest.kt` at this commit. Reverting them removes the
contracts; it does not delete anything, because no store exists yet.
