# KineticAiCoach — Google Play ASO Pack

Ready-to-paste listing copy for **Play Console → Grow → Store presence → Main store listing**.
Subscription: `kinetic_pro` (auto-renewing, Google Play billing). Category: **Health & Fitness**.
Char counts verified with `printf '%s' "…" | wc -m` (includes spaces). Re-check if you edit.

Grounded only in real, in-code features: on-device ML Kit pose detection, live red skeleton
overlay (Compose Canvas), Gemini (Vertex AI) rep counting + form correction via a Firebase
Cloud Function proxy, spoken coaching via Android TextToSpeech, AI-generated workout programs,
conversational coach chat (text/photo/video/file), workout history & analytics (Room + Firestore),
Google Sign-In or anonymous guest, 3-day free trial → Kinetic Pro subscription.

---

## Keyword picks

**PRIMARY:** `AI personal trainer`

**SECONDARY:**
1. `rep counter`
2. `form correction`
3. `AI fitness coach`
4. `pose detection`
5. `home workout`

Rationale: the app's defensible wedge is a live skeleton overlay **plus** a multimodal LLM that
actually watches the annotated footage to count reps and critique form — not a heuristic counter
or an ungrounded chatbot. Lead with high-intent "AI personal trainer" + the two concrete features
users search directly ("rep counter", "form correction"). Avoid over-stuffing saturated generic
terms ("workout tracker", "home workout") — weave them in naturally only.

---

## Title
**Limit: 30 — Count: 24 / 30** ✅

```
Kinetic AI Fitness Coach
```

---

## Short description
**Limit: 80 — Count: 71 / 80** ✅

```
AI personal trainer: rep counter, form correction & live pose feedback.
```

---

## Full description
**Limit: 4000 — Count: 3141 / 4000** ✅

```
Kinetic is the AI personal trainer that watches your form, counts your reps, and talks you through every set — using nothing but your phone's camera. No wearables. No gym check-in. Point your camera at yourself and train.

★★★★★ Loved by [X] users training smarter with AI.

WHY KINETIC

• AI personal trainer in your pocket — real-time coaching built around you, not a generic template.
• Live pose detection — on-device machine learning tracks your joints and overlays a red skeleton on your camera feed, so you see exactly how your body moves.
• AI form correction & rep counter — your annotated set is analyzed by Google Gemini through the app's own secure backend, which counts sets and reps and critiques your technique.
• Coaching out loud — text-to-speech reads your form tips and rep counts aloud mid-set, so you never break form to check your screen.
• AI-generated workout plans — tell the coach your goals and get a personalized program made for you.
• Conversational AI coach — ask anything and send photos, video, or files for feedback, not just text.
• Workout history & analytics — every session is logged so you can watch your reps, sets, and form improve over time.
• Google Sign-In or anonymous guest — your choice, synced securely when you're signed in.

HOW IT WORKS

Kinetic runs pose detection directly on your device and draws a live skeleton overlay so you always see what the app sees. That annotated video is sent over an encrypted connection to the app's own Cloud Function proxy, which forwards it to Google Gemini for one purpose: counting your reps and generating specific, real-time form feedback. Nothing is shared with advertisers.

WHO IT'S FOR

Home workout beginners who want a spotter for their form. Lifters who want an automatic rep counter so they stop losing count. Anyone who wants a personal trainer, a pose-detection form checker, and a workout tracker in one app — no session booking, no equipment.

WHAT KINETIC IS NOT

Kinetic is a training and form-feedback tool, not a medical device. It does not diagnose injuries, replace a physical therapist or certified trainer, or give medical advice. If you have a pre-existing condition or injury, talk to a qualified professional before starting a new exercise program.

MEMBERSHIP

Kinetic includes a 3-day free trial. After the trial, continued access to Kinetic Pro coaching is a recurring subscription billed through Google Play. Manage or cancel anytime in your Google Play account settings. Pricing and billing period are shown on this listing and in the app before you subscribe.

PRIVACY, BY DESIGN

• Pose detection runs on-device.
• Camera frames leave your device only to analyze a set for rep counting and coaching, over an encrypted (HTTPS) connection to our backend, which forwards them to Google's Gemini model for that single purpose.
• Use the app as an anonymous guest with no account at all.
• Full details are in our Privacy Policy (linked on this listing).

Start your 3-day free trial and turn your camera into your coach today.

Questions, feedback, or a deletion request? Contact support@agentlabs.cc — we read every message.
```

---

## Screenshot caption ideas (short overlay text)

1. `Your camera becomes your coach.`
2. `AI counts every rep — hands-free.`
3. `Live form correction as you move.`
4. `A workout plan built for your goals.`
5. `Track every set, watch your form improve.`

---

## Notes

- Replace `[X]` in the full description with a real installs/ratings number once available — do **not** invent a count before launch.
- Do **not** hardcode a price; Play renders the localized `kinetic_pro` price automatically.
- No medical claims — Health & Fitness review flags unsubstantiated medical language.
- Declare "Offers in-app purchases" (the `kinetic_pro` subscription) in Play Console Monetization.
- Privacy Policy URL: `https://dzianisv.github.io/KineticAiCoach/privacy-policy.html`
- Existing detailed listing reference: `docs/play-store/LISTING.md`.
