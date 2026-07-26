# Landing Page SEO

**Stable slug:** `landing-page-seo`

**Rationale:** The landing page (`https://dzianisv.github.io/KineticAiCoach/`) is the primary entry point from HN, Product Hunt, Reddit, and social links. It had no sitemap.xml, robots.txt, canonical URL, Twitter Card meta tags, or JSON-LD structured data — meaning Google, Twitter, and other crawlers were not being given proper indexing signals or rich snippet opportunities.

**Status:** Shipped. PR #14 — live verification pending GitHub Pages deploy.

**Acceptance criteria:**
- [x] `sitemap.xml` lists both landing page and privacy policy with proper `lastmod`/`changefreq`/`priority`
- [x] `robots.txt` allows all crawlers and points to sitemap
- [x] `canonical` link tag prevents duplicate content issues
- [x] `og:url` and `og:site_name` meta tags for Facebook/LinkedIn
- [x] `twitter:card` (summary_large_image), `twitter:title`, `twitter:description`, `twitter:image` meta tags
- [x] `og:image` URL is absolute (previously relative — broken in shared cards)
- [x] JSON-LD `SoftwareApplication` schema with app name, OS, category, description, Play Store link, pricing offer, and publisher

**Evidence:**
- `docs/sitemap.xml` — valid XML with 2 URLs, verified via `xml.etree.ElementTree`
- `docs/robots.txt` — `Allow: /` + sitemap directive
- `docs/index.html` — JSON-LD extracted and validated as valid `SoftwareApplication` schema
- All three files committed on branch `feat/landing-page-seo`

**Expected metric impact:**
- Google indexes the landing page faster with correct canonical (vs. potentially treating `dzianisv.github.io/KineticAiCoach` and `dzianisv.github.io/KineticAiCoach/` as dupes)
- Twitter/Facebook link previews display a large summary card with the feature graphic (previously had no card and a relative image URL)
- Google Search may show rich snippet (SoftwareApplication) with rating/price context
- Sitemap ensures privacy-policy URL is discoverable

**Next candidate:** Implement `verifySubscription` Cloud Function with real Play Developer API verification (requires Play Console service account); or add Functions CI workflow to run Node unit tests independently.
