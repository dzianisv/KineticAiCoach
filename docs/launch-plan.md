# Kinetic AI Fitness Coach — Organic Launch Playbook

Zero paid spend. Run this the day the app is approved on Google Play (Production/live).
Until then it is in **early access** — frame everything as "now in early access," never "live/available."

- **App:** Kinetic AI Fitness Coach
- **Play Store:** https://play.google.com/store/apps/details?id=com.aistudio.aicoach.vtzrkm
- **Landing page:** https://dzianisv.github.io/KineticAiCoach/
- **Support:** support@agentlabs.cc
- **The wedge:** on-device pose detection (live skeleton overlay) **+** Google Gemini actually watching the annotated video to count reps and critique form. Most competitors do one or the other, not both.
- **Hard rules:** no medical claims, no "diagnose/treat/cure," no guarantees, no fake stats/testimonials, no hardcoded price (Play renders localized pricing).

---

## 1. Prioritized channel list

| # | Channel | Why (tailored to a camera pose + Gemini AI form coach) |
|---|---------|--------------------------------------------------------|
| 1 | **r/bodyweightfitness** | Core audience — trains at home with no equipment; a camera-based form checker + rep counter is exactly their unmet need. Strict self-promo rules — read them first. |
| 2 | **Show HN (Hacker News)** | Technical audience rewards the on-device-pose + Gemini-backend architecture story; honest "early access / in review" framing lands well here. |
| 3 | **Product Hunt** | Built for launches; AI + fitness is an evergreen category; drives an install spike and durable backlink. |
| 4 | **r/androidapps** | Android-first users who actively look for new apps to try; a genuine "I built this Android app" post fits the sub. |
| 5 | **r/QuantifiedSelf** | Self-tracking crowd loves rep/set/form analytics and on-device data; privacy-by-design angle resonates. |
| 6 | **r/homegym** | Home lifters who lose count and want a spotter for form; automatic rep counting is the hook. |
| 7 | **r/fitness** | Huge reach, but **very strict** self-promo rules — only post if their rules allow it (often only in a weekly self-promo/"what apps do you use" thread). Check first; a wrong post = instant ban. |
| 8 | **Fitness Discord servers / Facebook groups** | Tight-knit communities; share as a helpful member after participating, not as a drive-by link. |
| 9 | **X/Twitter** | Build-in-public thread + short demo clip; cheap, repeatable, tags #buildinpublic #AndroidDev #fitnesstech. |
| 10 | **TikTok / Instagram Reels / YouTube Shorts** | Short vertical demo clip of the skeleton overlay counting reps is inherently visual and shareable — highest organic upside once a video asset exists. |

---

## 2. Ready-to-paste post copy per channel

> Adjust the `[approved / live]` wording to match reality at post time. During review keep "early access."

### Reddit — Post A: r/bodyweightfitness

**Title:** I built an Android app that uses your phone camera to count reps and check your form (on-device pose tracking + AI) — early access, would love feedback

**Body:**
```
I train at home and kept losing count mid-set and had no way to check my form without filming myself and reviewing later. So I built an app to fix my own problem.

How it works: it runs pose detection on-device and draws a live skeleton over your camera feed, then sends the annotated video (encrypted, through my own backend) to Google Gemini to count your reps/sets and give specific form feedback — spoken aloud so you don't have to look at the screen mid-set.

Honest caveats:
- It's a training and form-feedback tool, not a medical device — it doesn't diagnose anything or replace a trainer/PT.
- It's just launched in early access on Google Play, so there will be rough edges. Pose tracking runs on-device; the AI feedback needs internet.
- Free to start with a trial; the advanced coaching is a subscription later (Play handles billing, cancel anytime). No account needed — guest mode works.

I'm mainly here for feedback: what exercises should I prioritize for rep counting accuracy? Happy to answer anything technical. Link in a comment if the mods allow, otherwise DM me.
```

### Reddit — Post B: r/androidapps

**Title:** [App] Kinetic AI Fitness Coach — camera-based rep counter + AI form feedback, now in early access (Android 7.0+)

