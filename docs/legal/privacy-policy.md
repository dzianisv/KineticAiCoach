# Privacy Policy — Kinetic AI Coach

**Effective date:** July 21, 2026

**Developer / data controller:** VIBE TECHNOLOGIES LLC ("we", "us", "our")
**App:** Kinetic AI Coach (Android package `com.aistudio.aicoach.vtzrkm`)
**Contact:** vibeteaichnologies@gmail.com

> **Hosting note:** this document must be published at a stable, publicly reachable HTTPS
> URL and that exact URL entered into Play Console's "Privacy Policy" field before
> submitting for Production review. It is written to stand alone — do not require repo
> access, login, or additional context to read it.

---

## 1. Introduction

Kinetic AI Coach is an AI-powered personal fitness coaching app. This Privacy Policy
explains what data we collect when you use the app, why we collect it, who we share it
with, how long we keep it, and the choices and rights you have over it. By using Kinetic
AI Coach, you agree to the collection and use of information as described here.

---

## 2. Data we collect

### 2.1 Camera imagery (live video / frames)
The app's core feature is real-time camera-based pose tracking. While you are actively
using a workout session:
- Your device camera captures a live video feed.
- On-device machine learning analyzes that feed to detect your body's pose and draws a
  skeleton/bones overlay on screen in real time.
- The pose-annotated video is sent from your device to our backend (a Google Cloud
  Function we operate) and forwarded to **Google Gemini (Vertex AI)** for analysis. This
  is how the app counts your sets and reps and generates spoken form/technique feedback.

We only capture and transmit camera imagery while you are actively engaged in a workout
or coaching session in the app; the app does not access your camera in the background or
outside of an active session.

### 2.2 Account information
- **Google Sign-In:** if you choose to sign in with Google, we receive your name, email
  address, and Google account identifier to create and manage your account.
