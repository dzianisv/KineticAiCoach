package com.example.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class FitRepository(
    private val db: AppDatabase,
    private val firestoreSync: FirestoreSync = FirestoreSync()
) {
    val userProfile: Flow<UserProfile?> = db.userProfileDao().getProfileFlow()
    val workoutSessions: Flow<List<WorkoutSession>> = db.workoutSessionDao().getAllSessions()
    val leaderboard: Flow<List<LeaderboardEntry>> = db.leaderboardDao().getLeaderboard()
    val badges: Flow<List<Badge>> = db.badgeDao().getAllBadges()

    suspend fun checkAndSeedDatabase() {
        // 1. Seed Leaderboard if empty
        val currentLeaderboard = db.leaderboardDao().getLeaderboard().firstOrNull() ?: emptyList()
        if (currentLeaderboard.isEmpty()) {
            val seedLeaderboard = listOf(
                LeaderboardEntry(name = "Svetlana", points = 1250),
                LeaderboardEntry(name = "Sophia (Iron Woman)", points = 940),
                LeaderboardEntry(name = "Julian (Calisthenics Beast)", points = 720),
                LeaderboardEntry(name = "User (You)", points = 0, isCurrentUser = true),
                LeaderboardEntry(name = "Emma (Yoga Guru)", points = 450),
                LeaderboardEntry(name = "Marcus (Squat Lord)", points = 210)
            )
            db.leaderboardDao().insertEntries(seedLeaderboard)
        }

        // 2. Seed Badges if empty
        val currentBadges = db.badgeDao().getAllBadges().firstOrNull() ?: emptyList()
        if (currentBadges.isEmpty()) {
            val seedBadges = listOf(
                Badge("first_step", "First Step", "Complete your first workout session!", "ic_badge_first"),
                Badge("form_master", "Form Master", "Achieve a form score of 90% or higher!", "ic_badge_form"),
                Badge("iron_will", "Iron Will", "Complete at least 3 fitness workout sessions!", "ic_badge_sessions"),
                Badge("xp_milestone", "Streak Maker", "Reach a workout streak of 2 days or more!", "ic_badge_streak")
            )
            db.badgeDao().insertBadges(seedBadges)
        }

        // 3. Ensure User Profile exists
        val currentProfile = db.userProfileDao().getProfileDirect()
        if (currentProfile == null) {
            db.userProfileDao().insertOrUpdate(UserProfile(id = 1, name = "", isLoggedIn = false))
        }
    }

    suspend fun onSignedIn(uid: String, name: String, email: String) {
        val local = db.userProfileDao().getProfileDirect() ?: UserProfile()
        val remote = firestoreSync.pullProfile(uid)

        val merged: UserProfile = if (remote != null) {
            // Remote wins for cloud-owned fields.
            local.copy(
                uid = uid,
                isLoggedIn = true,
                name = if (local.name.isNotBlank()) local.name else name,
                email = if (local.email.isNotBlank()) local.email else email,
                goals = remote.goals,
                height = remote.height,
                weight = remote.weight,
                weeklyGoalDays = remote.weeklyGoalDays,
                experiencePoints = remote.experiencePoints,
                streakDays = remote.streakDays,
                workoutProgram = remote.workoutProgram
            )
        } else {
            // No remote profile: keep local values, seed cloud from local.
            local.copy(
                uid = uid,
                isLoggedIn = true,
                name = if (local.name.isNotBlank()) local.name else name,
                email = if (local.email.isNotBlank()) local.email else email
            )
        }

        db.userProfileDao().insertOrUpdate(merged)

        if (remote == null) {
            firestoreSync.pushProfile(uid, merged)
        }
    }

    suspend fun saveProfile(profile: UserProfile) {
        db.userProfileDao().insertOrUpdate(profile)
        if (profile.uid.isNotBlank()) {
            firestoreSync.pushProfile(profile.uid, profile)
        }
    }

    suspend fun addWorkoutSession(exerciseName: String, durationSeconds: Int, reps: Int, formScore: Double, feedback: String) {
        val points = (reps * (formScore / 100.0) * 15).toInt() + 10 // Base points
        val newSession = WorkoutSession(
            exerciseName = exerciseName,
            durationSeconds = durationSeconds,
            reps = reps,
            formScore = formScore,
            pointsEarned = points,
            feedback = feedback
        )
        // 1. Save session
        db.workoutSessionDao().insertSession(newSession)

        // 2. Update user profile
        val profile = db.userProfileDao().getProfileDirect() ?: UserProfile()
        val newXp = profile.experiencePoints + points
        // Dynamic streak handling
        val newStreak = if (profile.streakDays == 0) 1 else profile.streakDays + 1
        val updatedProfile = profile.copy(
            experiencePoints = newXp,
            streakDays = newStreak
        )
        db.userProfileDao().insertOrUpdate(updatedProfile)
        // 3. Update current user on leaderboard
        db.leaderboardDao().updateCurrentUserPoints(newXp)

        // 4. Check & Unlock Badges
        val now = System.currentTimeMillis()
        
        // Badge 1: First Step (Unconditionally unlocked on first session)
        db.badgeDao().unlockBadge("first_step", now)

        // Badge 2: Form Master (Unlocked if form score >= 90%)
        if (formScore >= 90.0) {
            db.badgeDao().unlockBadge("form_master", now)
        }

        // Badge 3: Iron Will (Unlocked if session count >= 3)
        val sessions = db.workoutSessionDao().getAllSessions().firstOrNull() ?: emptyList()
        if (sessions.size >= 2) { // counting current session as well
            db.badgeDao().unlockBadge("iron_will", now)
        }

        // Badge 4: Streak Maker (Streak >= 2 days)
        if (newStreak >= 2) {
            db.badgeDao().unlockBadge("xp_milestone", now)
        }

        // 5. Best-effort cloud sync
        if (updatedProfile.uid.isNotBlank()) {
            firestoreSync.pushSession(updatedProfile.uid, newSession)
            firestoreSync.pushProfile(updatedProfile.uid, updatedProfile)
        }
    }
}