**Body:**
```
Solo dev here. I built an Android fitness app that uses the phone camera instead of a wearable.

- On-device pose detection with a live skeleton overlay (no wearable needed)
- AI rep/set counting + form critique via Google Gemini through the app's backend
- Spoken coaching via text-to-speech so you keep your hands free
- Personalized AI-generated workout programs and a chat coach you can send photos/video to
- Guest mode — no signup required — or sign in with Google to sync history

It's in early access on Google Play right now, so I'm looking for real-world feedback on device compatibility and rep-counting accuracy. Pose tracking is on-device; the AI parts need a connection. It's a form-feedback tool, not medical advice.

Store link in the comments (per sub rules). Anything you'd want a camera-based coach to do that it doesn't yet?
```

### Product Hunt

**Tagline (short):**
```
Your phone camera as an AI personal trainer — counts reps, corrects form.
```

**Description draft:**
```
Kinetic AI Fitness Coach turns your phone camera into a form checker and rep counter. On-device pose detection draws a live skeleton over your camera feed; the annotated video is analyzed by Google Gemini (through the app's own secure backend) to count your sets and reps and critique your technique in real time — spoken aloud so you never break form to check the screen.

- Real-time pose detection with live skeleton overlay
- AI form correction + rep counting
- Spoken coaching (text-to-speech)
- AI-generated personalized workout programs
- Conversational AI coach — send photos/video for feedback
- Guest mode, no signup required

It's a training and form-feedback tool, not a medical device. Now in early access on Google Play (Android 7.0+). Free to start with a 3-day trial; Kinetic Pro is an optional subscription billed through Google Play, cancel anytime. Feedback goes straight to a solo dev: support@agentlabs.cc
```

### Show HN (Hacker News)

**Title (exact format):**
```
Show HN: Kinetic – On-device pose detection + Gemini to count reps and coach form
```

**First comment draft:**
```
I built this to solve my own home-workout problem: losing count mid-set and having no idea if my form was breaking down.

How it works technically:
- Pose detection runs on-device (phone camera → joint tracking), rendering a live skeleton overlay so you can see exactly what the app sees. This part works offline.
- For rep counting and form critique, the annotated video is sent over HTTPS to a Cloud Function proxy I run, which forwards it to Google Gemini (Vertex AI). The model counts sets/reps and returns specific, real-time form feedback, which is read aloud via text-to-speech. Nothing goes to advertisers; guest mode needs no account.
- Why a backend proxy instead of calling Gemini directly from the app: to keep API keys off-device and control cost/abuse.

Honest status: it's Android-only and currently in early access on Google Play (Open Testing → pending Production review), so expect rough edges. It's a form-feedback tool, not a medical device — it doesn't diagnose anything. Free to start with a trial; advanced coaching is a subscription (Play billing, cancel anytime).

The interesting hard part has been rep-counting accuracy across body types, camera angles, and lighting — the pose+LLM loop is more robust than pure angle-math heuristics but costs a round trip. Happy to go deep on any of it. Play link: https://play.google.com/store/apps/details?id=com.aistudio.aicoach.vtzrkm
```

### X / Twitter thread (3–5 tweets)

```
1/ I built an AI fitness coach that uses just your phone camera. Point it at yourself, and it counts your reps and checks your form in real time — spoken aloud so you don't break form to look at the screen. Now in early access on Google Play 🧵 #buildinpublic

2/ How: on-device pose detection draws a live skeleton over your camera feed (works offline). Then the annotated video goes — encrypted, through my own backend — to Google Gemini, which counts sets/reps and critiques your technique. Two layers, one loop.

3/ Why it's different: most "AI fitness" apps either do pose math with no real critique, or chatbot coaching with no camera grounding. This does both — a multimodal model actually watching your form, not a canned counter.

4/ Honest bits: it's a form-feedback tool, not a medical device. Android 7.0+. Guest mode, no signup. Free to start with a trial; Pro is an optional subscription (Play billing, cancel anytime).

5/ It's early access, so I want your feedback — especially on rep-counting accuracy across devices. Try it and tell me what breaks 👇
https://play.google.com/store/apps/details?id=com.aistudio.aicoach.vtzrkm
```

---

## 3. Two-week posting cadence