- **Guest / anonymous mode:** you may also use Kinetic AI Coach without creating an
  account or providing any personal identifiers. In this mode we assign an anonymous
  device/session identifier (via Firebase Authentication's anonymous auth) so your
  workout data can still be saved to that identifier, but no name or email is collected.

### 2.3 Workout and fitness activity
We collect the fitness activity you generate in the app, including: exercises performed,
set and rep counts, workout duration and history, AI-generated form/technique feedback,
and AI-generated personalized workout programs. This data is stored in **Firebase
Firestore** under your account (or anonymous identifier) so your history and analytics
persist across sessions and, if signed in, across devices.

### 2.4 Coach chat content
If you use the conversational AI coach chat, we collect the text, photos, video, or files
you send, and the AI-generated responses, in order to provide that feature. Chat content
you submit for feedback is processed the same way as workout video — sent to our backend
and forwarded to Google Gemini for a response.

### 2.5 Analytics and diagnostic data
We use **Firebase Analytics** to understand feature usage and app engagement (e.g., which
screens are used, session length, general usage patterns) and **Firebase Crashlytics** to
collect crash reports and diagnostic/performance data so we can identify and fix bugs.
This data is associated with a device/installation identifier, not your name or email,
unless you are signed in with Google.

### 2.6 Data we do not collect
We do not request or access your device contacts, precise location, SMS/call logs,
microphone recordings outside of app-provided text-to-speech playback (which is
output-only — the app does not record your voice), or files on your device other than
those you explicitly choose to attach in the coach chat feature.

---

## 3. How we use your data

| Purpose | Data used |
|---|---|
| Real-time pose tracking, form correction, and rep/set counting | Camera imagery (processed on-device and via Google Gemini) |
| Generating personalized workout programs and coaching responses | Chat content, workout history |
| Saving and syncing your workout history and progress | Account identifier, workout/fitness activity |
| Account creation and sign-in | Name, email (Google Sign-In only) |
| Improving app quality, fixing bugs, understanding feature usage | Analytics and crash/diagnostic data |
| Billing and subscription management (Kinetic Pro) | Handled entirely by Google Play; we do not directly collect or store your payment card details |

We do not use your camera imagery, chat content, or workout data to serve advertising,
and we do not sell your personal data to third parties.

---

## 4. Third parties we share data with

We share data only with service providers who process it on our behalf to operate the
app's features, under their own data-processing and security terms:

| Third party | Data shared | Purpose |
|---|---|---|
| **Google Firebase** (Authentication, Firestore, Analytics, Crashlytics) | Account identifiers, email (if Google Sign-In), workout/fitness activity, analytics events, crash/diagnostic logs | Backend infrastructure: authentication, data storage/sync, product analytics, crash reporting |
| **Google Cloud / Vertex AI (Gemini)** | Camera-derived pose-annotated video/images, coach chat text/photos/video/files | AI analysis: rep/set counting, form and technique feedback, conversational coaching responses, workout program generation |
| **Google Play (Google LLC)** | Subscription/purchase status | Processing your Kinetic Pro subscription and free trial; Google Play handles your payment details directly — we do not receive or store your card information |

All data in transit between the app and our backend, and between our backend and Google
Cloud/Vertex AI, is encrypted (HTTPS/TLS).

We do not share your data with data brokers, advertising networks, or any party for
their own independent marketing purposes.

---

## 5. Data retention and deletion

- **Workout history, chat content, and account data** are retained for as long as you
  maintain an active account, so you can view your history and progress over time.
- **Camera imagery** sent for pose/rep analysis is used to generate the corresponding
  in-app response (rep counts, form feedback) and is not retained by us for any purpose
  beyond producing that response and standard, time-limited backend request logging used
  for security and abuse prevention.
- **Analytics and crash data** are retained by Firebase Analytics and Crashlytics
  according to Google's standard retention periods for those services.
- **You can request deletion** of your account and associated data at any time by
  contacting us at **vibeteaichnologies@gmail.com**. We will delete your account data
  (profile, workout history, chat history) within a reasonable time after verifying your
  request. If you used the app only as an anonymous guest, uninstalling the app removes
  the local association to your anonymous identifier; you may still request deletion of
  any server-side data tied to that identifier by contacting us with the relevant device
  or session details you have available.

---

## 6. Children's privacy

Kinetic AI Coach is not directed to children under the age of 13, and we do not knowingly
collect personal information from children under 13. If you believe a child under 13 has
provided us with personal information, please contact us at
**vibeteaichnologies@gmail.com** and we will take steps to delete such information.

---

## 7. Your choices and rights

- You may use Kinetic AI Coach as an anonymous guest to avoid providing your name or
  email.
- You may sign out or delete your account at any time, and may request deletion of your
  data as described in Section 5.
- Depending on your jurisdiction, you may have additional rights (such as access,
  correction, portability, or objection to processing) under applicable data protection
  law. Contact us at vibeteaichnologies@gmail.com to exercise these rights.
- Subscription management (including cancellation of the free trial or Kinetic Pro
  subscription) is handled through your Google Play account settings.

---

## 8. Data security

We rely on Google Firebase and Google Cloud infrastructure, which provide encryption in
transit (HTTPS/TLS) for all data sent between the app, our backend, and Google's AI
services. Access to backend systems and data is restricted to what is necessary to
operate the app. No method of transmission or storage is 100% secure, and we cannot
guarantee absolute security.

---

## 9. Changes to this policy

We may update this Privacy Policy from time to time to reflect changes in the app, our
practices, or legal requirements. We will update the "Effective date" above when changes
are made. Continued use of the app after an update constitutes acceptance of the revised
policy.

---

## 10. Contact us

If you have questions about this Privacy Policy, want to exercise a data right, or wish
to request deletion of your data, contact us at:

**Email:** vibeteaichnologies@gmail.com
**Developer:** VIBE TECHNOLOGIES LLC

> **Note for the founder:** the contact email above is a placeholder shared with other
> project/Workspace administration. Confirm before Production launch whether to keep
> this address as the public-facing privacy contact or replace it with a dedicated
> support alias — Play Console and this policy should use the same, consistent address.
