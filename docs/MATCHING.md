# Matching a camera look from photos

FUN CAM matches a look by **measuring photos**, never by using another app's files.
`tools/match_look.py` compares an untouched original with the same scene rendered by the look
you want. It then fits:
- a 33³ colour LUT
- vignette
- grain
- sharpness

It also aligns handheld pairs automatically, and ignores frames and the date-stamp corner.

**Self-test:** we applied a known film look plus a 0.40 vignette, grain, a date stamp and a 2°
handheld tilt to four photos. The tool recovered:
- **colour:** average error 2.9/255 on unseen colours
- **vignette:** 0.38

## How to capture reference pairs

**Best: use the app's import feature (pixel-perfect).**
1. Take a normal photo with the iPhone Camera app.
2. In the other app, import that same photo into the camera you want to match. Turn the date
   stamp off if you can.
3. Save the result. Name the files `<camera>_orig_1.jpg` and `<camera>_target_1.jpg`.

**For cameras that can't import: two shots from the same spot.**
Hold the phone still, or lean it on something. Take one shot with the iPhone camera and one
with the camera you want to match. Turn the flash off for both, unless you're matching flash.

**What to photograph (2–4 pairs per camera):**
- daylight with sky and greenery
- a face, for skin tones
- an indoor scene
- a colourful scene: clothes, signs, food

## Run it

```bash
pip install opencv-python-headless scipy numpy
python3 tools/match_look.py --name sure85 reference/sure85_orig_1.jpg reference/sure85_target_1.jpg \
                                         reference/sure85_orig_2.jpg reference/sure85_target_2.jpg
```

This writes:
- `app/src/main/assets/luts/m_sure85.png` and `web/luts/m_sure85.png`: the fitted LUT
- `docs/matches/sure85.json`: the fit error and suggested `vignette`, `grain`, `sharpen` and
  `softAll`
- `docs/matches/sure85_check.jpg`: original | target | our match

Then point the camera's `Look(lut = "m_sure85", ...)` in `model/Cameras.kt` at the new LUT and
apply the suggested values.
