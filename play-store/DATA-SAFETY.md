# Play Console — Data Safety Form Answers

This is a code-verified draft for **Play Console → Policy → App content → Data safety**.
It reflects what the app in this repo actually does today (checked against
`app/src/main/AndroidManifest.xml`, `app/src/main/java/com/example/network/GeminiApiClient.kt`,
`app/src/main/java/com/example/data/FirestoreSync.kt`, `app/src/main/java/com/example/data/Entities.kt`,
`app/src/main/java/com/example/ui/screens/LoginScreen.kt`, `app/src/main/java/com/example/billing/BillingConfig.kt`,
and `app/build.gradle.kts` on `main` @ `ee8c9ce`) — not aspirational marketing copy.
**Re-verify against the code before every submission**, especially if auth,
analytics, billing, or the proxy payload change.

## Verified facts driving these answers

| Fact | Source |
|---|---|
| Permissions requested: `INTERNET`, `CAMERA` only. No storage, location, contacts, mic-record permission declared. | `app/src/main/AndroidManifest.xml` |
| Sign-in: Google Sign-In (email + display name) or Firebase Anonymous ("guest"). | `LoginScreen.kt` (`GetGoogleIdOption`, `signInAnonymously()`) |
| Camera frames are pose-annotated **on-device**, tiled into a montage, base64-encoded, and POSTed with a Firebase ID token over **HTTPS** to a Firebase Cloud Function (`geminiProxy`), which forwards them to Google's Gemini model (Vertex AI) for rep counting / form tips. | `GeminiApiClient.kt` (`BASE_URL = "https://generativelanguage.googleapis.com/"`, `FirebaseProxyService.callProxy`, `imageBase64` field), README architecture diagram |
| No raw camera image/video is persisted locally — Room only stores structured data: profile (name, email, height, weight, goals, XP, streak), `workout_sessions` (exercise, reps, sets, form score, feedback text), `chat_messages` (role + text), leaderboard, badges. | `app/src/main/java/com/example/data/Entities.kt` |
| Firestore mirrors the same structured data (profile + `workoutSessions` subcollection) per signed-in `uid`, for cross-device sync. Guest/anonymous users still get a Firebase `uid`, so anonymous data can sync too until the app is uninstalled or the user signs out. | `FirestoreSync.kt` |
| **Firebase Analytics** (`firebase-analytics`) and **Firebase Crashlytics** (`firebase-crashlytics` + the Crashlytics Gradle plugin) are both present as app dependencies — they collect app-usage/interaction events and crash/diagnostic data respectively. | `app/build.gradle.kts` (`implementation(libs.firebase.analytics)`, `implementation(libs.firebase.crashlytics)`, `alias(libs.plugins.firebase.crashlytics)`) |
| **Google Play Billing is integrated** (`billing-ktx` 9.1.0) to sell the `kinetic_pro` auto-renewing subscription (monthly $7.25 / yearly $43.50 base plans), preceded by a 3-day app-side free trial with no card required upfront. | `app/build.gradle.kts` (`implementation(libs.billing.ktx)`), `app/src/main/java/com/example/billing/BillingConfig.kt` (`PRODUCT_ID_PRO = "kinetic_pro"`, monthly/yearly base plan pricing comments) |

---

## Data types collected

### Personal info
| Data type | Collected? | Shared? | Purpose | Optional? |
|---|---|---|---|---|
| Name | Yes | No | App functionality (personalizes coaching, shown in profile) | Yes — guests skip this |
| Email address | Yes | No | Account management (Google Sign-In identity, Firestore sync) | Yes — guests skip this |
| User IDs | Yes (Firebase `uid`, incl. anonymous guest UID) | No | App functionality, account management | No (required to sync/save progress) |

> Note: none of this personal info is sent to the AI proxy — only the workout
> chat prompt text and camera-frame montage are sent for analysis. Name/email
> only flow to Firebase Auth and Firestore (Google-operated infrastructure
> acting as our backend, not a data sale).

### Financial info
| Data type | Collected? | Shared? | Purpose | Optional? |
|---|---|---|---|---|
| Purchase history (Kinetic Pro trial/subscription status: active, trial, cancelled) | Yes | Yes — with Google Play Billing (the payment processor) | App functionality, account management (unlocking Kinetic Pro features) | No — required to manage the subscription entitlement |

> Note: the app does **not** collect or process raw payment card/instrument
> details itself — Google Play Billing handles the actual payment method and
> transaction directly. We only receive the resulting subscription/purchase
> status via the Billing Library.

### Photos and videos
| Data type | Collected? | Shared? | Purpose | Optional? |
|---|---|---|---|---|
| Photos or videos (camera frames) | Yes | **Yes — with a service provider** | App functionality: pose/rep analysis and form-tip generation | Camera use is required for the core AI form-tracking feature; the app does not currently offer an equivalent camera-free mode |

Declare this as: **"Shared with a third/service-provider party"** →
purpose **"App functionality"**, processed **ephemerally** (ML/AI feature),
**not** used for advertising, **not** sold. In the Play form's "Data
handling" screen, also check:
- "Data is processed ephemerally" — the backend forwards each request to
  Gemini and does not persist the image afterward (per current
  implementation; if the Cloud Function logging config changes to persist
  request bodies, update this answer).
- "Users can request that data be deleted" — Yes, via support email.

