# Server-Side Subscription Verification (verifySubscription)

**Stable slug:** `server-subscription-verification`

**Rationale:** verifySubscription existed as a stub returning `verified: false`. The app relied entirely on client-side Play Billing purchase acknowledgment for entitlement — a technically sophisticated user could bypass the paywall. With monetization now live (subscriptions created, paywall on), server-side entitlement hardening is the #1 revenue-protection gap.

**Status:** Code complete, tested, pending SA grant + deploy.

**Acceptance criteria:**
- [x] `functions/lib/verifySubscription.js` contains pure business logic (verifyPurchase)
- [x] `verifyPurchase` accepts injectable `fetchFn` for testability
- [x] Real implementation `fetchSubscriptionFromPlay` calls Google Play Developer API purchases.subscriptionsv2.get via ADC
- [x] `functions/index.js` wires verifyPurchase as the Firebase callable verifySubscription
- [x] SUBSCRIPTION_STATE_ACTIVE and SUBSCRIPTION_STATE_IN_GRACE_PERIOD → `verified: true`
- [x] All other states (EXPIRED, CANCELED, PAUSED, ON_HOLD, UNKNOWN) → `verified: false`
- [x] Returns: verified, subscriptionState, latestOrderId, expiryTime, productId
- [x] 11 unit tests pass (all states, edge cases, error propagation)

**Deployment dependency:** Cloud Functions runtime SA `kinetic-ai-coach-50627@appspot.gserviceaccount.com` must be granted the `androidpublisher` role in Google Cloud IAM, and "Enable API access" must be linked in Play Console. Without this, the function throws 403 at runtime.

**Evidence:**
- Implementation: `functions/lib/verifySubscription.js`
- Firebase callable wrapper: `functions/index.js`
- Tests: `functions/test/verifySubscription.test.mjs` — 11/11 pass
- Prod verification: `firebase deploy --only functions` (blocked by SA grant)
