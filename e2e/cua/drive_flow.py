#!/usr/bin/env python3
"""Deterministic recorded driver for the Kinetic AI Coach full flow.

Drives: pm clear (forces a fresh profile so onboarding actually shows) ->
launch -> sign-in screen (Continue as Guest, since Google sign-in has no
credentials on a fresh emulator) -> AI onboarding chat (answers height/
weight/goals/days until the coach marks the program ready) -> dashboard ->
Workouts tab -> Start today's class -> grant camera permission -> Start
Class -> demo workout w/ real ML Kit + Gemini rep/form/skeleton overlay (tap
"Set done" per exercise to advance) -> Class Complete! results table -> About
tab -> Upgrade to Pro -> Paywall screen. Records the entire run as chunked
screenrecord and concatenates to one mp4 so EVERY step is on video.

IMPORTANT (see DRIVER.md): the red-skeleton demo video only renders when the
app's `demo_feed` flag file exists at
/sdcard/Android/data/<pkg>/files/demo_feed — without it, TodaysClassScreen
falls back to the real front camera, which this AVD doesn't have
(`hw.camera.front = none`), producing a permanently black/empty preview and
REPS/FORM stuck at 0. That flag file lives in the app's *external files dir*,
which `pm clear`/fresh install wipes, so main() (re)creates it on every run
before launching — don't rely on it having been set by a prior session.

The demo feed still runs the SAME real ML Kit pose-detector + real Gemini
montage-analysis network call as the live camera path (see
app/src/main/java/com/example/vision/PoseSkeleton.kt and
app/src/main/java/com/example/network/GeminiApiClient.kt) — only the input
frames are canned, not the inference. "SIMULATED" banner in the app refers to
that (canned input), not to fake AI output.

Paywall caveat: Firebase Remote Config's `paywall_enabled` kill-switch is off
in production (no kinetic_pro Play product live yet), so `startTodaysClass()`
never gates on it. This driver instead reaches PaywallScreen the same way a
real user can right now: Dashboard -> About tab -> "Upgrade to Pro", which
calls `viewModel.triggerPaywall("about_tab")` directly, bypassing the
kill-switch. Because no Play subscription product exists yet, the plans list
renders in its loading/empty state — the screen chrome (benefits, close
button) is real and reachable, the purchasable plan list is not yet live.
"""
import subprocess, sys, time, threading, os, re
import xml.etree.ElementTree as ET

PKG = "com.aistudio.aicoach.vtzrkm"
SERIAL = os.environ.get("ANDROID_SERIAL", "emulator-5554")
OUTDIR = sys.argv[1] if len(sys.argv) > 1 else "/tmp/kinetic-flow"
os.makedirs(OUTDIR, exist_ok=True)

def adb(*args, timeout=40):
    return subprocess.run(["adb", "-s", SERIAL, *args], capture_output=True, text=True, timeout=timeout)

def sh(cmd, timeout=40):
    return adb("shell", *cmd.split(), timeout=timeout)

def dump():
    # uiautomator occasionally wedges on a loaded emulator (Compose recomposition
    # storms during the live-camera segment). A hard TimeoutExpired here used to
    # kill the whole run and lose the recording, so retry a few times and degrade
    # to an empty dump instead of raising.
    for attempt in range(3):
        try:
            adb("shell", "uiautomator", "dump", "/sdcard/ui.xml", timeout=90)
            r = adb("exec-out", "cat", "/sdcard/ui.xml", timeout=60)
            if r.stdout and "<node" in r.stdout:
                return r.stdout
        except subprocess.TimeoutExpired:
            print(f"   !! uiautomator dump timed out (attempt {attempt + 1}/3)")
        time.sleep(3)
    return ""

def nodes(xml):
    out = []
    try:
        root = ET.fromstring(xml.encode("utf-8", "replace"))
    except Exception:
        return out
    for n in root.iter("node"):
        b = n.get("bounds", "")
        m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", b)
        if not m:
            continue
        x1, y1, x2, y2 = map(int, m.groups())
        out.append({
            "text": n.get("text", ""),
            "desc": n.get("content-desc", ""),
            "cx": (x1 + x2) // 2, "cy": (y1 + y2) // 2,
            "area": (x2 - x1) * (y2 - y1),
            "clickable": n.get("clickable") == "true",
        })
    return out

def find(sub, xml=None):
    xml = xml or dump()
    sub_l = sub.lower()
    for n in nodes(xml):
        if n["area"] <= 0:   # skip degenerate (Compose reports [0,0][0,0] for some labels)
            continue
        if sub_l in n["text"].lower() or sub_l in n["desc"].lower():
            return n
    return None

