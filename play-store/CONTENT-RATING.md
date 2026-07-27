# Play Console — Content Rating (IARC) Questionnaire — Draft Answers

Fill this out at **Play Console → Policy → App content → Content ratings**.
The IARC questionnaire wording varies slightly by category/version — the
answers below are organized by the standard IARC topic areas so you can map
them to whatever exact wording Play shows. Category: **Reference / Utility
→ Health & Fitness** app.

## Category selection
Choose the closest fit Play offers, typically:
```
Health & Fitness / Reference, News, or Education
```
(Not "Game" — this is a utility/coaching app.)

## Violence
| Question | Answer |
|---|---|
| Does the app contain depictions of violence? | No |
| Realistic or cartoon violence, blood, gore | No |
| Violence against children | No |

Rationale: the app shows a stick-figure pose skeleton overlay for exercise
form (squats, pushups) — not weapons, combat, or injury depictions.

## Sexual content / nudity
| Question | Answer |
|---|---|
| Nudity (any) | No |
| Sexually suggestive content | No |
| Graphic sexual content | No |

Rationale: camera view shows the user's own pose skeleton overlay for form
correction, not photorealistic body imagery designed for display to others;
no nudity or sexual content is part of the product.

## Profanity / crude humor
| Question | Answer |
|---|---|
| Profanity or crude humor | No |

Rationale: AI coach copy is motivational/instructional fitness language
("Perfect Squats technique!", "Give me squat tips"). No profanity is in the
UI strings or system prompts shipped in this repo.

## Controlled substances
| Question | Answer |
|---|---|
| References to alcohol, tobacco, or drugs | No |

## Gambling
| Question | Answer |
|---|---|
| Simulated gambling | No |
| Real-money gambling / facilitates gambling | No |

Rationale: the "Leaderboard" and "badges" features are point-based fitness
gamification (XP, streaks) — no wagering, no random reward mechanics tied
to real or virtual currency purchase.

## Miscellaneous / user interaction
| Question | Answer | Notes |
|---|---|---|
| Does the app share the user's location? | No | No location permission is requested (`AndroidManifest.xml` only declares `INTERNET` and `CAMERA`). |
| Does the app allow users to interact/communicate with each other (chat, share content)? | No | The only "chat" is a 1:1 conversation between the user and the AI coach (Gemini) — there is no user-to-user messaging, forum, or public content sharing. |
| Does the app allow purchase of digital goods? | **Yes** | The app sells the `kinetic_pro` auto-renewing subscription (monthly $7.25 / yearly $43.50 base plans) via Google Play Billing (`billing-ktx` dependency, `app/src/main/java/com/example/billing/BillingConfig.kt`), preceded by a 3-day free trial. Declare digital goods / in-app purchases = Yes and configure the subscription under Play Console's Monetization setup. |
| Does the app share personal information with third parties? | Yes, limited | Camera-frame montages and chat prompts are sent to Google's Gemini model via our backend proxy to generate coaching feedback; Firebase Analytics and Crashlytics also receive app-usage and crash/diagnostic data; Google Play Billing receives subscription/purchase status. Disclose all of this here and it will also appear in the Data Safety section. Declare "shared with third party for app functionality," not for advertising. |

## Expected outcome

Given the answers above, IARC should return a rating around **PEGI 3 / ESRB
Everyone / USK 0** — no age-gating content. The "sensitive" answers are the
third-party data sharing note and the digital-goods purchase (in-app
subscription) declared above — these affect the Data Safety and
Monetization/in-app-purchases sections and the "Offers in-app purchases"
store badge, not the age rating itself.

## Before submitting

- Re-run this questionnaire honestly inside Play Console — IARC ratings are
  legally binding per-region declarations, and this file is a starting
  draft, not a substitute for actually completing the live form.
- If future versions add a social/community feature (e.g., public
  leaderboards with user-generated names, comments, photo sharing), redo
  this questionnaire — the "user interaction" answers above would change.
