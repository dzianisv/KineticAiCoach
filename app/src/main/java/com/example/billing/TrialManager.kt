package com.example.billing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Tracks the app-side, no-credit-card 3-day free trial (length from
 * [BillingConfig.TRIAL_DAYS]).
 *
 * ## Reinstall-resistance rationale
 * A purely local trial marker can be reset by uninstall+reinstall or clearing
 * app data, letting a user farm infinite free trials. To prevent that for real
 * accounts, the authoritative trial start is stored server-side (see
 * [ServerTrialStore]) keyed by the durable Firebase Auth uid.
 *
 * ## Anonymous vs. durable-uid distinction
 * The app supports Firebase Anonymous Auth ("Continue as Guest"). An anonymous
 * uid is NOT durable — it is regenerated on reinstall, so it is exactly as
 * resettable as local storage. Therefore:
 *  - **Anonymous / no uid:** local store ONLY. We never read or write the
 *    server, since doing so would give a false sense of durability while
 *    remaining resettable, and would pollute the backend with throwaway
 *    records.
 *  - **Real (non-anonymous) sign-in:** the server is authoritative. On first
 *    reconcile we migrate any earlier local start time up to the server (fair
 *    to the user, and blocks trial-extension abuse via guest-then-sign-in).
 *
 * ## Resolution model
 * [trialStartedAt] starts `null`. On construction we do a one-shot read of the
 * local store value so a fully-offline anonymous user gets a value fast.
 * After that, the value is only updated by [reconcile] calls (which are invoked
 * at ViewModel init and again after every sign-in). A one-shot resolve model is
 * intentionally used instead of a live-collecting flow — it is simpler and
 * correct for this write-rarely, read-often usage.
 *
 * The gate functions ([isTrialActive], [trialDaysRemaining], [trialExpired]) are
 * synchronous, pure reads of `trialStartedAt.value` and [clockMillis] — they
 * never touch I/O, avoiding ANR risk, mirroring the synchronous gate pattern
 * used elsewhere in this codebase.
 *
 * [trialLocalStore], [clockMillis], and [serverTrialStore] are injectable so
 * business logic can be unit-tested without Android DataStore or Firestore
 * (see `TrialManagerTest`).
 */
class TrialManager(
    private val externalScope: CoroutineScope,
    private val trialLocalStore: TrialLocalStore,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
    private val serverTrialStore: ServerTrialStore = DefaultServerTrialStore(
        clockMillis = clockMillis,
    ),
) {

    private val _trialStartedAt = MutableStateFlow<Long?>(null)

    /** Epoch millis when the trial started, or null if not yet resolved/started. */
    val trialStartedAt: StateFlow<Long?> = _trialStartedAt.asStateFlow()

    /**
     * Job for the best-effort local resolve kicked off at construction, so an
     * offline/anonymous user has a value fast. Exposed (vs. fire-and-forget)
     * so tests can deterministically await the initial resolve instead of
     * racing the dispatcher; production callers can ignore it.
     */
    val initJob: Job = externalScope.launch {
        val local = trialLocalStore.readStartMillis()
        if (local != null && _trialStartedAt.value == null) {
            _trialStartedAt.value = local
        }
    }

    /**
     * Ensures a trial start is recorded and reconciled for the given identity.
     * - uid == null or blank, OR isAnonymous == true  -> local-store-only path.
     * - uid non-blank AND isAnonymous == false          -> server-authoritative path
     *   (reconciles/migrates any existing local value).
     * Safe to call multiple times; never resets an already-started trial.
     * Launches its own coroutine on [externalScope] and does not suspend the
     * caller — production call sites fire-and-forget. Returns the [Job] so
     * tests can deterministically await completion instead of racing the
     * dispatcher.
     */
    fun reconcile(uid: String?, isAnonymous: Boolean): Job = externalScope.launch {
        if (uid.isNullOrBlank() || isAnonymous) {
            reconcileLocalOnly()
        } else {
            reconcileWithServer(uid)
        }
    }

    /** True while now < trialStartedAt + TRIAL_DAYS days. False if not started or ended. */
    fun isTrialActive(): Boolean {
        val start = _trialStartedAt.value ?: return false
        return clockMillis() < start + trialWindowMillis()
    }

    /**
     * Days remaining in the trial, 0..TRIAL_DAYS, never negative. Returns
     * TRIAL_DAYS if trialStartedAt is still null (trial not started/resolved yet)
     * so the UI doesn't flash "0 days left" before reconcile() completes.
     */
    fun trialDaysRemaining(): Int {
        val start = _trialStartedAt.value ?: return BillingConfig.TRIAL_DAYS.toInt()
        val end = start + trialWindowMillis()
        val remainingMillis = end - clockMillis()
        if (remainingMillis <= 0L) return 0
        val days = Math.ceil(remainingMillis.toDouble() / TimeUnit.DAYS.toMillis(1).toDouble()).toInt()
        return days.coerceIn(0, BillingConfig.TRIAL_DAYS.toInt())
    }

    /** True only once trialStartedAt is known AND the window has elapsed. */
    fun trialExpired(): Boolean {
        val start = _trialStartedAt.value ?: return false
        return clockMillis() >= start + trialWindowMillis()
    }

    private fun trialWindowMillis(): Long = TimeUnit.DAYS.toMillis(BillingConfig.TRIAL_DAYS)

    private suspend fun reconcileLocalOnly() {
        val existing = _trialStartedAt.value ?: trialLocalStore.readStartMillis()
        val start = existing ?: clockMillis()
        if (existing == null) {
            trialLocalStore.writeStartMillis(start)
        }
        _trialStartedAt.value = start
    }

    /**
     * Server-authoritative path: delegates to [serverTrialStore], keeping the
     * local mirror in sync so offline reads stay authoritative-consistent.
     * Falls back to the local value (or now) if the server call fails.
     */
    private suspend fun reconcileWithServer(uid: String) {
        val local = _trialStartedAt.value ?: trialLocalStore.readStartMillis()
        val resolved = serverTrialStore.reconcile(uid, local)
        val effective = resolved ?: local ?: clockMillis()
        if (local != effective) {
            trialLocalStore.writeStartMillis(effective)
        }
        _trialStartedAt.value = effective
    }
}
