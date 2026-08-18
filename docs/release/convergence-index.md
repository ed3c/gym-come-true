# Release convergence index (Issue #44)

**Convergence date:** 2026-08-18 · **Product direction:** `docs/product/mvp-redesign.md`

This index binds the exact heads that converged into `main` and the verification evidence each
integration ran. It is convergence-only: it authorizes nothing by itself. Store submission,
signing, and legal review remain Human Admit (see `docs/product/store-listing.md` gate table).

## Bound heads

| Round | Integration commits on main | Lane heads bound |
|---|---|---|
| Stack + 7 domain lanes | `505b66f` (incl. merges `0335da8`…`5c56f01`) | `10b033e` taiwan-corpus · `c25d9ec` ios-native · `5adfbc1` android-health · `d2c26f4` exercise-catalog · `79dd6da` explanation-gateway · `3966494` entitlement-privacy · `fa51f78` nutrition |
| MVP repositioning | `68b281e`, `4246648`, `0d5490a` | `764fb67` mvp-ai-provider · `abc9273` mvp-product-surface |
| Round 3 | merges `d139b13`, `6d3ae15`, `2b2ee12`, `4d3225c` | `f426629` muscle-viz · `9eff74e` provider-deepening · `a00b0a9` surface-validators · `9e07a48` foundation-hardening |

## Verification executed per integration (all exit 0, real exit codes)

- Python suite: `validate_repository` (incl. notice byte binding to `legal/DISCLAIMER.md`),
  `validate_taiwan_rule_pack`, `validate_taiwan_source_lifecycle`,
  `validate_taiwan_source_hardening`, `validate_stacked_delivery`,
  `validate_taiwan_corpus_contract`, `validate_product_surface` (+ `--selftest`),
  `data/exercise-catalog/validate_catalog` (+ `--selftest`, 28 planted defects detected),
  `data/llm-gateway/validate_gateway_corpus`.
- Gradle (serial, Android Studio JBR): `:shared:jvmTest`, `:androidApp:assembleDebug`,
  `:androidApp:lintDebug`, `:webApp:composeCompatibilityBrowserDistribution`.
- iOS: `xcodegen generate` + `xcodebuild -sdk iphonesimulator` on the canonical
  `iosApp/project.yml`.

## Lanes that remain absent from any release claim

Hosted CI (`PRE_RUN_BLOCKED`, #45) · store submission/signing/SBOM (#40 schema only) ·
legal review of shipped wording · real provider credentials (OpenAI/Anthropic) ·
real-device health/reminder/OCR evidence. Absence of a lane here is a denial of that claim,
not a weaker approval.
