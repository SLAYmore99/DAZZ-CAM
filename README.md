# FUN CAM

A free, open-source retro film camera for **Android**. It has 28 vintage
camera bodies, live film looks, slow-shutter video and a darkroom. Everything is included: no
subscriptions, no ads, no tracking.

FUN CAM is an original app inspired by the vintage-camera app genre. All camera names, looks,
icons and code are its own.

## Features

- **Live film viewfinder**: every look renders in real time on the GPU (OpenGL ES 3). What you
  see is what you get.
- **28 cameras** in five groups:
  - **Digital (5):** Pixel 02, Cyber Mini, Glow CCD, Snap Pro, Manual P
  - **Vintage 135 (9):** Sunny 200, Fun Saver, Night 1600, Slide E6, Half Cam, Gold 400,
    Mono 400, SR Classic, Expired
  - **Vintage 120 (4):** Toy 120, Medium 6x7, Quad Lens, 3D Wiggle
  - **Instant (3):** Insta Square, Insta Mini, Insta Wide
  - **Video (7):** Super 8, 16 Mil, VHS-C, Handycam 99, Projector, Dream Blur, Manual V
- **Film character**: grain, halation, bloom, light leaks, vignette, colour shifts,
  cross-processing, chromatic aberration, gate weave, flicker, dust, VHS tracking and scanlines.
- **Frames**: instant prints, 120 rebates, 35mm sprocket strips and slide mounts.
- **Date stamps**: an orange film-camera date, a digicam date, camcorder date/time and a VHS
  `PLAY` overlay.
- **Slow-shutter video and long exposures**: Dream Blur and Manual V/P blend frames on the GPU
  to simulate shutters up to 1 second. You can also set ISO and frame rate (1–30 fps). Phones
  that support manual sensor control also use a real longer exposure.
- **Multi-lens bodies**: half-frame diptychs, a 4-lens 2×2 action sequence, and a 3-lens
  wiggle clip.
- **Accessories that stack on any camera**: fisheye, prism, star filter, ND filter, coloured
  flash gels, double exposure and light leaks on/off.
- **Controls**: flash/torch, 3s/10s timer, grid, aspect ratios per camera, exposure,
  temperature, tint, zoom presets and pinch zoom, tap to focus, front/back camera, preview
  stabilization, and mic on/off.
- **Import**: develop any photo from your gallery with a camera's look.
- **Darkroom**: every shot keeps a private "negative", so you can re-develop it later with
  another camera, exposure or white balance.
- **Saving**: photos go to `Pictures/FUN CAM` and videos go to `Movies/FUN CAM`.

## Install

Download `FUN-CAM.apk` from the latest GitHub Actions run (**Actions → Build FUN CAM APK →
Artifacts**) or from a Release, then open it on your phone and allow installing from this
source. You need Android 8.0 or later with OpenGL ES 3.

## iPhone (test version)

`web/` holds a web-app version of FUN CAM that runs in Safari and can be added to the Home
Screen. It uses the same shader and cameras as the Android app. See `web/README.md`.

## Build

```bash
./gradlew assembleRelease     # app/build/outputs/apk/release/app-release.apk
./gradlew testDebugUnitTest lintDebug
```

You need JDK 17+ and the Android SDK (platform 35).

## How it works

```
CameraX Preview ─▶ SurfaceTexture (OES)
        │
        ▼
 input pass: rotate/mirror + accumulate frames   (slow shutter = exponential frame blend)
        │
        ▼
 film pass: one uber-shader driven by each camera's Look recipe
        ├─▶ viewfinder (SurfaceView)
        └─▶ MediaCodec H.264 surface at the camera's frame rate (+ AAC mic) ─▶ MP4
ImageCapture (full res) ─▶ StillProcessor (same shader, offscreen) ─▶ JPEG
```

The main source files:

- `app/src/main/java/com/funcam/app/model/Cameras.kt`: the camera roster. Each camera is plain
  data, so adding one is easy.
- `gl/Shaders.kt`: the film shader.
- `gl/FilmRenderer.kt`: the live GL thread.
- `gl/StillProcessor.kt`: full-resolution developing.
- `video/Encoders.kt`: the recorders.
- `ui/`: the Compose UI.

## License

MIT
