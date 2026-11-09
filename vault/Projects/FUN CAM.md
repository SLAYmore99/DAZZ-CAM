---
type: project
status: active
repo: slaymore99/dazz-cam
branch: claude/dreamy-gauss-27k7dh
updated: 2026-11-09
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
