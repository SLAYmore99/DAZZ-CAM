# FUN CAM — Research & Feature Plan

FUN CAM is an original, free, open-source Android retro-camera app. It is
*inspired by the category* of vintage-camera apps (e.g. "Dazz Cam – Vintage
Camera" on iOS) but uses its own name, UI, artwork and film looks. No code,
assets, filter data or branding from any other app are used. Every camera and
accessory in FUN CAM is free — there is no paywall.

## 1. Reference app research (Dazz Cam, iOS, researched Nov 2026)

Sources: App Store listing (id1422471180), parallaxaview.com review/filters/
slow-shutter guides (2025), iphoneappsarena.com (Aug 2026), tapsmart.com (2020).
Third-party sources disagree in places; treat details as approximate.

- **Latest version seen:** 2.17.7 — new "SRX" camera (based on its 135 SR), new
  C400 film filter, iPhone 18 Pro optimisation, Lock Screen / Camera Control launch.
- **Requires:** iOS/iPadOS 16+. ~150 MB. 4.8★ (118K ratings). No Android version listed.
- **Monetisation:** free with IAP. Pro $6.99 subscription or $19.99 one-time;
  single cameras/accessories $0.99–$2.99. Free tier can be very limited.
- **Menu categories:** Digital · Video · Vintage 135 · Vintage 120 · Inst · Accessories.
- **Photo camera types:** 135 film, 120 film, toy, slide, half-frame, instant,
  3D, disposable, compact digital, CCD digital. ~20+ photo cameras.
- **Video camera types (~11):** 8mm, 16mm, VHS, DCR camcorder, movie camera,
  slide projector, dreamy/hazy "live photo" style, and a manual "original" camera.
- **Controls:** flash (+ colour flash), timer, exposure comp, ISO, shutter speed,
  FPS, white balance temp/tint, focal-length presets/free zoom, stabilisation,
  grids, aspect ratios per camera, borders, date stamp (format adjustable),
  light-leak randomness, jitter, audio on/off for video.
- **Slow-shutter video:** achieved on a manual video camera by lowering FPS,
  which unlocks a slower shutter (below ~1/20s) → dreamy motion-blur trails.
- **Extras:** double exposure, import photos (some cameras), save "negative" and
  re-develop later with different exposure/temperature.
- **Accessories:** fisheye, prism, starburst/star filter, ND filter, colour flash.

## 2. FUN CAM camera roster (original names & looks — all free)

### Digital
| Name | Look |
|---|---|
| **Pixel 02** | 2000s CCD point-and-shoot: cool cyan cast, crisp, harsh flash, orange LED date |
| **Cyber Mini** | Early 1.3MP digicam: low-res softness, blown highlights, purple fringing |
| **Glow CCD** | Pastel CCD with soft highlight bloom, pink skin tones |
| **Snap Pro** | Clean compact digital, slight warmth, mild noise |

### Vintage 135
| Name | Look |
|---|---|
| **Sunny 200** | Warm consumer colour negative, golden highlights, fine grain |
| **Fun Saver** | Disposable: warm, vignette, heavy flash falloff, date stamp |
| **Night 1600** | High-ISO film: chunky grain, green-shifted shadows, light leaks |
| **Slide E6** | Slide/reversal film: punchy saturation, deep blacks, mounted-slide border |
| **Half Cam** | Half-frame: two vertical frames side by side |
| **Gold 400** | Everyday film, rich reds/yellows, medium grain |
| **Mono 400** | Black & white, strong contrast, silver grain |
| **SR Classic** | Muted, faded 35mm SLR look, balanced contrast |
| **Expired** | Expired film: shifted hues, colour cross-processing, heavy grain |

### Vintage 120 / Toy
| Name | Look |
|---|---|
| **Toy 120** | Plastic toy camera: heavy vignette, soft corners, light leaks, square |
| **Medium 6x7** | Medium format 6x7: fine grain, creamy tones, film-rebate border |
| **Quad Lens** | 4-lens action sampler grid (2x2 sequence) |
| **3D Wiggle** | Stereo 3-lens camera → animated wiggle GIF/MP4 |

### Instant
| Name | Look |
|---|---|
| **Insta Square** | Square instant print with white frame, soft contrast |
| **Insta Mini** | Credit-card instant print, cool tint |
| **Insta Wide** | Wide instant print |

### Video
| Name | Look |
|---|---|
| **Super 8** | 8mm: gate weave, flicker, warm fade, dust & scratches, 4:3 |
| **16 Mil** | 16mm: finer grain, cinematic colour |
| **VHS-C** | Tape: scanlines, chroma bleed, tracking noise, REC overlay + timestamp |
| **Handycam 99** | DCR camcorder: oversharpened, interlace, date/time overlay |
| **Dream Blur** | Slow-shutter video: frame accumulation for motion trails/light streaks |
| **Manual V** | Manual video: FPS (1–30), shutter (1/2s–1/4000), ISO; slow shutter unlocked at low FPS |
| **Projector** | Slide/film projector: hot centre, vignette, flicker, dust |

### Accessories (apply to any camera)
Fisheye · Prism (rainbow ghosting) · Starburst · ND filter (longer shutter) ·
Colour flash (red/blue/green/yellow gels) · Double exposure · Light leaks on/off.

## 3. Slow-shutter video — how FUN CAM will do it

Android phones rarely allow true exposures longer than the frame interval in
video. FUN CAM simulates it the way the effect actually looks:
1. Capture frames from CameraX at native rate (30/60 fps).
2. In a GPU shader, accumulate frames into an exponential moving-average buffer
   (`acc = mix(acc, frame, 1/N)`), where N = output shutter time ÷ frame time.
3. Output at the chosen FPS (e.g. 6–15 fps) → long motion trails, light streaks.
4. Where Camera2 supports manual `SENSOR_EXPOSURE_TIME`, use the real sensor
   exposure as well, and lower ISO to keep brightness.

## 4. Tech stack

- Kotlin, Jetpack Compose, min SDK 26.
- CameraX (preview, capture, video) + Camera2 interop for manual ISO/shutter/FPS.
- OpenGL ES 3 shader pipeline: each camera = a JSON "recipe" (tone curve, LUT,
  grain, vignette, halation, light leaks, border, date stamp, video artifacts).
- MediaCodec encoder for processed video; MediaStore for saving.
- License: MIT. No ads, no tracking, no paywall.

## 5. Milestones
1. **v0.1** — project skeleton, live GL preview, 6 cameras, photo capture, date stamp, save.
2. **v0.2** — full photo roster, import, borders, flash/timer/grid/exposure/WB.
3. **v0.3** — video cameras, Manual V, slow-shutter Dream Blur, audio toggle.
4. **v0.4** — accessories, double exposure, half-frame/quad/3D wiggle.
5. **v0.5** — re-develop "negative" (save RAW-ish source + recipe), polish, release APK via GitHub Actions.