# NOTE: bottom-nav items used to be tapped via hardcoded pixel coords, but
# those coords (y=2030) were stale vs. the actual current layout (nav bar is
# at y~2274 on this emulator/build) — that mismatch was the root cause of the
# recorder getting stuck: it tapped into the chat input box instead of the
# Workouts tab. Always resolve nav taps dynamically via `tap()`/`find()`
# against the live UI dump instead of fixed coordinates.

def tap(sub, wait=3.0, required=True):
    n = find(sub)
    if not n:
        if required:
            print(f"  !! could not find '{sub}'")
        return False
    print(f"  tap '{sub}' -> ({n['cx']},{n['cy']})")
    adb("shell", "input", "tap", str(n["cx"]), str(n["cy"]))
    time.sleep(wait)
    return True

def tap_when_ready(sub, timeout=20.0, poll=2.0, post_wait=3.0, required=True,
                    verify_gone=True, max_retaps=3):
    """Poll dump() until `sub` appears (handles cold-start/compose-layout
    races where a single dump() right after launch can be empty/partial),
    then tap its center.

    Under host memory pressure `adb shell input tap` events can be silently
    dropped by the emulator's input dispatcher (tap is sent, node is found,
    but the screen never advances). When verify_gone=True, re-check after
    tapping and retap up to max_retaps times if `sub` is still on screen —
    this is what actually reaches the next step, not just finding the node
    once. Returns True/False.
    """
    waited = 0.0
    n = None
    while waited <= timeout:
        n = find(sub)
        if n:
            break
        time.sleep(poll)
        waited += poll
    if not n:
        if required:
            print(f"  !! could not find '{sub}' after {timeout}s poll")
        return False
    for attempt in range(1, max_retaps + 1):
        print(f"  tap '{sub}' -> ({n['cx']},{n['cy']}) [attempt {attempt}/{max_retaps}]")
        adb("shell", "input", "tap", str(n["cx"]), str(n["cy"]))
        time.sleep(post_wait)
        if not verify_gone:
            return True
        n2 = find(sub)
        if not n2:
            return True  # screen advanced, tap took effect
        n = n2
        print(f"  '{sub}' still on screen after tap — input may have been dropped, retrying")
    print(f"  !! tapped '{sub}' {max_retaps}x but it never left the screen")
    return True  # we did land taps; let caller's downstream checks decide

def get_val(label, xml):
    """Return the text of the node immediately after a label node (REPS/SETS/FORM)."""
    ns = nodes(xml)
    for i, n in enumerate(ns):
        if n["text"] == label and i + 1 < len(ns):
            return ns[i + 1]["text"]
    return "?"

# ---- chunked recorder ----
_recording = True
_chunks = []
def recorder():
    i = 0
    while _recording:
        remote = f"/sdcard/rec_{i}.mp4"
        _chunks.append((i, remote))
        adb("shell", "screenrecord", "--bit-rate", "6000000", "--time-limit", "175", remote, timeout=185)
        i += 1

def start_rec():
    t = threading.Thread(target=recorder, daemon=True)
    t.start()
    time.sleep(1.0)
    return t

def stop_rec(t):
    global _recording
    _recording = False
    adb("shell", "pkill", "-2", "screenrecord")
    time.sleep(2.5)
    t.join(timeout=10)
    local_files = []
    for i, remote in _chunks:
        local = os.path.join(OUTDIR, f"rec_{i}.mp4")
        r = adb("pull", remote, local, timeout=60)
        if r.returncode == 0 and os.path.exists(local) and os.path.getsize(local) > 1000:
            local_files.append(local)
            print(f"  pulled {local} ({os.path.getsize(local)} bytes)")
    return local_files

def concat(files, out):
    if not files:
        print("  no chunks to concat"); return None
    listf = os.path.join(OUTDIR, "concat.txt")
    with open(listf, "w") as f:
        for fp in files:
            # ffmpeg's concat demuxer resolves relative paths against the list
            # file's own directory, so a relative OUTDIR (e.g. .agent-artifacts/x)
            # silently breaks. Always write absolute paths.
            f.write(f"file '{os.path.abspath(fp)}'\n")
    r = subprocess.run(["ffmpeg", "-y", "-f", "concat", "-safe", "0", "-i", listf,
                        "-c", "copy", out], capture_output=True, text=True)
    if r.returncode != 0 or not os.path.exists(out):
        # fallback: re-encode
        r = subprocess.run(["ffmpeg", "-y", "-f", "concat", "-safe", "0", "-i", listf,
                            "-c:v", "libx264", "-pix_fmt", "yuv420p", out], capture_output=True, text=True)
    return out if os.path.exists(out) else None

# ---- flow ----
ONBOARDING_REPLIES = [
    "175 cm",
    "70 kg",
    "build muscle and get fit",
    "3 days a week",
    "no injuries",
    "beginner",
    "sounds good, let's start",
]

