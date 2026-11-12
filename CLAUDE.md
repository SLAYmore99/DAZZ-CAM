# FUN CAM: full context for new sessions

Read this whole file first, then `vault/Projects/FUN CAM.md`, for the detailed timeline.

---

## 1. Who this is for and what they want

**The owner** (GitHub `SLAYmore99`) has always used Android and was jealous of iPhone users
because of **Dazz Cam**. That's the hugely popular iOS-only retro film camera app (developer
DAZZ PTE. LTD.; App Store id 1422471180; about 4.8★ from 118K ratings; Pro is $6.99/yr or
$19.99 one-time).

**The goal:** an Android app that gives the Dazz Cam *experience*, as close and as smooth as
possible, with *all* the features. That means live film looks, lots of cameras, video cameras,
slow-shutter video and the fun multi-lens cameras. It's called **FUN CAM**.
- **Free and open source (MIT):** every camera and accessory included, no paywall, no ads,
  no tracking.
- **Something they can share with friends:** an APK for Android, and an iPhone version (a
  Safari web app plus an IPA).

**Hard rule (stated by the assistant and accepted by the user after several requests):** FUN
CAM uses original names, art, code and looks. **Never extract, use or study Dazz Cam's own
files.** That means no IPA assets, LUTs, frames, textures or decrypted code. The user made a
repo `SLAYmore99/dazz-cam-ipa` (now private) and asked several times for it to be read. Each
time this was declined:
- The binary is FairPlay DRM, and decrypting it is off-limits.
- Learning the looks from their filter files is derivation from copyrighted work.

The legal route, and the one in use, is **measuring outputs**: the user provides
iPhone-original vs Dazz-processed photo pairs, and `tools/match_look.py` fits our own LUT,
vignette, grain and sharpness. Copying *behaviour* (features, UI flow) is fine; copying
*assets* is not.

## 2. How the user likes to work
- **Non-technical, mobile-first.** They follow the cloud session from the Claude app and test
  on their Android phone. Give plain-language steps, short numbered lists, and links. Send the
  APK as a file with `SendUserFile` when asked; it lives at `build/FUN-CAM.apk` after
  `assembleRelease`.
- **"Commit and push everything."** A stop hook also enforces a clean working tree, so commit
  and push every change to the session's working branch (v0.4 was built on `claude/clever-dijkstra-ij8cnl`).
