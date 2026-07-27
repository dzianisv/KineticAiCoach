# Landing Page Structured Data & Social Meta

**Stable slug:** `landing-page-structured-data`
**Rationale:** The landing page at `https://dzianisv.github.io/KineticAiCoach/` is the primary organic acquisition surface (Play Store → landing page → install). Without SoftwareApplication JSON-LD structured data, Google cannot show rich results (Install button, rating stars) in search results. Without absolute Open Graph / Twitter Card URLs, share cards render broken images on social platforms. Fixing both directly improves organic click-through rate — the #1 bottleneck (0 installs).
**Status:** Complete — local edit ready, pending push to main for GitHub Pages deploy.
**Acceptance criteria:**
- [x] JSON-LD SoftwareApplication structured data present with name, operatingSystem, applicationCategory, offers, sameAs (Play Store URL), author, datePublished
- [x] og:url and og:image use absolute HTTPS URLs
- [x] twitter:card set to summary_large_image with title, description, image
- [x] JSON-LD validated as syntactically correct JSON
- [x] HTML structure fully intact (12/12 checks pass)
- [x] All relative asset paths preserved (video, icon, screenshots)
- [ ] Rich Results Test confirms eligible rich result after deploy
**Evidence:** File at `docs/index.html`. JSON-LD snippet validated via Python `json.loads`. HTML structure verified with 12 automated checks (all PASS). Not yet deployed — requires `git push origin main`.
