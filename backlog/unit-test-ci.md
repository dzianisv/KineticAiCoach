# Unit Test CI Pipeline

**Stable slug:** `unit-test-ci`
**Rationale:** Offline-capable unit tests exist (`FitRepositoryTest`, `ExampleUnitTest`, `ExampleRobolectricTest`, `GreetingScreenshotTest`) but ran only locally at the time this gap was first identified — no CI enforced them, so work landing on `main` could silently break streak/badge/XP logic.

**Status:** Done — already resolved on `main` by `.github/workflows/android-ci.yml` (landed in PR #24, `fix(release): inject FIREBASE_PROXY_URL in CI, fail the build on placeholders`). Correcting an earlier version of this note that called it "Blocked": that assessment was from before PR #24 landed; it's now stale.

`android-ci.yml`'s `assembleRelease + unit tests` job already runs `./gradlew --no-daemon :app:assembleRelease :app:testDebugUnitTest` on every PR and push to `main`, gated behind provisioning `google-services.json` and `.env`. The `sdk=35` Robolectric pin in `app/src/test/resources/robolectric.properties` (from the earlier investigation into the `compileSdk 36.1` / `DefaultSdkProvider` incompatibility) is what makes that job's Robolectric-backed tests (`FitRepositoryTest`, `ExampleRobolectricTest`, `GreetingScreenshotTest`) pass in CI.

**This run's actual contribution:** closing the one real remaining test gap — `TrialManager` (the 3-day free-trial gate, monetization-critical) had 0% test coverage. Refactored for testability with **zero production behavior change**:
- `ServerTrialStore` / `DefaultServerTrialStore` (`ServerTrialStore.kt`) — extracts the Firestore reconcile transaction so it's fakeable
- `TrialLocalStore` / `DataStoreTrialLocalStore` (`TrialLocalStore.kt`) — extracts the on-device DataStore read/write so it's fakeable
- `clockMillis` injectable (same pattern as the existing `FitRepository`)
- `reconcile()` / `initJob` now return `Job` so tests can `.join()` deterministically instead of racing a dispatcher
- 17 new tests in `TrialManagerTest.kt` cover trial-active/expired boundary math, local-only vs. server-authoritative reconcile branching, and persistence across reconcile calls
- `MainViewModel` call site updated (`TrialManager(viewModelScope, DataStoreTrialLocalStore(application))`) — behavior-preserving

These new tests run automatically under the existing `android-ci.yml` job — no new workflow needed (an earlier draft of this change added a duplicate `android-unit-tests.yml`; removed once `android-ci.yml` was found already covering the same ground).

**Acceptance criteria:**
- [x] Robolectric runs against `compileSdk 36.1` via the `sdk=35` pin in `robolectric.properties`
- [x] `./gradlew :app:testDebugUnitTest` is green locally (47/47: `FitRepositoryTest` 27, `TrialManagerTest` 17 (new), `ExampleUnitTest`/`ExampleRobolectricTest`/`GreetingScreenshotTest` 1 each)
- [x] Workflow triggers on push to `main`, PR to `main` — via existing `android-ci.yml` (`pull_request:` + `push: branches: [main]`)
- [x] JDK 17 (temurin) via `setup-java`; Gradle caching via `gradle/actions/setup-gradle` — via existing `android-ci.yml`
- [x] `google-services.json` decoded from `GOOGLE_SERVICES_JSON_BASE64` before the Gradle run — via existing `android-ci.yml`
- [x] Test report uploaded as artifact on failure — via existing `android-ci.yml`
- [ ] CI run on this PR confirmed green (pending — see PR for the Actions run link)

**Evidence:**
- Local verification: `ANDROID_HOME=<sdk> GRADLE_USER_HOME=<home> ./gradlew --no-daemon :app:testDebugUnitTest` → **BUILD SUCCESSFUL**, 47 tests / 0 failures / 0 errors across all 5 suites (2026-07-27).
- New coverage: `app/src/test/java/com/example/billing/TrialManagerTest.kt` (17 tests)

**Prior art:** PR #13 (`chore/android-ci`) attempted a unit-test workflow and was closed without merging. PR #24 landed `android-ci.yml`, which independently solved the CI-enforcement half of this gap.

**Not in scope this run:** merge (guardrail — needs human approval).
