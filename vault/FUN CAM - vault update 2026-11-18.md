---
type: vault-update
project: FUN CAM
updated: 2026-11-18
covers: sessions from 2026-11-09 to 2026-11-12 (v0.1 to v0.4)
tags: [android, camera, film, open-source, learnings]
---

# FUN CAM: everything to put in the vault

One file you can drop into the vault. It replaces nothing; it sums up where FUN CAM stands
and what was learned. The detailed notes are still in the repo's `vault/` folder
(`Projects/FUN CAM.md` and `Sessions/`).

## Where to put it

1. Copy this file into the vault's `Projects/` folder (or merge it into `Projects/FUN CAM.md`).
2. Add a link to it on `Home.md` under Projects: `[[FUN CAM - vault update 2026-11-18]]`.
3. Optionally also copy `vault/Sessions/2026-11-12 FUN CAM v0.4 three editions.md` into
   the vault's `Sessions/` folder.

---

## 1. What FUN CAM is

- A **free, open-source (MIT) Android camera app** that gives the experience of Dazz Cam
  (an iPhone-only retro film camera app): live film looks, lots of cameras, video cameras,
  slow shutter, multi-lens cameras.
- No paywall, no ads, no tracking. Shared with friends as an APK, plus a Safari web app for
  iPhone users.
- Repo: https://github.com/SLAYmore99/DAZZ-CAM
- Current working branch: `claude/clever-dijkstra-ij8cnl` (not merged into `main` yet).

## 2. Current state (v0.4.0)

### Four apps from one engine

They install side by side, so all four can live on the phone at once.

| Edition | Inspired by | What it feels like | Android | iPhone (Safari) |
|---|---|---|---|---|
| **DOT** | Nothing OS | Black and white, dot-matrix type, one red accent, shutter inside a ring of dots | `FUN-CAM-DOT.apk` | not published |
| **GLASS** | iOS Camera | Translucent rounded controls, live strip of every film look, control tray | `FUN-CAM-GLASS.apk` | **live** |
| **FIELD** | Braun / Teenage Engineering hardware | Bone or graphite body, pressable keys, LCD, thumbwheel, light-meter needle, orange shutter | `FUN-CAM-FIELD.apk` | not published |
| **Classic** | (the v0.3 app) | The camera wheel, kept as it was, 42 cameras | `FUN-CAM.apk` | live |

### Links

- **All APKs:** https://github.com/SLAYmore99/DAZZ-CAM/releases/tag/latest
- **GLASS on iPhone:** https://slaymore99.github.io/DAZZ-CAM/glass/
- **Classic on iPhone:** https://slaymore99.github.io/DAZZ-CAM/

### What the three new editions share

- **Viewfinder gestures:** tap to focus, press and hold to lock focus and exposure, drag up
  or down for exposure, swipe sideways for the next camera, pinch to zoom.
- **A dial with clicks** for exposure, warmth, tint, shutter, ISO, zoom and frame rate.
- **One "Auto" control** that resets everything.
- **Photo / Video switch** that remembers the last camera in each mode.
- **Live look previews:** the camera list shows each film on what the lens sees right now.
- **Favourites**, grid, horizon level and histogram (FIELD has a light-meter needle instead).
- **Slow shutter on every camera**, always shown by a badge and a coloured shutter ring.
- **Customize** per camera, one tap away.

### Film engine v4 (new editions only)

- Soft highlight roll-off instead of clipping.
- Wide glow and the red "halation" halo around lights.
- New lenses: **Mist** (soft dreamy glow) and **Streak** (blue movie-style flares).
- Lens distortion on cheap cameras, dust on film stills, paper texture on instant prints.
- **51 cameras** (9 new): Cine 800T, Gold 200, Purple Haze, Bleach, X-Pro, Tintype,
  Night Shot, Neon 800T (video), Night Shot V (video).

### How to install on Android

