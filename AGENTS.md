# Agent notes

## Build & test
- Build: `./gradlew assembleDebug`; unit tests: `./gradlew testDebugUnitTest` (JDK 17, Android SDK 34).
- CI: `.github/workflows/test.yml` runs tests on every PR; `.github/workflows/build.yml` runs tests + builds the debug APK (uploaded as artifact) when a PR is merged.

## Writing tests
- Unit tests are plain JUnit 4 (`junit:junit:4.13.2`) in `app/src/test/kotlin/org/palmstead/launcher/`, e.g. `LogicTest.kt`.
- Keep testable logic pure (no Android classes), like `Geometry` and `LauncherLogic`, and test it there.
- Every change to logic must come with tests; use `assertEquals(expected, actual, delta)` for floats.
