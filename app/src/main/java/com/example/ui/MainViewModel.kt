package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Badge
import com.example.data.FitRepository
import com.example.data.LeaderboardEntry
import com.example.data.ProgramExercise
import com.example.data.UserProfile
import com.example.data.WorkoutClass
import com.example.data.WorkoutSession
import com.example.network.RetrofitClient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Locale

// --- G4/G5/G6: Coach tab attachments (image / video / file) ---
// Kept as its own enum + additive, defaulted fields on ChatMessage so this stays
// a small, additive change alongside other agents' edits to chat persistence.
enum class AttachmentType { IMAGE, VIDEO, FILE }

data class ChatMessage(
    val sender: String, // "user" or "coach"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    // Attachment metadata (null for plain-text messages).
    val attachmentUri: String? = null,
    val attachmentType: AttachmentType? = null,
    val attachmentName: String? = null // display filename, used for FILE chips
)

class MainViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {
    private val database = AppDatabase.getDatabase(application)
    private val repository = FitRepository(database)

    // Flows from database
    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val workoutSessions: StateFlow<List<WorkoutSession>> = repository.workoutSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val leaderboard: StateFlow<List<LeaderboardEntry>> = repository.leaderboard
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val badges: StateFlow<List<Badge>> = repository.badges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chat states
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("coach", "Hello! I am your AI Fitness Coach. Set up your profile, and let's crush your fitness goals together! Ask me anything about squats, pushups, form, or schedule.")
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isGeneratingProgram = MutableStateFlow(false)
    val isGeneratingProgram: StateFlow<Boolean> = _isGeneratingProgram.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // Text To Speech
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    init {
        // Initialize Database and Seeding
        viewModelScope.launch {
            repository.checkAndSeedDatabase()
            loadPersistedChatMessages()
        }
        // Initialize TTS
        try {
            tts = TextToSpeech(application, this)
        } catch (e: Exception) {
            Log.e("MainViewModel", "TTS Init failed", e)
        }
    }

    // Loads persisted chat history from Room into the in-memory chat state.
    // If nothing has been persisted yet (first run), the seeded welcome message
    // above is left alone so it isn't duplicated.
    private suspend fun loadPersistedChatMessages() {
        val persisted = repository.getChatMessages()
        if (persisted.isNotEmpty()) {
            _chatMessages.value = persisted.map { ChatMessage(it.role, it.content, it.timestamp) }
        }
    }