### App activity
| Data type | Collected? | Shared? | Purpose | Optional? |
|---|---|---|---|---|
| App interactions (workout sessions: exercise, reps, sets, form score, timestamps) | Yes | No | App functionality (progress tracking, analytics tab, leaderboard), Account management | No — this is the core value of the app |
| In-app search / chat messages (text prompts to "Coach Iron") | Yes | Yes — sent to Gemini for the response, same as the camera analysis path | App functionality | Yes — chat is optional |
| App interactions (screens viewed, feature usage, session length — via Firebase Analytics) | Yes | Yes — with Google (Firebase Analytics, acting as our analytics service provider) | Analytics, App functionality | Yes (declare as required for basic app operation only if analytics cannot be disabled independently of app use; otherwise mark optional if a future opt-out is added) |

### Health and fitness
| Data type | Collected? | Shared? | Purpose | Optional? |
|---|---|---|---|---|
| Fitness info (height, weight, workout goals, reps/sets/streak/XP) | Yes | No (stored in Firebase Auth/Firestore only; also sent as text context to the AI chat/proxy for personalization) | App functionality | Yes — profile fields are optional; app still works with defaults |

### App info and performance
| Data type | Collected? | Shared? | Purpose | Optional? |
|---|---|---|---|---|
| Crash logs (via Firebase Crashlytics) | Yes | Yes — with Google (Firebase Crashlytics, acting as our crash-reporting service provider) | Analytics, App functionality (bug fixing/stability) | No — collected automatically to keep the app stable; not user-toggleable in the current build |
| Diagnostics / other performance data (via Firebase Crashlytics & Analytics) | Yes | Yes — with Google | Analytics | No |

### Device or other identifiers
| Data type | Collected? | Shared? | Purpose | Optional? |
|---|---|---|---|---|
| Device or other IDs (Firebase installation ID / analytics instance ID, used by Firebase Analytics & Crashlytics) | Yes | Yes — with Google (Firebase Analytics/Crashlytics) | Analytics | No |

---

## Security practices

- **Data is encrypted in transit.** All network calls use HTTPS
  (`https://generativelanguage.googleapis.com/`, Firebase Cloud Functions
  default to HTTPS, Firebase Auth/Firestore SDKs use TLS, Google Play
  Billing traffic is handled by the Play Billing Library over Google's
  secured channels).
- **You can request data deletion.** There is no self-serve in-app delete
  button today — declare deletion as available **via a request to
  support@agentlabs.cc**, and answer "Yes" to "You provide a way for users
  to request that their data be deleted" in the form. Note: Firebase
  Analytics/Crashlytics data may persist in de-identified, aggregate form
  per Google's standard retention even after an account-level deletion
  request — disclose this limitation if asked.
- **Committed to the Play Families Policy?** No — this is a general
  audience Health & Fitness app, not designed or marketed to children.
  Answer "No" unless the app is later positioned for children.
- **Independent security review?** No third-party security audit has been
  performed. Answer "No".

## Data sale
The app does **not** sell personal data to third parties. Data sent to
Google's Gemini model, Firebase Analytics/Crashlytics, and Google Play
Billing is solely to deliver core app functionality (rep/form analysis,
crash/usage diagnostics, and subscription management — all "service
provider" data flows, not sales) — declare "No" to "Does your app sell
user data?".

## What to fill in the Play Console UI, step by step

1. **Does your app collect or share any of the required user data types?** → Yes
2. Data types to check: **Personal info → Name, Email address, User IDs**;
   **Financial info → Purchase history**; **Photos and videos → Photos or
   videos**; **App activity → App interactions, In-app search history**
   (map "chat messages" here); **Health and fitness → Health info** (map
   height/weight/goals here); **App info and performance → Crash logs,
   Diagnostics**; **Device or other IDs → Device or other IDs**.
3. For each: mark **Collected = Yes**; mark **Shared = Yes** for *Photos/
   videos*, *chat/app-activity text* (both go to the Gemini model via our
   proxy), *Purchase history* (shared with Google Play Billing), and the
   *Firebase Analytics/Crashlytics* rows (App interactions, Crash logs,
   Diagnostics, Device or other IDs — all shared with Google as our
   analytics/crash-reporting service provider) — mark core Personal info
   and structured fitness stats as **Shared = No** (they stay in Firebase
   Auth/Firestore, which Play does not count as third-party sharing for
   your own backend).
4. Purpose for most rows: **App functionality**. Additionally check
   **Analytics** for the Firebase Analytics/Crashlytics rows. Do not check
   Advertising or Personalization for any row — this app has no ads SDK.
5. Answer **"Is this data processed ephemerally?"** = Yes for the
   photos/videos row only.
6. Answer **"Is data collection optional?"** = Yes for Name, Email, chat
   messages, and fitness profile fields (guests can skip all of these);
   No for User IDs, workout-session app activity, purchase history, and
   the Firebase Analytics/Crashlytics rows (all collected automatically as
   part of core app operation in the current build).
7. Encryption in transit: **Yes**, for every row.
8. Deletion request mechanism: **Yes** → describe as "Users can request
   deletion by emailing support@agentlabs.cc". Note the Analytics/
   Crashlytics retention caveat above where the form allows free text.
9. **In-app purchases / subscriptions:** declare the `kinetic_pro`
   auto-renewing subscription (monthly and yearly base plans, preceded by
   a 3-day free trial) under Play Console's Monetization setup — this is
   separate from, but referenced by, the Financial info → Purchase history
   row above.

## Known gap to close before shipping

There is currently no **in-app** "Delete my account/data" flow — only an
email-based process. If time allows before submission, either (a) add a
simple in-app deletion request flow, or (b) at minimum make sure
`support@agentlabs.cc` is monitored and can action deletion requests
(Firebase Auth user delete + Firestore document delete) promptly, since
Play's data-safety form and the privacy policy both promise this path.