**Golden rules (avoid bans/removals):**
- **Read each community's self-promo rules before posting.** r/fitness and many large fitness subs restrict self-promo to weekly threads or ban it outright — a wrong post = instant ban.
- **Never cross-post identical copy the same day.** Rewrite per community; reuse the *story*, not the exact text.
- **Engage authentically** — comment/reply in the community for a few days before you post; after posting, answer every reply. Don't drop a link and leave.
- **Space submissions out** — one primary channel per day max; let each thread breathe.
- **Lead with value/story, not the store link.** Put the link in a comment where subreddit rules require it.

| Day | Action |
|-----|--------|
| **Day 0 (approval day)** | Flip landing page + all copy from "early access" to "live." Do the biggest single push: **Show HN** (mornings PT do well). Sit on it all day and answer every comment. |
| **Day 1** | **Product Hunt** launch (schedule for 12:01am PT). Rally any personal network for genuine engagement — no vote manipulation. Reply to every comment. |
| **Day 2** | Rest from big launches. **X/Twitter thread** (#buildinpublic). Start commenting genuinely in r/bodyweightfitness to build presence. |
| **Day 3** | **r/androidapps** post (Post B). Answer everything. |
| **Day 4** | Post the short demo clip (once created) to **TikTok / Reels / YouTube Shorts**. |
| **Day 5** | **r/homegym** post (adapt Post A around "losing count / spotter for form"). |
| **Day 6** | Engage-only day: reply to earlier threads, DMs, PH/HN comments. No new post. |
| **Day 7** | **r/bodyweightfitness** post (Post A) — your highest-value sub, so post here only after you've built presence and know the rules cold. |
| **Day 8** | Share in 1–2 **fitness Discord servers / Facebook groups** you're already active in. |
| **Day 9** | **r/QuantifiedSelf** post (lead with rep/set/form analytics + on-device privacy angle). |
| **Day 10** | Engage-only day. Compile early feedback; ship a small fix if warranted and mention it. |
| **Day 11** | **r/fitness** — **only** via their allowed self-promo/weekly thread if one exists; otherwise skip. Check rules first. |
| **Day 12** | Second **X/Twitter** post: share an early metric or a piece of user feedback (build-in-public momentum). |
| **Day 13** | Post a second short demo clip variation (different exercise) to Shorts/Reels/TikTok. |
| **Day 14** | Retrospective: fill in the tracking table, double down on the 1–2 channels that converted, drop the ones that didn't. |

---

## 4. Manual tracking table (no paid analytics)

| Channel | Date posted | Link | Signups/installs | Notes |
|---------|-------------|------|------------------|-------|
| Show HN | | | | |
| Product Hunt | | | | |
| r/androidapps | | | | |
| r/bodyweightfitness | | | | |
| r/homegym | | | | |
| X/Twitter | | | | |
| TikTok/Reels/Shorts | | | | |
| | | | | |
| | | | | |
| | | | | |

Track installs via **Google Play Console → Statistics** (free) and correlate spikes with post dates/times. No paid attribution tools.

---

## 5. Assets needed checklist

**Ready to reuse (already in the repo):**
- [x] 5 screenshots — `docs/play-store/screenshots/` (`01-coach-ai.png`, `02-workouts.png`, `03-todays-class.png`, `04-analytics.png`, `05-about.png`, 1080×2072)
- [x] Feature graphic — `docs/play-store/feature-graphic.png` (1024×500)
- [x] Hi-res icon — `docs/play-store/hi-res-icon-512.png` (512×512)
- [x] Landing page — https://dzianisv.github.io/KineticAiCoach/
- [x] Approved store copy — `docs/play-store/LISTING.md`

**Must create before launch:**
- [ ] **Demo video/GIF — DOES NOT EXIST YET.** No video file (e.g. `testlab_video.mp4`) exists anywhere in the repo. Record a **15–30s** screen recording (phone screen mirror or emulator capture) showing: camera pose overlay → live rep count → spoken form feedback. Needed for Product Hunt, Reddit, TikTok/Reels/Shorts, and the X thread. This is the single highest-leverage missing asset — the product is visual and the clip is what will actually convert.
- [ ] Vertical (9:16) cut of the demo for TikTok/Reels/Shorts.
- [ ] 1–2 caption/subtitle overlays on the clip (most social video is watched muted).