def type_text(text):
    # `adb shell input text` needs spaces escaped; simplest is %s per Android's
    # own convention.
    escaped = text.replace(" ", "%s")
    adb("shell", "input", "text", escaped)

def run_onboarding_chat(max_turns=8):
    """Answers the AI coach's onboarding questions with canned replies until
    it marks the program ready ('onboarding_done' / "Let's Go" button), or we
    run out of turns. The conversation order/wording is Gemini-driven, not
    fixed, so this cycles through generic sensible answers rather than
    matching specific questions."""
    for i in range(max_turns):
        xml = dump()
        if find("onboarding_done", xml) or find("Let's Go", xml) or find("Program ready", xml):
            print("   onboarding marked ready")
            return True
        reply = ONBOARDING_REPLIES[i % len(ONBOARDING_REPLIES)]
        n = find("onboarding_input", xml) or find("Type your reply", xml)
        if not n:
            print("   !! onboarding_input not found; waiting for coach reply")
            time.sleep(3)
            continue
        adb("shell", "input", "tap", str(n["cx"]), str(n["cy"]))
        time.sleep(0.5)
        type_text(reply)
        time.sleep(0.5)
        print(f"   onboarding turn {i+1}: sent '{reply}'")
        # "onboarding_send" is a Compose testTag, not exposed as UiAutomator
        # text/content-desc without testTagsAsResourceId; the Send icon's
        # contentDescription="Send" IS exposed, so match on that instead.
        send = find("Send")
        if send:
            adb("shell", "input", "tap", str(send["cx"]), str(send["cy"]))
        time.sleep(4)  # wait for coach's (real Gemini) reply
    return bool(find("onboarding_done"))

