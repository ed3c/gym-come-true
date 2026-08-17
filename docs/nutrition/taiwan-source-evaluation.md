# Taiwan food/nutrition source evaluation (Issue #46)

> This document is an engineering control system, not legal advice, following the same disclaimer
> as `docs/copyright-and-data-governance.md`. Counsel or another qualified reviewer must decide any
> disputed or high-value rights question. Nothing here captures, hashes, or vendors real data —
> every source stays `CANDIDATE` until a separate operator-driven packet captures exact bytes.

## Goal

Evaluate real, official-or-permissively-licensed Taiwan food-composition sources well enough to
scope a future capture packet, **without** scraping, downloading, or hard-coding any real nutrient
value into this repository. This lane ships only a synthetic sample catalog
(`data/nutrition-catalog/food-catalog.example.json`); every real-source record is illustrative shape
only (`data/nutrition-catalog/source-candidates.example.json`), matching how
`legal/taiwan-official-resource-candidates.json` records supplement-side candidates.

## Method

Reusing the core rule from `docs/copyright-and-data-governance.md`:

```text
unknown rights -> DENY
marketing claim only -> REVIEW
repository license without asset authorship -> REVIEW
executed scope + exact asset + immutable hash -> eligible for ALLOW
```

A source is a `CANDIDATE` only. It becomes eligible for real capture only after an operator pins an
exact dataset/page version, downloads it through the existing local-only
`scripts/capture_taiwan_source.py` (no HTTP client, `HASH_VERIFIED + DENY` by default — out of this
lane's lease to touch, but already built by the Taiwan supplement lane and directly reusable), and a
qualified reviewer completes legal review.

## Candidate sources

| Candidate | Publisher | Observed engineering use | Explicit boundary |
|---|---|---|---|
| 台灣食品成分資料庫 (Taiwan Food Composition Database) | Taiwan Food and Drug Administration (TFDA) | Canonical per-100g macro/micronutrient reference-value candidate for common Taiwan foods — the natural backing for `FoodNutrientProfile` | Exact dataset page, version, and distribution format are `ABSENT`/`HUMAN_ADMIT_REQUIRED`; this repository has not located or pinned a specific `data.gov.tw` dataset ID for it (unlike the supplement lane's datasets 9047/9640/8938, which were already pinned by that lane). Being an official government database does not establish personalized dietary safety or a therapeutic target. |
| 政府資料開放平臺 (data.gov.tw) | National Development Council | General discovery portal for locating and version-pinning the exact food-composition dataset entry above | A portal listing is not a captured source; `OGL-TW-1.0` is the licensing pattern already recorded by the supplement lane for this portal family, but the exact license text for a specific food-composition dataset entry must be independently confirmed once that entry is located. |
| 國人膳食營養素參考攝取量 (Dietary Reference Intakes, DRIs) | Ministry of Health and Welfare / Health Promotion Administration | Reference-range text candidate for showing an official target range next to a compiled plan's totals, comparison only | Population reference values, not a personalized prescription. `REVIEWED_HEALTH_RULES_ONLY` still applies: no daily total computed by this slice may be presented as medically appropriate, and no rule may be derived from DRI text without qualified review, exactly like the existing Taiwan rule-pack contract. Website/attachment reuse terms are pending review (`MOHW-SITE-TERMS-PENDING`, reused from the existing licenses table). |

Machine-readable shape (illustrative, non-executable):
[`data/nutrition-catalog/source-candidates.example.json`](../../data/nutrition-catalog/source-candidates.example.json).

### Explicitly out of scope for this jurisdiction-`TW` lane

- **USDA FoodData Central** and other non-Taiwan public-domain nutrient databases were considered as
  a possible generic-macro fallback. They are **not** modeled as candidate records here: the reused
  `ImmutableSourceArtifactValidator` hard-requires `jurisdiction == "TW"`
  (`shared/src/commonMain/.../domain/TaiwanSourceLifecycle.kt`), and mixing a US-jurisdiction source
  into a Taiwan-labeled catalog would misrepresent provenance. A non-TW nutrient-reference lane, if
  ever built, needs its own jurisdiction handling — recorded as a follow-up, not silently forced in
  here.
- **Food photography/packaging imagery** for any of the above sources is a separate rights domain
  (see `docs/copyright-and-data-governance.md`'s rights-domain table) and is not evaluated by this
  document. `FoodCatalogEntry` has no media field.

## What remains before any real catalog entry can exist

```text
CANDIDATE
  -> operator pins exact dataset/page version and downloads through the
     existing local-only capture command (no network in CI/app, HASH_VERIFIED + DENY default)
  -> legal/license review of the exact pinned resource -> LEGAL_REVIEWED
  -> exact SourceFieldMapping per target field (e.g. energyKcalPer100g),
     VERIFIED with an evidence-excerpt hash, reusing SourceFieldMappingValidator
  -> qualified-reviewer attestation for any REFERENCE_VALUE claim scope
     (already required by SourceFieldMappingValidator's qualifiedReviewScopes)
  -> FoodCatalogAdmissionValidator.validate(..., production = true) == ADMITTED
```

Every step above is `ABSENT` or `HUMAN_ADMIT_REQUIRED` for every real source named in this document.
This lane delivers the deterministic validator and the synthetic proof of the machinery — not a
completed admission.

## Local/offline subset for KMP clients

`FoodIdentity`, `FoodNutrientProfile`, `FoodServingDefinition`, and `FoodCatalogEntry`
(`shared/src/commonMain/.../nutrition/FoodCatalog.kt`) are plain `@Serializable` Kotlin
Multiplatform data classes with no platform, network, or provider import. A future admitted catalog
can ship as a bundled JSON/serialized resource per platform (Android/iOS/Web) and be deserialized
fully offline; nothing in this slice requires a live network call at runtime.
