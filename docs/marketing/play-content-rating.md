# Google Play Console — Content Rating Questionnaire (IARC)

Recommended answers for **Play Console → Policy → App content → Content ratings**, for
Kinetic AI Coach (`com.aistudio.aicoach.vtzrkm`), Health & Fitness category.

**Expected outcome: Everyone (ESRB) / PEGI 3 / USK 0 / equivalent "all ages" rating across
IARC territories.** Kinetic AI Coach is a fitness-coaching app with camera-based pose
tracking and an AI chat coach — it contains no violence, sexual content, gambling,
controlled substances, or user-generated social content that would trigger a higher
rating tier.

**⚠️ Before submitting:** the IARC questionnaire is a legal self-certification. Re-confirm
each answer against the actual shipped build (especially the AI chat feature — see Section
4 below on unmoderated user-generated/AI content) before submitting, since a wrong answer
can result in the app being taken down or re-rated by Google/IARC after the fact.

---

## 1. Violence

| Question | Answer | Rationale |
|---|---|---|
| Does the app contain violent content, or references to violence? | **No** | The app is a fitness-coaching tool (squats, pushups, general exercise form/rep tracking). No combat, weapons, or violent imagery/audio of any kind. |
| Realistic or cartoon violence, blood/gore | **No** | Not present. |

## 2. Sexual content and nudity

| Question | Answer | Rationale |
|---|---|---|
| Does the app contain nudity or sexual content? | **No** | The camera feature is used for exercise pose tracking (fitness form analysis), not for content generation of a sexual/nude nature. Standard workout attire is expected and no explicit content is generated or displayed by the app itself. |
| Suggestive themes | **No** | Not present in app-authored content. |

## 3. Language

| Question | Answer | Rationale |
|---|---|---|
| Does the app contain profanity or crude humor? | **No** | All AI-generated coaching copy (form feedback, chat responses) is professional/instructional in tone; no app-authored profanity. |

## 4. Controlled substances

| Question | Answer | Rationale |
|---|---|---|
| References to alcohol, tobacco, or drugs | **No** | Not present; app scope is exercise coaching only. |

## 5. Gambling

| Question | Answer | Rationale |
|---|---|---|
| Does the app offer simulated or real-money gambling? | **No** | No gambling mechanics of any kind. Monetization is a standard auto-renewing subscription (3-day trial → Kinetic Pro), billed through Google Play — not gambling/loot mechanics. |

## 6. User-generated content / unmoderated communication

This is the section most relevant to Kinetic AI Coach because of the **conversational AI
coach chat** (users can send text, photos, video, and files; the AI responds).

| Question | Answer | Rationale |
|---|---|---|
| Does the app include user-generated content shared with other users? | **No** | Chat is 1:1 between the user and the AI coach. There is no social feed, no user-to-user messaging, no public posting, and no other user ever sees another user's chat, photos, video, or workout data. |
| Does the app include unmoderated user-to-user communication? | **No** | No user-to-user communication exists in the app at all — the only "conversation" is user-to-AI. |
| Does the app allow users to interact with an AI/chatbot that generates content? | **Yes** | Disclose the AI chat coach and AI-generated workout programs/form feedback honestly wherever the questionnaire has an AI-content-disclosure field. The content is instructional/coaching in nature and scoped by the app's system prompt to fitness coaching, not open-ended/unrestricted chat. |

> **Recommendation:** if Play's IARC flow (or a separate Play policy declaration) asks
> specifically about "apps with AI-generated content" or "chatbot" functionality, answer
> **Yes** and describe the scope truthfully (fitness coaching Q&A + photo/video feedback,
> not a general-purpose/open-domain chatbot). This does not change the expected
> Everyone/PEGI 3 age rating on its own, but it is a separate, mandatory disclosure from
> the violence/sexual-content/gambling questions above — do not skip it.

## 7. Miscellaneous / shares location, personal info

| Question | Answer | Rationale |
|---|---|---|
| Does the app share the user's location? | **No** | The app does not request or use device location (see Data Safety form — no location data type declared). |
| Does the app allow purchase of digital goods? | **Yes** | Kinetic Pro subscription (3-day free trial, then recurring paid subscription) is billed via Google Play Billing — declare as in-app purchases/subscription in the relevant field. This affects the "In-app purchases" listing badge, not the age-rating tier itself. |

---

## Expected rating outcome by territory (informational — Google/IARC computes the final rating)

| Rating body | Expected rating |
|---|---|
| ESRB (US/Canada) | Everyone |
| PEGI (Europe) | PEGI 3 |
| USK (Germany) | USK 0 |
| Classind (Brazil) | Livre (L) |
| ACB (Australia) | G |
| GSRR (Taiwan) | Suitable for all ages |

These are the standard "no objectionable content" outcomes IARC assigns to fitness/utility
apps with no violence, sexual content, profanity, drugs, or gambling, and with
one-to-one (not social/public) AI chat. Google Play computes and assigns the final rating
automatically from the questionnaire answers — this table is guidance for what to expect,
not something to enter manually.

---

## Notes

- **Health & Fitness category apps are not automatically low-rated** — the rating depends
  entirely on the questionnaire answers above, not the category. Answer accurately even
  though the expected result is favorable.
- **Re-take the questionnaire if app content changes materially** — e.g., if a future
  version adds social features, user-to-user sharing of workout videos, or an
  unrestricted/general-purpose chat mode, these answers must be revisited and likely
  re-submitted, since they could change the "unmoderated user-to-user communication" or
  "user-generated content shared with other users" answers above.
- Play requires the content rating questionnaire to be completed (and a rating obtained)
  before an app can be published to Production, even when no objectionable content is
  expected — this is a mandatory gate, not optional metadata.
