# Taiwan corpus, OCR evaluation, and reviewed rule-pack admission

Owner of Issues **#24 (TW1)**, **#25 (TW2)**, and **#26 (TW3)**.

This directory documents three deterministic contracts. It documents **no measurement, no review,
and no admission**, because none has occurred. Read the "What is ABSENT" table before reading
anything else.

## State transitions actually reached

| Issue | Target transition | Reached | Why |
| --- | --- | --- | --- |
| #24 TW1 | `CORPUS_UNKNOWN -> CONSENT_CONTRACT_DRAFT` | `CONSENT_CONTRACT_DRAFT` | Schemas, consent resolution, deletion completeness, validator and tests exist locally. |
| #25 TW2 | `CONSENT_CONTRACT_DRAFT -> OCR_EVALUATION_DRAFT` | `OCR_EVALUATION_DRAFT` | The evaluation and privacy contract exists; no engine has run. |
| #26 TW3 | `OCR_EVALUATED -> REVIEWED_TAIWAN_RULE_PACK` | **NOT REACHED** | Admission-checking logic only. Every external input is `ABSENT`. |

`OCR_EVALUATED` was never entered, so #26's own precondition is unmet. The gate delivered for #26
is the logic that would decide admission if the evidence existed.

## What is ABSENT

Every row below is `ABSENT` or `HUMAN_ADMIT_REQUIRED`. None of it is simulated anywhere in this
repository, and none of it may be.

| Gate | State | Owner |
| --- | --- | --- |
| Real consenting human subjects | `ABSENT` | Human Admit |
| Privacy / legal review of the consent flow | `ABSENT` | Human Admit |
| Consent-management system issuing real receipts | `ABSENT` | Human Admit |
| Production storage, deletion executor, erasure verifier | `ABSENT` | Human Admit |
| Consented Traditional Chinese label corpus | `ABSENT` | Human Admit |
| Authorized Android device ML Kit execution | `ABSENT` | Human Admit |
| Authorized iOS device Apple Vision execution | `ABSENT` | Human Admit |
| Approved immutable MOHW/TFDA source bytes and hashes | `ABSENT` | Human Admit |
| Legal reuse/redistribution review of official sources | `ABSENT` | Human Admit |
| Qualified clinical reviewer and COI declaration | `ABSENT` | Human Admit |
| Reviewed user-facing wording | `ABSENT` | Human Admit |
| Cryptographic signatures and signing material | `ABSENT` | Human Admit |
| Activation, revocation, and rollback receipts | `ABSENT` | Human Admit |

The machine-readable form is [`data/taiwan-supplement/rule-pack-external-gates.absent.json`](../../data/taiwan-supplement/rule-pack-external-gates.absent.json),
which `scripts/validate_taiwan_corpus_contract.py` checks against the `ExternalGate` enum so a gate
cannot be quietly dropped from either side.

## #24 — Consent corpus contract

`shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/domain/TaiwanConsentCorpus.kt`

### Consent denies by default

`ConsentResolver.resolve(grant, asOfIsoDate)` returns one of five states. Only `ACTIVE` permits use,
and it is the only state that requires positive evidence to reach:

| State | Produced when | Effect |
| --- | --- | --- |
| `UNKNOWN` | no grant is bound to the record | DENY |
| `UNVERIFIABLE` | no receipt hash, no hash-bound wording, no scope, no bounded window, an inconsistent ledger, or an unparsable evaluation date | DENY |
| `WITHDRAWN` | the subject withdrew on or before the evaluation date | DENY |
| `EXPIRED` | the consent window closed before the evaluation date | DENY |
| `ACTIVE` | verifiable, unwithdrawn, inside a bounded window | permits the scopes it names |

Three consequences worth stating explicitly:

- **An unbounded consent is not active.** A grant with no expiry resolves `UNVERIFIABLE`, because
  "consent forever" cannot be verified as current.
- **An unreadable evaluation date cannot yield `ACTIVE`.** If the date is unparsable, neither expiry
  nor withdrawal can be evaluated, so use stops instead of defaulting to permitted.
- **Consent for one purpose is not consent for another.** `ConsentScope` is checked per use.

### Deletion cannot be manifest-only

`CorpusDeletionValidator` classifies a request as `NOT_STARTED`, `MANIFEST_ONLY`, `PARTIAL`, or
`VERIFIED_COMPLETE`. Reaching `VERIFIED_COMPLETE` requires **every declared storage location** to
carry its own receipt with `verifiedAbsent`, verification-evidence SHA-256, operator signature
SHA-256, and a date inside the request window.

- Setting a manifest flag with zero receipts is `MANIFEST_ONLY` and is reported as a blocker.
- "The delete job ran" is not "the data is gone": a receipt without `verifiedAbsent` does not count.
- A record used for OCR evaluation must declare `DERIVED_OCR_METRICS`. Deleting the label while
  keeping the rows derived from it is not deletion.

