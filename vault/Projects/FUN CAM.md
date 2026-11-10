---
type: project
status: active
repo: slaymore99/dazz-cam
branch: claude/dreamy-gauss-27k7dh
updated: 2026-11-10
tags: [android, camera, kotlin, opengl, open-source]
---

# FUN CAM

**Goal:** an Android remake of the *experience* of Dazz Cam (iOS retro camera app), as
smooth and complete as possible. It uses its own name, art and looks, and it's free and open
source (MIT). Every camera is free, with no paywall.

## Status (2026-11-09)
- v0.1.0 code is complete and on GitHub. Debug and release APKs build, 6/6 unit tests pass and
  lint is clean.
- Every shader was compiled and rendered in WebGL2. See the contact sheet at
  `docs/looks-contact-sheet.png`.
- **Not yet tested on a real phone.** The user needs to install the APK and report anything
  that's wrong.

## Update 2026-11-10 (later): v0.2 overhaul
The user said the cameras looked random, basic and had "weird lines", the videos were dull and
the UI cluttered. They asked for proper research and a real overhaul.
- **Research:**
  - A community GitHub gist maps every Dazz camera to its real camera: CPM35 = Canon Sure Shot
    85, NT16 = Yashica T4, CT2F = Contax T2, HOGA = Holga, Golf = Fuji Byu-N16 (16 lenses),
    D3D = Nishika N8000, KV88 = Kiev 88, S 67 = Pentax 67, Inst C/SQ/SQC = Instax.
  - A JP review covers all 30 filters and 11 video cams.
  - Frames from the user's Instagram reel show the Original V UI (ISO 160, S 1/4, 13 mm).
  - Captions of the YouTube tutorial were read.
  - YouTube video download was blocked by its bot check, and I didn't circumvent it.
- **The lines came from** a float `fract()` hash in the grain/leak noise, which breaks on
  mobile GPUs. Fixed with an integer PCG hash.
- **Engine v2:**
  - GRADE (lens, flash, WB in linear, then a 3D LUT, then tone)
  - DOWN, then BLUR (separable Gaussian)
  - COMPOSITE (glow, halation, softness, organic leaks, grain, video artefacts, stamp,
    frames/gates)
- **LUTs:**
  - 25 film stocks from the RawTherapee HaldCLUT pack (CC BY-SA 4.0), plus 5 custom looks
    (ccd, grd, infra, vhs, dcr).
  - Built by `tools/make_luts.py`; 33³ cubes stored as 33×1089 PNGs, 900 KB total.
- **Roster:** 42 cameras (30 photo / 12 video), mirroring Dazz's lineup 1:1 in concept with
  original names. Spec in `docs/CAMERAS.md`.
- **New capture modes:** COLLAGE (4 shots on a white card), GRID16, WIGGLE with 4 lenses.
- **New frames:** BLACK, NEGATIVE (edge print), SPROCKET (image over the holes), GATE8/16
  (rounded), TOY (ragged edges). The instant frame colour is selectable.
- **Date stamp:** a 7-segment LED date drawn with glow.
- **UI (Dazz-like):** viewfinder; round dials (WB · lens mm · EV · ISO/S/FPS on manual) with a
  ruler slider; bottom row of gallery, shutter and current-camera body button; a picker
  sectioned by category; swipe the viewfinder to change camera.
- **Verified:** WebGL contact sheets of all 42 cameras on real test photos, a 100% crop check
  (no lines), web e2e test with a fake camera, Android build + lint + unit tests.
- **Still not done:**
  - Not tested on a real phone. Kino isn't landscape-aware.
  - The exact Dazz look per camera is still an approximation of the real camera/film, not a
    measured match. Side-by-side reference shots would let me tune it.

## Update 2026-11-10
- **Sideways preview fixed.** CameraX reports `hasCameraTransform() = true`: the
  SurfaceTexture matrix already rotates (and mirrors the front camera), so the input shader
  now applies 0°. Settings has a "Rotate preview" fallback (`rotfix` pref).