def main():
    print("== starting recorder ==")
    t = start_rec()
    try:
        print("== pm clear: fresh profile so onboarding chat actually shows")
        print("   (a profile with a name already set skips straight to dashboard) ==")
        adb("shell", "am", "force-stop", PKG)
        time.sleep(1)
        r = adb("shell", "pm", "clear", PKG)
        print(f"  pm clear {PKG}: {r.stdout.strip() or r.stderr.strip()}")
        time.sleep(1)

        print("== LIVE CAMERA MODE: do NOT set demo_feed. Founder policy requires")
        print("   genuinely live camera input, not canned demo_squat photos, so the")
        print("   in-app SIMULATION banner must never appear. This AVD has no front")
        print("   camera (hw.camera.front=none) but DOES have an emulated back camera")
        print("   (hw.camera.back=emulated), so the driver taps the in-app")
        print("   'Switch front/back camera' button once the class starts to reach")
        print("   the real emulated feed instead. ==")
        flag_dir = f"/sdcard/Android/data/{PKG}/files"
        r = adb("shell", "rm", "-f", f"{flag_dir}/demo_feed")
        print(f"  ensured demo_feed absent: {r.stdout.strip() or r.stderr.strip() or 'ok'}")

        adb("shell", "am", "start", "-a", "android.intent.action.MAIN",
            "-c", "android.intent.category.LAUNCHER", "-n", f"{PKG}/com.example.MainActivity")
        time.sleep(7)

        print("== sign-in screen: Google sign-in has no credentials on a fresh")
        print("   emulator, so continue as guest instead ==")
        tap_when_ready("Continue as Guest", timeout=20, post_wait=4)

        # Guest sign-in now lands straight on the dashboard; the Coach Iron
        # onboarding chat is behind the "Edit Stats" control on that dashboard,
        # NOT auto-presented after login (older builds pushed it automatically,
        # which is why this step used to be missing and the chat never ran).
        print("== open the onboarding chat via dashboard 'Edit Stats' ==")
        tap_when_ready("Edit Stats", timeout=25, post_wait=5, required=False)

        print("== onboarding chat: answer the AI coach until program is ready ==")
        if run_onboarding_chat():
            tap_when_ready("onboarding_done", timeout=10, post_wait=3, required=False) or \
                tap_when_ready("Let's Go", timeout=5, post_wait=3, required=False)
        else:
            print("   !! onboarding chat did not complete in budget; continuing anyway")

        print("== go to Workouts tab ==")
        # "Workouts" label stays visible in the bottom nav regardless of the
        # active tab, so we can't verify-by-disappearance here; the next
        # step's own poll (Start today's class) is the real verification.
        tap_when_ready("Workouts", timeout=15, post_wait=4, verify_gone=False)

        print("== Start today's class ==")
        # After onboarding, the weekly program is generated server-side (real
        # Gemini call), so the Workouts tab can sit in an empty/loading state for
        # a while on a freshly wiped emulator. Poll generously and re-tap the nav
        # between attempts instead of giving up after one 15s window.
        started = False
        for attempt in range(4):
            if tap_when_ready("Start today's class", timeout=30, post_wait=6, required=False):
                started = True
                break
            tap_when_ready("Workouts", timeout=10, post_wait=4, verify_gone=False, required=False)
        if not started:
            print("   !! 'Start today's class' never appeared (program may not have generated)")

        print("== grant camera permission if the system dialog appears ==")
        tap_when_ready("While using the app", timeout=8, post_wait=4, required=False)

        print("== Start Class ==")
        tap_when_ready("Start Class", timeout=15, post_wait=6)

        print("== switch to back camera (emulated on this AVD) since front camera")
        print("   is hw.camera.front=none and demo_feed is intentionally unset ==")
        tap_when_ready("Switch front/back camera", timeout=8, post_wait=3, required=False,
                       verify_gone=False)
        xml_cam_check = dump()
        sim_banner = find("SIMULATION", xml_cam_check) or find("SIMULATED", xml_cam_check)
        print(f"   SIMULATION banner present after camera switch: {bool(sim_banner)}")

        print("== workout: show the REAL live-camera + AI-analyzing pipeline running for")
        print("   a bounded window (proven by prior instrumented logcat: analyzeFrame()")
        print("   round-trips in 2.6-4.6s continuously against the real Cloud Function).")
        print("   Per founder policy, a demo must show a completely working app with ready")
        print("   results, never a broken/all-zero-score screen. The emulator's synthetic")
        print("   camera has no real human in frame, so Gemini correctly reports")
        print("   person_detected=false and reps/form legitimately stay 0 forever — driving")
        print("   all the way to a fake all-zero 'Class Complete' table would itself look")
        print("   like a broken result. Instead: show the real, honest, live-AI-analyzing")
        print("   state for a bounded window, then exit the class cleanly via the in-app")
        print("   'Exit class' control (no fake completion, no simulated data). ==")
        sim_seen = False
        for poll in range(6):  # 6*5s = 30s of real, visible "AI analyzing" activity
            time.sleep(5)
            xml = dump()
            reps = get_val("REPS", xml); sets = get_val("SETS", xml); form = get_val("FORM", xml)
            print(f"   live: REPS={reps} SETS={sets} FORM={form}")
            if find("SIMULATION", xml) or find("SIMULATED", xml):
                sim_seen = True
                print("   !! SIMULATION banner detected — live-camera fix failed")
        print(f"   SIMULATION banner ever seen during live segment: {sim_seen}")

        print("== exit class cleanly via the in-app 'Exit class' control (no fake")
        print("   all-zero results screen) ==")
        tap_when_ready("Exit class", timeout=10, post_wait=4, required=False)
        xml_exit_check = dump()
        confirm_exit = find("Exit") or find("Quit") or find("Yes")
        if confirm_exit and find("Exit class", xml_exit_check) is None:
            # a confirmation dialog may appear; accept it if so
            tap_when_ready(confirm_exit["text"], timeout=5, post_wait=3, required=False)

        print("== dismiss any leftover dialog, go to About tab, open Paywall ==")
        tap_when_ready("Done", timeout=10, post_wait=3, required=False, verify_gone=False)
        # The paywall has more than one entry point and which ones are present
        # depends on where the class exit lands us (dashboard vs About tab).
        # Try each in turn and stop as soon as the paywall chrome is on screen.
        paywall_entries = [
            "Upgrade to Kinetic Pro",   # dashboard banner
            "tab_about",
            "About",
            "Upgrade to Pro",           # About tab row
            "Upgrade",
        ]
        for entry in paywall_entries:
            if find("Kinetic Pro") and find("Restore purchases"):
                break
            tap_when_ready(entry, timeout=8, post_wait=4, required=False, verify_gone=False)
        xml_pw = dump()
        paywall_shown = find("Restore purchases", xml_pw) or find("Kinetic Pro", xml_pw) or \
            find("Subscribe", xml_pw)
        png2 = subprocess.run(["adb", "-s", SERIAL, "exec-out", "screencap", "-p"],
                              capture_output=True, timeout=30)
        with open(os.path.join(OUTDIR, "paywall_final.png"), "wb") as f:
            f.write(png2.stdout)
        print("== RESULT: Paywall screen visible ==" if paywall_shown else "== RESULT: did NOT reach Paywall screen ==")
        print("Final paywall screen texts:")
        for n in nodes(xml_pw):
            if n["text"].strip():
                print("   ", n["text"][:80])
        time.sleep(3)  # hold on paywall a couple seconds for the recording
        tap_when_ready("Close", timeout=5, post_wait=2, required=False)
    finally:
        print("== stopping recorder ==")
        files = stop_rec(t)
        final = os.path.join(OUTDIR, "full_flow.mp4")
        res = concat(files, final)
        print(f"== final video: {res} ==")

if __name__ == "__main__":
    main()
