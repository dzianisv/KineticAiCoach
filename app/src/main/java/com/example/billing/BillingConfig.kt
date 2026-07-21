package com.example.billing

/**
 * Centralized, easy-to-tune monetization constants. Keeping the free usage
 * limit here (rather than scattered through the app) matters because it is
 * also a cost cap on Gemini Vertex AI calls: every AI-analyzed class costs
 * real money, so this number directly controls infra spend for free users.
 */
object BillingConfig {
    const val PRODUCT_ID_PRO = "kinetic_pro"
    const val BASE_PLAN_MONTHLY = "monthly"
    const val BASE_PLAN_YEARLY = "yearly"

    /** Free tier: max AI-analyzed classes allowed in a rolling window. Tune here only. */
    const val FREE_WEEKLY_CLASS_LIMIT = 3
    const val FREE_WINDOW_DAYS = 7L
}