- **Keep the memory vault updated.** The user has a local Obsidian vault ("VAULT FOR CLAUDE
  MEMORY") that cloud sessions can't reach. Mirror the notes in the repo's `vault/` folder
  (`Home.md`, `Projects/FUN CAM.md`, `Sessions/*.md`); the user copies them over at the end
  of a session. Update the project note after any meaningful work.
- **Wants things to look and feel premium:** "not basic", "not cluttered", "smooth". They
  react strongly to visible artefacts and to UI churn.
- **Decisions they've made (respect them):**
  - Keep the **current UI** (v0.3): the closed-loop camera wheel, the **Customize** button,
    the adjust chips behind the top-bar sliders icon, and lenses/effects in the top-right ✨
    panel. Build on it. **Do not revert to older layouts**, and don't redesign it wholesale
    without asking. (They disliked the v0.2 "Dazz-style" redesign with round dials and a
    camera-body button; it was rolled back.)
  - **Slow shutter on every camera, photo and video,** and they must always *know* when it's
    on. Today that's the amber `SLOW 1/x ✕` badge in the viewfinder plus an amber shutter
    ring, and the setting persists across cameras.
  - Per-camera customisation must be easy to reach (the Customize sheet).
- **Be honest about what wasn't tested** (no real phone or iPhone in the cloud). Say exactly
  what was verified and how.

## 3. Current state (v0.4.0)

**Four apps from one engine** (they install side by side; see README "v0.4"):

| Edition | Reference | Android | Web |
|---|---|---|---|
| DOT | Nothing OS | `FUN-CAM-DOT.apk` (`com.funcam.app.dot`) | `/DAZZ-CAM/dot/` |
| GLASS | iOS Camera | `FUN-CAM-GLASS.apk` (`.glass`) | `/DAZZ-CAM/glass/` |
| FIELD | Braun / Teenage Engineering hardware | `FUN-CAM-FIELD.apk` (`.field`) | `/DAZZ-CAM/field/` |
| Classic | the v0.3 UI, **kept as it was** | `FUN-CAM.apk` (`com.funcam.app`) | `/DAZZ-CAM/` |

- **APKs:** https://github.com/SLAYmore99/DAZZ-CAM/releases/tag/latest (rolling, built by CI).
- **Working branch:** `claude/clever-dijkstra-ij8cnl` (v0.3 lives on `claude/dreamy-gauss-27k7dh`).
  `main` hasn't been merged yet.
- **Web: only GLASS is published** (`/DAZZ-CAM/glass/`), by the user's choice (2026-11-12). `web.yml`
  removes `web/dot`, `web/field` and `web/editions` and rewrites `web/published.js` before uploading.
  To publish another edition later, edit that step.
- **Pages only deploys from `claude/dreamy-gauss-27k7dh`** (environment rule; on any other branch
  `web.yml` fails in seconds with no log). To update the site, push the same commits there too
  (`git push origin HEAD:claude/dreamy-gauss-27k7dh`); the user approved this for the GLASS deploy.
- **The user has not yet said which edition they prefer.** Ask, then build on that one.
  "Leave the last version as it is" was their instruction: don't change the classic edition.

**Engine v4** (new editions only; classic runs with `Engine.v4 = false` / `CAMERAS` without v4
fields): highlight shoulder, 1/16-size wide blur for veiling glow + far halation + Mist lens,
anamorphic Streak, barrel distortion, dust specks on stills, instant-paper texture.
**51 cameras** (37 photo, 14 video); the 9 new ones are flagged `v4 = true`.

**Shared by DOT, GLASS and FIELD:** tap focus, hold to lock AE/AF, vertical drag = exposure,
sideways swipe = next camera, pinch zoom; a dial with detents (EV, WB, tint, shutter, ISO,
zoom, FPS); Photo/Video switch; live look previews; favourites; grid, level, histogram / light
meter; one "Auto" reset; Customize one tap away; slow-shutter badge; per-edition shutter
animation.

## 4. Architecture

```
app/src/classic|dot|glass|field/   one source set per edition: ui/Edition.kt defines EditionRoot(vm);
                       classic also holds the v0.3 CameraScreen.kt and Sheets.kt. Flavors set BuildConfig.V4.
app/src/test*/         FrameSpecTest + Roborazzi screenshot tests (PNG to app/build/screens/)
app/src/main/java/com/funcam/app/
  ui/kit/              shared by the v4 editions: Skin (DOT/GLASS/FIELD), Dial, Widgets (switch, slider, pill,
                       tile, KSheet), Panels (cameras, lenses, customize, settings), Viewfinder (gestures,
                       CameraSnapRow), Host (lifecycle, panels, messages)
  gl/Shaders.kt        INPUT (OES → upright + accumulation) → GRADE (lens, flash, WB, 3D LUT, tone)
                       → DOWN (¼) → BLUR (separable Gaussian H,V) → COMPOSITE (glow, halation, leaks,
                       grain, video artefacts, stamp, frames). COMMON block holds the PCG hash and noise.
  gl/FilmPipeline.kt   runs the passes; RenderParams, FrameSpec (frame layout), LutCache (33³ RGB8 3D textures)
  gl/FilmRenderer.kt   live GL thread: SurfaceTexture → accumulation → viewfinder + MediaCodec encoder
  gl/StillProcessor.kt full-res stills, wiggle MP4
  gl/StampRenderer.kt  7-segment date, camcorder/VHS text, film edge print
  model/Cameras.kt     the 42 recipes (Look = lut + params; ratios; border; capture; stamp; fps)
  model/Recipe.kt      enums, Look, Accessories, Tweak (per-camera user customisation)
  camera/CameraController.kt  CameraX bind, zoom, focus, manual ISO/shutter (Camera2 interop), torch
  video/Encoders.kt    LiveRecorder (H.264 + AAC + muxer), SimpleVideoEncoder
  ui/                  CameraScreen (wheel, badge, chips), Sheets (picker, lenses, settings, Customize),
                       Darkroom, CameraIcon (shaded original artwork), CamViewModel
web/                   classic web app (index.html, app.js) + WebGL2 engine (gl.js), icons.js, roll.js, sw.js;
                       data.js is GENERATED (CAMERAS = classic roster, CAMERAS4 = engine-v4 roster)
web/core.js, kit.js    v4 core (camera, state, capture, meter, look previews) and shared UI parts
web/dot|glass|field/   one edition each: index.html, style.css, ui.js, manifest, icons
web/editions/          chooser page
ios-app/               Capacitor 8 (SPM) shell around web/; scripts/configure-ios.sh sets Info.plist and the icon
tools/
  make_luts.py         HaldCLUT → 33³ LUT PNGs (33×1089; row = g + b·33, col = r) + custom looks
  gen_web_data.py      Shaders.kt + Cameras.kt → web/data.js (CI fails if it's out of sync)
  match_look.py        fit LUT/vignette/grain/sharpness from photo pairs (aligns handheld pairs)
  shader-preview/      older contact-sheet tool
.github/workflows/     android.yml (tests, lint, APK → 'latest'), ios.yml (unsigned IPA on macos-15 → 'latest'),
                       web.yml (GitHub Pages, configure-pages enablement: true)
docs/                  CAMERAS.md (roster research), MATCHING.md, RESEARCH.md, screenshots
vault/                 mirror of the user's Obsidian notes
```

**Build and test**
```bash
./gradlew testDotDebugUnitTest lintDotDebug assembleRelease   # task names need the edition (Classic/Dot/Glass/Field); assembleRelease builds all four
# Android SDK: install cmdline-tools + platforms;android-35 + build-tools;35.0.0 to /opt/android-sdk, then echo sdk.dir=/opt/android-sdk > local.properties
# APKs: app/build/outputs/apk/<edition>/release/app-<edition>-release.apk (copied to build/FUN-CAM[-DOT|-GLASS|-FIELD].apk)
python3 tools/gen_web_data.py                           # ALWAYS after editing Shaders.kt or Cameras.kt
python3 -m http.server -d web 8765                      # then test with Playwright (Chromium is preinstalled)
```

**Visual verification pattern:** a small harness page (kept in the git-ignored `web/_test/`)
imports `../data.js` and `../gl.js`, calls `film.loadLuts(...)`, then `film.develop(img, P,
...)` for each camera, and shows a contact sheet. Run it with Playwright Chromium using
`--use-gl=angle --use-angle=swiftshader --enable-unsafe-swiftshader`. For end-to-end tests use
`--use-fake-device-for-media-stream --use-file-for-fake-video-capture=<file.y4m>`. Make the
y4m from a real photo with ffmpeg. Test photos came from picsum.photos (testing only, not
shipped).

**Seeing Android screens without a phone:** `./gradlew testGlassDebugUnitTest` renders the Compose
screens with Robolectric + Roborazzi into `app/build/screens/*.png` (`FilmRenderer.headless = true`,
so the viewfinder is empty). Robolectric needs `android-all-instrumented` jars for SDK 34 and 35;
if its own download fails behind the proxy, fetch them with `curl -L` into
`~/.m2/repository/org/robolectric/android-all-instrumented/<version>/` (jar, pom and their .sha512).
**Web editions:** `web/_test/ui.js <edition> <outPrefix> [script]` (git-ignored) opens an edition on a
phone-sized page with a fake camera. picsum.photos timed out in v0.4; images.unsplash.com worked.

## 5. Research findings (sources in docs/CAMERAS.md)
- **Dazz menu groups:** Digital · Video · Vintage 135 · Vintage 120 · Inst · Accessories.
  Around 30 photo filters (about 16 free) and 11 video cameras.
- **Dazz camera → real camera** (community gist):

  | Dazz | Real camera | Dazz | Real camera |
  |---|---|---|---|
  | CPM35 | Canon Sure Shot 85 | S Classic | Rolleiflex / Yashica-Mat TLR |
  | D Classic | Canon P / Leica M rangefinder | S 67 | Pentax 67 |
  | GR F | Ricoh GR (B&W) | KV88 | Kiev 88 |
  | CT2F | Contax T2 | HOGA | Holga |
  | D Exp | Fujica 35SE | Inst C | Instax Mini 8 |
  | NT16 | Yashica T4 | Inst SQ | Instax SQ10 (B&W) |
  | D3D | Nishika N8000 (4 lenses) | Inst SQC | Instax square colour |
  | 135 NE | slide film, black frame | D FunS | Kodak Fun Saver |
  | IR | colour infrared | Classic U | Leica R3 |
  | DQS | Fuji Simple Ace | Golf | Fuji Byu-N16 (16 lenses) |
  | 135 SR | Lomo Sprocket Rocket | D Half | Konica Recorder |
  | D Slide | slide mount | FQS R | QuickSnap-like, newer |

  Digital: Original, GRD R, CCD R, Puli (soft dreamy), FXN R, Collage.
- **Dazz video cameras:** Original V, V Classic, 16mm, 8mm, VHS, Kino (landscape), InstSS,
  V FunS, DCR, Glow, Slide P.
- **The viral slow-shutter video look** (seen in the user's Instagram reel) is Original V with
  ISO 160, S 1/4, 13 mm, giving warm, smeared night motion. FUN CAM's **Dream Blur** presets
  this.
- **Dazz's real UI** (from video frames and captions): a viewfinder rectangle; a lens chip in
  focal lengths (13/24/35/50/120); WB and EV controls; ISO and shutter as ruler sliders; a
  bottom row of gallery, big shutter and the current camera's icon (opens the camera list);
  import, double exposure, timer, flash and flip. Settings include export to album, keep
  originals, mirror front camera, and timestamp format.
- **YouTube** blocks downloads from the cloud with a bot check; don't try to get around it.
  Instagram reels *can* be fetched with yt-dlp for frame inspection.
- **Open-source references studied:** FilmCamera-Android (MIT; the grade→bloom→composite and
  PCG grain approach, credited in THIRD_PARTY.md), RetroCam, film-filters, filcamera (GPL,
  not used).

## 6. Lessons learned (bugs and gotchas)
- **Sideways preview:** CameraX `SurfaceRequest.TransformationInfo.hasCameraTransform()` is
  true. The SurfaceTexture matrix *already* rotates (and mirrors the front camera), so the
  input shader must apply 0°. There's a manual "Rotate preview" setting as a fallback.
- **"Weird vertical lines" in photos:** caused by a `fract(sin/dot)` float hash breaking on
  mobile GPUs at large coordinates. Always use the integer **PCG3D** hash (in Shaders COMMON).
- **Blocky ghost edges:** caused by building bloom from a few fixed offset taps. Use a real
  downsample + separable Gaussian.
- **Flash looked blown out:** keep the "near subject" boost small; flash falloff is applied in
  linear light before the LUT.
- **Infrared:** an IR estimate needs a vegetation mask (`g > b`), or water and sky turn
  purple.
- **ImageCapture** gives full-res stills; multi-frame and long-exposure modes grab the
  accumulated GL frame instead.
- **Half-float accumulation** (RGBA16F) is needed for smooth slow-shutter blending. There's an
  RGBA8 fallback.
- **Web:** `uniform1ui` takes uint seeds (use `>>> 0`). Upload textures with `UNPACK_FLIP_Y`
  so v=1 is the top row. The MediaRecorder `onstop` callback must use a local recorder
  reference.
- **GitHub Pages** needs `configure-pages` with `enablement: true` (or the user enabling it).
  Private repos need a paid plan.
- **iOS:** an unsigned IPA can't be installed by tapping it. It needs AltStore/Sideloadly with
  the installer's Apple ID (7-day re-sign on free accounts). TestFlight needs a $99/yr Apple
  Developer account (signing secrets in CI). `getUserMedia` in WKWebView works on iOS 14.3+;
  camera permission may be asked twice. Not yet verified on a device.
- **Generator gotcha:** `gen_web_data.py` reads each `Look(...)` up to the first `),` at a line end.
  Never end a line inside a `Look` with a `V3(...),`; keep V3 values mid-line. (This silently cut
  Sure 85's look short on the web from v0.2 until v0.4.)
- **SurfaceView can't be clipped** to rounded corners; `kit/Viewfinder.kt` paints the corners over it.
- **Signing:** release builds use the debug key, which differs per machine and CI run, so an update
  needs an uninstall first. Do not commit a keystore: a commit containing one was blocked. A stable
  key needs a GitHub Actions secret that the user sets up.
- **Gradle task names are ambiguous without the edition** (`testDebugUnitTest` fails).
- **Cloud-session limits:** no KVM (no Android emulator); WebKit isn't installed (test web in
  Chromium only); the GitHub API `/pages` endpoint is blocked by the proxy; destructive
  wildcard `rm -rf *` is blocked by a safety check, so use fresh directories instead.
- **Android lint:** `ExifInterface.getDateTime*` is restricted API; parse
  `TAG_DATETIME_ORIGINAL` instead. `mipmap-anydpi` (without `-v26`) failed AAPT linking; keep
  `-v26`.

## 7. Open items / roadmap
1. **Real-device testing of DOT, GLASS and FIELD** (nothing in v0.4 has run on a phone): live
   viewfinder under the painted corners, haptics, focus lock, level, look previews (they grab a
   full-res frame; check speed), dial feel, performance.
2. **The user's pick** between the editions, then polish that one.
3. **Match the looks:** still waiting on iPhone-vs-Dazz pairs in `reference/` (see `docs/MATCHING.md`).
4. **In-app roll on Android** for the new editions (they open the system gallery today).
5. **Kino / Neon 800T** should be landscape-aware.
6. **iOS IPA** wraps the classic web app only; the new editions are web-only for now (user asked to
   skip the iOS app).
7. **Stable signing key** via a GitHub secret, so updates install over the old app.
8. **Possibly:** a PR to merge into `main`; TestFlight; Play Store listing.
