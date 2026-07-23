# Repository Unit Tests

**Stable slug:** `repository-unit-tests`
**Rationale:** Core business logic (streak calculation, badge unlocking, XP/points, profile merge) has zero test coverage. The streak fix (epoch-day math) and badge threshold changes are in-progress uncommitted work that needs test confidence before shipping. No regression safety net exists for the most complex method, `addWorkoutSession`.
**Status:** Done
**Acceptance criteria:**
- [x] Tests cover streak logic: null lastWorkoutDate → streak=1, same-day → preserve, consecutive day → +1, gap → reset to 1
- [x] Tests cover XP/points calculation for various rep/formScore combos
- [x] Tests cover badge unlocking conditions: first_step, form_master, iron_will, xp_milestone
- [x] Tests cover checkAndSeedDatabase initial seeding
- [x] Tests cover onSignedIn merge logic (local vs remote)
- [x] Tests cover onSignedIn no-remote fallback
- [x] All tests pass with `./gradlew :app:testDebugUnitTest`
- [x] No mocking of Room — uses real in-memory database
**Evidence:** Single consolidated suite at `app/src/test/java/com/example/data/FitRepositoryTest.kt`
(27 tests) with a fake `CloudSync` (`app/src/test/java/com/example/data/NoopSync.kt`) and an
injected `clockMillis` lambda for deterministic epoch-day streak tests — no Room mocking, real
`Room.inMemoryDatabaseBuilder`. The earlier redundant `RepositoryTest.kt` was merged into this
suite and removed. Verified green on 2026-07-23 via
`GRADLE_USER_HOME=.scratch/gradle-home ./gradlew :app:testDebugUnitTest` → **BUILD SUCCESSFUL**,
`FitRepositoryTest` reporting `tests="27" skipped="0" failures="0" errors="0"`.
