# Unit Test CI Pipeline

**Stable slug:** `unit-test-ci`
**Rationale:** Offline-capable unit tests exist (`FitRepositoryTest`, `ExampleUnitTest`, `ExampleRobolectricTest`, `GreetingScreenshotTest`) but run only locally. No CI enforces them — work landing on `main` can silently break streak/badge/XP logic.

**Status:** Done — verified green locally, pending CI run confirmation on the PR.

Prior cycle found the real blocker: Robolectric 4.16.1 cannot instantiate a sandbox for
`compileSdk 36.1` (`java.lang.UnsupportedOperationException at DefaultSdkProvider.java:170`).
`app/src/test/resources/robolectric.properties` already carries a `sdk=35` pin from that
investigation. This run verified the pin actually resolves it — `./gradlew :app:testDebugUnitTest`
is green locally (47/47: `FitRepositoryTest` 27, new `TrialManagerTest` 17, `ExampleUnitTest`,
`ExampleRobolectricTest`, `GreetingScreenshotTest` 1 each) — and re-added the workflow this time
gated on that confirmed-green state.

**Acceptance criteria:**
- [x] Robolectric runs against `compileSdk 36.1` via the `sdk=35` pin in `robolectric.properties`
- [x] `./gradlew :app:testDebugUnitTest` is green locally before re-adding the workflow (47/47, verified with `google-services.json` provisioned + a generated debug keystore, matching what CI will do)
- [x] Workflow (`.github/workflows/android-unit-tests.yml`) triggers on push to `main`, PR to `main`, and `workflow_dispatch`
- [x] JDK 17 (temurin) via `setup-java`; Gradle caching via `gradle/actions/setup-gradle`
- [x] Debug keystore generated in CI (`keytool -genkey`) to satisfy the `debug` signing config
- [x] `google-services.json` decoded from `GOOGLE_SERVICES_JSON_BASE64` before the Gradle run
- [x] Test results uploaded as artifact (`app/build/reports/tests/testDebugUnitTest/`)
- [ ] First CI run on the PR confirmed green (pending — see PR for the Actions run link)

**Evidence:**
- Workflow: `.github/workflows/android-unit-tests.yml`
- Local verification: `ANDROID_HOME=<sdk> GRADLE_USER_HOME=<home> ./gradlew --no-daemon :app:testDebugUnitTest` → **BUILD SUCCESSFUL**, 47 tests / 0 failures / 0 errors across all 5 suites (2026-07-27).
- Additive: `TrialManager` (the 3-day free-trial gate — monetization-critical, previously 0% covered) was refactored for testability: `ServerTrialStore`/`DefaultServerTrialStore` (`ServerTrialStore.kt`) extracts the Firestore reconcile so it's fake-able; `TrialLocalStore`/`DataStoreTrialLocalStore` (`TrialLocalStore.kt`) extracts the on-device DataStore read/write so it's fake-able; `clockMillis` is now injectable like `FitRepository`'s existing pattern; `reconcile()`/`initJob` now return `Job` so tests can `.join()` deterministically instead of racing a dispatcher. 17 new tests in `TrialManagerTest.kt` cover the trial-active/expired boundary math, local-only vs. server-authoritative reconcile branching, and persistence-across-calls. `MainViewModel` call site updated (`TrialManager(viewModelScope, DataStoreTrialLocalStore(application))`) — behavior-preserving, no functional change to the app.

**Prior art:** PR #13 (`chore/android-ci`) attempted the same thing and was closed without merging. A workflow was written and pulled back out of PR #23 pending this SDK-pin verification.

**Not in scope this run:** merge (guardrail — needs human approval).
