# Android Unit Test CI Workflow

**Stable slug:** `android-ci-workflow`

**Rationale:** The repo has 27 repository unit tests (`FitRepositoryTest.kt`) plus Robolectric and screenshot tests, but zero CI enforcement. No Android build runs on PRs or commits to main. This means:
- A PR that breaks tests merges silently unless the author remembers to run `./gradlew :app:testDebugUnitTest` locally.
- The existing `e2e.yml` workflow only tests the Cloud Functions proxy — the Android app itself has no automated quality gate.
- Gradle's configuration-cache and build-cache benefits are never validated outside a local machine.

Adding Android CI makes test failures visible on every PR, blocks merging on red, and surfaces build regressions (dependency conflicts, API breakage, Gradle plugin compatibility) before they reach main.

**Status:** Implemented

**Acceptance criteria:**
- [x] Workflow triggers on PRs targeting `main` and pushes to `main`
- [x] Workflow also supports `workflow_dispatch` for manual runs
- [x] Java 17 (JDK 17) for Android Gradle Plugin compatibility
- [x] Gradle wrapper + dependency caching (separate read/write caches for correctness)
- [x] Runs `./gradlew :app:testDebugUnitTest` — the same command a developer runs locally
- [x] Does NOT require `google-services.json`, keystore, or `.env` secrets (build uses `WARN`/passthrough mode for all of these)
- [x] Test results published as a human-readable summary in the Actions tab

**Evidence:**
- File: `.github/workflows/android-ci.yml`
- Verified: Workflow parses correctly (`gh workflow list` shows it)

**Expected metric impact:**
- Every PR and push to main gets a green/red Android build check
- Zero silent regression landings on `FitRepositoryTest.kt` or any unit test
- ~5-7 minute CI time with Gradle caching; ~15 min cold cache

**Next candidate:** Functions deploy CI or demo-video/Play Store promo URL.
