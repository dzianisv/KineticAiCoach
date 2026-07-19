const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { GoogleGenAI } = require("@google/generative-ai");
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
exports.geminiProxy = functions.https.onRequest(async (req, res) => {
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

  // 3. Authenticate user: Check Google standard ID Token or Firebase Auth ID Token
  try {
    // Attempt verification as a standard Google Account ID token
    try {
      const ticket = await oauth2Client.verifyIdToken({
        idToken: token
      });
      const payload = ticket.getPayload();
      authenticatedUser = {
        uid: payload.sub,
        email: payload.email,
        name: payload.name,
        provider: "google"
      };
      console.log(`Successfully verified Google standard token for: ${authenticatedUser.email}`);
    } catch (googleError) {
      // If standard Google verification fails, fall back to Firebase Auth verification
      console.log("Not a standard Google ID token, trying Firebase Auth token verification...");
      const decodedToken = await admin.auth().verifyIdToken(token);
      authenticatedUser = {
        uid: decodedToken.uid,
        email: decodedToken.email,
        name: decodedToken.name || "",
        provider: "firebase"
      };
      console.log(`Successfully verified Firebase ID token for: ${authenticatedUser.email}`);
    }
  } catch (authError) {
    console.error("Authentication failed:", authError);
    return res.status(403).json({
      error: "Forbidden",
      message: "Google Account Authentication failed. The provided token is invalid or expired."
    });
  }

  // 4. Retrieve Gemini API Key securely
  // In production, configure this via: firebase functions:secrets:set GEMINI_API_KEY="your-key"
  const geminiApiKey = process.env.GEMINI_API_KEY || functions.config().gemini?.key;
  if (!geminiApiKey) {
    console.error("GEMINI_API_KEY is not configured in Firebase Cloud Functions env or config.");
    return res.status(500).json({
      error: "Internal Server Error",
      message: "The server's Gemini API gateway is temporarily unconfigured. Provide a GEMINI_API_KEY in functions env."
    });
  }

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
    const ai = new GoogleGenAI({ apiKey: geminiApiKey });
    
    // Using gemini-2.5-flash or gemini-1.5-flash as the fast, efficient model
    const response = await ai.models.generateContent({
      model: "gemini-1.5-flash",
      contents: prompt,
      config: systemPrompt ? {
        systemInstruction: systemPrompt
      } : undefined
    });

    const responseText = response.text || "No response generated.";
    
    // Return response with audited logging metadata
    return res.status(200).json({
      text: responseText,
      model: "gemini-1.5-flash",
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
