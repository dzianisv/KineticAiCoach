#!/usr/bin/env python3
"""Deterministic recorded driver for the Kinetic AI Coach full flow.

Drives: launch -> Workouts tab -> (dismiss paywall) -> Start today's class ->
Start Class -> real Gemini-counted workout (wait per exercise, tap Set done to
advance) -> Class Complete! results table. Records the entire run as chunked
screenrecord and concatenates to one mp4 so EVERY step is on video.
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
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    r = adb("exec-out", "cat", "/sdcard/ui.xml")
    return r.stdout

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

# Bottom-nav item centers (label text nodes report zero bounds in Compose).
NAV = {"Coach AI": (99, 2030), "Workouts": (320, 2030), "Analytics": (541, 2030),
       "Leaderboard": (761, 2030), "About": (981, 2030)}

def tap_nav(name, wait=4.0):
    x, y = NAV[name]
    print(f"  tap nav '{name}' -> ({x},{y})")
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(wait)

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
            f.write(f"file '{fp}'\n")
    r = subprocess.run(["ffmpeg", "-y", "-f", "concat", "-safe", "0", "-i", listf,
                        "-c", "copy", out], capture_output=True, text=True)
    if r.returncode != 0 or not os.path.exists(out):
        # fallback: re-encode
        r = subprocess.run(["ffmpeg", "-y", "-f", "concat", "-safe", "0", "-i", listf,
                            "-c:v", "libx264", "-pix_fmt", "yuv420p", out], capture_output=True, text=True)
    return out if os.path.exists(out) else None

# ---- flow ----
def main():
    print("== starting recorder ==")
    t = start_rec()
    try:
        print("== launch app ==")
        adb("shell", "am", "force-stop", PKG)
        time.sleep(1)
        adb("shell", "am", "start", "-a", "android.intent.action.MAIN",
            "-c", "android.intent.category.LAUNCHER", "-n", f"{PKG}/com.example.MainActivity")
        time.sleep(7)

        print("== go to Workouts tab ==")
        tap_nav("Workouts", wait=4)

        print("== Start today's class ==")
        if not tap("Start today's class", wait=6):
            # dashboard may need a scroll; try nav again then tap
            tap_nav("Workouts", wait=3)
            tap("Start today's class", wait=6)

        print("== Start Class ==")
        tap("Start Class", wait=6)

        print("== workout: let Gemini count, advance through exercises ==")
        for ex in range(6):  # up to 6 exercises; break on finish
            # let Gemini count reps for this exercise
            for _ in range(5):
                time.sleep(6)
                xml = dump()
                reps = get_val("REPS", xml); sets = get_val("SETS", xml); form = get_val("FORM", xml)
                print(f"   ex{ex+1}: REPS={reps} SETS={sets} FORM={form}")
                if find("Class Complete", xml):
                    break
            xml = dump()
            if find("Class Complete", xml):
                print("== CLASS COMPLETE reached =="); break
            if find("Finish class", xml):
                print("== last exercise -> Finish class ==")
                tap("Finish class", wait=6); break
            if find("Next exercise", xml):
                tap("Next exercise", wait=5)
            else:
                print("   (no advance button found; waiting)")
                time.sleep(4)

        time.sleep(4)
        xml = dump()
        cc = find("Class Complete", xml)
        # capture final screenshot (binary)
        png = subprocess.run(["adb", "-s", SERIAL, "exec-out", "screencap", "-p"],
                             capture_output=True, timeout=30)
        with open(os.path.join(OUTDIR, "results_final.png"), "wb") as f:
            f.write(png.stdout)
        print("== RESULT: Class Complete visible ==" if cc else "== RESULT: did NOT reach Class Complete ==")
        # dump final texts
        print("Final screen texts:")
        for n in nodes(xml):
            if n["text"].strip():
                print("   ", n["text"][:80])
    finally:
        print("== stopping recorder ==")
        files = stop_rec(t)
        final = os.path.join(OUTDIR, "full_flow.mp4")
        res = concat(files, final)
        print(f"== final video: {res} ==")

if __name__ == "__main__":
    main()
