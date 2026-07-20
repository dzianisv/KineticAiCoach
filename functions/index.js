const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { GoogleGenAI } = require("@google/genai");
const { OAuth2Client } = require("google-auth-library");

// Initialize Firebase Admin SDK
admin.initializeApp();

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

  // 5. Parse Prompt and System Instructions from the Request Body
  const { prompt, systemPrompt } = req.body;
  if (!prompt || typeof prompt !== "string") {
    return res.status(400).json({
      error: "Bad Request",
      message: "Missing 'prompt' string parameter in request body."
    });
  }

  // 6. Contact Gemini API using Official Google Generative AI SDK
  try {
    const ai = useVertexBackend ? new GoogleGenAI({
      vertexai: true,
      project: process.env.GCLOUD_PROJECT || "kinetic-ai-coach-50627",
      location: process.env.VERTEX_LOCATION || "us-central1"
    }) : new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });
    
    // Using gemini-2.5-flash as the fast, efficient model
    const response = await ai.models.generateContent({
      model: "gemini-2.5-flash",
      contents: prompt,
      config: systemPrompt ? {
        systemInstruction: systemPrompt
      } : undefined
    });

    const responseText = response.text || "No response generated.";
    
    // Return response with audited logging metadata
    return res.status(200).json({
      text: responseText,
      model: "gemini-2.5-flash",
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
