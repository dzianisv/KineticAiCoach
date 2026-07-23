package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val isLoggedIn: Boolean = false,
    val goals: String = "",
    val height: Double = 0.0,
    val weight: Double = 0.0,
    val weeklyGoalDays: Int = 3,
    val experiencePoints: Int = 0,
    val streakDays: Int = 0,
    val lastWorkoutDate: Long? = null, // System.currentTimeMillis() of last completed session
    val workoutProgram: String = "" // Custom AI fitness program description
)

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val exerciseName: String,
    val durationSeconds: Int,
    val reps: Int,
    val sets: Int = 0,
    val formScore: Double,
    val pointsEarned: Int,
    val feedback: String,
    val timestamp: Long = System.currentTimeMillis(),
    val classId: Int? = null
)

@Entity(tableName = "program_exercises")
data class ProgramExercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderIndex: Int,
    val name: String,
    val targetSets: Int = 3,
    val targetReps: Int = 12,
    val restSeconds: Int = 30,
    val notes: String = ""
)

@Entity(tableName = "workout_classes")
data class WorkoutClass(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startedAt: Long,
    val completedAt: Long,
    val exerciseCount: Int,
    val totalReps: Int,
    val sets: Int = 0,
    val avgFormScore: Double,
    val totalPoints: Int
)

@Entity(tableName = "leaderboard")
data class LeaderboardEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val points: Int,
    val isCurrentUser: Boolean = false
)

@Entity(tableName = "badges")
data class Badge(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val unlockedAt: Long? = null // Non-null if unlocked
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String, // "user" or "coach"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
