# Landing Page GA4 Analytics

**Stable slug:** `landing-ga4-analytics`
**Rationale:** Live landing page (`docs/index.html`) has zero visitor tracking. Cannot measure acquisition, conversion, or campaign effectiveness without analytics.
**Status:** In progress
**Acceptance criteria:**
- [x] GA4 gtag.js snippet in `<head>` of `docs/index.html` (configurable `G-MEASUREMENT_ID`)
- [x] Play Store CTA button click events tracked as `play_store_click` GA4 event
- [x] `scripts/setup-web-analytics.sh` automates Firebase Web app + data stream creation
- [x] `docs/robots.txt` and `docs/sitemap.xml` for crawlability
- [ ] Real GA4 measurement ID configured (run `scripts/setup-web-analytics.sh`, set `GA_MEASUREMENT_ID` in CI)
- [ ] Conversion data visible in Google Analytics dashboard
**Evidence:** PR #X with `feat/landing-ga4-analytics` branch.
**Metric impact:** Enables conversion rate measurement (landing → Play Store click → install). Baseline: unknown. Target: measure and report.
**Next candidate:** Configure real measurement ID, set up GA4 conversion goals for Play Store installs.
