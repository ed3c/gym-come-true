# `data/exercise-catalog`

Draft fixtures for the exercise-catalog lane (Issues #32, #33, #34, #48). Nothing here can admit
itself to production; the executable contract lives in
`shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/catalog/`, and the reasoning lives in
[`docs/content/`](../../docs/content/README.md).

| File | What it is |
|---|---|
| `taxonomy.v1.json` | Machine mirror of the Kotlin vocabulary, bilingual muscle labels, intensity semantics, and the muscle-map region binding. Generated from the Kotlin sources; do not hand-edit one side. |
| `catalog.v1.json` | 50 first-party bilingual exercise records with per-field provenance. Every field is `DRAFT`; no editorial or rights review has been executed. |
| `media-intake.synthetic.json` | Media pipeline fixture. Contains no licensed media and nothing above `HASH_VERIFIED`. |
| `validate_catalog.py` | The gate. Standard library only, no network, no build. |

## Run it

```bash
python3 data/exercise-catalog/validate_catalog.py
python3 data/exercise-catalog/validate_catalog.py --selftest
```

The plain run checks five things across artifacts: the JSON vocabulary matches the Kotlin enums token
for token, every mapped muscle region exists in `assets/first-party/muscle-map-schematic.svg`, the 50
records only use vocabulary that exists and claim no review that did not happen, the media fixture
stays inside its evidence ceiling (including a recomputed SHA-256 of the real asset bytes), and the
provenance record names no reviewer.

`--selftest` runs the validator's planted-defect matrix and fails unless every defect is rejected.
The matrix is intentionally maintained beside the validator rather than duplicated as a count in
documentation; a gate that has never been shown to go red is a gate whose green means nothing.

## CI ownership

Issue #54 wires both the baseline and `--selftest` commands into the `policy-and-provenance` GitHub
Actions job. This closes the previous convergence gap between the merged exercise lane and its owning
repository oracle. Hosted evidence still counts only when the exact-head job receives a runner and
executes the commands.

Editorial acceptance, rights review, real licensed media, and production promotion remain separate
`HUMAN_ADMIT_REQUIRED` gates. CI wiring does not upgrade any `DRAFT` content or media state.
