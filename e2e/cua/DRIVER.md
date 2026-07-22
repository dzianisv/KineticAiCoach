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
