# Landing Page Conversion Analytics

**Stable slug:** `landing-page-conversion-analytics`

**Rationale:** The landing page (`docs/index.html`, deployed via GitHub Pages at `https://dzianisv.github.io/KineticAiCoach/`) is the primary organic acquisition surface — search → landing → Play Store install. It has ZERO analytics instrumentation. We cannot measure:
- How many visits the page gets
- Where visitors come from (source/medium)
- How many click through to the Play Store (visit → click CTA conversion rate)

Without this data, every landing page improvement is an unmeasurable guess, and the $10k MRR path cannot be debugged at the top of the funnel. This is the measurement gap that gates all acquisition optimization — adding analytics is the prerequisite before any ad spend, SEO iteration, or conversion rate optimization.

**Status:** Committed + pushed to `feat/landing-page-analytics`, PR open. Ready for review + merge + GA4 measurement ID finalization.

**Acceptance criteria:**
- [x] Google Analytics 4 gtag.js snippet added to `docs/index.html` before `<style>` block
- [x] Measurement ID uses placeholder `G-MEASUREMENT_ID` with setup instructions in HTML comment
- [x] `anonymize_ip: true` set for GDPR compliance
- [x] Outbound click event (`play_store_click`) fires on every CTA that links to Play Store
- [x] Click tracking uses event delegation (single listener, no touches to existing HTML structure)
- [x] Zero changes to app code, backend, or other files
- [x] HTML validates — no broken tags, no JS syntax errors
- [ ] Google Analytics DebugView or real-time report confirms events after measurement ID is configured

**Evidence:**
- File: `docs/index.html` (2 insertion sites: gtag in `<head>`, click tracker before `</main>`)
- Verification: gtag.js async load + config call wrapped in `onclick` guard (no errors on undefined `gtag` if script not loaded)
- Click tracker uses `e.target.closest('[href*="play.google.com/store/apps/details"]')` — covers both hero and bottom CTA without modifying HTML
- No app code, backend, or dependencies changed

**Expected metric impact:** Enables landing page conversion funnel in Google Analytics:
- `page_view` → `play_store_click` events → conversion rate (visit-to-click)
- Source/medium/campaign attribution for any traffic source we drive
- Baseline data before any landing page A/B tests or ad campaigns

**Next steps after merge + measurement ID config:**
1. Swap `G-MEASUREMENT_ID` for real GA4 web stream ID (from Firebase Console → Project Settings or Google Analytics Admin)
2. Verify events appear in GA4 DebugView (Chrome GA4 Debugger extension or `?debug=1` mode)
3. Set up GA4 conversion event for `play_store_click` in Google Analytics Admin
4. Connect GA4 to Google Ads if paid campaigns start