1. Open the releases link on the phone.
2. Download the APK for the edition you want.
3. Tap it and allow "install from this source".
4. **To update later:** uninstall that edition first, then install the new APK. (Each build
   has a different test signature, so Android won't install over the old one.)

### How to install on iPhone

1. Open the GLASS link in **Safari**.
2. Tap Share, then **Add to Home Screen**.
3. Open it from the Home Screen and allow the camera.

---

## 3. Version history

| Version | Date | What happened |
|---|---|---|
| v0.1 | 2026-11-09 | First build: live GPU film looks, 28 cameras, video, darkroom |
| v0.1.1 | 2026-11-10 | Fixed the sideways preview; first iPhone web app |
| v0.2 | 2026-11-10 | Big overhaul: 42 cameras, real film colour, fixed "weird lines". **User disliked the new "Dazz-style" UI**, so it was rolled back |
| v0.2.1 | 2026-11-10 | Old UI restored, slow shutter on every camera |
| v0.3 | 2026-11-10 | Closed-loop camera wheel, Customize per camera, SLOW badge, iOS app shell |
| v0.4 | 2026-11-12 | Three new interfaces (DOT, GLASS, FIELD), engine v4, 51 cameras, classic kept as it was |
| (web) | 2026-11-12 | Only GLASS published on Safari, by choice |

---

## 4. My preferences (for Claude to remember)

- **Non-technical and mobile-first.** Plain language, short numbered steps, links, send APKs
  as files.
- **Commit and push everything.** Keep the memory vault notes updated after real work.
- **Premium feel matters:** "not basic", "not cluttered", "smooth". Visible glitches and
  constant UI changes are a big turn-off.
- **Slow shutter on every camera**, and it must always be obvious when it's on.
- **Customize must be easy to reach.**
- **When asked to leave a version alone, leave it alone.** (The classic v0.3 app stays as
  it is.)
- **Be honest about what wasn't tested.** Nothing has been tried on a real phone from the
  cloud.

## 5. The hard rule (agreed)

- **FUN CAM never uses Dazz Cam's own files:** no IPA, filters, LUTs, frames or decrypted
  code. The `dazz-cam-ipa` repo was offered several times and declined each time (copyright,
  and the app is DRM-protected).
- **The allowed way to match a look:** take the same photo with the iPhone camera and with
  Dazz, then `tools/match_look.py` measures the difference and builds FUN CAM's own version.
  Copying **features and behaviour** is fine; copying **assets** is not.

---

## 6. Learnings

### Things that went wrong and how they were fixed

- **Sideways preview:** the camera's picture was already rotated by Android, and FUN CAM
  rotated it again. Fix: trust Android's rotation; a "Rotate preview" setting stays as a
  backup.
- **"Weird vertical lines" in photos:** a cheap random-number trick breaks on phone graphics
  chips. Fix: a proper integer-based random generator for grain and leaks.
- **Blocky ghost glow:** fake blur from a few samples. Fix: a real blur.
- **Flash looked blown out:** the "close subject" boost was too strong. Fix: much smaller.
- **Infrared turned water and sky purple:** fix by only changing plants (green-over-blue).
- **Sure 85 looked wrong on the web since v0.2:** the tool that copies camera settings to
  the web stopped reading too early and dropped its grain, glow and dark corners. Found and
  fixed in v0.4. Lesson: a check that compares the web roster against the Android one is
  worth having.
- **Rounded viewfinder corners on Android:** the camera surface can't be clipped, so the
  corners are painted over it instead.

### Decisions that worked

- **One engine, many skins.** The film engine and camera logic are shared; each interface is
  just its own screen. Adding an edition doesn't touch the others.
- **Keeping the old version untouched** behind a switch (`Engine.v4` off for Classic), so
  new work can't break what already worked.
- **Testing without a phone:**
  - Web: a desktop browser with a fake camera walks through every panel, takes a photo and
    records a video.
  - Android: the real screens are drawn as pictures on the computer (Robolectric +
    Roborazzi), since the cloud has no Android emulator.

### Limits of the cloud setup

- No Android emulator and no real phone: **camera, haptics, focus lock, level and speed are
  untested** until tried on the phone.
- No iPhone or Safari: the web app was tested in Chrome only.
- YouTube blocks downloads from the cloud; Instagram reels can be fetched.
- Some image sites time out (picsum.photos did); images.unsplash.com worked for test photos.
- Committing a signing key to the repo was blocked by a safety check, so APKs still need an
  uninstall before each update.

### GitHub gotchas

- **GitHub Pages only deploys from the branch `claude/dreamy-gauss-27k7dh`.** On any other
  branch the web deploy fails in seconds with no log. To update the Safari site, the same
  commits are pushed there too (approved for the GLASS deploy).
- **Build task names need the edition** (for example `testGlassDebugUnitTest`); the plain
  name fails now that there are four apps.
- An unsigned iPhone app (IPA) can't be installed by tapping it; it needs AltStore or
  Sideloadly and a re-sign every 7 days on a free Apple ID. TestFlight needs a $99/yr Apple
  Developer account.

### Research worth keeping

- Dazz camera groups: Digital, Video, Vintage 135, Vintage 120, Instant, Accessories.
- Many Dazz cameras imitate real ones (CPM35 = Canon Sure Shot 85, NT16 = Yashica T4,
  CT2F = Contax T2, HOGA = Holga, Golf = Fuji 16-lens, D3D = Nishika 4-lens 3D, and more).
  The full table is in the repo's `CLAUDE.md` and `docs/CAMERAS.md`.
- **The viral slow-shutter night video look** = a plain video camera at ISO 160, shutter
  1/4. FUN CAM's **Dream Blur** camera presets this.

---

## 7. Open items (what's next)

1. **Try the three new apps on the phone** and say what feels wrong (camera, smoothness,
   haptics, focus lock, the dial).
2. **Pick a favourite edition** so the next round of polish goes there.
3. **Look matching:** send iPhone-original vs Dazz photo pairs (see `docs/MATCHING.md`).
4. An in-app photo roll on Android for the new editions (they open the phone's gallery now).
5. Kino / Neon 800T should work in landscape.
6. A stable signing key (set up as a GitHub secret) so updates install over the old app.
7. Later, maybe: merge into `main`, TestFlight, a Play Store listing.
