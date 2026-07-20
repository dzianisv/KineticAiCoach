package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

@JsonClass(generateAdapter = true)
data class FirebaseProxyRequest(
    val prompt: String,
    val systemPrompt: String? = null,
    val imageBase64: String? = null,
    val mimeType: String? = null,
    val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class FirebaseProxyResponse(
    val text: String,
    val model: String? = null,
    val timestamp: Long? = null
)

interface FirebaseProxyService {
    @POST("geminiProxy")
    suspend fun callProxy(
        @retrofit2.http.Header("Authorization") authHeader: String,
        @Body request: FirebaseProxyRequest
    ): FirebaseProxyResponse
}

interface GeminiApiService {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }

    // Lazy initialization of Firebase proxy service based on configured URL in .env
    val proxyService: FirebaseProxyService? by lazy {
        val proxyUrl = BuildConfig.FIREBASE_PROXY_URL
        if (proxyUrl.isNotEmpty() && !proxyUrl.contains("your-project-id")) {
            val retrofit = Retrofit.Builder()
                .baseUrl(proxyUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
            retrofit.create(FirebaseProxyService::class.java)
        } else {
            null
        }
    }

    /**
     * Sends a single camera frame (base64 JPEG) to the secure proxy for live
     * workout form analysis. Returns the raw model text (expected to be JSON when
     * responseMimeType is application/json). Returns null on any failure so the
     * caller can decide how to degrade — we never fabricate analysis.
     */
    suspend fun analyzeFrame(
        imageBase64: String,
        prompt: String,
        systemPrompt: String? = null,
        responseMimeType: String? = "application/json"
    ): String? {
        val proxy = proxyService ?: run {
            Log.e("GeminiApiClient", "Frame analysis requires the Firebase proxy; none configured.")
            return null
        }
        val user = FirebaseAuth.getInstance().currentUser
        val idToken = user?.let {
            try {
                Tasks.await(it.getIdToken(false)).token
            } catch (e: Exception) {
                Log.e("GeminiApiClient", "Failed to fetch Firebase ID token for frame analysis", e)
                null
            }
        } ?: return null

        return try {
            val response = proxy.callProxy(
                authHeader = "Bearer $idToken",
                request = FirebaseProxyRequest(
                    prompt = prompt,
                    systemPrompt = systemPrompt,
                    imageBase64 = imageBase64,
                    mimeType = "image/jpeg",
                    responseMimeType = responseMimeType
                )
            )
            response.text
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Frame analysis proxy call failed", e)
            null
        }
    }

    // --- G4/G5/G6: Coach tab attachments (image / video-frame / file) ---
    // Generic multimodal chat call: sends the user's text plus a single base64
    // attachment (image, PDF, or plain text) through the same Firebase proxy the
    // live frame analyzer uses. The proxy's `imageBase64`/`mimeType` fields are
    // just inlineData passthrough, so any Gemini-supported mimeType works here
    // (image/jpeg, image/png, application/pdf, text/plain, ...).
    suspend fun askGeminiWithAttachment(
        prompt: String,
        attachmentBase64: String,
        mimeType: String,
        systemPrompt: String? = null
    ): String {
        val proxy = proxyService ?: return "Attachments require the Firebase Cloud Function Proxy to be configured (FIREBASE_PROXY_URL). Text chat still works offline."
        val user = FirebaseAuth.getInstance().currentUser
        val idToken = user?.let {
            try {
                Tasks.await(it.getIdToken(false)).token
            } catch (e: Exception) {
                Log.e("GeminiApiClient", "Failed to fetch Firebase ID token for attachment analysis", e)
                null
            }
        } ?: return "Please sign in again to analyze attachments."

        return try {
            val response = proxy.callProxy(
                authHeader = "Bearer $idToken",
                request = FirebaseProxyRequest(
                    prompt = prompt,
                    systemPrompt = systemPrompt,
                    imageBase64 = attachmentBase64,
                    mimeType = mimeType
                )
            )
            response.text
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Attachment analysis proxy call failed", e)
            "I couldn't analyze that attachment right now. (${e.localizedMessage})"
        }
    }

    suspend fun askGemini(prompt: String, systemPrompt: String? = null): String {
        // Try calling the Firebase Cloud Function Proxy first if configured
        val proxy = proxyService
        if (proxy != null) {
            val user = FirebaseAuth.getInstance().currentUser
            val idToken = user?.let {
                try {
                    Tasks.await(it.getIdToken(false)).token
                } catch (e: Exception) {
                    Log.e("GeminiApiClient", "Failed to fetch Firebase ID token", e)
                    null
                }
            }

            if (idToken != null) {
                Log.d("GeminiApiClient", "Routing request securely through Firebase Cloud Function Proxy...")
                try {
                    val authHeader = "Bearer $idToken"

                    val response = proxy.callProxy(
                        authHeader = authHeader,
                        request = FirebaseProxyRequest(prompt = prompt, systemPrompt = systemPrompt)
                    )
                    return response.text
                } catch (proxyError: Exception) {
                    Log.e("GeminiApiClient", "Firebase Cloud Function Proxy failed, falling back to direct API connection.", proxyError)
                }
            } else {
                Log.w("GeminiApiClient", "No authenticated Firebase user / ID token available; skipping proxy and using direct API.")
            }
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("GeminiApiClient", "API Key is missing or default placeholder!")
            return "Hi there! I'm your offline AI Fitness Coach. I can guide you through squats, pushups, and jumping jacks, or we can customize your training schedule. (Configure GEMINI_API_KEY in Secrets or deploy your Firebase Proxy for live AI responses!)"
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = systemPrompt?.let { Content(parts = listOf(Part(text = it))) }
        )

        return try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response from AI Coach."
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "API call failed", e)
            "Coach response: I'm currently working offline, but I've updated your workout statistics! Let's get moving! (${e.localizedMessage})"
        }
    }
}

