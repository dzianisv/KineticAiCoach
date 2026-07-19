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
import com.example.data.UserProfile
import com.example.data.WorkoutSession
import com.example.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfile()
            val updated = current.copy(
                name = if (current.name.isBlank()) name else current.name,
                email = email,
                isLoggedIn = true
            )
            repository.saveProfile(updated)
        }
    }

    fun signOutUser() {
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

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
    }
}
