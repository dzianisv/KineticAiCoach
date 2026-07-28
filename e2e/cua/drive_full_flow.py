#!/usr/bin/env python3
"""Deterministic recorded driver for the FULL Kinetic AI Coach PRD-v2 journey.

Extends drive_flow.py (which only covers Workouts tab -> Start Class -> results)
to also exercise: guest login -> Coach tab -> Edit Stats -> onboarding chat ->
dashboard -> Start today's class -> Start Class -> real Gemini-counted workout ->
Class Complete! results -> Done -> Coach tab -> Upgrade -> paywall.

Reuses drive_flow.py's chunked-screenrecord + ffmpeg-concat recorder and
UIAutomator tap/find helpers so the ENTIRE run (every screen) lands in one
continuous MP4, not split across disconnected clips.

Real selectors below are taken directly from the app source (not guessed):
  - LoginScreen.kt        -> "Continue as Guest" (testTag guest_signin_button)
  - OnboardingChatScreen.kt -> onboarding chat questions are answered via a
    deterministic local state machine (HEIGHT -> WEIGHT -> GOALS -> DAYS), no
    network call needed until the final "building your program" step. The
    input field shows placeholder "Type your reply..." when empty; send icon
    has contentDescription "Send". Completion button is "Let's Go"
    (testTag onboarding_done).
  - DashboardScreen.kt    -> bottom nav "Coach AI" tab (testTag tab_coach) is
    selectedTab=0 by DEFAULT every time DashboardScreen recomposes fresh (e.g.
    after popping back from onboarding or from class results), so we land on
    the Coach tab automatically without an extra tap in those cases.
    CoachTab's "Edit Stats" icon (contentDescription "Edit Stats") navigates
    back into the SAME onboarding route via onNavigateToOnboarding. Because
    MainViewModel.isOnboardingComplete is a ViewModel-scoped (not per-screen)
    StateFlow that stays true once the first onboarding finishes, re-entering
    onboarding via Edit Stats shows "Let's Go" immediately with no need to
    re-answer the 4 questions.
    CoachTab also shows "Upgrade to Kinetic Pro for unlimited AI classes" as a
    plain clickable Text row (only when !isPro) that calls
    viewModel.triggerPaywall("coach_program_card") DIRECTLY -- this bypasses
    the isEntitled()/remote-config paywall kill-switch gate entirely (that
    gate only guards starting a class / sending a coach message), so tapping
    "Upgrade" reliably opens PaywallScreen even with Firebase Remote Config's
    paywall_enabled defaulted to false (see RemoteConfigManager.kt).
  - PaywallScreen.kt      -> renders header "Kinetic Pro" once shown.
  - ClassResultsScreen.kt -> "Done" button (testTag results_done) pops back to
    "dashboard", which resets DashboardScreen's selectedTab to 0 (Coach AI),
    landing us right back on the tab with the "Upgrade" row.

Prereqs: same as drive_flow.py (booted emulator, debug APK installed, demo_feed
flag file touched, ffmpeg on PATH).

Run:
    ANDROID_SERIAL=emulator-5556 python3 e2e/cua/drive_full_flow.py /path/to/out
"""
import os
import subprocess
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from drive_flow import (  # noqa: E402  (reuse recorder/tap/find helpers)
    NAV,
    OUTDIR,
    PKG,
    SERIAL,
    adb,
    concat,
    dump,
    find,
    get_val,
    nodes,
    start_rec,
    stop_rec,
    tap,
    tap_nav,
)


def type_text(text: str):
    """Types into whatever field currently has focus (adb `input text` needs
    literal spaces escaped as %s)."""
    escaped = text.replace(" ", "%s")
    adb("shell", "input", "text", escaped)
    time.sleep(0.4)


def onboarding_answer(text: str, wait: float = 3.0):
    """Focus the onboarding chat input, type an answer, tap Send."""
    tap("Type your reply", wait=0.8, required=False)
    type_text(text)
    tap("Send", wait=wait, required=False)


def wait_for(sub: str, tries: int = 10, interval: float = 1.5):
    """Poll uiautomator dump until `sub` is found (or give up)."""
    for _ in range(tries):
        xml = dump()
        n = find(sub, xml)
        if n:
            return True
        time.sleep(interval)
    return False


def screenshot(name: str):
    png = subprocess.run(
        ["adb", "-s", SERIAL, "exec-out", "screencap", "-p"],
        capture_output=True, timeout=30,
    )
    path = os.path.join(OUTDIR, name)
    with open(path, "wb") as f:
        f.write(png.stdout)
    print(f"  saved screenshot: {path} ({len(png.stdout)} bytes)")
    return path


