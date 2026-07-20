# Google Play Console — Store Listing Copy

Copy-paste the fields below into **Play Console → Grow → Store presence → Main store listing**.
Character counts are measured including spaces and were verified with `wc -m` at the time of writing.
Re-check counts if you edit any field before pasting.

---

## App name
**Limit: 30 characters**

```
Kinetic AI Coach
```
Length: 16 / 30

---

## Short description
**Limit: 80 characters**

```
AI camera coach: live form fixes, rep counting, and a weekly workout plan.
```
Length: 74 / 80

---

## Full description
**Limit: 4000 characters**

```
Kinetic AI Coach turns your phone's camera into a personal trainer. Point it
at yourself, start a movement, and get real-time feedback on your form —
no wearables, no gym equipment required.

WHY KINETIC AI COACH

• Live form correction — on-device pose detection tracks your joints in
  real time and overlays a skeleton so you can see your own alignment as
  you move.
• Automatic rep & set counting — squats and pushups (with more exercises
  on the way) are counted for you, so you can focus on technique instead
  of losing count.
• AI coach chat — ask Coach Iron for squat tips, a quick 5-minute program,
  or how the form tracker works, and get natural-language answers powered
  by Google's Gemini model.
• Personalized weekly program — tell the coach your goals and it builds a
  "Today's Class" schedule of exercises sized to your week.
• Progress analytics — track total sessions, workout streaks, XP, rep
  growth over time, and your form-safety accuracy trend.
• Leaderboard & badges — light gamification to keep you coming back:
  compare points with others and unlock badges as you build a habit.
• Guest mode or Google Sign-In — try the app instantly as a guest, or sign
  in with Google to sync your profile and history across devices.

HOW IT WORKS

The camera view runs pose detection directly on your device and draws a
skeleton overlay so you always see what the app sees. Recent frames are
tiled into a single image and sent, over an encrypted connection, to a
secure cloud function that verifies your sign-in and forwards the image to
Google's Gemini model. The model counts your reps and returns short,
specific technique tips, which the app speaks aloud so you don't have to
look at your screen mid-set.

WHAT KINETIC AI COACH IS NOT

Kinetic AI Coach is a training and form-feedback tool, not a medical
device. It does not diagnose injuries, replace a physical therapist or
certified trainer, or provide medical advice. If you have a pre-existing
condition or injury, talk to a qualified professional before starting a
new exercise program.

PRIVACY, BY DESIGN

• Pose detection itself runs on-device.
• Camera frames are only sent off-device when needed to analyze a set for
  rep counting and coaching feedback, over an encrypted (HTTPS) connection
  to our backend, which forwards them to Google's Gemini model for that
  single purpose.
• You can use the app as an anonymous guest with no account at all.
• Full details are in our Privacy Policy (linked on this listing).

Questions, feedback, or a deletion request? Contact us at
support@agentlabs.cc — we read every message.
```
Length: 2606 / 4000

---

## Category
```
Health & Fitness
```

## Tags (Play Console "Tags" picker — pick up to 5 that match)
```
Fitness coaching
Exercise tracker
Workout planner
Personal trainer
Home workouts
```

## Contact details
```
Email: support@agentlabs.cc
```
(Phone and website are optional in Play Console; leave blank unless the
business decides to add a public website later.)

## Privacy Policy URL
```
https://dzianisv.github.io/KineticAiCoach/privacy-policy.html
```

---

## Notes for whoever uploads this listing

- **No medical claims.** The copy above intentionally avoids words like
  "diagnose", "treat", "cure", "injury prevention guarantee", etc. Keep it
  that way — Play's Health & Fitness category has a review process that
  flags unsubstantiated medical claims.
- **Screenshots**: see `screenshots/` in this same directory (5 phone
  screenshots, 1080×2072, cropped to remove the emulator's system nav bar).
- **Feature graphic**: `feature-graphic.png` (1024×500).
- **Hi-res icon**: `hi-res-icon-512.png` (512×512) — see `ICON-CHECK.md`
  for why this had to be generated instead of reused from the repo.
- App does **not** currently have a public marketing website — only the
  GitHub repo and the Play listing itself. If one is created later, add it
  to the "Website" field.
