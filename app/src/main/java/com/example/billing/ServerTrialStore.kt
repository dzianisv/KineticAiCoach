package com.example.billing

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Abstracts the server-authoritative side of the trial reconcile so
 * [TrialManager] is testable without a real Firestore instance.
 */
fun interface ServerTrialStore {
    /**
     * Returns the authoritative trial start for [uid], seeding the server
     * record from [localStartMillis] (or now) if none exists yet. Returns
     * null on failure so the caller can fall back to the local value.
     */
    suspend fun reconcile(uid: String, localStartMillis: Long?): Long?
}

/**
 * Firestore-backed [ServerTrialStore]. Uses a transaction so two
 * near-simultaneous reconcile() calls can't race into different trial-start
 * values. If `users/{uid}.trialStartedAt` already exists, that value wins.
 * Otherwise it is seeded with [localStartMillis] (or now), merged so other
 * profile fields on users/{uid} are never clobbered.
 */
class DefaultServerTrialStore(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
) : ServerTrialStore {

    override suspend fun reconcile(uid: String, localStartMillis: Long?): Long? = try {
        withContext(Dispatchers.IO) {
            val docRef = firestore.collection("users").document(uid)
            Tasks.await(
                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)
                    val remote = (snapshot.get("trialStartedAt") as? Number)?.toLong()
                    if (remote != null) {
                        remote
                    } else {
                        val start = localStartMillis ?: clockMillis()
                        transaction.set(
                            docRef,
                            mapOf("trialStartedAt" to start),
                            SetOptions.merge()
                        )
                        start
                    }
                }
            )
        }
    } catch (e: Exception) {
        Log.w("DefaultServerTrialStore", "reconcile failed", e)
        null
    }
}
