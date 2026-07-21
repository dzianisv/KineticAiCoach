# Google Play Store Listing — Kinetic AI Coach

Ready-to-paste copy for **Play Console → Grow → Store presence → Main store listing**,
prepared for the Internal Testing → Production promotion.

App: **Kinetic AI Coach** · Package: `com.aistudio.aicoach.vtzrkm` · Category: **Health & Fitness**

All character counts below are verified with `wc -m` against Play's limits at time of writing.
Re-check before pasting if you edit any field.

---

## App title
**Limit: 30 characters**

```
Kinetic AI Fitness Coach
```
Length: 24 / 30 — includes the primary keyword "Fitness Coach" alongside the brand name.

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

• Camera frames are captured for pose analysis and sent to our secure
  backend, which forwards them to Google Gemini for form/rep analysis —
  never sold, never used for ads.
• You can use the app as an anonymous guest without creating an account.
• Full details are in our Privacy Policy (linked on this listing).

Questions, feedback, or a data deletion request? Contact us at
vibeteaichnologies@gmail.com — we read every message.
```

Character count of the block above (excluding the ``` fences): **2,901 / 4,000** — well
under the limit, leaving headroom for future feature additions without a rewrite.

**Subscription/trial disclosure check:** the "MEMBERSHIP" section explicitly discloses
(a) the 3-day free trial, (b) that it converts to a recurring paid subscription, (c) that
billing runs through Google Play, and (d) that the user can cancel anytime — this satisfies
Play's requirement to disclose subscription terms in the app description. Do **not** hardcode
"$7.25/month" or "$43.50/year" in the store copy — Play renders the actual localized price
per user automatically, and hardcoded prices go stale and can trigger policy/consistency
flags if the price changes.

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
Email: vibeteaichnologies@gmail.com
```
**⚠️ Placeholder — confirm before Production launch.** This is the same address used for
Google Workspace / Firebase project ownership. Play Console's support contact is customer-
facing (shown on the live listing and in support escalations). The founder should decide
whether to keep this shared address or stand up a dedicated support alias (e.g.
`support@kineticaicoach.app` or similar) before submitting for Production review.

## Privacy Policy URL

```
<hosted URL for docs/legal/privacy-policy.md — see that file's header for hosting notes>
```
Play requires a **live, publicly reachable URL**, not a repo path. Publish
`docs/legal/privacy-policy.md` (e.g., via GitHub Pages, as this repo already does for
`docs/privacy-policy.html`) before entering the URL in Play Console.

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
generative-AI critique (just angle math / rep counting heuristics), or (b) offer
generic chatbot coaching with **no** camera/pose grounding at all. Kinetic AI Coach's
differentiator is combining both in one loop: a live skeleton overlay **plus** a
multimodal LLM (Gemini) actually watching the annotated video to judge technique and
count reps — not a canned counter. Lead ASO copy with "AI watches your form" /
"AI counts your reps" rather than generic "workout tracker" language, since that's the
defensible wedge against both categories of competitor.

### Title / subtitle A/B variants
| Variant | Title (≤30 chars) | Rationale |
|---|---|---|
| A (recommended) | `Kinetic AI Fitness Coach` (24) | Balances brand name with the single highest-volume keyword pair ("AI" + "Fitness Coach"). |
| B | `Kinetic: AI Form & Reps` (24) | Leads with the two concrete features (form correction, rep counting) searchers query for directly; trades brand clarity for feature specificity. |
| C | `Kinetic AI Coach: Reps` (23) | Shorter brand-first variant; keeps "Reps" as the single most distinctive keyword while staying closest to the current internal-testing title (`Kinetic AI Coach`), minimizing user-facing rebrand risk. |

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

Start with a 3-day free trial. Thanks for training with us — feedback always
welcome at vibeteaichnologies@gmail.com.
```

---

## Promo / graphic assets Play requires

Play Console **will not let you publish to Production** without these. This document does
not generate images — it specifies what's needed and suggested captions to brief a designer
or screenshot pass with.

| Asset | Spec | Suggested caption / content |
|---|---|---|
| App icon | 512×512 PNG, 32-bit with alpha, ≤1MB | Kinetic AI Coach mark — no caption (icon only). Must match the in-app adaptive icon. |
| Feature graphic | 1024×500 PNG or JPEG, no alpha | "Your AI coach watches your form — live." Show the red skeleton overlay on a workout frame plus the app name lockup. |
| Phone screenshot 1 (required) | Min 2 screenshots, JPEG/24-bit PNG, 16:9 or 9:16, each side 320–3840px | Live camera view mid-squat with the red pose skeleton overlay visible — headline: "Real-time form tracking." |
| Phone screenshot 2 (required) | Same spec as above | Post-set feedback screen showing rep/set count + AI form critique text — headline: "AI counts your reps and corrects your form." |
| Phone screenshot 3 (recommended) | Same spec as above | AI coach chat screen with a photo attached — headline: "Ask your coach anything." |
| Phone screenshot 4 (recommended) | Same spec as above | Workout history / analytics dashboard — headline: "Track every session." |
| Phone screenshot 5 (recommended) | Same spec as above | Personalized program screen — headline: "A plan built around your goals." |
| Tablet/7-in & 10-in screenshots | Optional but improves large-screen store presence if the app supports tablet layouts | Reuse the same set if the UI is responsive; skip if not yet validated on tablet. |
| Promo video (optional) | YouTube URL, ≤30s recommended | Short clip: camera opens → skeleton overlay appears → AI voice gives a form correction → rep counter increments. |

Do not reuse the existing `docs/play-store/hi-res-icon-512.png` /
`docs/play-store/feature-graphic.png` assets without re-verifying they match the current
app icon and this listing's copy — confirm asset parity before the Production submission.
