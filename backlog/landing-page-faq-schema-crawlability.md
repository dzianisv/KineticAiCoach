# Landing Page FAQ Schema & Crawlability

**Stable slug:** `landing-page-faq-schema-crawlability`

**Rationale:** The product has 0 installs — acquisition is the P0 bottleneck, and the landing page at `https://dzianisv.github.io/KineticAiCoach/` is the primary organic acquisition surface (search → landing → Play install). The page already ships `SoftwareApplication` JSON-LD and a visible 5-question FAQ section, but (1) the FAQ had **no `FAQPage` structured data**, so Google could not render FAQ rich snippets that expand the page's SERP footprint and CTR, and (2) there was **no `robots.txt` or `sitemap.xml`**, weakening crawl/index signals for the GitHub Pages site. Both are additive, reversible, deploy-free on-page SEO wins that directly enlarge the organic surface without touching app code or requiring blocked human-only actions.

**Status:** Done — verified locally. Uncommitted on branch `docs/aso-pack`; pending human-approved commit + push (GitHub Pages deploy).

**Acceptance criteria:**
- [x] `FAQPage` JSON-LD added to `docs/index.html` as a distinct second `<script type="application/ld+json">` block (not merged into `SoftwareApplication`)
- [x] `mainEntity` array has exactly 5 `Question` objects, each with `name` + `acceptedAnswer.text` matching the on-page FAQ; HTML tags stripped to plain text (privacy answer's `<a>` → "See our Privacy Policy for full detail.")
- [x] Existing `SoftwareApplication` JSON-LD left intact and still valid JSON
- [x] `docs/robots.txt` created with `Allow: /` and a `Sitemap:` line
- [x] `docs/sitemap.xml` created, well-formed, 2 `<url>` entries (home priority 1.0, privacy 0.3), correct `http://www.sitemaps.org/schemas/sitemap/0.9` namespace, lastmod 2026-07-24
- [x] No unrelated files touched (footprint = the 3 target files only)
- [ ] Google Rich Results Test confirms FAQ eligibility after deploy (post-deploy, human-gated)

**Evidence:**
- Files: `docs/index.html` (FAQPage block), `docs/robots.txt`, `docs/sitemap.xml`
- Verification (actual output): both JSON-LD blocks `json.loads` cleanly (2 blocks found; SoftwareApplication PASS, FAQPage PASS with 5 mainEntity items, each non-empty name + acceptedAnswer.text); `xml.dom.minidom` parses sitemap → well-formed, 2 `<url>` elements; robots.txt contains Sitemap line (line 3).
- `git status --short` footprint = `M docs/index.html`, `?? docs/robots.txt`, `?? docs/sitemap.xml` (all other dirty/untracked entries pre-existing, not touched).

**Expected metric impact:** Enables FAQ rich results (expandable Q&A) in Google SERP for the landing page, enlarging its footprint and organic CTR; robots.txt + sitemap improve crawl discovery/indexing of the two public pages. Leading indicator after deploy: Rich Results Test shows the page eligible for FAQ rich result; Search Console reports sitemap submitted + FAQ enhancement detected.

**Not in scope this run:** commit/push (guardrail — needs approval), and actual SERP/Search Console verification (requires the change to be live on GitHub Pages).
