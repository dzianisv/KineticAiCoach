const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { GoogleGenAI } = require("@google/genai");
const { OAuth2Client } = require("google-auth-library");
const { google } = require("googleapis");

// Initialize Firebase Admin SDK
admin.initializeApp();

// --- Google Play subscription verification constants -----------------------
// OAuth scope required to call the Android Publisher (Google Play Developer) API.
const ANDROIDPUBLISHER_SCOPE = "https://www.googleapis.com/auth/androidpublisher";
// The app's Play package. Used as a safe default if the client omits it.
const DEFAULT_PACKAGE_NAME = "com.aistudio.aicoach.vtzrkm";
// The single subscription product id (see BillingConfig.PRODUCT_ID_PRO).
const DEFAULT_PRODUCT_ID = "kinetic_pro";
// subscriptionsv2 states that count as an entitled (paying/valid) subscriber.
const ENTITLED_V2_STATES = new Set([
  "SUBSCRIPTION_STATE_ACTIVE",
  "SUBSCRIPTION_STATE_IN_GRACE_PERIOD"
]);

// Instantiates a Google Auth Client to verify standard Google Sign-In ID Tokens
const oauth2Client = new OAuth2Client();

/**
 * Cloud Function: geminiProxy
 * 
 * Secure REST Endpoint serving as a proxy for Gemini AI requests.
 * 
 * Security:
 * - Requires "Authorization: Bearer <Google_or_Firebase_ID_Token>" header.
 * - Authenticates the caller's Google Account.
 * - Keeps the private GEMINI_API_KEY hidden on the server (environment / secret manager).
 * 
 * Request Body:
 * {
 *   "prompt": "User query here...",
 *   "systemPrompt": "System instruction here (optional)..."
 * }
 */
exports.geminiProxy = functions.runWith({ secrets: ["GEMINI_API_KEY"] }).https.onRequest(async (req, res) => {
  // 1. Enable CORS for mobile clients
  res.set("Access-Control-Allow-Origin", "*");
  if (req.method === "OPTIONS") {
    res.set("Access-Control-Allow-Methods", "POST");
    res.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    res.set("Access-Control-Max-Age", "3600");
    return res.status(204).send("");
  }

  if (req.method !== "POST") {
    return res.status(405).json({ error: "Only POST requests are supported." });
  }

  // 2. Extract Authorization Header
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return res.status(401).json({
      error: "Unauthorized",
      message: "Missing or invalid Authorization header. Please provide a Bearer Google ID Token."
    });
  }

  const token = authHeader.split("Bearer ")[1];
  let authenticatedUser = null;

  // 3. Authenticate user: Firebase Auth ID token first (primary path for the Android app),
  //    falling back to standard Google Sign-In ID token verification.
  try {
    try {
      const decodedToken = await admin.auth().verifyIdToken(token);
      authenticatedUser = {
        uid: decodedToken.uid,
        email: decodedToken.email,
        name: decodedToken.name || "",
        provider: "firebase"
      };
      console.log(`Successfully verified Firebase ID token for: ${authenticatedUser.email}`);
    } catch (firebaseError) {
      // If Firebase Admin verification fails, fall back to standard Google ID token verification
      console.log("Not a Firebase ID token, trying standard Google ID token verification...");
      const verifyOptions = { idToken: token };
      if (process.env.GOOGLE_WEB_CLIENT_ID) {
        verifyOptions.audience = process.env.GOOGLE_WEB_CLIENT_ID;
      }
      const ticket = await oauth2Client.verifyIdToken(verifyOptions);
      const payload = ticket.getPayload();
      authenticatedUser = {
        uid: payload.sub,
        email: payload.email,
        name: payload.name,
        provider: "google"
      };
      console.log(`Successfully verified Google standard token for: ${authenticatedUser.email}`);
    }
  } catch (authError) {
    console.error("Authentication failed:", authError);
    return res.status(403).json({
      error: "Forbidden",
      message: "Google Account Authentication failed. The provided token is invalid or expired."
    });
  }

  // 4. Select GenAI backend based on environment configuration
  const useVertexBackend = process.env.GENAI_BACKEND === "vertex" || !process.env.GEMINI_API_KEY;

  // 5. Parse Prompt, System Instructions and optional image from the Request Body
  const { prompt, systemPrompt, imageBase64, mimeType, responseMimeType } = req.body;
  if (!prompt || typeof prompt !== "string") {
    return res.status(400).json({
      error: "Bad Request",
      message: "Missing 'prompt' string parameter in request body."
    });
  }
  if (imageBase64 && typeof imageBase64 !== "string") {
    return res.status(400).json({
      error: "Bad Request",
      message: "'imageBase64' must be a base64-encoded string when provided."
    });
  }

  // 6. Contact Gemini API using Official Google Generative AI SDK
  try {
    const ai = useVertexBackend ? new GoogleGenAI({
      vertexai: true,
      project: process.env.GCLOUD_PROJECT || "kinetic-ai-coach-50627",
      // gemini-3.6-flash is only served from the `global` Vertex location, so we
      // default there. Overridable via VERTEX_LOCATION.
      location: process.env.VERTEX_LOCATION || "global"
    }) : new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });

    // Build contents: multimodal (image + text) when an image is supplied,
    // otherwise a plain text prompt. Used by the live workout form analyzer.
    const contents = imageBase64 ? [{
      role: "user",
      parts: [
        { text: prompt },
        { inlineData: { mimeType: mimeType || "image/jpeg", data: imageBase64 } }
      ]
    }] : prompt;

    const generationConfig = {};
    if (systemPrompt) generationConfig.systemInstruction = systemPrompt;
    if (responseMimeType) generationConfig.responseMimeType = responseMimeType;

    // Model is configurable via the GENAI_MODEL env var. Defaults to
    // gemini-3.6-flash (served from the `global` Vertex location) — the model the
    // PRD requires. Override GENAI_MODEL to flip without a source change.
    const modelName = process.env.GENAI_MODEL || "gemini-3.6-flash";
    const response = await ai.models.generateContent({
      model: modelName,
      contents,
      config: Object.keys(generationConfig).length ? generationConfig : undefined
    });

    const responseText = response.text || "No response generated.";
    
    // Return response with audited logging metadata
    return res.status(200).json({
      text: responseText,
      model: modelName,
      user: {
        email: authenticatedUser.email,
        provider: authenticatedUser.provider
      },
      timestamp: Date.now()
    });

  } catch (apiError) {
    console.error("Gemini API call failed:", apiError);
    return res.status(502).json({
      error: "Bad Gateway",
      message: "Gemini AI API service request failed.",
      details: apiError.message
    });
  }
});

