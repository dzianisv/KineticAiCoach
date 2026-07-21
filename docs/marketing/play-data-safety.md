# Google Play Console — Data Safety Form Answers

Ready-to-use answers for **Play Console → Policy → App content → Data safety**, for the
Internal Testing → Production promotion of Kinetic AI Coach (`com.aistudio.aicoach.vtzrkm`).

These answers reflect the app's actual data flows as described in the product/engineering
brief for this release:
- Camera frames are captured for pose analysis and sent to the app's own Cloud Function
  proxy, which forwards them to **Google Gemini (Vertex AI)** for rep counting and form
  feedback.
- Account email is collected only via Google Sign-In (guests provide none).
- Workout activity is stored in **Firebase Firestore**.
- **Firebase Analytics** and **Firebase Crashlytics** are used for product analytics and
  crash reporting.

**⚠️ Before submitting: re-verify every row below against the current codebase**
(`AndroidManifest.xml` permissions, the network/proxy client, Firestore sync code, and the
Firebase dependency list in `app/build.gradle.kts`). Play's Data Safety form is a legal
declaration to Google and to users — it must match what the shipped build actually does at
submission time, not just this document.

---

## Does your app collect or share any of the required user data types?

**Yes.**

## Is all of the user data collected by your app encrypted in transit?

**Yes** — all network traffic (auth, Firestore sync, and the Cloud Function → Gemini/Vertex
AI proxy path) uses HTTPS/TLS.

## Do you provide a way for users to request that their data be deleted?

**Yes** — via the support contact email listed in the store listing and privacy policy
(`vibeteaichnologies@gmail.com`).

---

## Data type declarations

### Personal info

| Data type | Collected? | Shared? | Purpose(s) | Encrypted in transit? | User can request deletion? |
|---|---|---|---|---|---|
| **Email address** | Yes (Google Sign-In users only; guests do not provide one) | No — not shared with third parties beyond the Firebase/Google infrastructure we use to operate the app (processed as a service provider, not a data sale) | Account management, App functionality | Yes | Yes |
| Name | Yes (Google Sign-In users only) | No | Account management, App functionality (personalization) | Yes | Yes |
| User IDs | Yes (Firebase Auth UID, including anonymous guest UID) | No | Account management, App functionality | Yes | Yes |

### Health and fitness

| Data type | Collected? | Shared? | Purpose(s) | Encrypted in transit? | User can request deletion? |
|---|---|---|---|---|---|
| **Health info / fitness activity** (exercises, sets, reps, workout history, AI form feedback, AI-generated programs) | Yes | **Yes — with Google Gemini/Vertex AI** for generating rep counts and technique feedback (declare as "shared with a service provider" for App functionality, not sold, not used for advertising) | App functionality (progress tracking, coaching, personalized programs) | Yes | Yes |

### Photos and videos

| Data type | Collected? | Shared? | Purpose(s) | Encrypted in transit? | User can request deletion? |
|---|---|---|---|---|---|
| **Photos or videos** (camera frames captured during a workout session, pose-annotated and sent for AI analysis; also any photos/video/files a user attaches in coach chat) | Yes | **Yes — with a service provider** (Google Gemini / Vertex AI) | App functionality: pose/rep/form analysis, conversational coach feedback | Yes | Yes |

Declare the handling detail as:
- "Data is processed ephemerally" if the backend does not persist the image/video payload
  beyond generating the response (confirm current Cloud Function logging behavior before
  checking this box).
- **Not** used for advertising or account personalization outside the app.
- **Not** sold to third parties.

### App activity

| Data type | Collected? | Shared? | Purpose(s) | Encrypted in transit? | User can request deletion? |
|---|---|---|---|---|---|
| App interactions (screens viewed, feature usage, session length — via Firebase Analytics) | Yes | Yes — with Google (Firebase Analytics, acting as our analytics service provider) | Analytics, App functionality | Yes | Yes (account-level deletion removes associated data where technically feasible; anonymized aggregate analytics may not be individually deletable — see note below) |
| In-app search / chat messages (coach chat text prompts) | Yes | Yes — sent to Google Gemini to generate the response | App functionality | Yes | Yes |

### App info and performance

| Data type | Collected? | Shared? | Purpose(s) | Encrypted in transit? | User can request deletion? |
|---|---|---|---|---|---|
| **Crash logs** (via Firebase Crashlytics) | Yes | Yes — with Google (Firebase Crashlytics, acting as our crash-reporting service provider) | Analytics, App functionality (bug fixing/stability) | Yes | Not typically deletable per-user (crash reports are diagnostic/aggregate by design); disclose this limitation to users on request |
| **Diagnostics** (performance data via Firebase Crashlytics/Analytics) | Yes | Yes — with Google | Analytics, App functionality | Yes | Same as above |
| Other app performance data | Yes | Yes — with Google | Analytics | Yes | Same as above |

### Device or other IDs

| Data type | Collected? | Shared? | Purpose(s) | Encrypted in transit? | User can request deletion? |
|---|---|---|---|---|---|
| Device or other identifiers (Firebase installation ID / analytics instance ID) | Yes | Yes — with Google (Firebase Analytics) | Analytics | Yes | Yes, where technically feasible (see note below) |

---

## Notes to carry into the Play Console UI

1. **"Shared" vs. "Processed by a service provider":** Play's form distinguishes data you
   *share with third parties* from data *processed by a service provider on your behalf*.
   Google Firebase (Auth, Firestore, Analytics, Crashlytics) and Google Cloud/Vertex AI
   (Gemini) should be declared under the **service provider** category for each relevant
   data type — check "This data is not shared with third parties for their own business
   purposes" alongside "Data is used by a service provider on your behalf" where the form
   offers that distinction, since we do not sell or share this data for Google's or
   anyone else's independent advertising/marketing use.
2. **Camera permission runtime disclosure:** because camera imagery is the app's core
   data flow and is sent off-device for AI analysis, ensure the in-app runtime permission
   prompt and any first-run consent/onboarding screen clearly state that camera video is
   analyzed by an AI service — this supports the Data Safety declaration and Play's
   "prominent disclosure" policy for sensitive permissions.
3. **Deletion limitations for aggregate/diagnostic data:** Crashlytics/Analytics data may
   be retained in de-identified, aggregate form even after an account deletion request,
   per Google's standard service terms. State this limitation plainly if a user asks, and
   reflect it in the Privacy Policy (see `docs/legal/privacy-policy.md`, Section 5).
4. **No ads SDK / no data sold:** confirm before submission that the app has no
   advertising SDK and does not sell data — if that remains true, answer "No" to Play's
   "Does your app share user data with third parties, as described in the ads/monetization
   section" style questions beyond the service-provider disclosures above.
5. **Re-verify before every submission.** This document is a starting point tied to the
   feature set described for this release (camera pose analysis via Gemini, Firestore
   workout storage, Firebase Analytics/Crashlytics, Google Sign-In + anonymous guest). If
   engineering adds new SDKs, new data flows, or removes any of the above before the
   Production submission, update this document and the live Play Console form together.
