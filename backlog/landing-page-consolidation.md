# Landing Page Consolidation

**Stable slug:** `landing-page-consolidation`
**Rationale:** Two diverged copies of the landing page — `landing/index.html` (deployed, light theme, has SEO/Twitter/JSON-LD tags) and `docs/index.html` (not deployed, polished dark theme with demo video + screenshot gallery, no SEO/GA4 tags). The demo video and asset files live in `docs/media/` and `docs/play-store/` but are not deployed because `gh-pages` branch only contained two files. Fix: consolidate into a single canonical `docs/index.html` with both the polished dark design AND all SEO/GA4 tags, deploy `docs/` to `gh-pages` via CI.
**Status:** Done — merged to `main` and live.
**Acceptance criteria:**
- [x] `docs/index.html` has all meta tags: canonical URL, OG (with absolute image URL), Twitter Cards summary_large_image, JSON-LD SoftwareApplication schema, JSON-LD FAQPage schema — verified present in `origin/main:docs/index.html`
- [x] GA4 gtag.js snippet + Play Store click tracking in `docs/index.html` — present, but see caveat below
- [x] `landing/index.html` removed (no longer needed — consolidated into `docs/index.html`) — verified absent from `origin/main`
- [x] CI workflow (`.github/workflows/deploy-landing.yml`) deploys `docs/` to `gh-pages` on push — verified present on `origin/main`; its first run on merge succeeded (`gh run list --workflow=deploy-landing.yml`)
- [x] `robots.txt` and `sitemap.xml` in `docs/` for crawlability — verified present on `origin/main` and live (both return HTTP 200)
- [x] Live site verified at `https://dzianisv.github.io/KineticAiCoach/` after deploy — HTTP 200, confirmed serving updated `docs/index.html` (canonical tag, twitter:card, GA4 snippet all present in served HTML); `robots.txt` and `sitemap.xml` also live at HTTP 200
- [x] Stale PRs #12-#18 closed as superseded — all seven confirmed CLOSED (not merged) via `gh pr view`
**Known caveat:** `docs/index.html` ships with a literal placeholder GA4 measurement ID (`G-MEASUREMENT_ID`, ~lines 416/421) instead of a real property ID — analytics currently collects nothing live. This is founder-owned config (real GA4 property ID) and was intentionally not fabricated. Follow-up: founder sets up a real GA4 Web stream and swaps the ID in `docs/index.html`.
**Evidence:** PR #20 — branch `feat/landing-page-consolidation`, merged to `main` as squash commit `b48bbcb1ad8bf6dba164388814f4e767962fba67`. (PR #19 covered the same SEO/meta-tag work but was closed as superseded by #20, which is a strict superset including the GA4/CI/consolidation work.)
**Metric impact:** Single canonical landing page eliminates maintenance confusion. Demo video now served (conversion lift expected). GA4 wiring is in place but inert until a real measurement ID is configured — no funnel data collected yet.
**Next candidate:** Founder sets a real GA4 measurement ID via Firebase Web stream, then monitor landing page conversion in GA4 dashboard.
