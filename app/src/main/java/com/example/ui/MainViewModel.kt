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

    fun speak(text: String) {
        if (isTtsInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
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

    // Persist the completed class: save the WorkoutClass first to get its id, then link each per-exercise session to it.
    suspend fun finishTodaysClass(): Int {
        val results = _classResults.value
        val now = System.currentTimeMillis()
        val workoutClass = WorkoutClass(
            startedAt = if (classStartedAt == 0L) now else classStartedAt,
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
        return classId
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