- **UI decluttered:**
  - The carousel shows icons only, with the selected name and category above it.
  - A camera-name toast appears on switch.
  - Zoom pills moved below the viewfinder, so nothing covers the picture.
  - EXP/TEMP/TINT sit behind an Adjust button.
  - Grid, import, Darkroom and Settings are in a ⋮ menu.
- **iPhone test version** = a web app/PWA in `web/` (WebGL2 port).
  - `tools/gen_web_data.py` generates `web/data.js` from Shaders.kt and Cameras.kt, and CI
    checks they stay in sync.
  - Deployed by `.github/workflows/web.yml` to GitHub Pages.
  - The repo is **private**, so Pages needs GitHub Pro, or the user makes the repo public, or
    hosts `web/` elsewhere.
- **Native iOS app:** not possible from a Linux session. It needs a Mac and Xcode (or a
  macOS CI runner) plus an Apple ID for signing / AltStore sideloading.
- **Accuracy vs Dazz Cam:** the looks are NOT copied from Dazz. They're original approximations.
  To match, we need reference photos (ideally side-by-side iPhone native vs Dazz camera
  shots). Waiting on the user.

## Stack
- Kotlin, Jetpack Compose, CameraX 1.4.1 (+ Camera2 interop for manual ISO/shutter), OpenGL
  ES 3, MediaCodec/MediaMuxer.
- minSdk 26, targetSdk 35, AGP 8.7.3, Gradle 8.11.1, Kotlin 2.0.21.
- A GitHub Actions workflow (`.github/workflows/android.yml`) builds `FUN-CAM.apk` as an
  artifact, and publishes a Release when a `v*` tag is pushed.

## Architecture
- `model/Cameras.kt`: 28 camera recipes. Each one is a `Look` (shader params), ratios, border,
  capture mode, stamp style, video fps and a manual flag.
- `gl/Shaders.kt`: the INPUT pass (OES → upright, plus frame accumulation for slow shutter) and
  the FILM uber-shader.
- `gl/FilmRenderer.kt`: the live GL thread. It draws to the viewfinder SurfaceView and the
  encoder surface.
- `gl/StillProcessor.kt`: offscreen full-res developing, and wiggle MP4s.
- `video/Encoders.kt`: LiveRecorder (H.264 + AAC) and SimpleVideoEncoder.
- `media/`: MediaStore saving, and Negatives (darkroom originals + JSON).
- `ui/`: CameraScreen, sheets (cameras / accessories / settings), the Darkroom and CameraIcon
  (vector art drawn in code).

## Camera roster (v0.2) — see docs/CAMERAS.md
- **Digital:** Plain, CCD-05, GR-D, Pastel, Fine X, Collage
- **135:** Range, GR Mono, T2, Fade 35, T4 Super, 3D Wiggle, Slide NE, Fun Saver, Infra,
  SLR Classic, Simple, Quick 800, Byu 16, Sure 85, Sprocket, Half, Slide
- **120:** TLR, Toy 120, Six7, Kiev
- **Inst:** Insta Mini, Insta Mono, Insta SQ
- **Video:** Plain V, Classic V, 16 Mil, Super 8, VHS, Kino, Insta V, Fun Saver V, Handycam,
  Glow V, Projector, Dream Blur

## Key decisions
- Slow-shutter video is simulated with an exponential frame blend (alpha = frameDt / shutter)
  in a half-float buffer. On phones with manual-sensor support, a real exposure is added once
  ISO is set manually.
- Grain, blur and other effects are image-relative, so the preview matches the saved
  full-res photo.
- Bitmaps are uploaded flipped, so every texture uses v=1 as the top row.
- The release build is signed with the debug key so it installs straight from GitHub.

## Next ideas
- Test on a device and fix rotation, aspect and performance issues.
- More cameras (Dazz has 20+ photo and 11 video bodies), and a camera favourites/ordering menu.
- Video import, a custom date for the stamp, an editable stamp format.
- Landscape-aware capture (the app is locked to portrait).
- Burst / Live Photo style clips.

## Links
- Research: `docs/RESEARCH.md`
- Shader preview tool: `tools/shader-preview/`
