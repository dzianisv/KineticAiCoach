package com.example.ui

import android.app.Application
import android.speech.tts.TextToSpeech
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
import java.util.Locale

data class ChatMessage(
    val sender: String, // "user" or "coach"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
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
        }
        // Initialize TTS
        try {
            tts = TextToSpeech(application, this)
        } catch (e: Exception) {
            Log.e("MainViewModel", "TTS Init failed", e)
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
            _chatMessages.value = _chatMessages.value + ChatMessage("coach", "I have generated your custom program! Check it out in your Coach tab:\n\n$generatedText")
        }
    }

    // Chat Actions
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = ChatMessage("user", text)
        _chatMessages.value = _chatMessages.value + userMsg
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

            _chatMessages.value = _chatMessages.value + ChatMessage("coach", response)
            _isChatLoading.value = false
            speak(response.take(120)) // Speak the first 120 chars for audio feedback
        }
    }

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
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    "coach",
                    "Updated your program!\n$summary"
                )
            } else {
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    "coach",
                    "Sorry, I couldn't update your program right now. Please try again in a moment."
                )
            }
        } catch (e: Exception) {
            _chatMessages.value = _chatMessages.value + ChatMessage(
                "coach",
                "Sorry, I couldn't update your program right now. Please try again in a moment."
            )
        }
    }

    // Workout Progress Saving
    fun completeWorkout(exerciseName: String, durationSeconds: Int, reps: Int, formScore: Double, feedback: String) {
        viewModelScope.launch {
            repository.addWorkoutSession(
                exerciseName = exerciseName,
                durationSeconds = durationSeconds,
                reps = reps,
                formScore = formScore,
                feedback = feedback
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
