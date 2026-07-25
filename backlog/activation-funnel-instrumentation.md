# Activation Funnel Instrumentation

**Stable slug:** `activation-funnel-instrumentation`

**Rationale:** The app has 0 installs and the analytics funnel started at `sign_in_success` — the entire top-of-funnel (app open → onboarding → first workout → activation) was uninstrumented. The PRD names the **"First Step" badge as the activation milestone**, yet no event fired when it unlocked. Existing events (`sign_in_success`, `program_generated`, `class_started/completed`, full purchase funnel) let us measure monetization but not *whether new users activate*. Once installs begin (the real bottleneck), we would be blind to where onboarding drops users off — the same measurement gap the purchase-funnel run fixed, but for the top-of-funnel that gates every downstream metric (retention, monetization, $10k MRR). This is a prerequisite, reversible, additive analytics change with zero UX risk.

**Status:** Done — verified green locally. Uncommitted on branch `docs/aso-pack`; pending human-approved commit.

**Acceptance criteria:**
- [x] `onboarding_started` event added and fired in `MainViewModel.startOnboardingChat()` (after the idempotency guard — one per onboarding session)
- [x] `onboarding_completed` event added and fired in the `finally` block of the `OnboardingStep.DAYS` branch, after initial profile commit + program build (`_isOnboardingComplete = true`)
- [x] `first_workout_completed` event added and fired in `finishTodaysClass()` only on the first-ever completion
- [x] `first_step_badge_unlocked` event (PRD activation milestone) fired alongside first workout, matching the repository's `streakDays == 0` / `lastWorkoutDate == null` single-fire badge gate
- [x] First-workout detection reads `userProfile.value?.lastWorkoutDate == null` BEFORE the persisting write, so the save doesn't flip the gate (single-fire, durable)
- [x] Repository kept analytics-free (no new import); events fired from the ViewModel to match existing style
- [x] Debug Kotlin compiles clean (no errors)
- [x] All existing unit tests remain green (30/30)

**Evidence:**
- Files: `app/src/main/java/com/example/analytics/Analytics.kt` (4 new events + constants), `app/src/main/java/com/example/ui/MainViewModel.kt` (3 insertion sites: `startOnboardingChat`, onboarding `finally`, `finishTodaysClass`)
- Verification: `GRADLE_USER_HOME=.scratch/gradle-home ./gradlew --offline --no-daemon :app:compileDebugKotlin :app:testDebugUnitTest` → **BUILD SUCCESSFUL** (2026-07-24), **30 tests / 0 failures / 0 errors / 0 skipped** (FitRepositoryTest 27, ExampleUnitTest 1, ExampleRobolectricTest 1, GreetingScreenshotTest 1).
- Single-fire gates: onboarding start guarded by `_onboardingMessages.isNotEmpty()` return; first-workout guarded by durable `lastWorkoutDate == null` (equivalent to repo `streakDays == 0` badge unlock at `Repository.kt:137`).

**Expected metric impact:** Enables the full activation funnel in Firebase Analytics once installs begin: `onboarding_started` → `onboarding_completed` → `first_workout_completed` → `first_step_badge_unlocked`. Lets us compute onboarding-completion rate and activation rate (first-workout / install), and locate the drop-off step. Leading indicator: DebugView shows all four events on a fresh first-run walkthrough.

**Not in scope this run:** commit/merge (guardrail — needs approval), acquisition (getting the installs themselves), and per-step onboarding parameters (which step users abandon on) — could be a follow-up if completion rate proves low.