The repository fixture is the **failing** shape,
[`deletion-request.manifest-only-counterexample.json`](../../data/taiwan-supplement/deletion-request.manifest-only-counterexample.json).
A fixture with populated erasure receipts would be fabricated evidence of deletions that never ran.

### Synthetic fixtures

`consent-grant.synthetic.json` references two repository-authored text files and carries their
**real** SHA-256 digests and byte lengths. The validator recomputes both. No hexadecimal string in
this contract was invented, and no human consented to anything.

## #25 — OCR field-level evaluation contract

`shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/domain/TaiwanOcrEvaluation.kt`

### Binding

A run must name its `OcrEngine`, engine version, recognition model version, device model, OS
version, and must declare a Traditional Chinese recognition language. A blank version is rejected
rather than defaulted to "unknown" — an accuracy number that cannot say what produced it is not a
measurement and cannot be compared to another run.

### First pass and correction stay separate

`OcrEvaluationCompiler` reports `firstPassExactAccuracy` and `correctionCompletion` as two numbers.
User correction never repairs the first-pass number. A run with nothing to correct reports
`correctionCompletion = null`, never `1.0`; "no corrections were needed" and "all corrections
succeeded" are different facts. Run-level rates are recomputed from raw observations rather than
averaged over per-field rates, so a field with three samples cannot outweigh one with three hundred.

### The aggregate carries no label content

`OcrEvaluationReport` has no field for expected text, observed text, corrected text, corpus record
identifiers, image references, or file paths — the type makes them unrepresentable, and
`recordCount` is a count rather than a list. `OcrAggregateLeakScanner` then checks the remaining
free-text strings for Han characters, path separators, URIs, and suspiciously long values. A run
that leaks is `REJECTED` and publishes no report at all.

Consent is enforced at this boundary too: any record whose `ConsentState` is not `ACTIVE` blocks the
run.

### Nothing has been measured

[`ocr-evaluation-report.absent.json`](../../data/taiwan-supplement/ocr-evaluation-report.absent.json)
records zero records and zero observations, with `measurement: ABSENT` and every version literally
`ABSENT`. The counts are zero because zero labels were recognized — not because recognition was
perfect. Any accuracy figure for ML Kit or Apple Vision in this repository would be fabricated.

## #26 — Reviewed rule-pack admission gate

`shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/domain/TaiwanReviewedRulePackGate.kt`

`TaiwanReviewedRulePackGate.evaluate` composes the existing `TaiwanRulePackAdmissionValidator` with
a conflict-of-interest check and an external-gate ledger, and returns one of:

- `REJECTED` — the pack's own deterministic evidence is defective. Checked **first**, so "the pack is
  broken" is never reported as "we are waiting on a reviewer".
- `EXTERNAL_GATES_ABSENT` — the pack is internally consistent; one or more gates are not
  human-admitted. **This is the only answer repository evidence can produce.**
- `ADMITTED` — every gate is `HUMAN_ADMITTED`.

Additions beyond the pre-existing pack validator:

- A **bounded effective window** is mandatory; `effectiveUntil = null` is rejected.
- A **conflict-of-interest declaration** is mandatory, must be signed, must name the same reviewer as
  the attestation, and must record a mitigation whenever an interest is declared. A declared
  interest does not disqualify a reviewer; an undeclared or unmitigated one does.
- An unlisted gate is `ABSENT`. Silence is never admission.

`ExternalGateState.HUMAN_ADMITTED` is never assigned by repository code, and the validator asserts
that no assignment exists. It is present so the decision is a real function of the ledger rather
than a hard-coded rejection — the difference between "we check and it fails" and "we never check".
`TaiwanReviewedRulePackGateTest` exercises both directions.

## Verification

```bash
python3 scripts/validate_taiwan_corpus_contract.py
```

Kotlin tests (run by the integrator, not by this lane — no JVM build was executed here):

```text
shared/src/commonTest/kotlin/dev/ed3c/gymcometrue/domain/TaiwanConsentCorpusTest.kt
shared/src/commonTest/kotlin/dev/ed3c/gymcometrue/domain/TaiwanOcrEvaluationTest.kt
shared/src/commonTest/kotlin/dev/ed3c/gymcometrue/domain/TaiwanReviewedRulePackGateTest.kt
```

### Evidence strength of this lane

| Arrival | Status |
| --- | --- |
| `STATIC` — schemas, types, documentation | present |
| `SANDBOX` — `validate_taiwan_corpus_contract.py`, exit 0, 12 planted defects each detected | present |
| `SANDBOX` — Kotlin `commonTest` | **written, NOT EXECUTED** in this lane |
| `PROD` — real devices, real corpus, real reviewers | `ABSENT` |

The Kotlin tests were authored without running Gradle, so their green is `NOT_EXERCISED` until the
serial integrator runs `sh ./gradlew :shared:jvmTest`.
