# Deterministic full-flow driver (`drive_flow.py`)

A vision-free companion to the CUA `full_flow.yaml`. It drives the real PRD-v2
journey with fixed coordinates + UIAutomator text lookups and records the whole
run as one concatenated MP4, so **every step is on video** (beats screenrecord's
180s cap by chunking).

Flow: launch → Workouts tab → **Start today's class** → **Start Class** → the
real ML Kit red-skeleton + gemini-3.5-flash montage pipeline counts reps/form per
exercise → tap **Set done — Next exercise** to advance → **Class Complete!** table.

## Prereqs
- Emulator booted (`ANDROID_SERIAL`, default `emulator-5554`).
- Debug APK installed and the `demo_feed` flag set:
  `adb shell touch /sdcard/Android/data/com.aistudio.aicoach.vtzrkm/files/demo_feed`
  (makes Today's Class replay 43 bundled squat frames through the SAME pipeline
  the live camera uses — no physical camera needed).
- `ffmpeg` on PATH (chunk concatenation).

## Run
```bash
ANDROID_SERIAL=emulator-5554 python3 e2e/cua/drive_flow.py /path/to/output_dir
```
Output: `full_flow.mp4`, `results_final.png`, per-chunk `rec_*.mp4`.

The demo feed shows continuous squats (no rest gaps), so Gemini's rest-based
`set_complete` rarely fires; the driver taps "Set done — Next exercise" to lock in
each exercise's real Gemini-measured reps and advance — the same button a user taps.

## Full-flow variant (`drive_full_flow.py`)

Extends the above to the FULL PRD-v2 journey in one continuous recording:
guest login -> Coach tab -> **Edit Stats** -> onboarding chat -> dashboard ->
Workouts tab -> Start today's class -> Start Class -> AI workout -> Class
Complete! results -> **Done** -> Coach tab -> **Upgrade** -> paywall.

```bash
ANDROID_SERIAL=emulator-5554 python3 e2e/cua/drive_full_flow.py /path/to/output_dir
```

Notes on real (source-verified, non-guessed) selectors used:
- Guest login: `LoginScreen.kt` "Continue as Guest" (`guest_signin_button`).
- Onboarding chat: `OnboardingChatScreen.kt` is a deterministic local state
  machine (HEIGHT -> WEIGHT -> GOALS -> DAYS), no vision/LLM needed to answer
  it. Completion button is "Let's Go" (`onboarding_done`).
- `MainViewModel.isOnboardingComplete` is a ViewModel-scoped `StateFlow`
  (survives navigation), so re-entering onboarding via the Coach tab's **Edit
  Stats** icon (`DashboardScreen.kt`, contentDescription "Edit Stats") shows
  "Let's Go" immediately without re-answering.
- `DashboardScreen`'s `selectedTab` resets to `0` (Coach AI) on every fresh
  composition (e.g. after popping back from onboarding or class results), so
  the Coach tab is visible by default after both of those returns.
- **Upgrade**: the Coach tab's "Upgrade to Kinetic Pro..." row calls
  `viewModel.triggerPaywall(...)` directly, bypassing the `isEntitled()` /
  Firebase Remote Config paywall kill-switch (`RemoteConfigManager.kt`,
  `paywall_enabled` defaults to `false` since Play Store billing isn't live
  yet) — that gate only guards *starting* a class / *sending* a coach
  message, not this explicit upgrade tap. So the paywall reliably opens even
  with monetization enforcement off.
