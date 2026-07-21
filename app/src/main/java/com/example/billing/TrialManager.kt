package com.example.billing

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

private val Context.trialDataStore by preferencesDataStore(name = "trial_prefs")

/**
 * Tracks the app-side, no-credit-card 3-day free trial (length from
 * [BillingConfig.TRIAL_DAYS]).
 *
 * ## Reinstall-resistance rationale
 * A purely local trial marker can be reset by uninstall+reinstall or clearing
 * app data, letting a user farm infinite free trials. To prevent that for real
 * accounts, the authoritative trial start is stored server-side in Firestore at
 * `users/{uid}.trialStartedAt`, keyed by the durable Firebase Auth uid.
 *
 * ## Anonymous vs. durable-uid distinction
 * The app supports Firebase Anonymous Auth ("Continue as Guest"). An anonymous
 * uid is NOT durable — it is regenerated on reinstall, so it is exactly as
 * resettable as local storage. Therefore:
 *  - **Anonymous / no uid:** local DataStore ONLY. We never read or write
 *    Firestore, since doing so would give a false sense of durability while
 *    remaining resettable, and would pollute Firestore with throwaway records.
 *  - **Real (non-anonymous) sign-in:** Firestore is authoritative. On first
 *    reconcile we migrate any earlier local start time up to Firestore (fair to
 *    the user, and blocks trial-extension abuse via guest-then-sign-in), using a
 *    transaction so concurrent reconcile() calls can't create two start values.
 *
 * ## Resolution model
 * [trialStartedAt] starts `null`. On construction we do a one-shot read of the
 * local DataStore value so a fully-offline anonymous user gets a value fast.
 * After that, the value is only updated by [reconcile] calls (which are invoked
 * at ViewModel init and again after every sign-in). A one-shot resolve model is
 * intentionally used instead of a live-collecting flow — it is simpler and
 * correct for this write-rarely, read-often usage.
 *
 * The gate functions ([isTrialActive], [trialDaysRemaining], [trialExpired]) are
 * synchronous, pure reads of `trialStartedAt.value` and the wall clock — they
 * never touch I/O, avoiding ANR risk, mirroring the synchronous gate pattern
 * used elsewhere in this codebase.
 */
class TrialManager(
    private val context: Context,
    private val externalScope: CoroutineScope,
) {

    private val _trialStartedAt = MutableStateFlow<Long?>(null)

    /** Epoch millis when the trial started, or null if not yet resolved/started. */
    val trialStartedAt: StateFlow<Long?> = _trialStartedAt.asStateFlow()

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    init {
        // Best-effort local resolve so an offline/anonymous user has a value fast.
        externalScope.launch {
            val local = readLocal()
            if (local != null && _trialStartedAt.value == null) {
                _trialStartedAt.value = local
            }
        }
    }

    /**
     * Ensures a trial start is recorded and reconciled for the given identity.
     * - uid == null or blank, OR isAnonymous == true  -> local-DataStore-only path.
     * - uid non-blank AND isAnonymous == false          -> Firestore-authoritative path
     *   (reconciles/migrates any existing local value).
     * Safe to call multiple times; never resets an already-started trial.
     * Fire-and-forget: launches its own coroutine on [externalScope] and does
     * not suspend the caller.
     */
    fun reconcile(uid: String?, isAnonymous: Boolean) {
        externalScope.launch {
            if (uid.isNullOrBlank() || isAnonymous) {
                reconcileLocalOnly()
            } else {
                reconcileWithFirestore(uid)
            }
        }
    }

    /** True while now < trialStartedAt + TRIAL_DAYS days. False if not started or ended. */
    fun isTrialActive(): Boolean {
        val start = _trialStartedAt.value ?: return false
        return System.currentTimeMillis() < start + trialWindowMillis()
    }

    /**
     * Days remaining in the trial, 0..TRIAL_DAYS, never negative. Returns
     * TRIAL_DAYS if trialStartedAt is still null (trial not started/resolved yet)
     * so the UI doesn't flash "0 days left" before reconcile() completes.
     */
    fun trialDaysRemaining(): Int {
        val start = _trialStartedAt.value ?: return BillingConfig.TRIAL_DAYS.toInt()
        val end = start + trialWindowMillis()
        val remainingMillis = end - System.currentTimeMillis()
        if (remainingMillis <= 0L) return 0
        val days = Math.ceil(remainingMillis.toDouble() / TimeUnit.DAYS.toMillis(1).toDouble()).toInt()
        return days.coerceIn(0, BillingConfig.TRIAL_DAYS.toInt())
    }

    /** True only once trialStartedAt is known AND the window has elapsed. */
    fun trialExpired(): Boolean {
        val start = _trialStartedAt.value ?: return false
        return System.currentTimeMillis() >= start + trialWindowMillis()
    }

    private fun trialWindowMillis(): Long = TimeUnit.DAYS.toMillis(BillingConfig.TRIAL_DAYS)

    private suspend fun reconcileLocalOnly() {
        val existing = _trialStartedAt.value ?: readLocal()
        val start = existing ?: System.currentTimeMillis()
        if (existing == null) {
            writeLocal(start)
        }
        _trialStartedAt.value = start
    }

    /**
     * Firestore-authoritative path. Uses a transaction so two near-simultaneous
     * reconcile() calls can't race into different trial-start values. If the
     * document already has trialStartedAt, that wins. Otherwise we seed it with
     * the earliest known local value (or now), merged so other profile fields on
     * users/{uid} are never clobbered. Falls back to the local value on error.
     */
    private suspend fun reconcileWithFirestore(uid: String) {
        val local = _trialStartedAt.value ?: readLocal()
        val resolved: Long? = try {
            withContext(Dispatchers.IO) {
                val docRef = firestore.collection("users").document(uid)
                Tasks.await(
                    firestore.runTransaction { transaction ->
                        val snapshot = transaction.get(docRef)
                        val remote = (snapshot.get("trialStartedAt") as? Number)?.toLong()
                        if (remote != null) {
                            remote
                        } else {
                            val start = local ?: System.currentTimeMillis()
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
            Log.w("TrialManager", "reconcileWithFirestore failed", e)
            null
        }

        val effective = resolved ?: local ?: System.currentTimeMillis()
        // Keep the local mirror in sync so offline reads stay authoritative-consistent.
        if (local != effective) {
            writeLocal(effective)
        }
        _trialStartedAt.value = effective
    }

    private suspend fun readLocal(): Long? = try {
        context.trialDataStore.data.first()[TRIAL_STARTED_KEY]
    } catch (e: Exception) {
        Log.w("TrialManager", "readLocal failed", e)
        null
    }

    private suspend fun writeLocal(millis: Long) {
        try {
            context.trialDataStore.edit { prefs ->
                prefs[TRIAL_STARTED_KEY] = millis
            }
        } catch (e: Exception) {
            Log.w("TrialManager", "writeLocal failed", e)
        }
    }

    companion object {
        private val TRIAL_STARTED_KEY: Preferences.Key<Long> =
            longPreferencesKey("trial_started_at_millis")
    }
}
