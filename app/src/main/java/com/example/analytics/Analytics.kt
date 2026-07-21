package com.example.analytics

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Thin wrapper around FirebaseAnalytics + FirebaseCrashlytics so call sites
 * throughout the app stay one-liners. Call [init] once (MainViewModel does
 * this in its init block) before logging events; logging before init is a
 * safe no-op so nothing crashes if a call site races startup.
 */
object Analytics {
    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        if (firebaseAnalytics != null) return
        firebaseAnalytics = FirebaseAnalytics.getInstance(context.applicationContext)
        // Crashlytics auto-initializes via its ContentProvider, but we make
        // collection explicit here for clarity/control over crash reporting.
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
    }

    fun logSignInSuccess() = log(EVENT_SIGN_IN_SUCCESS)
    fun logProgramGenerated() = log(EVENT_PROGRAM_GENERATED)
    fun logClassStarted() = log(EVENT_CLASS_STARTED)
    fun logClassCompleted() = log(EVENT_CLASS_COMPLETED)
    fun logPaywallViewed(source: String) = log(EVENT_PAYWALL_VIEWED, bundleOf(PARAM_SOURCE to source))
    fun logPurchaseStarted(basePlanId: String) = log(EVENT_PURCHASE_STARTED, bundleOf(PARAM_BASE_PLAN to basePlanId))
    fun logPurchaseCompleted(basePlanId: String) = log(EVENT_PURCHASE_COMPLETED, bundleOf(PARAM_BASE_PLAN to basePlanId))
    fun logPurchaseFailed(reason: String) = log(EVENT_PURCHASE_FAILED, bundleOf(PARAM_REASON to reason))
    fun logTrialStarted() = log(EVENT_TRIAL_STARTED)
    fun logTrialExpired() = log(EVENT_TRIAL_EXPIRED)
    fun logSubscriptionRestored() = log(EVENT_SUBSCRIPTION_RESTORED)

    private fun log(name: String, params: Bundle? = null) {
        firebaseAnalytics?.logEvent(name, params)
    }

    private const val EVENT_SIGN_IN_SUCCESS = "sign_in_success"
    private const val EVENT_PROGRAM_GENERATED = "program_generated"
    private const val EVENT_CLASS_STARTED = "class_started"
    private const val EVENT_CLASS_COMPLETED = "class_completed"
    private const val EVENT_PAYWALL_VIEWED = "paywall_viewed"
    private const val EVENT_PURCHASE_STARTED = "purchase_started"
    private const val EVENT_PURCHASE_COMPLETED = "purchase_completed"
    private const val EVENT_PURCHASE_FAILED = "purchase_failed"
    private const val EVENT_TRIAL_STARTED = "trial_started"
    private const val EVENT_TRIAL_EXPIRED = "trial_expired"
    private const val EVENT_SUBSCRIPTION_RESTORED = "subscription_restored"
    private const val PARAM_SOURCE = "source"
    private const val PARAM_BASE_PLAN = "base_plan"
    private const val PARAM_REASON = "reason"
}
