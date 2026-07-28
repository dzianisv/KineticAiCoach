package com.example.billing

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.trialDataStore by preferencesDataStore(name = "trial_prefs")

/**
 * Abstracts the on-device trial-start persistence so [TrialManager] is
 * testable without a real Android DataStore/Context.
 */
interface TrialLocalStore {
    suspend fun readStartMillis(): Long?
    suspend fun writeStartMillis(millis: Long)
}

/** Production [TrialLocalStore] backed by Jetpack DataStore Preferences. */
class DataStoreTrialLocalStore(
    private val context: Context,
) : TrialLocalStore {

    override suspend fun readStartMillis(): Long? = try {
        context.trialDataStore.data.first()[TRIAL_STARTED_KEY]
    } catch (e: Exception) {
        Log.w("DataStoreTrialLocalStore", "readStartMillis failed", e)
        null
    }

    override suspend fun writeStartMillis(millis: Long) {
        try {
            context.trialDataStore.edit { prefs ->
                prefs[TRIAL_STARTED_KEY] = millis
            }
        } catch (e: Exception) {
            Log.w("DataStoreTrialLocalStore", "writeStartMillis failed", e)
        }
    }

    companion object {
        private val TRIAL_STARTED_KEY: Preferences.Key<Long> =
            longPreferencesKey("trial_started_at_millis")
    }
}
