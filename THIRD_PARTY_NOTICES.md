# Third-party notices

This file is an engineering inventory, not legal advice. Release owners must regenerate the dependency report, preserve notices required by shipped artifacts, and review current service terms before each store submission.

## Build and runtime dependencies

| Component | Pinned line | Role | License / terms to preserve |
|---|---:|---|---|
| Kotlin and Kotlin Multiplatform Gradle plugin | 2.4.10 | Shared language and targets | Apache License 2.0; verify bundled compiler/runtime notices |
| Compose Multiplatform | 1.11.1 | Shared UI and browser targets | Apache License 2.0; verify transitive notices |
| Compose Material 3 artifact | 1.10.0-alpha05 | Shared UI components | Apache License 2.0; alpha compatibility must be reviewed before release |
| Android Gradle Plugin | 9.1.0 | Android/KMP Android build | Android SDK/Google terms plus component notices |
| AndroidX Activity and Core | 1.13.0 / 1.18.0 | Android host and notification helpers | Apache License 2.0 |
| kotlinx.coroutines | 1.11.0 | Asynchronous work | Apache License 2.0 |
| kotlinx.datetime | 0.8.0 | Shared time primitives | Apache License 2.0 |
| kotlinx.serialization | 1.11.0 | Structured evidence payloads | Apache License 2.0 |
| Google ML Kit Text Recognition | 16.0.1 | Android on-device OCR | Google ML Kit / Google APIs terms; not treated as repository-owned open source |
| Google ML Kit Barcode Scanning | 17.3.0 | Android on-device barcode candidate | Google ML Kit / Google APIs terms; not treated as repository-owned open source |
| Apple Vision, CryptoKit, UserNotifications, SwiftUI, UIKit | platform SDK | iOS evidence and host capabilities | Apple SDK and developer-program agreements |
| XcodeGen | 2.42.0+ tool requirement | Deterministic Xcode project generation | MIT license; build tool is not vendored in this repository |
| Gradle | 9.5.1 | Build execution | Apache License 2.0; launcher refuses unpinned versions |

## Exercise data and media

No third-party exercise image, GIF, video, SVG anatomy map, or 3D model is currently admitted. The three records under `data/seed/exercises.example.json` are original schema examples with `mediaRef: null`.

Candidate upstream sources are tracked in `legal/source-registry.json`. A source listed as `REVIEW` or `DENY` contributes no redistribution right and must not be packaged into a release.

## Future release evidence

Before release, generate and archive:

1. Gradle dependency graphs for every shipped target.
2. Android and iOS binary notices and store privacy manifests.
3. Executed commercial-media agreements, invoices, platform/territory/term scope, and asset SHA-256 values.
4. Exact upstream commit or package versions for any admitted metadata or visualization code.
5. A reviewer-signed delta from the previous release.
