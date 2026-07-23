package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.example.analytics.Analytics
import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns the Google Play Billing lifecycle for the "Kinetic Pro" subscription.
 *
 * Exposes reactive state ([isPro], [proPlans], [isConnected]) that the rest of
 * the app observes, and two public actions ([launchPurchaseFlow],
 * [queryPurchasesAsync]). All connection, product-details querying and
 * acknowledgement logic is a private implementation detail.
 */
class BillingManager(
    context: Context,
    private val externalScope: CoroutineScope
) : PurchasesUpdatedListener {

    data class PlanOffer(
        val basePlanId: String,
        val formattedPrice: String,   // e.g. "$9.99" — the recurring price, NOT the trial's $0
        val priceAmountMicros: Long,  // raw recurring price in micros (1_000_000 micros = 1 currency unit) for dynamic savings math
        val billingPeriod: String,    // e.g. "P1M" / "P1Y" (ISO-8601 duration of the recurring phase)
        val freeTrialDays: Int?       // parsed from a $0 intro pricing phase if present, else null
    )

    data class ProPlans(
        val monthly: PlanOffer? = null,
        val yearly: PlanOffer? = null
    )

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _proPlans = MutableStateFlow(ProPlans())
    val proPlans: StateFlow<ProPlans> = _proPlans.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    /** The fetched [ProductDetails] for [BillingConfig.PRODUCT_ID_PRO], once loaded. */
    private var productDetails: ProductDetails? = null

    /** Offer token to use when launching the flow, keyed by base plan id. */
    private val offerTokensByBasePlan = mutableMapOf<String, String>()

    /** Firebase Callable client for best-effort server-side purchase verification. */
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()

    /** App package name, captured at construction for the server verify payload. */
    private val appPackageName: String = context.applicationContext.packageName

    private val billingClient: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isConnected.value = true
                    queryProductDetails()
                    queryPurchasesAsync()
                } else {
                    Log.w(TAG, "Billing setup failed: ${billingResult.responseCode} ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                _isConnected.value = false
                Log.w(TAG, "Billing service disconnected; scheduling reconnect.")
                // enableAutoServiceReconnection() should retry automatically, but we add a
                // manual short-backoff reconnect as defense-in-depth in case that flag's
                // behavior isn't honored in some OS/Play Store combination.
                externalScope.launch {
                    delay(RECONNECT_DELAY_MS)
                    startConnection()
                }
            }
        })
    }

    private fun queryProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BillingConfig.PRODUCT_ID_PRO)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "queryProductDetails failed: ${billingResult.responseCode} ${billingResult.debugMessage}")
                return@queryProductDetailsAsync
            }
            val details = productDetailsResult.productDetailsList
                .firstOrNull { it.productId == BillingConfig.PRODUCT_ID_PRO }
            if (details == null) {
                Log.w(TAG, "No ProductDetails returned for ${BillingConfig.PRODUCT_ID_PRO}")
                return@queryProductDetailsAsync
            }
            productDetails = details
            processOffers(details)
        }
    }

    private fun processOffers(details: ProductDetails) {
        val offerDetails = details.subscriptionOfferDetails ?: emptyList()
        offerTokensByBasePlan.clear()

        val monthly = buildPlanOffer(offerDetails, BillingConfig.BASE_PLAN_MONTHLY)
        val yearly = buildPlanOffer(offerDetails, BillingConfig.BASE_PLAN_YEARLY)

        _proPlans.value = ProPlans(monthly = monthly, yearly = yearly)
    }

    private fun buildPlanOffer(
        offerDetails: List<ProductDetails.SubscriptionOfferDetails>,
        basePlanId: String
    ): PlanOffer? {
        val candidates = offerDetails.filter { it.basePlanId == basePlanId }
        val chosen = candidates.firstOrNull { it.offerId != null } ?: candidates.firstOrNull() ?: return null

        val phases = chosen.pricingPhases.pricingPhaseList
        if (phases.isEmpty()) return null

        // The last phase is the recurring price. Any earlier $0 phase is the free trial.
        val recurring = phases.last()
        val freeTrialDays = phases
            .dropLast(1)
            .firstOrNull { it.priceAmountMicros == 0L }
            ?.let { parseIso8601DurationToDays(it.billingPeriod) }

        offerTokensByBasePlan[basePlanId] = chosen.offerToken

        return PlanOffer(
            basePlanId = basePlanId,
            formattedPrice = recurring.formattedPrice,
            priceAmountMicros = recurring.priceAmountMicros,
            billingPeriod = recurring.billingPeriod,
            freeTrialDays = freeTrialDays
        )
    }

    /**
     * Parses a simple ISO-8601 duration into a day count. Handles at least
     * `P<n>D` (days) and `P<n>W` (weeks). Returns `n` for days, `n*7` for weeks,
     * or null if it can't parse.
     */
    private fun parseIso8601DurationToDays(period: String): Int? {
        val match = ISO_DURATION_DAYS_WEEKS.matchEntire(period) ?: return null
        val amount = match.groupValues[1].toIntOrNull() ?: return null
        return when (match.groupValues[2]) {
            "D" -> amount
            "W" -> amount * 7
            else -> null
        }
    }

    /**
     * Launches the Play Billing purchase flow for the given base plan
     * ("monthly" or "yearly"). No-op with a Log.w if productDetails/offer aren't
     * loaded yet or the client isn't connected.
     */
    fun launchPurchaseFlow(activity: Activity, basePlanId: String) {
        val details = productDetails
        val offerToken = offerTokensByBasePlan[basePlanId]
        if (details == null || offerToken == null) {
            Log.w(TAG, "launchPurchaseFlow: product details/offer not loaded for '$basePlanId' yet.")
            return
        }
        if (!billingClient.isReady) {
            Log.w(TAG, "launchPurchaseFlow: billing client not ready.")
            return
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()

        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Analytics.logPurchaseFailed("user_canceled")
            }
            else -> {
                Analytics.logPurchaseFailed(
                    billingResult.debugMessage.ifBlank { "error_${billingResult.responseCode}" }
                )
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        // Ignore anything not fully PURCHASED — never grant entitlement for PENDING
        // purchases; they'll be re-delivered here once finalized.
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        // Basic local integrity checks.
        if (purchase.purchaseToken.isBlank()) return
        if (!purchase.products.contains(BillingConfig.PRODUCT_ID_PRO)) return

        // TODO(server-verification): This only does a local integrity check (purchase state +
        // non-blank token). A follow-up must add a Firebase Function that calls the Play Developer
        // API (purchases.subscriptions.get) with this purchase.purchaseToken to verify the
        // subscription is genuinely active and not refunded/fraudulent before trusting it long-term.
        // Acknowledging locally is sufficient to satisfy Play's 3-day acknowledgment requirement but
        // is NOT proof of a legitimate purchase.

        if (!purchase.isAcknowledged) {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(ackParams) { ackResult ->
                if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isPro.value = true
                    verifyPurchaseServerSide(purchase)
                    Analytics.logPurchaseCompleted(resolveBasePlanId(purchase))
                } else {
                    Log.w(TAG, "acknowledgePurchase failed: ${ackResult.responseCode} ${ackResult.debugMessage}")
                }
            }
        } else {
            // Already acknowledged: covers the queryPurchasesAsync restore path.
            _isPro.value = true
            verifyPurchaseServerSide(purchase)
        }
    }

    /**
     * The [Purchase] object does not expose the base plan id directly, so this is
     * "unknown" unless it becomes resolvable in a future API.
     */
    private fun resolveBasePlanId(purchase: Purchase): String = "unknown"

    /**
     * Fire-and-forget, best-effort server-side verification of a purchase via the
     * `verifySubscription` callable Firebase Function.
     *
     * The server (functions/index.js) now calls the real Google Play Developer API
     * and writes a server-authoritative entitlement to Firestore. This call stays
     * NON-BLOCKING and best-effort: a verify failure (network, cold start, not-yet
     * -deployed) must NEVER revoke a legit local purchase. The server result is
     * logged for observability/reconciliation only.
     */
    private fun verifyPurchaseServerSide(purchase: Purchase) {
        externalScope.launch {
            try {
                val data = hashMapOf(
                    "packageName" to appPackageName,
                    "productId" to (purchase.products.firstOrNull() ?: BillingConfig.PRODUCT_ID_PRO),
                    "purchaseToken" to purchase.purchaseToken
                )
                val result = withContext(Dispatchers.IO) {
                    Tasks.await(functions.getHttpsCallable("verifySubscription").call(data))
                }
                // Server returns { verified, state, expiryTimeMillis, productId }.
                val verified = (result.data as? Map<*, *>)?.get("verified")
                Log.d(TAG, "verifySubscription (non-blocking) verified=$verified data=${result.data}")
            } catch (e: Exception) {
                Log.w(TAG, "verifySubscription call failed (non-blocking, entitlement unaffected)", e)
            }
        }
    }

    /**
     * Surfaces Play's own native "payment declined, please fix your payment method"
     * snackbar (with a deep link into Play to fix it) if the user has a subscription
     * with a payment issue. This is Google's own recommended client-side mechanism
     * for grace-period/account-hold messaging without needing Real-Time Developer
     * Notifications infrastructure. Safe to call anytime (e.g. from onResume of the
     * screen that hosts the Paywall/About tab); no-ops if there's nothing to show.
     */
    fun showInAppMessagesIfNeeded(activity: Activity) {
        val params = InAppMessageParams.newBuilder()
            .addInAppMessageCategoryToShow(InAppMessageParams.InAppMessageCategoryId.TRANSACTIONAL)
            .build()
        billingClient.showInAppMessages(activity, params) { result ->
            if (result.responseCode == InAppMessageResult.InAppMessageResponseCode.SUBSCRIPTION_STATUS_UPDATED) {
                // The user just fixed a payment issue via the in-app message flow.
                // Re-query purchases so isPro reflects the recovered subscription promptly.
                queryPurchasesAsync()
            }
        }
    }

    /**
     * Re-queries active purchases (startup restore AND user-triggered "Restore
     * purchases"). Safe to call anytime; no-ops if the billing client isn't
     * connected yet (the connection callback triggers the initial query too).
     */
    fun queryPurchasesAsync() {
        if (!billingClient.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { _, purchases ->
            if (purchases.isEmpty()) {
                _isPro.value = false
            } else {
                purchases.forEach { handlePurchase(it) }
            }
        }
    }

    private companion object {
        const val TAG = "BillingManager"
        const val RECONNECT_DELAY_MS = 2000L
        val ISO_DURATION_DAYS_WEEKS = Regex("^P(\\d+)([DW])$")
    }
}
