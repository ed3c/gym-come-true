# Contributing

## Change shape

Keep each pull request to one state transition. Prefer a narrow vertical slice over a wide collection of unverified stubs.

A pull request must include:

- the user outcome;
- the previous and proposed state;
- allowed paths;
- safety, privacy, and rights invariants;
- validation commands and observed results;
- rollback;
- explicit non-goals.

## Branch and commit conventions

- Branch: `agent/<short-description>` or another intentionally named feature branch.
- Commits: small, descriptive, and reversible.
- Pull requests start as drafts.
- Do not bypass hooks or weaken a check to obtain green CI.

## Required checks

```bash
python3 scripts/validate_repository.py
./gradlew :shared:jvmTest
./gradlew :androidApp:assembleDebug :androidApp:lintDebug
./gradlew :webApp:composeCompatibilityBrowserDistribution
```

iOS changes also require:

```bash
cd iosApp
xcodegen generate --spec project.yml
xcodebuild \
  -project GymComeTrue.xcodeproj \
  -scheme GymComeTrue \
  -sdk iphonesimulator \
  -configuration Debug \
  CODE_SIGNING_ALLOWED=NO \
  build
```

## Data and media contributions

Do not commit third-party exercise media directly. Start with a registry proposal in `legal/` and attach:

- source and rights holder;
- exact license or signed contract;
- commercial products and territories covered;
- derivative/modification rights;
- attribution requirements;
- redistribution restrictions;
- evidence URL or retained contract reference;
- local file hash.

A reviewer must change the decision from `REVIEW` to `ALLOW` before product code references the asset.

## Health rule contributions

A health rule pack is not a normal code contribution. It requires:

- jurisdiction and population scope;
- versioned primary evidence;
- qualified reviewer identity and review date;
- conflict-of-interest disclosure;
- conservative missing-data behavior;
- unit tests and counterexamples;
- expiry/re-review date;
- rollback plan.

Until then, the rule pack remains `MISSING` or `DRAFT`.

## Security

Never include real user label images, health exports, API keys, tokens, signing assets, or vendor contracts in issues or pull requests. See `SECURITY.md`.
