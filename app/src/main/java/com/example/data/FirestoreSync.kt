package com.example.data

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Best-effort cloud sync of user profile and workout sessions to Firestore.
 * All operations swallow errors so local Room writes are never blocked by network issues.
 */
class FirestoreSync {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    init {
        // Offline persistence can only be configured before the first Firestore use.
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            firestore.firestoreSettings = settings
        } catch (e: Exception) {
            Log.w("FirestoreSync", "Failed to enable Firestore persistence", e)
        }
    }

    suspend fun pushProfile(uid: String, profile: UserProfile) {
        if (uid.isBlank()) return
        try {
            withContext(Dispatchers.IO) {
                Tasks.await(
                    firestore.collection("users")
                        .document(uid)
                        .set(profileToMap(profile), SetOptions.merge())
                )
            }
        } catch (e: Exception) {
            Log.w("FirestoreSync", "pushProfile failed", e)
        }
    }

    suspend fun pushSession(uid: String, session: WorkoutSession) {
        if (uid.isBlank()) return
        try {
            withContext(Dispatchers.IO) {
                Tasks.await(
                    firestore.collection("users")
                        .document(uid)
                        .collection("workoutSessions")
                        .add(sessionToMap(session))
                )
            }
        } catch (e: Exception) {
            Log.w("FirestoreSync", "pushSession failed", e)
        }
    }

    suspend fun pullProfile(uid: String): UserProfile? {
        if (uid.isBlank()) return null
        return try {
            withContext(Dispatchers.IO) {
                val snapshot = Tasks.await(
                    firestore.collection("users").document(uid).get()
                )
                if (!snapshot.exists()) {
                    null
                } else {
                    UserProfile(
                        id = 1,
                        uid = uid,
                        name = snapshot.getString("name") ?: "",
                        email = snapshot.getString("email") ?: "",
                        goals = snapshot.getString("goals") ?: "",
                        height = (snapshot.get("height") as? Number)?.toDouble() ?: 0.0,
                        weight = (snapshot.get("weight") as? Number)?.toDouble() ?: 0.0,
                        weeklyGoalDays = (snapshot.get("weeklyGoalDays") as? Number)?.toInt() ?: 3,
                        experiencePoints = (snapshot.get("experiencePoints") as? Number)?.toInt() ?: 0,
                        streakDays = (snapshot.get("streakDays") as? Number)?.toInt() ?: 0,
                        workoutProgram = snapshot.getString("workoutProgram") ?: ""
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("FirestoreSync", "pullProfile failed", e)
            null
        }
    }

    private fun profileToMap(profile: UserProfile): Map<String, Any?> = mapOf(
        "name" to profile.name,
        "email" to profile.email,
        "goals" to profile.goals,
        "height" to profile.height,
        "weight" to profile.weight,
        "weeklyGoalDays" to profile.weeklyGoalDays,
        "experiencePoints" to profile.experiencePoints,
        "streakDays" to profile.streakDays,
        "workoutProgram" to profile.workoutProgram
    )

    private fun sessionToMap(session: WorkoutSession): Map<String, Any?> = mapOf(
        "exerciseName" to session.exerciseName,
        "durationSeconds" to session.durationSeconds,
        "reps" to session.reps,
        "formScore" to session.formScore,
        "pointsEarned" to session.pointsEarned,
        "feedback" to session.feedback,
        "timestamp" to session.timestamp
    )
}
