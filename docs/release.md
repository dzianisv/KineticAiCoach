# Play Store release checklist (KineticAiCoach)

This is a **prep checklist**, not an actual store upload. It documents the concrete steps to
take KineticAiCoach (`com.aistudio.aicoach.vtzrkm`) from a debug build to a signed, minified
release artifact ready for the Play Console.

## 0. Google Sign-In: register signing SHA certificates (REQUIRED)

Firebase Google Sign-In **fails** (sign-in never completes / `DEVELOPER_ERROR`) unless the
app's signing certificate SHA-1 is registered on the Firebase Android app. The debug key
(`debug.keystore`, SHA-1 `77:76:D5:8F:27:E1:DF:F2:C5:64:4D:60:AB:F0:7F:10:0B:C0:A7:D7`) is
already registered. For release you must **also** register the Play App Signing SHA-1 (and
SHA-256), available from **Play Console → Setup → App integrity** after the first AAB upload.

Register programmatically (no console needed) with an owner token:

```bash
TOKEN=$(gcloud auth print-access-token --account=vibeteaichnologies@gmail.com)
PROJ=kinetic-ai-coach-50627
APP="projects/$PROJ/androidApps/1:169391482464:android:c92b0f444eb842caf04505"
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "X-Goog-User-Project: $PROJ" \
  -H "Content-Type: application/json" "https://firebaserules.googleapis.com/../$APP/sha" \
  -d '{"shaHash":"<PLAY_APP_SIGNING_SHA1_lowercase_no_colons>","certType":"SHA_1"}'
```

After registering, download a fresh `google-services.json` (Firebase console or the
`.../androidApps/<id>/config` API) so it contains the matching Android OAuth client.

## 1. Generate the upload keystore (one time)

```bash
keytool -genkeypair -v \
  -keystore upload-keystore.jks \
  -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000
```

- **Do not commit `upload-keystore.jks`.** It's covered by `.gitignore` (`*.jks`, `*.keystore`).
- Store the keystore file itself, and its `storePassword`/`keyPassword`/`keyAlias`, in
  **Bitwarden, in the `dev` collection**, under this project's folder — that's this repo's
  convention for all secrets (see `docs/secrets.md`). Never paste them into a tracked file.
- Locally, copy `keystore.properties.template` → `keystore.properties` at the repo root and
  fill in the real `storeFile` path + passwords pulled from Bitwarden (or set the equivalent
  `KEYSTORE_PATH` / `KEYSTORE_STORE_PASSWORD` / `KEYSTORE_KEY_ALIAS` / `KEYSTORE_KEY_PASSWORD`
  env vars — e.g. in CI). `keystore.properties` is gitignored.
- If neither `keystore.properties` nor the env vars are present, `app/build.gradle.kts` leaves
  the `release` build type **unsigned** (R8/shrink-resources still run) so contributors without
  upload-key access can still build and verify the release variant locally.

## 2. Build the release AAB

```bash
./gradlew :app:bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

(A `.apk` for manual/sideload testing can be produced with `./gradlew :app:assembleRelease`,
output at `app/build/outputs/apk/release/app-release.apk`, but Play Console uploads expect the
**AAB**, not the APK.)

## 3. Size expectations: R8, shrinkResources, and Play's per-device delivery

- The current **debug** APK is ~110MB, dominated by the bundled ML Kit pose-detection model
  and unstripped code/resources.
- `isMinifyEnabled = true` (R8) removes unused code and obfuscates what remains.
  `isShrinkResources = true` removes unused resources (only takes effect together with R8).
  Together these shrink the app's own code+resources meaningfully, but do **not** shrink the
  bundled ML Kit model itself (it's a binary asset, not stripped by R8).
- Play Store distributes an **Android App Bundle**, not a monolithic APK: Play generates
  per-device **split APKs** (by ABI, screen density, language) at install time, so end users
  download only what their device needs — this alone reduces the effective download size
  compared to the current universal debug APK, without touching the ML Kit model.
- **Future option (not done here):** move the ML Kit pose model to **Play Feature Delivery**
  (a dynamic/on-demand feature module) or use ML Kit's unbundled/Google-Play-Services model
  (downloaded on first use via Google Play services instead of packaged in the app), which
  would cut the base install size directly. Track as a follow-up, not part of this G8 prep.

## 4. Play Console — app setup

1. **Create app**: Play Console → "Create app" → set app name, default language, app/game,
   free/paid.
2. **Store listing**:
   - Title, short description, full description.
   - App icon: 512×512 PNG.
   - Feature graphic: 1024×500 PNG/JPEG.
   - Phone screenshots (min 2, 16:9 or 9:16, per Play's current size requirements).
3. **Content rating**: complete the questionnaire (camera/pose-tracking fitness app — no
   objectionable content expected, but must still be completed per-app).
4. **Data Safety form**: declare data collection/use accurately —
   - **Camera**: used on-device for pose detection (frames processed locally via ML Kit).
   - **Photos/video (camera frames)**: a montage of pose-annotated frames is sent to our
     Firebase Cloud Function proxy, which forwards it to Vertex AI (Gemini) for form/rep
     analysis — declare this as data shared with a service provider for app functionality,
     not sold to third parties.
   - **Account info**: Firebase Auth (anonymous + Google Sign-In) — declare account
     identifiers collected.
   - **App activity / usage data**: Firestore-synced workout history — declare as collected
     for app functionality.
5. **Privacy policy URL**: required before publishing. Point to a hosted privacy policy;
   support contact is `support@agentlabs.cc`.
6. **Select countries/regions** for distribution.
7. **App content / target audience, ads declaration, government apps** — fill in remaining
   Play Console policy sections as prompted (all apps require these regardless of category).

## 5. Rollout

- Upload the signed AAB to the **Internal testing** track first (fastest review, small
  audience) and validate install/update flow, Data Safety accuracy, and crash-free sessions.
- Only promote to Closed/Open testing or Production after internal testing passes.

## Out of scope for this checklist

- Actually generating/uploading the production keystore or AAB (secrets stay in Bitwarden;
  this doc is instructions, not an execution log).
- Writing final marketing copy, screenshots, or the hosted privacy policy page.
- Migrating the ML Kit model to Play Feature Delivery (noted above as a future option).
