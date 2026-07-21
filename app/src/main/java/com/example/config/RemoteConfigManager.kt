package com.example.config

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.tasks.Tasks
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Firebase Remote Config "paywall kill-switch".
 *
 * ## Why this exists
 * The app monetizes via a 3-day no-card trial -> Google Play subscription
 * (`kinetic_pro`, see [com.example.billing.BillingConfig.PRODUCT_ID_PRO]), but
 * that Play product doesn't exist yet (blocked on a founder-side merchant
 * setup step). Without this switch, a user whose trial expires would hit
 * PaywallScreen with no products to show — an infinite "Loading plans…" dead
 * end. This lets us ship the app now for organic growth/ASO with monetization
 * OFF, then flip it on remotely from the Firebase console once `kinetic_pro`
 * exists in Play Console, with zero app update required.
 *
 * ## Fail-safe behavior
 * [paywallEnabled] starts at the in-app default (`false`) and is only ever
 * updated after a *successful* [fetchAndActivate]. A fetch failure (offline,
 * throttled, console misconfigured, etc.) leaves it at its last-known value
 * and never throws — worst case the paywall silently stays off, which is the
 * safe direction (never traps a user).
 */
class RemoteConfigManager(
    @Suppress("UNUSED_PARAMETER") context: Context,
    private val externalScope: CoroutineScope,
) {
    // `context` is accepted (unused) purely so construction mirrors the other
    // per-ViewModel managers (BillingManager(application, viewModelScope),
    // TrialManager(application, viewModelScope)); FirebaseRemoteConfig.getInstance()
    // itself operates on the default FirebaseApp and needs no explicit context.

    private val _paywallEnabled = MutableStateFlow(false)

    /** True once monetization should be enforced. Defaults to false (kill-switch off). */
    val paywallEnabled: StateFlow<Boolean> = _paywallEnabled.asStateFlow()

    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    init {
        val settings = FirebaseRemoteConfigSettings.Builder()
            // In debug builds, skip Remote Config's normal throttling so devs can
            // flip the flag from the console and see it locally without waiting.
            .setMinimumFetchIntervalInSeconds(
                if (BuildConfig.DEBUG) 0L else MINIMUM_FETCH_INTERVAL_SECONDS
            )
            .build()
        remoteConfig.setConfigSettingsAsync(settings)
        remoteConfig.setDefaultsAsync(mapOf(KEY_PAYWALL_ENABLED to false))
        refresh()
    }

    /**
     * Fetches and activates the latest Remote Config values, updating
     * [paywallEnabled]. Safe to call anytime (e.g. app resume) to pick up a
     * console change sooner than the next cold start. Any failure is caught
     * and logged; [paywallEnabled] simply stays at its last-known value.
     */
    fun refresh() {
        externalScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    Tasks.await(remoteConfig.fetchAndActivate())
                }
                _paywallEnabled.value = remoteConfig.getBoolean(KEY_PAYWALL_ENABLED)
            } catch (e: Exception) {
                Log.w(TAG, "fetchAndActivate failed; keeping paywallEnabled=${_paywallEnabled.value}", e)
            }
        }
    }

    private companion object {
        const val TAG = "RemoteConfigManager"
        const val KEY_PAYWALL_ENABLED = "paywall_enabled"
        const val MINIMUM_FETCH_INTERVAL_SECONDS = 3600L
    }
}