/**
 * Extracts and validates the authenticated Firebase uid from a callable context.
 *
 * Firebase callable functions verify the caller's Firebase ID token before the
 * handler runs and expose the result on `context.auth`. This mirrors the
 * ID-token verification the geminiProxy performs (admin.auth().verifyIdToken):
 * an absent/blank uid means the caller is unauthenticated, and we reject with a
 * callable "unauthenticated" error (surfaced to HTTP callers as 401).
 *
 * @param {*} context Firebase callable context.
 * @returns {string} A non-empty, trimmed uid.
 * @throws {functions.https.HttpsError} code "unauthenticated" when no valid uid.
 */
function requireUid(context) {
  const uid = context && context.auth && typeof context.auth.uid === "string"
    ? context.auth.uid.trim()
    : "";
  if (!uid) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "Authentication is required to verify subscriptions."
    );
  }
  return uid;
}

/**
 * Derives entitlement from an androidpublisher purchases.subscriptionsv2 resource.
 * ACTIVE and IN_GRACE_PERIOD both count as entitled. expiryTimeMillis is the
 * latest lineItem.expiryTime across the subscription (RFC3339 -> epoch ms).
 *
 * @param {object} sub subscriptionsv2 response body (SubscriptionPurchaseV2).
 * @returns {{active: boolean, state: string, expiryTimeMillis: (number|null)}}
 */
function deriveEntitlementV2(sub) {
  const state = (sub && typeof sub.subscriptionState === "string")
    ? sub.subscriptionState
    : "SUBSCRIPTION_STATE_UNSPECIFIED";
  const active = ENTITLED_V2_STATES.has(state);

  let expiryTimeMillis = null;
  const lineItems = (sub && Array.isArray(sub.lineItems)) ? sub.lineItems : [];
  for (const li of lineItems) {
    if (li && typeof li.expiryTime === "string") {
      const ms = Date.parse(li.expiryTime);
      if (!Number.isNaN(ms)) {
        expiryTimeMillis = expiryTimeMillis == null ? ms : Math.max(expiryTimeMillis, ms);
      }
    }
  }
  return { active, state, expiryTimeMillis };
}

/**
 * Derives entitlement from a legacy purchases.subscriptions (v1) resource, used
 * only as a fallback when subscriptionsv2.get is unavailable. A subscription is
 * entitled when it has not expired and is not stuck in a pending payment state
 * (paymentState === 0 means "payment pending").
 *
 * @param {object} sub subscriptions.get (v1) response body.
 * @param {number} nowMillis current epoch ms (injectable for tests).
 * @returns {{active: boolean, state: string, expiryTimeMillis: (number|null)}}
 */
function deriveEntitlementV1(sub, nowMillis = Date.now()) {
  const expiryTimeMillis = (sub && sub.expiryTimeMillis != null && sub.expiryTimeMillis !== "")
    ? Number(sub.expiryTimeMillis)
    : null;
  const paymentPending = sub && sub.paymentState === 0; // 0 = payment pending
  const notExpired = expiryTimeMillis != null && !Number.isNaN(expiryTimeMillis) && expiryTimeMillis > nowMillis;
  const active = Boolean(notExpired && !paymentPending);
  return {
    active,
    state: active ? "SUBSCRIPTION_STATE_ACTIVE" : "SUBSCRIPTION_STATE_EXPIRED_OR_PENDING",
    expiryTimeMillis: (expiryTimeMillis != null && Number.isNaN(expiryTimeMillis)) ? null : expiryTimeMillis
  };
}

