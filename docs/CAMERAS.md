# FUN CAM camera roster (v0.2)

Each FUN CAM camera matches one Dazz Cam camera *concept*: the same real-world camera or film
it imitates. The name, art and implementation are FUN CAM's own. FUN CAM recreates the look
of the **real camera and film** using public film-emulation data. Nothing is extracted from
Dazz.

## Sources
- **Camera ↔ real-world mapping:** community list, https://gist.github.com/hrchu/f520b4b24579a205ba2d303c1811ea2f
- **Per-filter visual notes and the video camera list:** "I tried all 30 filters" (chiba, 2025),
  https://note.com/smallworks/n/n5750bbd5d571
- **UI and Original V settings:** a user tutorial video (frames inspected), plus the captions
  of a YouTube tutorial (Zzfs5CQSyJM).
- **Film colour:** RawTherapee Film Simulation HaldCLUT collection (CC BY-SA 4.0). See
  `THIRD_PARTY.md`.

## Photo cameras (30)
| FUN CAM | Dazz concept | Real basis | Look we build |
|---|---|---|---|
| Plain | Original | your phone | Clean colour. Manual ISO/shutter for long exposures. |
| CCD-05 | CCD R | 2000s CCD compact | Saturated, crisp, cool whites, hard flash, digital orange date |
| GR-D | GRD R | Ricoh GR Digital | High contrast, deep blacks, cool and muted, gritty noise |
| Pastel | Puli | soft-focus digicam | Soft, dreamy, bright, glowing highlights |
| Fine X | FXN R | Fujifilm FinePix CCD | Clean Fuji greens/cyans, crisp, digital date |
| Collage | Collage | — | 4 separate shots laid out on one white card |
| Range | D Classic | Canon P / Leica M rangefinder | Nostalgic warm negative, fine grain, gentle halation |
| GR Mono | GR F | Ricoh GR film | Contrasty black & white, strong grain |
| T2 | CT2F | Contax T2 | Sharp; slight magenta cast; strong grain; rich |
| Fade 35 | D Exp | Fujica 35SE (old roll) | Faded, washed colours, cyan shadows, leaks |
| T4 Super | NT16 | Yashica T4 | Punchy, high-saturation, slide-like contrast |
| 3D Wiggle | D3D | Nishika N8000 (4 lenses) | 4-frame wobble clip |
| Slide NE | 135 NE | 35mm slide film | Slide colour inside a thick black film frame |
| Fun Saver | D FunS | Kodak Fun Saver | Disposable: flash on, warm, vignette, soft corners, date |
| Infra | IR | colour-infrared film | False colour: foliage → magenta/red, sky → aqua |
| SLR Classic | Classic U | Leica R3 SLR | Warm, restrained, film date imprint |
| Simple | DQS | Fuji Simple Ace disposable | Green-leaning consumer film, flash, grain |
| Quick 800 | FQS R | Fuji QuickSnap 800 | Deep colours, chunky grain |
| Byu 16 | Golf | Fuji Byu-N16 (16 lenses) | 16 sequential frames in a 4×4 grid |
| Sure 85 | CPM35 | Canon Sure Shot 85 | Bluish, pale, nostalgic, orange date — the classic |
| Sprocket | 135 SR | Lomo Sprocket Rocket | Panoramic, exposed over the sprocket holes |
| Half | D Half | Konica Recorder | Two vertical half-frames side by side |
| Slide | D Slide | slide mount | Ektachrome colour in a card slide mount |
| TLR | S Classic | Rolleiflex / Yashica-Mat | Square, black frame, deep saturated contrast |
| Toy 120 | HOGA | Holga | Heavy vignette, cut-off edges, soft corners, leaks |
| Six7 | S 67 | Pentax 67 | 6×7 frame in black rebate, fine grain, film colour |
| Kiev | KV88 | Kiev 88 | Square negative-strip frame with edge markings, cool colour |
| Insta Mini | Inst C | Instax Mini 8 | Mini instant print, subdued colour, gentle |
| Insta Mono | Inst SQ | Instax SQ10 (B&W) | Square instant, high-contrast black & white |
| Insta SQ | Inst SQC | Instax Mini 90 colour | Square instant colour; frame colour selectable |

## Video cameras (12)
| FUN CAM | Dazz concept | Look |
|---|---|---|
| Plain V | Original V | Manual ISO / shutter / FPS. Slow shutter for light trails (reel: ISO 160, 1/4 s, 13 mm) |
| Classic V | V Classic | Film-negative video, grain, halation, 24 fps |
| 16 Mil | 16mm | Reversal colour, slightly rounded gate, weave, 24 fps |
| Super 8 | 8mm | Kodachrome colour, rounded gate, flicker, dust, 18 fps |
| VHS | VHS | Composite blur, chroma bleed, tracking noise, PLAY overlay |
| Kino | Kino | 2.39:1 cinema letterbox, teal/orange grade, 24 fps |
| Insta V | InstSS | Video inside an instant-print frame |
| Fun Saver V | V FunS | Disposable look with date stamp |
| Handycam | DCR | Late-90s camcorder: sharpened, punchy, date/time overlay |
| Glow V | Glow | Dreamy bloom, pastel |
| Projector | Slide P | Projected film: hot centre, vignette, flicker |
| Dream Blur | (FUN CAM extra) | Original V's popular slow-shutter night look, preset |

## Accessories
Fisheye W (full-frame), Fisheye C (circular), Prism, Star, ND, Flash colour gels, double
exposure, light leaks on/off, date stamp on/off.

## Added in engine v4 (DOT, GLASS and FIELD editions)

These are FUN CAM's own additions, not counterparts of another app's cameras. Their colour comes
from formulas in `tools/make_luts.py`, except X-Pro, which uses the cross-process film LUT.

| Camera | Group | What it recreates |
|---|---|---|
| Cine 800T | Vintage 135 | Tungsten cinema negative at night: teal shadows, strong red halation around lights |
| Gold 200 | Vintage 135 | Consumer daylight film: golden highlights, warm skin, rich reds |
| Purple Haze | Vintage 135 | Colour-shifting film: greens turn violet, blues go teal |
| Bleach | Vintage 135 | Bleach bypass: low saturation, hard contrast, silvery |
| X-Pro | Vintage 135 | Cross-processed slide film in a toy camera |
| Tintype | Vintage 120 | Wet-plate collodion: blue-sensitive, brown-black tones, dust, soft swirly edges |
| Night Shot | Digital | Camcorder night vision: green phosphor, noise, scan lines |
| Neon 800T | Video | Cine 800T with anamorphic blue streaks, 2.39:1, 24 fps |
| Night Shot V | Video | Night-vision camcorder video with date and time |