    // Appends a chat message to the in-memory state immediately, and persists it
    // to Room (and best-effort to Firestore) in the background.
    private fun addAndPersistChatMessage(sender: String, text: String) {
        _chatMessages.value = _chatMessages.value + ChatMessage(sender, text)
        viewModelScope.launch {
            repository.addChatMessage(sender, text)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("MainViewModel", "TTS Language is not supported")
            } else {
                isTtsInitialized = true
            }
        } else {
            Log.e("MainViewModel", "TTS Initialization failed")
        }
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (isTtsInitialized) {
            tts?.speak(text, queueMode, null, null)
        } else {
            Log.w("MainViewModel", "TTS is not initialized yet. Text: $text")
        }
    }

    // Onboarding and Profile Actions
    fun signInUser(name: String, email: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        viewModelScope.launch {
            // onSignedIn handles blank uid gracefully (local-only: pull/push no-op,
            // remote treated as absent), so a separate blank-uid branch is unnecessary.
            repository.onSignedIn(uid, name, email)
            if (uid.isNotBlank()) {
                repository.syncChatMessagesFromCloud(uid)
                loadPersistedChatMessages()
            }
        }
    }

    fun signOutUser() {
        FirebaseAuth.getInstance().signOut()
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfile()
            val updated = current.copy(
                isLoggedIn = false
            )
            repository.saveProfile(updated)
        }
    }

    fun updateProfile(name: String, goals: String, height: Double, weight: Double, weeklyDays: Int) {
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfile()
            val updated = current.copy(
                name = name,
                goals = goals,
                height = height,
                weight = weight,
                weeklyGoalDays = weeklyDays
            )
            repository.saveProfile(updated)
        }
    }

    fun generateCustomProgram() {
        val profile = userProfile.value ?: return
        _isGeneratingProgram.value = true
        viewModelScope.launch {
            val systemPrompt = "You are an elite, highly encouraging AI Fitness Coach. Provide structured programs."
            val prompt = """
                Generate a custom weekly workout schedule and program description for:
                Name: ${profile.name}
                Goals: ${profile.goals}
                Height: ${profile.height} cm
                Weight: ${profile.weight} kg
                Schedule: ${profile.weeklyGoalDays} days per week.
                
                Keep the program highly structured, list specific workouts (like Squats, Pushups, Jumping Jacks), and acceptable timings/routines. Keep it within 180 words, using clear headers and bullet points.
            """.trimIndent()

            val generatedText = withContext(Dispatchers.IO) {
                RetrofitClient.askGemini(prompt, systemPrompt)
            }

            repository.saveProfile(profile.copy(workoutProgram = generatedText))
            _isGeneratingProgram.value = false

            // Add to chat as well
            addAndPersistChatMessage("coach", "I have generated your custom program! Check it out in your Coach tab:\n\n$generatedText")
        }
    }

    // Chat Actions
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        addAndPersistChatMessage("user", text)
        _isChatLoading.value = true

        viewModelScope.launch {
            if (isProgramEditRequest(text)) {
                handleProgramEditRequest(text)
                _isChatLoading.value = false
                return@launch
            }

            val profile = userProfile.value ?: UserProfile()
            val systemPrompt = """
                You are Coach Iron, an encouraging, elite AI fitness trainer. You know a lot about workout forms, skeletal alignment, exercise sets, schedules, and healthy lifestyles. 
                Keep your responses short, direct, motivating, and professional. Incorporate user details if relevant: Name: ${profile.name}, Goals: ${profile.goals}.
            """.trimIndent()

            val response = withContext(Dispatchers.IO) {
                RetrofitClient.askGemini(text, systemPrompt)
            }

            addAndPersistChatMessage("coach", response)
            _isChatLoading.value = false
            speak(response.take(120)) // Speak the first 120 chars for audio feedback
        }
    }

    // ==================== G4/G5/G6: Coach tab attachments ====================
    // Image, video, and file attachments from the Coach chat's attachment button
    // (DashboardScreen.kt + CoachAttachments.kt). Each function immediately posts
    // a user ChatMessage carrying the attachment Uri/type/name so the UI can
    // render a thumbnail / player / file chip right away, then reads + base64
    // encodes the attachment on IO and forwards it to Gemini via the same
    // Firebase proxy used by askGemini/analyzeFrame.

    /** G4: user picked an image via PickVisualMedia(ImageOnly). */
    fun sendImageMessage(imageUri: Uri, caption: String) {
        val userMsg = ChatMessage(
            sender = "user",
            text = caption,
            attachmentUri = imageUri.toString(),
            attachmentType = AttachmentType.IMAGE
        )
        _chatMessages.value = _chatMessages.value + userMsg
        _isChatLoading.value = true

        viewModelScope.launch {
            val context = getApplication<Application>()
            val encoded = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(imageUri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.let { encodeBitmapToBase64Jpeg(it) }
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to read image attachment", e)
                    null
                }
            }

            val response = if (encoded != null) {
                val profile = userProfile.value ?: UserProfile()
                val systemPrompt = """
                    You are Coach Iron, an encouraging, elite AI fitness trainer. The user shared a photo (e.g. of their exercise form, body, or gym setup). Look at it and respond with short, direct, motivating, professional feedback. Incorporate user details if relevant: Name: ${profile.name}, Goals: ${profile.goals}.
                """.trimIndent()
                withContext(Dispatchers.IO) {
                    RetrofitClient.askGeminiWithAttachment(
                        prompt = caption.ifBlank { "Take a look at this photo and give me feedback." },
                        attachmentBase64 = encoded,
                        mimeType = "image/jpeg",
                        systemPrompt = systemPrompt
                    )
                }
            } else {
                "I couldn't read that image — please try attaching it again."
            }

            _chatMessages.value = _chatMessages.value + ChatMessage("coach", response)
            _isChatLoading.value = false
            speak(response.take(120))
        }
    }

    /**
     * G5: user picked a video via PickVisualMedia(VideoOnly). Full-video upload to
     * Gemini is out of scope (proxy only forwards a single inlineData blob); as a
     * nice-to-have we extract the first frame so the coach can still comment on
     * form. The video itself is always saved to the message list and playable
     * inline via VideoView (see CoachAttachments.kt).
     */
    fun sendVideoMessage(videoUri: Uri, caption: String) {
        val userMsg = ChatMessage(
            sender = "user",
            text = caption,
            attachmentUri = videoUri.toString(),
            attachmentType = AttachmentType.VIDEO
        )
        _chatMessages.value = _chatMessages.value + userMsg
        _isChatLoading.value = true

        viewModelScope.launch {
            val context = getApplication<Application>()
            val encoded = withContext(Dispatchers.IO) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, videoUri)
                    val frame = retriever.frameAtTime
                    retriever.release()
                    frame?.let { encodeBitmapToBase64Jpeg(it) }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to extract video thumbnail", e)
                    null
                }
            }

            val response = if (encoded != null) {
                val profile = userProfile.value ?: UserProfile()
                val systemPrompt = """
                    You are Coach Iron, an encouraging, elite AI fitness trainer. The user shared a workout video; you are only shown its first frame as a still image, so briefly acknowledge that limitation and give general form feedback based on what's visible. Keep it short, direct, motivating, professional. Incorporate user details if relevant: Name: ${profile.name}, Goals: ${profile.goals}.
                """.trimIndent()
                withContext(Dispatchers.IO) {
                    RetrofitClient.askGeminiWithAttachment(
                        prompt = caption.ifBlank { "Here's a video of my workout — can you check my form from this frame?" },
                        attachmentBase64 = encoded,
                        mimeType = "image/jpeg",
                        systemPrompt = systemPrompt
                    )
                }
            } else {
                "Got your video! I couldn't pull a preview frame to analyze, but it's saved above so you can review it yourself."
            }

            _chatMessages.value = _chatMessages.value + ChatMessage("coach", response)
            _isChatLoading.value = false
            speak(response.take(120))
        }
    }

    /** G6: user picked a document (PDF / text / other) via GetContent with a wildcard mime filter. */
    fun sendFileMessage(fileUri: Uri, fileName: String, mimeType: String?, caption: String) {
        val userMsg = ChatMessage(
            sender = "user",
            text = caption,
            attachmentUri = fileUri.toString(),
            attachmentType = AttachmentType.FILE,
            attachmentName = fileName
        )
        _chatMessages.value = _chatMessages.value + userMsg
        _isChatLoading.value = true

        viewModelScope.launch {
            val context = getApplication<Application>()
            val resolvedMimeType = mimeType ?: context.contentResolver.getType(fileUri) ?: "application/octet-stream"
            val encoded = withContext(Dispatchers.IO) {
                try {
                    val bytes = context.contentResolver.openInputStream(fileUri)?.use { it.readBytes() }
                    if (bytes != null && bytes.size <= MAX_ATTACHMENT_FILE_BYTES) {
                        Base64.encodeToString(bytes, Base64.NO_WRAP)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to read file attachment", e)
                    null
                }
            }

            val response = if (encoded != null) {
                val profile = userProfile.value ?: UserProfile()
                val systemPrompt = """
                    You are Coach Iron, an encouraging, elite AI fitness trainer. The user shared a document ($fileName). Read its contents and respond helpfully — e.g. summarize a training plan, answer questions about it, or extract relevant numbers. Keep it short, direct, motivating, professional. Incorporate user details if relevant: Name: ${profile.name}, Goals: ${profile.goals}.
                """.trimIndent()
                withContext(Dispatchers.IO) {
                    RetrofitClient.askGeminiWithAttachment(
                        prompt = caption.ifBlank { "Can you review this file: $fileName?" },
                        attachmentBase64 = encoded,
                        mimeType = resolvedMimeType,
                        systemPrompt = systemPrompt
                    )
                }
            } else {
                "That file ($fileName) is too large or unreadable for me to analyze right now (max ${MAX_ATTACHMENT_FILE_BYTES / (1024 * 1024)}MB)."
            }

            _chatMessages.value = _chatMessages.value + ChatMessage("coach", response)
            _isChatLoading.value = false
            speak(response.take(120))
        }
    }

    /** Downscales + JPEG-compresses a bitmap before base64 encoding, keeping the request payload small. */
    private fun encodeBitmapToBase64Jpeg(bitmap: Bitmap, maxDimension: Int = 1280, quality: Int = 85): String {
        val largestSide = maxOf(bitmap.width, bitmap.height)
        val scaled = if (largestSide > maxDimension) {
            val ratio = maxDimension.toFloat() / largestSide
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else {
            bitmap
        }
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }
    // ==================== end G4/G5/G6 ====================

    // --- PRD v2: conversational onboarding + program editing (Lane A) ---
    private val _onboardingMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val onboardingMessages: StateFlow<List<ChatMessage>> = _onboardingMessages.asStateFlow()

    private val _isOnboardingBuilding = MutableStateFlow(false)
    val isOnboardingBuilding: StateFlow<Boolean> = _isOnboardingBuilding.asStateFlow()

    private val _isOnboardingComplete = MutableStateFlow(false)
    val isOnboardingComplete: StateFlow<Boolean> = _isOnboardingComplete.asStateFlow()

    private enum class OnboardingStep { HEIGHT, WEIGHT, GOALS, DAYS, DONE }
    private var onboardingStep = OnboardingStep.HEIGHT
    private var pendingHeight: Double? = null
    private var pendingWeight: Double? = null
    private var pendingGoals: String? = null

    fun startOnboardingChat() {
        if (_onboardingMessages.value.isNotEmpty()) return
        onboardingStep = OnboardingStep.HEIGHT
        pendingHeight = null
        pendingWeight = null
        pendingGoals = null
        _isOnboardingBuilding.value = false
        _isOnboardingComplete.value = false
        _onboardingMessages.value = listOf(
            ChatMessage("coach", "Hey! I'm Coach Iron. Let's build your custom training plan together."),
            ChatMessage("coach", "First — what's your height in cm?")
        )
    }

    fun sendOnboardingReply(text: String) {
        _onboardingMessages.value = _onboardingMessages.value + ChatMessage("user", text)

        when (onboardingStep) {
            OnboardingStep.HEIGHT -> {
                val parsedHeight = extractFirstNumber(text)?.coerceIn(100.0, 250.0) ?: run {
                    _onboardingMessages.value = _onboardingMessages.value + ChatMessage(
                        "coach",
                        "I couldn't quite parse your height, so I'll assume 175 cm."
                    )
                    175.0
                }
                pendingHeight = parsedHeight
                _onboardingMessages.value = _onboardingMessages.value + ChatMessage("coach", "Got it. And your weight in kg?")
                onboardingStep = OnboardingStep.WEIGHT
            }

            OnboardingStep.WEIGHT -> {
                val parsedWeight = extractFirstNumber(text)?.coerceIn(30.0, 250.0) ?: run {
                    _onboardingMessages.value = _onboardingMessages.value + ChatMessage(
                        "coach",
                        "I couldn't quite parse your weight, so I'll assume 70 kg."
                    )
                    70.0
                }
                pendingWeight = parsedWeight
                _onboardingMessages.value = _onboardingMessages.value + ChatMessage(
                    "coach",
                    "What's your main training goal? (e.g. lose weight, build muscle, general fitness)"
                )
                onboardingStep = OnboardingStep.GOALS
            }

            OnboardingStep.GOALS -> {
                pendingGoals = text.trim().takeIf { it.isNotBlank() } ?: "General Fitness"
                _onboardingMessages.value = _onboardingMessages.value + ChatMessage(
                    "coach",
                    "How many days per week can you train? (1-7)"
                )
                onboardingStep = OnboardingStep.DAYS
            }

            OnboardingStep.DAYS -> {
                val parsedDays = extractFirstNumber(text)?.toInt()?.coerceIn(1, 7) ?: run {
                    _onboardingMessages.value = _onboardingMessages.value + ChatMessage(
                        "coach",
                        "I couldn't quite parse your training days, so I'll assume 3 days per week."
                    )
                    3
                }
                _onboardingMessages.value = _onboardingMessages.value + ChatMessage("coach", "Great — building your custom program now...")
                _isOnboardingBuilding.value = true
                onboardingStep = OnboardingStep.DONE

                viewModelScope.launch {
                    try {
                        val finalName = userProfile.value?.name?.takeIf { it.isNotBlank() } ?: "Champion"
                        updateProfile(
                            name = finalName,
                            goals = pendingGoals ?: "General Fitness",
                            height = pendingHeight ?: 175.0,
                            weight = pendingWeight ?: 70.0,
                            weeklyDays = parsedDays
                        )
                        buildProgramFromProfile()
                        val summaryLines = repository.getProgram()
                            .ifEmpty { defaultProgram() }
                            .joinToString("\n") { "- ${it.name}: ${it.targetSets}x${it.targetReps} (rest ${it.restSeconds}s)" }
                        _onboardingMessages.value = _onboardingMessages.value + ChatMessage(
                            "coach",
                            "Program ready! Here's your plan:\n$summaryLines"
                        )
                    } catch (e: Exception) {
                        _onboardingMessages.value = _onboardingMessages.value + ChatMessage(
                            "coach",
                            "Program ready! I set up a solid starter plan for you."
                        )
                    } finally {
                        _isOnboardingBuilding.value = false
                        _isOnboardingComplete.value = true
                    }
                }
            }

            OnboardingStep.DONE -> {
                _onboardingMessages.value = _onboardingMessages.value + ChatMessage("coach", "You're all set!")
            }
        }
    }

    suspend fun buildProgramFromProfile() {
        val profile = userProfile.value ?: UserProfile()
        val exercises = try {
            val systemPrompt = """
                You are Coach Iron, an expert fitness coach.
                Return ONLY a raw JSON array. No markdown, no code fences, no explanation.
                Each array object must be exactly:
                {"name":"...","targetSets":3,"targetReps":12,"restSeconds":30,"notes":"..."}
            """.trimIndent()
            val prompt = """
                Build a 5-6 exercise training program for this user profile:
                Goals: ${profile.goals}
                Height (cm): ${profile.height}
                Weight (kg): ${profile.weight}
                Weekly training days: ${profile.weeklyGoalDays}
            """.trimIndent()
            val raw = withContext(Dispatchers.IO) {
                RetrofitClient.askGemini(prompt, systemPrompt)
            }
            parseProgramExercises(raw) ?: defaultProgram()
        } catch (e: Exception) {
            defaultProgram()
        }

        repository.saveProgram(exercises)
        val summary = exercises.joinToString("\n") {
            "- ${it.name}: ${it.targetSets}x${it.targetReps} (rest ${it.restSeconds}s)"
        }
        repository.saveProfile(profile.copy(workoutProgram = "Custom Program:\n$summary"))
    }

    private fun extractFirstNumber(text: String): Double? {
        return Regex("-?\\d+(\\.\\d+)?").find(text)?.value?.toDoubleOrNull()
    }

    private fun parseProgramExercises(raw: String): List<ProgramExercise>? {
        return try {
            val withoutFences = raw
                .replace(Regex("```(json)?", RegexOption.IGNORE_CASE), "")
                .replace("```", "")
                .trim()
            val start = withoutFences.indexOf('[')
            val end = withoutFences.lastIndexOf(']')
            val jsonArrayText = if (start >= 0 && end > start) {
                withoutFences.substring(start, end + 1)
            } else {
                withoutFences
            }
            val array = JSONArray(jsonArrayText)
            val exercises = mutableListOf<ProgramExercise>()
            for (i in 0 until array.length()) {
                val item: JSONObject = array.optJSONObject(i) ?: continue
                val name = item.optString("name", "").trim()
                if (name.isBlank()) continue
                exercises += ProgramExercise(
                    orderIndex = exercises.size,
                    name = name,
                    targetSets = item.optInt("targetSets", 3).coerceAtLeast(1),
                    targetReps = item.optInt("targetReps", 12).coerceAtLeast(1),
                    restSeconds = item.optInt("restSeconds", 30).coerceAtLeast(0),
                    notes = item.optString("notes", "").trim()
                )
            }
            exercises.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }

    private fun defaultProgram(): List<ProgramExercise> {
        return listOf(
            ProgramExercise(orderIndex = 0, name = "Squats", targetSets = 3, targetReps = 12, restSeconds = 45, notes = "Keep chest up and drive through heels."),
            ProgramExercise(orderIndex = 1, name = "Push-ups", targetSets = 3, targetReps = 10, restSeconds = 45, notes = "Maintain a straight line from head to heels."),
            ProgramExercise(orderIndex = 2, name = "Lunges", targetSets = 3, targetReps = 10, restSeconds = 40, notes = "Alternate legs and keep knees aligned."),
            ProgramExercise(orderIndex = 3, name = "Plank", targetSets = 3, targetReps = 1, restSeconds = 30, notes = "Hold 30-45 seconds with a braced core."),
            ProgramExercise(orderIndex = 4, name = "Jumping Jacks", targetSets = 3, targetReps = 25, restSeconds = 30, notes = "Keep a steady rhythm and soft landings.")
        )
    }

    private fun isProgramEditRequest(text: String): Boolean {
        val lower = text.lowercase()
        val keywords = listOf("program", "add", "remove", "replace", "change", "more", "less", "swap")
        return keywords.any { keyword -> lower.contains(keyword) }
    }

    private suspend fun handleProgramEditRequest(text: String) {
        try {
            val currentProgram = repository.getProgram().ifEmpty { defaultProgram() }
            val currentProgramText = currentProgram.joinToString("\n") {
                "- ${it.name}: ${it.targetSets}x${it.targetReps}, rest ${it.restSeconds}s, notes: ${it.notes}"
            }
            val systemPrompt = """
                You are Coach Iron editing a workout plan.
                Return ONLY the full updated workout program as a raw JSON array.
                No markdown, no commentary.
                Each object must be exactly:
                {"name":"...","targetSets":3,"targetReps":12,"restSeconds":30,"notes":"..."}
            """.trimIndent()
            val prompt = """
                Current program:
                $currentProgramText

                User update request:
                $text

                Return a complete updated program with 5-6 exercises.
            """.trimIndent()
            val raw = withContext(Dispatchers.IO) {
                RetrofitClient.askGemini(prompt, systemPrompt)
            }
            val updated = parseProgramExercises(raw)
            if (updated != null) {
                repository.saveProgram(updated)
                val summary = updated.joinToString("\n") { "- ${it.name}: ${it.targetSets}x${it.targetReps}" }
                addAndPersistChatMessage(
                    "coach",
                    "Updated your program!\n$summary"
                )
            } else {
                addAndPersistChatMessage(
                    "coach",
                    "Sorry, I couldn't update your program right now. Please try again in a moment."
                )
            }
        } catch (e: Exception) {
            addAndPersistChatMessage(
                "coach",
                "Sorry, I couldn't update your program right now. Please try again in a moment."
            )
        }
    }

    // Workout Progress Saving
    fun completeWorkout(exerciseName: String, durationSeconds: Int, reps: Int, formScore: Double, feedback: String, sets: Int = 0) {
        viewModelScope.launch {
            repository.addWorkoutSession(
                exerciseName = exerciseName,
                durationSeconds = durationSeconds,
                reps = reps,
                formScore = formScore,
                feedback = feedback,
                sets = sets
            )
        }
    }

    // --- PRD v2: today's class sequencing (Lane B) ---
    private val defaultClassExercises = listOf(
        ProgramExercise(orderIndex = 0, name = "Squats"),
        ProgramExercise(orderIndex = 1, name = "Push-ups"),
        ProgramExercise(orderIndex = 2, name = "Lunges"),
        ProgramExercise(orderIndex = 3, name = "Plank"),
        ProgramExercise(orderIndex = 4, name = "Jumping Jacks")
    )

    // Exercises that make up "today's class". Falls back to a sensible default so the CTA always works.
    val todaysClass: StateFlow<List<ProgramExercise>> = repository.programExercises
        .map { if (it.isEmpty()) defaultClassExercises else it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultClassExercises)

    // Completed multi-exercise classes for the Dashboard class-history view.
    val workoutClasses: StateFlow<List<WorkoutClass>> = repository.workoutClasses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentClassIndex = MutableStateFlow(0)
    val currentClassIndex: StateFlow<Int> = _currentClassIndex.asStateFlow()

    private val _classResults = MutableStateFlow<List<ExerciseResult>>(emptyList())
    val classResults: StateFlow<List<ExerciseResult>> = _classResults.asStateFlow()

    private var classStartedAt: Long = 0L

    // Convenience accessor for the exercise currently in progress.
    val currentClassExerciseName: String
        get() = todaysClass.value.getOrNull(_currentClassIndex.value)?.name ?: "Workout"

    fun startTodaysClass() {
        _currentClassIndex.value = 0
        _classResults.value = emptyList()
        classStartedAt = System.currentTimeMillis()
    }

    fun recordExerciseResult(name: String, reps: Int, formScore: Int, points: Int) {
        _classResults.value = _classResults.value + ExerciseResult(name, reps, formScore, points)
    }

    // Advance to the next exercise. Returns true if more exercises remain, false when the class is finished.
    fun advanceClass(): Boolean {
        val next = _currentClassIndex.value + 1
        return if (next < todaysClass.value.size) {
            _currentClassIndex.value = next
            true
        } else {
            false
        }
    }

    // Finish the class: navigate to results immediately using an optimistic id,
    // and persist the WorkoutClass + per-exercise sessions in the background so
    // navigation never blocks on Room I/O (which previously let a stray tap
    // strand the user on the last exercise).
    fun finishTodaysClass(): Int {
        val results = _classResults.value
        val startedAt = if (classStartedAt == 0L) System.currentTimeMillis() else classStartedAt
        val optimisticId = (workoutClasses.value.maxOfOrNull { it.id } ?: 0) + 1
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val workoutClass = WorkoutClass(
                startedAt = startedAt,
                completedAt = now,
                exerciseCount = results.size,
                totalReps = results.sumOf { it.reps },
                avgFormScore = if (results.isNotEmpty()) results.map { it.formScore }.average() else 0.0,
                totalPoints = results.sumOf { it.points }
            )
            val classId = repository.saveClass(workoutClass)
            results.forEach { r ->
                repository.addWorkoutSession(
                    exerciseName = r.name,
                    durationSeconds = r.reps * 3,
                    reps = r.reps,
                    formScore = r.formScore.toDouble(),
                    feedback = "Class exercise: ${r.name} — ${r.formScore}% form.",
                    classId = classId
                )
            }
        }
        return optimisticId
    }
    // --- end PRD v2 (Lane B) ---

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
    }

    companion object {
        // Guard against oversized inline-data payloads on the Gemini proxy (G6 file attachments).
        // ~15MB raw -> ~20MB base64, matching typical Gemini inlineData request-size limits.
        private const val MAX_ATTACHMENT_FILE_BYTES = 15 * 1024 * 1024
    }
}

// --- PRD v2: today's class sequencing (Lane B) ---
// Per-exercise result row shown in the class results table.
data class ExerciseResult(
    val name: String,
    val reps: Int,
    val formScore: Int,
    val points: Int
)
// --- end PRD v2 (Lane B) ---
