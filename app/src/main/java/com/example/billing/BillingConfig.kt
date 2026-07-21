package com.example.billing

/**
 * Centralized, easy-to-tune monetization constants.
 *
 * Pricing note: PRICES ARE NOT HARDCODED HERE — the real price/period/currency
 * shown in the UI always comes from Play's ProductDetails at runtime (see
 * BillingManager.PlanOffer.formattedPrice / priceAmountMicros). The numbers in
 * this KDoc are just what will be configured in Play Console for reference:
 *   - monthly base plan: $7.25/mo
 *   - yearly base plan:  $43.50/yr  (~50% cheaper than paying monthly all year)
 */
object BillingConfig {
    const val PRODUCT_ID_PRO = "kinetic_pro"
    const val BASE_PLAN_MONTHLY = "monthly"
    const val BASE_PLAN_YEARLY = "yearly"

    /**
     * Length of the app-side, no-credit-card free trial granted on a user's
     * first authenticated launch. This REPLACES the old per-week usage cap —
     * there is no more "N classes per week" limit; after the trial window,
     * value features require an active kinetic_pro subscription.
     */
    const val TRIAL_DAYS = 3L

    /**
     * Deep link into Play Store's native subscription-management center for this
     * specific subscription product, scoped to this app's package. Used by
     * "Manage subscription" actions in the Paywall/About screens.
     */
    fun manageSubscriptionUrl(applicationId: String): String =
        "https://play.google.com/store/account/subscriptions?sku=$PRODUCT_ID_PRO&package=$applicationId"
}