/**
 * Cloud Function: verifySubscription (callable)
 *
 * Server-authoritative Google Play subscription verification. Called by the
 * Android client (BillingManager.kt) after a local purchase acknowledgment. This
 * makes entitlement reconcilable server-side rather than trusting the
 * (client-spoofable) local Play Billing state alone.
 *
 * Auth: requires a valid Firebase ID token (enforced by the callable protocol +
 * requireUid). Unauthenticated callers are rejected (HTTP 401 for raw callers).
 *
 * Input data: { packageName?, productId?, purchaseToken }
 *   - packageName defaults to the app package if omitted.
 *   - productId   defaults to kinetic_pro if omitted.
 *   - purchaseToken is required.
 *
 * Behaviour:
 *   - Calls androidpublisher.purchases.subscriptionsv2.get (current API) using
 *     Application Default Credentials (the deployed function's runtime service
 *     account, which must be granted androidpublisher access + Play "API access").
 *     Scope: https://www.googleapis.com/auth/androidpublisher.
 *   - Falls back to the legacy purchases.subscriptions.get if v2 fails with a
 *     404/NOT_FOUND (e.g. the API isn't v2-enabled for this app yet).
 *   - Persists the result to Firestore users/{uid}.proEntitlement so entitlement
 *     becomes server-authoritative and reconcilable.
 *   - On API/credential errors it returns a callable error (never throws
 *     uncaught / crashes the function).
 *
 * Returns: { verified, state, expiryTimeMillis, productId }
 *
 * Deployment note: do NOT deploy this function as part of this change; deploy is
 * explicitly out of scope. Deploy later with (per repo convention):
 *   gcloud functions deploy verifySubscription --source functions ...
 */
exports.verifySubscription = functions.https.onCall(async (data, context) => {
  // Auth gate (outside try so unauthenticated maps cleanly to 401, not "internal").
  const uid = requireUid(context);

  const purchaseToken = (data && typeof data.purchaseToken === "string")
    ? data.purchaseToken.trim()
    : "";
  if (!purchaseToken) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Missing required 'purchaseToken' string."
    );
  }

  const productId = (data && typeof data.productId === "string" && data.productId.trim())
    ? data.productId.trim()
    : DEFAULT_PRODUCT_ID;
  const packageName = (data && typeof data.packageName === "string" && data.packageName.trim())
    ? data.packageName.trim()
    : DEFAULT_PACKAGE_NAME;

  try {
    // Application Default Credentials: on Cloud Functions this is the function's
    // runtime service account, which must carry androidpublisher access.
    const authClient = await google.auth.getClient({ scopes: [ANDROIDPUBLISHER_SCOPE] });
    const androidpublisher = google.androidpublisher({ version: "v3", auth: authClient });

    let entitlement;
    let apiVersion = "subscriptionsv2";
    try {
      const v2 = await androidpublisher.purchases.subscriptionsv2.get({
        packageName,
        token: purchaseToken
      });
      entitlement = deriveEntitlementV2(v2.data || {});
    } catch (v2err) {
      const status = v2err && (v2err.code || (v2err.response && v2err.response.status));
      // Only fall back for "not found / not enabled"-style failures; re-throw the
      // rest so genuine auth/permission errors surface as a clear error below.
      if (status === 404 || status === 400) {
        apiVersion = "subscriptions";
        const v1 = await androidpublisher.purchases.subscriptions.get({
          packageName,
          subscriptionId: productId,
          token: purchaseToken
        });
        entitlement = deriveEntitlementV1(v1.data || {});
      } else {
        throw v2err;
      }
    }

    // Persist server-authoritative entitlement. Merge so we never clobber other
    // user fields; this doc is the reconcilable source of truth going forward.
    try {
      await admin.firestore().doc(`users/${uid}`).set({
        proEntitlement: {
          active: entitlement.active,
          productId,
          expiryTimeMillis: entitlement.expiryTimeMillis,
          purchaseToken,
          verifiedAt: admin.firestore.FieldValue.serverTimestamp()
        }
      }, { merge: true });
    } catch (fsErr) {
      // Persistence is best-effort observability; do not fail the verification
      // result just because the write failed.
      console.error("verifySubscription: Firestore write failed", fsErr && fsErr.message);
    }

    console.log("verifySubscription verified", {
      uid, productId, apiVersion, verified: entitlement.active, state: entitlement.state
    });

    return {
      verified: entitlement.active,
      state: entitlement.state,
      expiryTimeMillis: entitlement.expiryTimeMillis,
      productId
    };
  } catch (err) {
    // Defensive: never throw uncaught (would crash/500 opaquely). Return a
    // callable error with a safe, non-leaking message.
    console.error("verifySubscription failed", err && (err.message || err));
    throw new functions.https.HttpsError(
      "internal",
      "Subscription verification could not be completed. Entitlement was not changed."
    );
  }
});

// Exported for unit tests (pure logic; no network / no side effects).
exports._internal = { requireUid, deriveEntitlementV2, deriveEntitlementV1 };
