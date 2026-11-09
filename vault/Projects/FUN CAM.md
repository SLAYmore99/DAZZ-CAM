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

## Camera roster
- **Digital:** Pixel 02, Cyber Mini, Glow CCD, Snap Pro, Manual P
- **135:** Sunny 200, Fun Saver, Night 1600, Slide E6, Half Cam, Gold 400, Mono 400, SR Classic,
  Expired
- **120:** Toy 120, Medium 6x7, Quad Lens, 3D Wiggle
- **Inst:** Insta Square, Insta Mini, Insta Wide
- **Video:** Super 8, 16 Mil, VHS-C, Handycam 99, Projector, Dream Blur, Manual V

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
