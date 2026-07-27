# Landing Page Consolidation

**Stable slug:** `landing-page-consolidation`
**Rationale:** Two diverged copies of the landing page — `landing/index.html` (deployed, light theme, has SEO/Twitter/JSON-LD tags) and `docs/index.html` (not deployed, polished dark theme with demo video + screenshot gallery, no SEO/GA4 tags). The demo video and asset files live in `docs/media/` and `docs/play-store/` but are not deployed because `gh-pages` branch only contained two files. Fix: consolidate into a single canonical `docs/index.html` with both the polished dark design AND all SEO/GA4 tags, deploy `docs/` to `gh-pages` via CI.
**Status:** In progress
**Acceptance criteria:**
- [x] `docs/index.html` has all meta tags: canonical URL, OG (with absolute image URL), Twitter Cards summary_large_image, JSON-LD SoftwareApplication schema, JSON-LD FAQPage schema
- [x] GA4 gtag.js snippet + Play Store click tracking in `docs/index.html`
- [x] `landing/index.html` removed (no longer needed — consolidated into `docs/index.html`)
- [x] CI workflow (`.github/workflows/deploy-landing.yml`) deploys `docs/` to `gh-pages` on push
- [x] `robots.txt` and `sitemap.xml` in `docs/` for crawlability
- [ ] Live site verified at `https://dzianisv.github.io/KineticAiCoach/` after deploy
- [ ] Stale PRs #12-#18 closed as superseded
**Evidence:** PR #19 — branch `feat/landing-page-consolidation`.
**Metric impact:** Single canonical landing page eliminates maintenance confusion. Demo video now served (conversion lift expected). GA4 enables acquisition funnel measurement. Expected: +10-20% Play Store click-through from landing page once demo video renders. Next: configure real GA4 measurement ID.
**Next candidate:** Set up real GA4 measurement ID via Firebase Web stream, then monitor landing page conversion in GA4 dashboard.
