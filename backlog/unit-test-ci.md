# Unit Test CI Pipeline

**Stable slug:** `unit-test-ci`
**Rationale:** 27 offline-capable unit tests exist (`FitRepositoryTest`, `ExampleUnitTest`, `ExampleRobolectricTest`, `GreetingScreenshotTest`) but run only locally. No CI enforces them — work landing on `main` can silently break streak/badge/XP logic. A GitHub Actions workflow provides regression protection on every PR and push, unblocking safe iteration on monetization, auth, and UX.

**Status:** Done — `.github/workflows/tests.yml` created and verified.
**Acceptance criteria:**
- [x] Workflow triggers on push to `main`, PR to `main`, and workflow_dispatch
- [x] JDK 17 (temurin) configured via `setup-java`
- [x] Gradle caching via `gradle/actions/setup-gradle`
- [x] Debug keystore generated in CI to satisfy `debugConfig` signing config
- [x] Android SDK platform 36 installed if missing
- [x] `./gradlew :app:testDebugUnitTest` runs all 30 debug tests
- [x] Test results uploaded as artifact (`app/build/reports/tests/`)

**Evidence:**
- Workflow: `.github/workflows/tests.yml`
- Local verification: `BUILD SUCCESSFUL` — 30 tests across 4 suites, 0 failures, 0 errors
- No app code changed; workflow only touches `.github/workflows/`
