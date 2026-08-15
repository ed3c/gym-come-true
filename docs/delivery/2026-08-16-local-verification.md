# Local verification receipt — 2026-08-16

**Subject:** `main` after the stack integration and the Issue #23 delivery contract.
**Lane:** local developer machine. **This is not hosted evidence.**

## Why this receipt exists

Every GitHub Actions run on this repository has ended before runner allocation
(`PRE_RUN_BLOCKED_BY_ACTIONS_BUDGET`, Issue #45). At the time of this receipt the repository had
**zero successful hosted runs in its entire history**, so no lane had ever executed the build.

Nothing here ticks an exact-head acceptance box. Under the repository's own vocabulary a local run
is a `PASS` for the exact command against the stated commit and nothing more: it does not establish
the hosted result, the runner environment, or the artifact upload path.

## Environment

| Component | Value |
|---|---|
| Host | macOS 26.4 (`arm64`) |
| JDK | Temurin-equivalent OpenJDK 21.0.6 (Homebrew) |
| Gradle | 9.5.0 (the version `gradlew` pins) |
| Android SDK | platform + build-tools 36 |
| Xcode | 26.4 (build 17E192) |
| XcodeGen | 2.46.0 |

The hosted lane uses `ubuntu-24.04` and `macos-26`. Operating system, SDK image, and network egress
differ, so a local `PASS` does not predict the hosted result for anything OS-sensitive.

## Results

| Lane | Command | Result |
|---|---|---|
| policy | `python3 scripts/validate_repository.py` | `PASS` |
| policy | `python3 scripts/validate_taiwan_rule_pack.py` | `PASS` |
| policy | `python3 scripts/validate_taiwan_source_lifecycle.py` | `PASS` |
| policy | `python3 scripts/validate_taiwan_source_hardening.py` | `PASS` |
| policy | `python3 scripts/validate_stacked_delivery.py --self-test` | `PASS`, 16 mutation controls denied |
| git-town | `python3 scripts/git-town/verify_admission.py --candidate … --self-test` | `PASS`, 14 mutation controls denied |
| shared domain | `sh ./gradlew :shared:jvmTest` | `PASS`, 36 tests, 0 failures |
| android | `sh ./gradlew :androidApp:assembleDebug :androidApp:lintDebug` | see `## Android and Web` |
| web | `sh ./gradlew :webApp:composeCompatibilityBrowserDistribution` | see `## Android and Web` |
| ios | `:shared:linkDebugFrameworkIosSimulatorArm64` + `xcodegen` + `xcodebuild` | see `## iOS` |

The Git Town runtime canary remains `NOT_EXERCISED`. `verify_admission.py --self-test` verifies the
candidate metadata and the repository's own mutation controls; it downloads and executes nothing.

## Defects this lane found

The blocked hosted lane reported none of these, because it never ran a step.

1. **`scripts/validate_repository.py` failed on `main`.** The PR #20 README rewrite dropped four
   headings the governance gate requires (`## Safety contract`, `## Copyright and data admission`,
   `## Honest capability matrix`, `## Delivery state machine`). The `policy-and-provenance` job
   would have failed on every commit since `ad065c8`. Fixed by restoring the sections in `README.md`;
   the gate was not relaxed.
2. **`scripts/git-town/verify_admission.py` did not parse.** A malformed string literal at the
   canary-invariant list raised `SyntaxError`, so the first step of the `git-town-admission`
   workflow could never run. The three affected literals now use single quotes and assert the exact
   `run_disposable_canary.sh` tokens.
3. **`settings.gradle.kts` could not resolve the Kotlin JS/Wasm toolchain.** `PREFER_SETTINGS`
   ignores project-declared repositories, including the Ivy repositories the Kotlin plugin adds for
   `org.nodejs:node`, `com.yarnpkg:yarn`, and `com.github.webassembly:binaryen` — despite a comment
   claiming the mode allowed them. The web job would have failed on the hosted runner too. All
   three distributions are now declared in `dependencyResolutionManagement` with `content` filters,
   so resolution stays centrally declared instead of loosening the mode.
4. **`commonMain` used a JVM-only stdlib function.** `OcrMetricCompiler.summarize` called
   `Map.toSortedMap`, which does not exist on Kotlin/JS or Kotlin/Wasm, so the browser distribution
   could not compile. `:shared:jvmTest` passed throughout and could never have caught this — the JVM
   target is precisely the one platform where the call resolves. Replaced with
   `entries.sortedBy { it.key.ordinal }`, which is identical on every target. A repository-wide grep
   for `toSortedMap`, `sortedMapOf`, `TreeMap`, `java.`, and `kotlin.jvm` in shared and web sources
   found no second instance.

## What this does not establish

- No hosted run, no uploaded artifact, no exact-head evidence for any issue.
- No Git Town binary executed here.
- No Taiwan regulatory byte, clinical review, licensed media, store credential, provider credential,
  or device evidence was created. Those remain external gates.
