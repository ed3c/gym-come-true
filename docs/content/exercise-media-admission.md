# Media intake, admission, derivatives, and takedown

Issue #34. Intended transition: `RIGHTS_CLEAN_TOP50 -> LICENSED_MEDIA_PIPELINE`.

Delivered state: **contract core only**. The deterministic pipeline exists and is tested. The
transition is **not** reached, because reaching it requires executed commercial rights or commissioned
first-party artwork, and this repository holds neither.

```text
executed commercial rights            ABSENT
commissioned first-party artwork      ABSENT
legal / procurement review            HUMAN_ADMIT_REQUIRED
reviewer attestation hashes           ABSENT
admitted media assets                 0
```

## The ladder

```text
INTAKE ──> QUARANTINED ──> HASH_VERIFIED ──> RIGHTS_REVIEWED ──> ADMITTED
                                                                    │
                                                      SUSPENDED ◄───┤
                                                        REVOKED ◄───┘
```

`MediaAdmissionState.rank` is **declared**, not taken from `Enum.ordinal`, so reordering the members
cannot silently change which evidence a state requires. `SUSPENDED` keeps admitted-grade evidence
because it was admitted; `REVOKED` carries rank `0` and short-circuits validation entirely, because a
withdrawn asset has no standing to evaluate.

Each rung adds evidence and never inherits it from the rung below:

| State | Adds |
|---|---|
| `QUARANTINED` | nothing — this is where unknown origin stops and stays |
| `HASH_VERIFIED` | lowercase 64-hex SHA-256, positive byte length, content-addressed `repo://` or `evidence://` storage URI that contains its own hash |
| `RIGHTS_REVIEWED` | `EXECUTED_ASSET_SCOPE` or `FIRST_PARTY_OWNERSHIP`, a `private://` or `repo://` licence evidence reference, non-empty platform and territory scope, a valid term window, and a reviewer attestation hash |
| `ADMITTED` | a **bounded** term that covers the as-of date, and bilingual alternative text |

`HASH_VERIFIED != RIGHTS_REVIEWED`. The validator emits an explicit review note saying so, because
"we have the exact bytes" is the single most common thing mistaken for "we may use the bytes".

## Hard rejections

| Input | Result |
|---|---|
| `productionAdmitted: true` in an input manifest | rejected — admission is decided, never declared |
| `remoteUrl` non-null on anything past `INTAKE` | rejected — served media is never hotlinked |
| `storageUri` with any remote scheme | rejected — storage is content-addressed and local |
| `storageUri` that does not contain its own hash | rejected — a content address that is not the content |
| `licenseGrant = REPOSITORY_ROOT_LICENSE` | rejected — the root `LICENSE` never authorizes an asset |
| licence evidence pasted as bytes | impossible — the field holds a `private://` reference only |
| `attributionRequired` with no attribution text | rejected |
| `ADMITTED` with a missing locale in `altText` | rejected |
| `ADMITTED` outside its term window | rejected |

The rules are structural rather than a blocklist of vendor hostnames. A host list only rejects the
vendors someone remembered; "no remote scheme past intake" rejects all of them, including the one
that has not been invented yet. It also keeps vendor hostnames out of the Kotlin sources, where
`scripts/validate_repository.py` would flag them as hotlinks.

## Derivatives

A derivative declares `derivedFromSha256` and a `DerivativeTransform` (`RESIZE`, `CROP`, `TRANSCODE`,
`FRAME_EXTRACT`, `LOSSLESS_RECOMPRESS`). Both must be present or neither. The validator then requires:

- the parent exists in the supplied index and is itself `ADMITTED`;
- the parent's licence allows derivatives;
- the derivative's platform and territory scope is a **subset** of the parent's;
- the derivative does not claim redistribution the parent does not grant;
- the derivative inherits the parent's revocation key.

The last rule is the load-bearing one: a derivative with its own key is a derivative that a takedown
targeting the original would miss.

## Takedown and kill switch

`MediaTakedown.apply(records, revocationKey)` revokes every record carrying the key, then walks the
derivative graph transitively — a derivative of a withdrawn asset is still that asset. The transitive
walk is deliberately independent of the revocation-key rule above, so takedown still reaches
everything even when the ledger is malformed. `MediaAdmissionTest` proves this with a grandchild that
carries a stale key and is revoked anyway, through lineage.

An unmatched key returns `keyNotFound = true` rather than an empty success. "Nothing matched" and
"nothing needed to change" are different outcomes and must not look alike in a takedown log.

`MediaAdmissionLedger.admittedMediaIds(records, asOfIsoDate)` is the only surface that answers "may
this exercise show media right now?". It re-runs full validation rather than trusting the stored
state, so a record that was admitted and has since drifted out of its term stops being served without
anyone editing it.

## The fixture

`data/exercise-catalog/media-intake.synthetic.json` holds three rows and no licensed bytes:

1. the first-party schematic already in this repository, at `HASH_VERIFIED` — its
   `originSha256` and `byteLength` are the real digest and size of
   `assets/first-party/muscle-map-schematic.svg`, recomputed and compared by `validate_catalog.py` on
   every run, so the row goes red the moment the asset changes;
2. an `INTAKE` placeholder with no bytes attached;
3. a `QUARANTINED` placeholder for unknown origin — unknown rights stay quarantined and never age
   into admission.

`validate_catalog.py` enforces a ceiling on this file: no row may exceed `HASH_VERIFIED`, no row may
set `productionAdmitted`, no row may carry a `remoteUrl`, and no row may carry a reviewer attestation
hash. Writing one by hand would be exactly the fabricated evidence state the pipeline exists to
prevent, so the gate treats it as a defect and the self-test plants each of those four defects to
prove the gate is actually looking.

## Relationship to `legal/media-registry.json`

That file remains the repository's admission SSOT and is untouched by this lane — its `assets` array
is still empty and its `defaultPolicy` is still `DENY`. `MediaRightsScope` is the executable shape of
the same facts its `requiredAllowFields` list already names (`sourceId`, `licenseEvidenceRef`,
`allowedPlatforms`, `sha256`, `reviewedAt`, `reviewedBy`). A parallel JSON Schema describing the same
rights record a third time was deliberately not added: an unvalidated schema file is a drift source,
not a control.

## What a real admission would require

1. An executed scope naming platform, territory, term, derivative rights, and redistribution rights.
2. The exact asset bytes captured locally and hashed — never fetched at build or app start.
3. A qualified reviewer producing an attestation hash over that scope.
4. Bilingual alternative text authored for the asset.
5. A revocation key wired to the kill switch, and a drill proving the switch removes the asset.

Steps 1, 3, and 5 are human-owned. None of them can be produced by this lane, and none of them have
been simulated here.
