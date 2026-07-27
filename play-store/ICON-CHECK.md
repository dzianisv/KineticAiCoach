# Launcher / Play Store Icon — Verification & Findings

## 1. Does a launcher icon exist in the app?

Yes. The app declares an adaptive icon:

```
app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
  → <adaptive-icon> background=@drawable/ic_launcher_background
                     foreground=@drawable/ic_launcher_foreground
                     monochrome=@drawable/ic_launcher_foreground
app/src/main/res/drawable/ic_launcher_background.xml   (vector, #020617 bg + #0F172A inset square)
app/src/main/res/drawable/ic_launcher_foreground.xml   (layer-list, 66dp centered)
app/src/main/res/drawable/app_icon_logo_1784496004384.jpg  (1024×1024 raster source)
```

Plus legacy raster fallbacks for pre-Android-8.0 (API < 26) devices:
```
app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.webp
app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher_round.webp
```

Confirmed rendering live on `emulator-5562` (API 26+, adaptive icon path):
the home-screen/app-drawer icon shows a black circular badge with a white
running-figure mark and small text. Screenshot evidence was captured during
this session (not included in the final package — see Finding 2 below for
why the source image shouldn't be reused as-is).

## 2. Findings — two real problems, not just missing assets

### 🔴 Finding A: The adaptive-icon foreground artwork doesn't match the app

`app_icon_logo_1784496004384.jpg` is a stock/placeholder graphic reading
**"TECHRUN — DYNAMICS PERFORMANCE"** baked directly into the image as text —
unrelated branding to "Kinetic AI Coach". This is what actually renders on
the home screen and app drawer today. Two problems with it:

1. **Wrong brand name.** Users see "TechRun Dynamics Performance" text on
   the icon of an app called "Kinetic AI Coach" — confusing at best, and a
   plausible reason for Play review friction (misleading metadata) at
   worst.
2. **Baked-in text inside an adaptive icon is bad practice regardless of
   branding.** Adaptive icons get masked into circles/squircles/squares
   differently per launcher and are also scaled down to very small sizes
   (status bar, recents) — small caption text like "DYNAMICS PERFORMANCE"
   is illegible or clipped in most of those contexts.

**Recommendation:** replace `app_icon_logo_1784496004384.jpg` (and ideally
`ic_launcher_foreground.xml`/`ic_launcher_background.xml`) with real
Kinetic AI Coach branding before the first Play submission — this is an
app-code change, out of scope for this listing-package task, but it should
block release, not just the store listing.

### 🔴 Finding B: The legacy raster mipmap `.webp` fallbacks are corrupted

All ten `ic_launcher*.webp` files under `mipmap-{m,h,xh,xxh,xxxh}dpi/` fail
to decode:

```
$ identify app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp
identify: corrupt image ... error/webp.c/ReadWEBPImage/563.
$ python3 -c "from PIL import Image; Image.open('...ic_launcher.webp')"
PIL.UnidentifiedImageError: cannot identify image file
```

Hex-dumping the file shows a valid `RIFF....WEBPVP8X` header followed by
byte sequences consistent with the file's binary content having been passed
through a UTF-8 text transform at some point (interspersed `0xC2` bytes —
classic mojibake/double-encoding of raw bytes ≥ 0x80), corrupting the
payload. All 10 files (`ic_launcher.webp` and `ic_launcher_round.webp` at
mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi) exhibit the same corruption pattern, sized
1.4 KB–11.7 KB — plausible file sizes for a valid icon, so this wasn't a
zero-byte/missing-file problem, the bytes were mangled after the fact.

**Impact:** any device on **API 24–25** (below adaptive-icon support,
`minSdk = 24` per `app/build.gradle.kts`) would fail to render the launcher
icon from these fallbacks — those users would see a system default/blank
icon. Devices on API 26+ never touch these files (they use the adaptive
icon XML + JPG instead), which is why this wasn't visible on the API 26+
emulator used for this task's screenshots.

**Recommendation:** regenerate the mipmap webp/png fallbacks (Android
Studio: right-click `res` → New → Image Asset, or `gradlew` icon tooling)
from whatever corrected foreground/background art replaces Finding A. This
is also an app-code fix, out of scope here, but flagged so it isn't lost.

## 3. Does a 512×512 hi-res Play Store icon exist?

No — the repo had no dedicated Play Store hi-res icon asset (Play Console
requires a separate 512×512 32-bit PNG upload distinct from the APK's own
launcher mipmaps).

Given Finding A above, generating the 512×512 hi-res icon by simply
upscaling/cropping the existing "TechRun" JPG would ship the same
mismatched branding to the Play Store listing itself — arguably worse,
since it's the very first thing a prospective user sees in search results.

**What was generated instead:** `docs/play-store/hi-res-icon-512.png` — a
clean, on-brand 512×512 PNG built from scratch: solid black background,
rounded square, white pose-skeleton pictogram (matching the in-app red/white
bones motif used in the pose-tracker screens, rendered in white here per
the black/white brand direction). Source vector at
`docs/play-store/assets-src/icon-512.svg`, rendered with:

```
rsvg-convert -w 512 -h 512 assets-src/icon-512.svg -o hi-res-icon-512.png
```

This is a **placeholder-quality but on-brand and honest** stand-in — it
does not claim to be the final production icon. Before shipping, replace
both this hi-res icon and the in-app adaptive icon (Finding A) with a
single, consistent, designer-approved mark so the Play Store listing icon
and the installed app icon match exactly (a Play policy expectation).

## 4. Action items summary

| # | Item | Status | Blocking release? |
|---|---|---|---|
| 1 | Launcher icon resources exist (adaptive icon + legacy mipmaps) | ✅ Present | — |
| 2 | Adaptive icon foreground artwork matches "Kinetic AI Coach" branding | ❌ Shows unrelated "TechRun" placeholder text | Yes — fix in app code before submitting |
| 3 | Legacy `.webp` mipmap fallbacks are valid, decodable images | ❌ Corrupted (mojibake byte pattern) on all 10 files | Yes for API 24–25 users — fix in app code |
| 4 | 512×512 hi-res Play Console icon | ✅ Generated fresh (`hi-res-icon-512.png`), on-brand, but placeholder-quality | Recommend a real design pass before launch |
