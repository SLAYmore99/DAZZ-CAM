# FUN CAM

A free, open-source retro film camera for **Android**. It has 28 vintage
camera bodies, live film looks, slow-shutter video and a darkroom. Everything is included: no
subscriptions, no ads, no tracking.

FUN CAM is an original app inspired by the vintage-camera app genre. All camera names, looks,
icons and code are its own.

## Features

- **Live film viewfinder**: every look renders in real time on the GPU (OpenGL ES 3). What you
  see is what you get.
- **42 cameras (30 photo, 12 video)**, each recreating a classic camera or film. See
  [`docs/CAMERAS.md`](docs/CAMERAS.md) for the research behind each one.
  - **Digital:** Plain, CCD-05, GR-D, Pastel, Fine X, Collage
  - **Vintage 135:** Range, GR Mono, T2, Fade 35, T4 Super, 3D Wiggle, Slide NE, Fun Saver,
    Infra, SLR Classic, Simple, Quick 800, Byu 16, Sure 85, Sprocket, Half, Slide
  - **Vintage 120:** TLR, Toy 120, Six7, Kiev
  - **Instant:** Insta Mini, Insta Mono, Insta SQ
  - **Video:** Plain V, Classic V, 16 Mil, Super 8, VHS, Kino, Insta V, Fun Saver V, Handycam,
    Glow V, Projector, Dream Blur
- **Real film colour**: 3D LUTs built from film-emulation data (Portra, Superia, Kodachrome,
  Ektachrome, Velvia, Instax-style FP-100C, Polaroid…), plus FUN CAM's own CCD, VHS, camcorder
  and colour-infrared looks.
- **Film character**, rendered in four passes:
  - **Grade:** lens, flash and white balance, then the film LUT.
  - **Bloom:** a real Gaussian blur.
  - **Halation:** red glow around highlights.
  - **Composite:** organic light leaks, film grain built on an integer hash (no line artefacts),
    soft corners, gate weave, flicker, dust and hairs, VHS chroma bleed and tracking noise.
- **Frames**: instant prints, 120 rebates, 35mm sprocket strips and slide mounts.
- **Date stamps**: a glowing 7-segment orange film date (`'26 11 10`), a digicam date,
  camcorder date/time and a VHS `PLAY` overlay.
- **Slow-shutter video and long exposures**: Dream Blur and Manual V/P blend frames on the GPU
  to simulate shutters up to 1 second. You can also set ISO and frame rate (1–30 fps). Phones
  that support manual sensor control also use a real longer exposure.
- **Multi-shot cameras**:
  - half-frame pairs
  - a 4-shot collage card
  - a 16-lens 4×4 flip-book
  - a 4-lens 3D wiggle clip
- **Accessories that stack on any camera**: Fisheye W (full-frame), Fisheye C (circular),
  prism, star, ND, coloured flash gels, double exposure and light leaks on/off.
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

The code is MIT. The film LUTs are CC BY-SA 4.0 (see [`THIRD_PARTY.md`](THIRD_PARTY.md)).
