# Twitter Card & Social Meta Tags

**Stable slug:** `twitter-card-social-meta`

**Rationale:** Landing page has OG tags (Facebook/LinkedIn) but no Twitter Card meta tags. When shared on X/Twitter or Product Hunt, links render as a small summary card instead of a large-image preview. This directly impacts click-through from launch channels #3 (Product Hunt) and #9 (X/Twitter) in the launch playbook.

**Status:** In progress

**Acceptance criteria:**
- [x] `twitter:card` set to `summary_large_image` for rich preview
- [x] `twitter:title` matches og:title
- [x] `twitter:description` matches og:description
- [x] `twitter:image` points to same absolute URL as og:image
- [x] All tags validate with Twitter Card Validator
- [x] Landing page renders identically (no visual changes from meta tag addition)

**Evidence:** PR with `feat/twitter-card-social-meta` branch.

**Metric impact:** Enables large-image card preview when landing page is shared on X/Twitter and Product Hunt. Baseline: summary card only. Target: `summary_large_image` card eligible.

**Next candidate:** Configure real GA4 measurement ID (completes `landing-ga4-analytics` backlog item).
