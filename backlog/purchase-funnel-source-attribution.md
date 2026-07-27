# Purchase Funnel Source Attribution

**Stable slug:** `purchase-funnel-source-attribution`

**Rationale:** Monetization is LIVE (subscription `kinetic_pro`, paywall_enabled=true) but the purchase-funnel analytics could not answer *which paywall entry point converts*. `paywall_viewed` carried a `source`, but `purchase_started`, `purchase_completed`, and `purchase_failed` did not — and there was no `paywall_dismissed` event at all. Without source threaded through the full funnel, we cannot compute per-entry-point conversion (view → start → complete) or abandonment (view → dismiss), so we cannot optimize where/when to show the paywall. With 0→first paying users imminent, closing this measurement gap is a prerequisite for revenue optimization toward $10k MRR. Also, `resolveBasePlanId` returned a hardcoded `"unknown"`, so `purchase_completed` could not distinguish monthly vs yearly — blocking plan-mix / ARPU analysis.

**Status:** Done — verified green locally. Uncommitted on branch `docs/aso-pack`; pending human-approved commit.

**Acceptance criteria:**
- [x] `paywall_dismissed` event added with `source` param (`Analytics.logPaywallDismissed`)
- [x] `MainViewModel.dismissPaywall()` logs `paywall_dismissed` with the current `_paywallSource`
- [x] `purchase_started` / `purchase_completed` / `purchase_failed` accept and log a `source` param (default `""`, backward-compatible)
- [x] `_paywallSource` threaded from `MainViewModel` → `Analytics` and → `BillingManager.launchPurchaseFlow`
- [x] `BillingManager` retains `currentPaywallSource` and stamps it onto async billing callbacks (`USER_CANCELED`, error, completed)
- [x] `resolveBasePlanId` returns the real chosen base plan (from last `launchPurchaseFlow`) instead of `"unknown"` — covers the sequential single-flow case Play Billing enforces
- [x] Debug Kotlin compiles clean (no errors)
- [x] All existing unit tests remain green (30/30)

**Evidence:**
- Files: `app/src/main/java/com/example/analytics/Analytics.kt`, `app/src/main/java/com/example/ui/MainViewModel.kt`, `app/src/main/java/com/example/billing/BillingManager.kt`
- Verification: `GRADLE_USER_HOME=.scratch/gradle-home ./gradlew :app:compileDebugKotlin :app:testDebugUnitTest` → **BUILD SUCCESSFUL** (2026-07-24), **30 tests / 0 failures / 0 errors / 0 skipped**. Three funnel-source files compile clean (only pre-existing `bundleOf` deprecation warnings).
- Consistency check: `_paywallSource` set at MainViewModel.kt:827, consumed at :833/:838/:839; `PARAM_SOURCE="source"` and `EVENT_PAYWALL_DISMISSED="paywall_dismissed"` defined in Analytics.kt.

**Expected metric impact:** Enables per-entry-point funnel analysis in Firebase Analytics: conversion (`paywall_viewed` → `purchase_started` → `purchase_completed`) and abandonment (`paywall_viewed` → `paywall_dismissed`) sliced by `source`, plus monthly-vs-yearly plan mix on `purchase_completed`. Leading indicator: once first users exist, DebugView / BigQuery shows `source` and correct `base_plan` populated on all purchase events.

**Not in scope this run:** commit/merge (guardrail — needs approval), deploy, and the other uncommitted items (`server-subscription-verification`, `firestore-security-rules`, `landing-page-structured-data`) which are tracked separately.
