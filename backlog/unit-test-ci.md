# Unit Test CI Pipeline

**Stable slug:** `unit-test-ci`
**Rationale:** Offline-capable unit tests exist (`FitRepositoryTest`, `ExampleUnitTest`, `ExampleRobolectricTest`, `GreetingScreenshotTest`) but run only locally. No CI enforces them — work landing on `main` can silently break streak/badge/XP logic.

**Status:** Blocked — workflow written and then pulled back out of PR #23. Two defects found by actually running it in CI:

1. **Fixed.** `app/google-services.json` is gitignored, so the Google Services plugin never generates `R.string.default_web_client_id` and `LoginScreen.kt:168` fails to compile. `missingGoogleServicesStrategy = WARN` in `app/build.gradle.kts` suppresses the plugin error but does not produce the resource. Fix is to decode the existing `GOOGLE_SERVICES_JSON_BASE64` repo secret before the Gradle run, exactly as `release-apk.yml` already does. (Note: the closed PR #13 assumed "zero secrets required" — that assumption is wrong.)

2. **Open — this is the real blocker.** With the compile fixed, `:app:testDebugUnitTest` reaches the tests and 3 of 4 fail identically:

   ```
   ExampleRobolectricTest  > classMethod FAILED  java.lang.UnsupportedOperationException at DefaultSdkProvider.java:170
   GreetingScreenshotTest  > classMethod FAILED  java.lang.UnsupportedOperationException at DefaultSdkProvider.java:170
   FitRepositoryTest       > classMethod FAILED  java.lang.UnsupportedOperationException at DefaultSdkProvider.java:170
   4 tests completed, 3 failed
   ```

   Robolectric 4.16.1 (`gradle/libs.versions.toml:38`) cannot instantiate a sandbox for this project's SDK level: `compileSdk = release(36) { minorApiLevel = 1 }` / `targetSdk = 36` (`app/build.gradle.kts:42,47`). Every Robolectric-backed test is affected; only the plain JVM `ExampleUnitTest` passes.

   Evidence: https://github.com/dzianisv/KineticAiCoach/actions/runs/30230229541

   This is **pre-existing breakage on `main`**, not a regression — the same four test files are on `main` unchanged, they have simply never been executed by CI. Adding the workflow does not create the problem, it just surfaces it, which is why the workflow cannot land until the tests are green.

**Acceptance criteria:**
- [ ] Robolectric runs against `compileSdk 36.1` — either bump Robolectric past 4.16.1 once it supports the level, or pin `app/src/test/resources/robolectric.properties` to `sdk=35` (and re-record `app/src/test/screenshots/greeting.png` if the Roborazzi golden shifts)
- [ ] `./gradlew :app:testDebugUnitTest` is green locally before re-adding the workflow
- [ ] Workflow triggers on push to `main`, PR to `main`, and workflow_dispatch
- [ ] JDK 17 (temurin) via `setup-java`; Gradle caching via `gradle/actions/setup-gradle`
- [ ] Debug keystore generated in CI to satisfy the `debug` signing config
- [ ] `google-services.json` decoded from `GOOGLE_SERVICES_JSON_BASE64` before the Gradle run
- [ ] Test results uploaded as artifact (`app/build/reports/tests/`)

**Prior art:** PR #13 (`chore/android-ci`) attempted the same thing and was closed without merging.