def main():
    print("== starting recorder ==")
    t = start_rec()
    results = {}
    try:
        print("== launch app ==")
        adb("shell", "am", "force-stop", PKG)
        time.sleep(1)
        adb("shell", "am", "start", "-a", "android.intent.action.MAIN",
            "-c", "android.intent.category.LAUNCHER", "-n", f"{PKG}/com.example.MainActivity")
        time.sleep(7)

        # ---- 1. GUEST LOGIN ----
        print("== guest login: tap 'Continue as Guest' ==")
        ok = tap("Continue as Guest", wait=6)
        results["guest_login_tapped"] = ok

        # ---- 2. FIRST-RUN ONBOARDING CHAT (mandatory: profile.name is blank
        #         for a brand-new guest, so MainNavigation routes straight
        #         here before the dashboard/Coach tab can ever be shown) ----
        print("== first-run onboarding chat: answer height/weight/goals/days ==")
        wait_for("height", tries=6, interval=1.5)
        onboarding_answer("175 cm", wait=2.5)      # HEIGHT
        onboarding_answer("70 kg", wait=2.5)        # WEIGHT
        onboarding_answer("build muscle and get fit", wait=2.5)  # GOALS
        onboarding_answer("3 days", wait=2.5)       # DAYS -> triggers program build

        print("== waiting for 'Program ready' / Let's Go ==")
        got_ready = wait_for("Let's Go", tries=15, interval=2.0)
        results["first_onboarding_completed"] = got_ready
        tap("Let's Go", wait=5)

        # ---- 3. DASHBOARD -> COACH TAB (selectedTab defaults to 0) ----
        print("== dashboard: Coach AI tab (default tab) ==")
        xml = dump()
        results["coach_tab_visible_after_onboarding"] = find("Coach AI", xml) is not None or find("AI Program", xml) is not None
        tap_nav("Coach AI", wait=3)  # explicit visit, matches requested flow

        # ---- 4. EDIT STATS -> ONBOARDING CHAT AGAIN ----
        print("== Coach tab: tap 'Edit Stats' ==")
        tap("Edit Stats", wait=4)
        # isOnboardingComplete is already true (ViewModel-scoped), so this
        # should show "Let's Go" immediately without re-answering.
        got_ready2 = wait_for("Let's Go", tries=8, interval=1.5)
        results["edit_stats_onboarding_reentry"] = got_ready2
        if not got_ready2:
            # Fallback: if it DID reset (e.g. behavior differs from source
            # reading), answer again rather than getting stuck.
            print("   (Let's Go not found immediately; re-answering onboarding)")
            onboarding_answer("175 cm", wait=2.5)
            onboarding_answer("70 kg", wait=2.5)
            onboarding_answer("build muscle and get fit", wait=2.5)
            onboarding_answer("3 days", wait=2.5)
            wait_for("Let's Go", tries=15, interval=2.0)
        tap("Let's Go", wait=5)

        # ---- 5. DASHBOARD -> WORKOUTS TAB -> START TODAY'S CLASS ----
        print("== go to Workouts tab ==")
        tap_nav("Workouts", wait=4)

        print("== Start today's class ==")
        if not tap("Start today's class", wait=6):
            tap_nav("Workouts", wait=3)
            tap("Start today's class", wait=6)

        print("== Start Class ==")
        tap("Start Class", wait=6)

        # ---- 6. AI WORKOUT: let Gemini count reps, advance through exercises ----
        print("== workout: let Gemini count, advance through exercises ==")
        for ex in range(6):  # up to 6 exercises; break on finish
            for _ in range(5):
                time.sleep(6)
                xml = dump()
                reps = get_val("REPS", xml); sets = get_val("SETS", xml); form = get_val("FORM", xml)
                print(f"   ex{ex + 1}: REPS={reps} SETS={sets} FORM={form}")
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
        results["class_complete_reached"] = cc is not None
        screenshot("results_final.png")
        print("== RESULT: Class Complete visible ==" if cc else "== RESULT: did NOT reach Class Complete ==")

        # ---- 7. RESULTS -> DONE -> back to dashboard (Coach tab default) ----
        print("== results: tap 'Done' ==")
        tap("Done", wait=5)

        # ---- 8. COACH TAB -> UPGRADE -> PAYWALL ----
        print("== Coach tab: waiting for 'Upgrade' row ==")
        got_upgrade = wait_for("Upgrade", tries=8, interval=1.5)
        results["upgrade_row_found"] = got_upgrade
        tap("Upgrade", wait=4)

        print("== verifying paywall ==")
        xml = dump()
        paywall_shown = find("Kinetic Pro", xml) is not None
        results["paywall_reached"] = paywall_shown
        screenshot("paywall_final.png")
        print("== RESULT: Paywall visible ==" if paywall_shown else "== RESULT: did NOT reach Paywall ==")

        print("\n== SUMMARY ==")
        for k, v in results.items():
            print(f"   {k}: {v}")
    finally:
        print("== stopping recorder ==")
        files = stop_rec(t)
        final = os.path.join(OUTDIR, "full_flow.mp4")
        res = concat(files, final)
        print(f"== final video: {res} ==")


if __name__ == "__main__":
    main()
