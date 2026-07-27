# Kinetic AI Coach — CUA E2E Test Plan (a-test)

**Library:** [dzianisv/a-test](https://github.com/dzianisv/a-test) — pinned to SHA
`70229f16e87cfc68f6f5a0ddaefee019c60e0956`.
**App package:** `com.aistudio.aicoach.vtzrkm`
**Runner:** `e2e/cua/run.sh` (Android surface, Azure Dev AI backend, model `gpt-5.4`).

We CONSUME a-test primitives; we do NOT fork or reimplement them. Where a-test lacks a
primitive (camera injection), the gap is documented below and solved in-app, not by
forking a-test.

## a-test primitives we reuse (exact names, all at pinned SHA)

| Need | a-test primitive | File |
|---|---|---|
| Drive a case end-to-end (foreground → record → CUA loop → judge → GIF → result.json) | `run_case(case)` | `a_test/loop.py` |
| YAML/dataclass case schema | `TestCase`, `Verification` | `a_test/case.py` |
| Screenshot per step (retry + `/data/local/tmp` fallback) | `screenshot_b64()` | `a_test/android.py` |
| Deterministic text oracle (UIAutomator XML) | `check_ui_text()`, `ui_dump()` | `a_test/android.py` |
| OS notification assertion | `check_notification_drawer()` | `a_test/android.py` |
| Foreground launch + ADB polling | `ensure_app_foreground()` | `a_test/android.py` |
| First-launch consent auto-dismiss | `maybe_dismiss_telemetry_consent()` | `a_test/android.py` |
| Network chaos | `simulate_network_drop()` / `restore_network()` | `a_test/android.py` |
| Actions | `execute_action()` — tap/type/key/swipe/clear_field/wait/done/fail | `a_test/actions.py` |
| Vision-click grounding (two-tier: planner + Holo grounder) | `make_grounding_fn()` → `ground()` → `scale_holo_coords()`, `HoloRateLimiter` | `a_test/grounding.py` |
| LLM-as-judge (final screenshot YES/NO, verifier-failure = FAIL) | `judge_result()` | `a_test/judge.py` |
| Screen recording (adb screenrecord, threaded) | `start_screen_recording()` / `stop_screen_recording()` | `a_test/recording.py` |
| GIF from real recording (fallback to per-step PNGs) | `assemble_gif_from_video()` / `assemble_gif()` | `a_test/recording.py` |
| Video-plays-for-real gate (duration/faststart/decode/non-blank) | `validateVideo()` | `core/validate-video.ts` (Bun) |

**Artifacts per run:** `output_dir/step-NNN_*.png`, `<case>.mp4`, `demo.gif`,
`result.json` (`{verdict, reason, steps, gif}`).

## Primitive GAP: virtual camera / video into Android camera

a-test provides **ZERO** camera-injection support — no virtual webcam, no emulator
`-camera-back` wiring, no video-stream-into-camera primitive (grep across all
`.py/.ts/.md/.yml` at the pinned SHA returns nothing). The exercise-scanner journey
needs the vision pipeline (CameraX → ML Kit pose → Gemini rep-count) exercised without a
physical exerciser in front of a camera.

**Decision: we do NOT feed YouTube-as-virtual-camera and we do NOT fork a-test.** The app
already ships the correct, superior mechanism:

- **`demo_feed` flag file** — `adb shell touch /sdcard/Android/data/com.aistudio.aicoach.vtzrkm/files/demo_feed`.
  When present, `PoseTrackerScreen.kt:127` and `TodaysClassScreen.kt:146` replay 43
  bundled CC-BY squat keyframes (`app/src/main/assets/demo_squat/frame_00..42.jpg`, loaded
  by `FrameMontage.loadDemoFrames`) through the **SAME** ML-Kit-skeleton → Gemini-montage
  pipeline the live camera uses. It is not a mock — the real Gemini form-analysis runs.

Why this beats YouTube-as-virtual-camera:
- **Deterministic** — fixed 43 frames, no external network video, no CDN flakiness.
- **CI-safe** — no emulator camera device, no `-camera-back virtualscene`, no webcam bridge.
- **Real pipeline** — exercises production vision code, so a pass proves the scanner works.

Emulator `-camera-back webcam0` / `virtualscene` remains a fallback ONLY if a future
journey needs true CameraX-device-level coverage; not needed for the 7 journeys here.

## The 7 journeys → primitive mapping

| # | Journey | Case | Key primitives / oracle | Camera |
|---|---|---|---|---|
| 1 | Signup (email) | `signup.yaml` (new) | `run_case` + `check_ui_text` post-auth screen; `judge_result` | — |
| 2 | Signin (email) | `signin.yaml` (new) | same as 1 | — |
| 3 | Guest sign-in | `guest_smoke.yaml` (exists) | `run_case`; success = login screen gone | — |
| 4 | Subscription purchase | `subscription.yaml` (new) | `run_case`; Play billing test-track sandbox; oracle = entitlement screen text via `check_ui_text` | — |
| 5 | 3-day trial entitlement | `trial_entitlement.yaml` (new) | server truth via Firebase Functions + in-app entitlement text; `check_ui_text` | — |
| 6 | AI coach reply (golden eval) | `coach_reply.yaml` (new) | `run_case` onboarding chat; **golden eval** = `judge_result` `verification.prompt` scored YES/NO on reply content quality | — |
| 7 | Exercise scanning | `full_flow.yaml` (exists) | `run_case`; rep/set counter advances; "Class Complete!" table via `verification.prompt` | **demo_feed** |

Journeys 3 and 7 already have cases. Journeys 1,2,4,5,6 are to be authored (schema in
`e2e/cua/*.yaml`, driven by `run.sh`). Evidence for each: `demo.gif` + `<case>.mp4`
validated by `core/validate-video.ts`, plus `result.json` verdict.

## Local run commands

```bash
# 0. One-time: check out a-test pinned, create venv (sibling dir ../a-test)
git clone https://github.com/dzianisv/a-test ../a-test
cd ../a-test && git checkout 70229f16e87cfc68f6f5a0ddaefee019c60e0956 \
  && python3 -m venv .venv && ./.venv/bin/pip install -e . && cd -

# 1. Boot emulator, install debug APK, enable the demo feed (journey 7 only)
adb shell touch /sdcard/Android/data/com.aistudio.aicoach.vtzrkm/files/demo_feed

# 2. Run a CUA case (Azure Dev AI creds from ~/.env.d/azure-dev.env)
e2e/cua/run.sh e2e/cua/guest_smoke.yaml emulator-5554 /tmp/cua-guest
e2e/cua/run.sh e2e/cua/full_flow.yaml  emulator-5554 /tmp/cua-full

# 3. Deterministic vision-free full-flow driver (every step on video, beats 180s cap)
ANDROID_SERIAL=emulator-5554 python3 e2e/cua/drive_flow.py /tmp/cua-full

# 4. Validate the recording actually plays before calling it done
bun ../a-test/core/validate-video.ts /tmp/cua-full/full_flow.mp4 1
```

## Report table shape (to fill after runs)

| Journey | Pass/Fail | Video | Eval score |
|---|---|---|---|
| 1 signup | — | — | — |
| 2 signin | — | — | — |
| 3 guest | — | — | — |
| 4 subscription | — | — | — |
| 5 trial entitlement | — | — | — |
| 6 coach reply (golden) | — | — | — |
| 7 exercise scan | — | — | — |
