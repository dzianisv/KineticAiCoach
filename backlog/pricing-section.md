# Landing Page Pricing Section

**Stable slug:** `pricing-section`

**Rationale:** The landing page is the primary acquisition entry point but had zero pricing information — users had to click through to the Play Store to learn about cost. Adding a comparison section (Free Trial vs Kinetic Pro subscription) reduces purchase friction and increases conversion rate.

**Status:** Shipped. PR #15.

**Acceptance criteria:**
- [x] Pricing section appears between screenshots gallery and FAQ
- [x] Trial card: shows "Free" with 3-day trial, lists core features (rep counting, pose overlay, audio coaching, local history)
- [x] Kinetic Pro card: shows "$7.25/month", lists Pro features (unlimited coaching, personalized programs, coach chat, cloud sync, advanced analytics)
- [x] "Most popular" badge on Pro card
- [x] Both cards link to Play Store listing
- [x] Responsive 2-column grid collapses to 1 column on mobile (<640px)
- [x] Uses existing design tokens (dark theme vars, card pattern, CTA button)
- [x] No backend changes — pure HTML/CSS

**Evidence:**
- `docs/index.html` — 59 lines added for pricing section + CSS
- HTML validation: doctype present, all tags balanced
- CSS uses existing `--card`, `--border`, `--accent`, `--radius`, `--muted`, `--text` variables
- PR #15 — pending GitHub Pages deploy for live verification

**Expected metric impact:**
- Increased Play Store click-to-install conversion rate (baseline: zero pricing info → users leave page to find pricing before deciding)
- Reduced bounce rate on landing page (pricing info is a top objection)

**Next candidate:** Add social proof section (testimonials, Play Store rating badge, or user count) to further improve landing page conversion.
