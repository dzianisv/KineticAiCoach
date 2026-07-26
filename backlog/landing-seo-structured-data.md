# Landing Page SEO Structured Data

**Stable slug:** `landing-seo-structured-data`
**Rationale:** The new landing page (`docs/index.html`) at `feat/landing-ga4-analytics` is missing schema.org structured data (SoftwareApplication + FAQPage), canonical URL, proper OG:url, and absolute OG:image URLs — all present in the old `landing/index.html`. Without these, Google cannot show rich results (FAQ expandables, app install buttons) and may index the wrong canonical URL. This directly impacts organic acquisition — the primary channel for $10k MRR.
**Status:** In progress
**Acceptance criteria:**
- [x] Canonical URL `<link>` pointing to `https://dzianisv.github.io/KineticAiCoach/`
- [x] OG:url with absolute URL
- [x] OG:image with absolute URL (not relative path)
- [x] Schema.org `SoftwareApplication` JSON-LD (name, OS, category, Play Store URL, subscription offer)
- [x] Schema.org `FAQPage` JSON-LD matching all 5 FAQ questions on the page
- [x] All JSON-LD validates with no syntax errors
- [x] Landing page renders identically (no visual changes from structured data addition)
**Evidence:** PR with `feat/landing-seo-structured-data` branch.
**Metric impact:** Enables Google rich results (FAQ snippets, software app badge) for the landing page. Baseline: zero structured data on new landing page. Target: FAQ rich results eligible, software app schema present.
**Next candidate:** Configure real GA4 measurement ID (completes `landing-ga4-analytics` backlog item).
