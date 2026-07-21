# Google Play Console — Store Listing Copy

Copy-paste the fields below into **Play Console → Grow → Store presence → Main store listing**.
Character counts are measured including spaces and were verified with `wc -m` at the time of writing.
Re-check counts if you edit any field before pasting.

App: **Kinetic AI Coach** · Package: `com.aistudio.aicoach.vtzrkm` · Category: **Health & Fitness**

---

## App name
**Limit: 30 characters**

```
Kinetic AI Fitness Coach
```
Length: 24 / 30 — includes the primary keyword pair "AI" + "Fitness Coach" alongside the
brand name, replacing the plain `Kinetic AI Coach` (16/30) used during Internal Testing.
See the "Title / subtitle A/B variants" table below if a shorter, more brand-only title is
preferred instead.

---

## Short description
**Limit: 80 characters**

```
AI personal trainer: rep counter, form correction, live pose feedback.
```
Length: 70 / 80 — front-loads "AI personal trainer" and "rep counter" / "form correction",
the two highest-intent Health & Fitness search terms for this app.

---

## Full description
**Limit: 4000 characters**

```
Kinetic AI Coach is the AI fitness coach that watches your form, counts your
reps, and talks you through every set — using nothing but your phone's camera.

No wearables. No gym check-in. Just point your camera at yourself and train.

WHY KINETIC AI COACH

• Real-time pose detection — on-device machine learning tracks your joints
  live and overlays a red skeleton over your camera feed, so you can see
  exactly how your body is moving as you move it.
• AI form correction & rep counting — your annotated workout video is
  analyzed by Google Gemini through the app's own secure backend, which
  counts your sets and reps and critiques your technique in real time.
• Coaching out loud — text-to-speech reads your form tips and rep counts
  aloud mid-set, so you never have to break form to check your screen.
• AI-generated workout programs — tell the coach your goals and get a
  personalized training plan built for you, not a generic template.
• Conversational AI coach chat — ask your coach anything, and send photos,
  video, or files for feedback, not just text.
• Workout history & analytics — every session is logged so you can see
  your reps, sets, and form trends improve over time.
• Sign in with Google or train as an anonymous guest — your choice, synced
  securely with Firebase when you're signed in.

HOW IT WORKS

Kinetic AI Coach runs pose detection directly on your device and draws a
live skeleton overlay so you always see what the app sees. That annotated
video is sent, over an encrypted connection, to the app's own Cloud
Function proxy, which forwards it to Google Gemini (Vertex AI) for a single
purpose: counting your reps and generating specific, real-time form
feedback. Nothing is shared with advertisers.

WHAT KINETIC AI COACH IS NOT

Kinetic AI Coach is a training and form-feedback tool, not a medical
device. It does not diagnose injuries, replace a physical therapist or
certified trainer, or provide medical advice. If you have a pre-existing
condition or injury, talk to a qualified professional before starting a
new exercise program.

WHO IT'S FOR

Home workout beginners who want a spotter for their form. Lifters who want
an automatic rep counter so they can stop losing count. Anyone who wants a
personal trainer, a pose-detection form checker, and a workout tracker in
one app — without booking a session or buying equipment.

MEMBERSHIP

Kinetic AI Coach includes a 3-day free trial — no credit card required
upfront. After the trial, continued access to Kinetic Pro coaching features
is offered as a recurring subscription, billed through Google Play. You can
manage or cancel your subscription anytime in your Google Play account
settings. Pricing and billing period are shown on this listing and inside
the app before you subscribe.

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
Length: 3,324 / 4,000 — well under the limit, leaving headroom for future feature
additions without a rewrite.

**Subscription/trial disclosure check:** the "MEMBERSHIP" section explicitly discloses
(a) the 3-day free trial, (b) that it converts to a recurring paid subscription, (c) that
billing runs through Google Play, and (d) that the user can cancel anytime — this satisfies
Play's requirement to disclose subscription terms in the app description. Do **not**
hardcode "$7.25/month" or "$43.50/year" in the store copy — Play renders the actual
localized price per user automatically from the `kinetic_pro` subscription's base plans
(`app/src/main/java/com/example/billing/BillingConfig.kt`), and hardcoded prices go stale
and can trigger policy/consistency flags if pricing changes.

---

## Category & tags

```
Category: Health & Fitness
Tags (pick up to 5 in Play Console's tag picker):
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

## Keywords / ASO notes

### Primary keywords (highest search intent, use in title/short description)
- AI fitness coach
- rep counter
- form correction
- personal trainer

### Secondary keywords (weave naturally into the full description, do not stuff)
- workout tracker
- pose detection
- home workout
- AI workout plan
- exercise form checker
- workout analytics

### Competitor angle
Most "AI fitness" apps on Play either (a) do on-device pose detection with **no**
generative-AI critique (just angle math / rep counting heuristics), or (b) offer generic
chatbot coaching with **no** camera/pose grounding at all. Kinetic AI Coach's
differentiator is combining both in one loop: a live skeleton overlay **plus** a
multimodal LLM (Gemini) actually watching the annotated video to judge technique and count
reps — not a canned counter. Lead ASO copy with "AI watches your form" / "AI counts your
reps" rather than generic "workout tracker" language, since that's the defensible wedge
against both categories of competitor.

### Title / subtitle A/B variants
| Variant | Title (≤30 chars) | Rationale |
|---|---|---|
| A (recommended) | `Kinetic AI Fitness Coach` (24) | Balances brand name with the single highest-volume keyword pair ("AI" + "Fitness Coach"). |
| B | `Kinetic: AI Form & Reps` (24) | Leads with the two concrete features (form correction, rep counting) searchers query for directly; trades brand clarity for feature specificity. |
| C | `Kinetic AI Coach` (16) | Plain brand-only title used during Internal Testing; safest, lowest-risk option with zero rebrand friction for existing testers, but leaves ASO keyword value on the table. |

Recommend shipping **Variant A** for the initial Production release, then A/B testing
against **B** using Play's Store Listing Experiments once the app has enough Production
traffic to reach statistical significance (typically a few thousand store visitors).

---

## "What's new" — release notes for the first Production release

```
Kinetic AI Coach is live! 🎉

• Real-time camera pose tracking with a live skeleton overlay
• AI-powered rep counting and form feedback, spoken aloud as you train
• Personalized AI-generated workout programs
• Conversational AI coach — ask questions or send photos/video for feedback
• Workout history and progress analytics
• Sign in with Google, or train instantly as a guest

Start with a 3-day free trial, then continue with Kinetic Pro. Thanks for
training with us — feedback always welcome at support@agentlabs.cc.
```

---

## Notes for whoever uploads this listing

- **No medical claims.** The copy above intentionally avoids words like
  "diagnose", "treat", "cure", "injury prevention guarantee", etc. Keep it
  that way — Play's Health & Fitness category has a review process that
  flags unsubstantiated medical claims.
- **Screenshots**: see `screenshots/` in this same directory (5 phone
  screenshots, 1080×2072, cropped to remove the emulator's system nav bar):
  - `01-coach-ai.png` — AI coach chat screen — caption: "Ask your coach anything."
  - `02-workouts.png` — workout/exercise selection screen — caption: "AI counts your reps and corrects your form."
  - `03-todays-class.png` — personalized "Today's Class" program screen — caption: "A plan built around your goals."
  - `04-analytics.png` — progress/analytics dashboard — caption: "Track every session."
  - `05-about.png` — app info/about screen — caption: "Your AI coach watches your form — live."
- **Feature graphic**: `feature-graphic.png` (1024×500) — pair with the tagline "Your AI
  coach watches your form — live."
- **Hi-res icon**: `hi-res-icon-512.png` (512×512) — see `ICON-CHECK.md`
  for why this had to be generated instead of reused from the repo.
- App does **not** currently have a public marketing website — only the
  GitHub repo and the Play listing itself. If one is created later, add it
  to the "Website" field.
- **In-app purchases badge:** the store listing must declare "Offers in-app purchases"
  (the `kinetic_pro` auto-renewing subscription) under Play Console's Monetization setup —
  see `DATA-SAFETY.md` and `CONTENT-RATING.md` for the corresponding Data Safety and
  content-rating declarations.
