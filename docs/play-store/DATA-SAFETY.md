# Play Console — Data Safety Form Answers

This is a code-verified draft for **Play Console → Policy → App content → Data safety**.
It reflects what the app in this repo actually does today (checked against
`app/src/main/AndroidManifest.xml`, `app/src/main/java/com/example/network/GeminiApiClient.kt`,
`app/src/main/java/com/example/data/FirestoreSync.kt`, `app/src/main/java/com/example/data/Entities.kt`,
and `app/src/main/java/com/example/ui/screens/LoginScreen.kt`) — not aspirational marketing copy.
**Re-verify against the code before every submission**, especially if auth,
analytics, or the proxy payload change.

## Verified facts driving these answers

| Fact | Source |
|---|---|
| Permissions requested: `INTERNET`, `CAMERA` only. No storage, location, contacts, mic-record permission declared. | `app/src/main/AndroidManifest.xml` |
| Sign-in: Google Sign-In (email + display name) or Firebase Anonymous ("guest"). | `LoginScreen.kt` (`GetGoogleIdOption`, `signInAnonymously()`) |
| Camera frames are pose-annotated **on-device**, tiled into a montage, base64-encoded, and POSTed with a Firebase ID token over **HTTPS** to a Firebase Cloud Function (`geminiProxy`), which forwards them to Google's Gemini model (Vertex AI) for rep counting / form tips. | `GeminiApiClient.kt` (`BASE_URL = "https://generativelanguage.googleapis.com/"`, `FirebaseProxyService.callProxy`, `imageBase64` field), README architecture diagram |
| No raw camera image/video is persisted locally — Room only stores structured data: profile (name, email, height, weight, goals, XP, streak), `workout_sessions` (exercise, reps, sets, form score, feedback text), `chat_messages` (role + text), leaderboard, badges. | `app/src/main/java/com/example/data/Entities.kt` |
| Firestore mirrors the same structured data (profile + `workoutSessions` subcollection) per signed-in `uid`, for cross-device sync. Guest/anonymous users still get a Firebase `uid`, so anonymous data can sync too until the app is uninstalled or the user signs out. | `FirestoreSync.kt` |
| No Analytics SDK, no Crashlytics, no Ads SDK, no third-party trackers found anywhere in the dependency list or code. | `app/build.gradle.kts` dependency block; repo-wide grep for `analytics`/`crashlytics`/`admob` returned no hits |
| No in-app purchases / billing library present. | `app/build.gradle.kts` |

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

### Health and fitness
| Data type | Collected? | Shared? | Purpose | Optional? |
|---|---|---|---|---|
| Fitness info (height, weight, workout goals, reps/sets/streak/XP) | Yes | No (stored in Firebase Auth/Firestore only; also sent as text context to the AI chat/proxy for personalization) | App functionality | Yes — profile fields are optional; app still works with defaults |

### Device or other identifiers
Not collected. No advertising ID, no device identifier collection found in
the codebase.

---

## Security practices

- **Data is encrypted in transit.** All network calls use HTTPS
  (`https://generativelanguage.googleapis.com/`, Firebase Cloud Functions
  default to HTTPS, Firebase Auth/Firestore SDKs use TLS).
- **You can request data deletion.** There is no self-serve in-app delete
  button today — declare deletion as available **via a request to
  support@agentlabs.cc**, and answer "Yes" to "You provide a way for users
  to request that their data be deleted" in the form.
- **Committed to the Play Families Policy?** No — this is a general
  audience Health & Fitness app, not designed or marketed to children.
  Answer "No" unless the app is later positioned for children.
- **Independent security review?** No third-party security audit has been
  performed. Answer "No".

## Data sale
The app does **not** sell personal data to third parties. Data sent to
Google's Gemini model is solely to deliver the core AI-coaching feature
(a required "service provider" data flow, not a sale) — declare "No" to
"Does your app sell user data?".

## What to fill in the Play Console UI, step by step

1. **Does your app collect or share any of the required user data types?** → Yes
2. Data types to check: **Personal info → Name, Email address, User IDs**;
   **Photos and videos → Photos or videos**; **App activity → App
   interactions, In-app search history** (map "chat messages" here);
   **Health and fitness → Health info** (map height/weight/goals here).
3. For each: mark **Collected = Yes**; mark **Shared = Yes** only for
   *Photos/videos* and *chat/app-activity text* (both go to the Gemini
   model via our proxy) — mark Personal info and structured fitness stats
   as **Shared = No** (they stay in Firebase, which Play does not count as
   third-party sharing for your own backend).
4. Purpose for all rows: **App functionality**. Do not check Advertising,
   Analytics, Fraud prevention, Personalization (unless you later add
   analytics), or Account management for rows where it doesn't apply.
5. Answer **"Is this data processed ephemerally?"** = Yes for the
   photos/videos row.
6. Answer **"Is data collection optional?"** = Yes for Name, Email, chat
   messages, and fitness profile fields (guests can skip all of these);
   No for User IDs and workout-session app activity (core function).
7. Encryption in transit: **Yes**.
8. Deletion request mechanism: **Yes** → describe as "Users can request
   deletion by emailing support@agentlabs.cc".

## Known gap to close before shipping

There is currently no **in-app** "Delete my account/data" flow — only an
email-based process. If time allows before submission, either (a) add a
simple in-app deletion request flow, or (b) at minimum make sure
`support@agentlabs.cc` is monitored and can action deletion requests
(Firebase Auth user delete + Firestore document delete) promptly, since
Play's data-safety form and the privacy policy both promise this path.
