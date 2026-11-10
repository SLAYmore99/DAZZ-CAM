# Third-party material

## Film colour LUTs — CC BY-SA 4.0
`app/src/main/assets/luts/*.png` and `web/luts/*.png` (except `ccd`, `grd`, `infra`, `vhs` and
`dcr`, which are FUN CAM's own formulas) are resampled (33³) from the **RawTherapee Film
Simulation Collection** (version 2015-09-20).
- Authors: Pat David, Pavlov Dmitry, Michael Ezra.
- Licence: Creative Commons Attribution-ShareAlike 4.0, https://creativecommons.org/licenses/by-sa/4.0/
- Changes: resampled from Hald CLUT level 12 to a 33×33×33 cube by `tools/make_luts.py`.

The derived LUT files stay under CC BY-SA 4.0. Film-stock names are used in the tooling only to
describe which stock a table approximates. FUN CAM isn't affiliated with or endorsed by any film
manufacturer.

## Rendering approach
The grade → bloom → composite structure and the integer PCG hash used for grain follow the
approach of **FilmCamera-Android** by Yahia (MIT licence),
https://github.com/YahiaAngelo/FilmCamera-Android. The PCG hash itself is from Jarzynski &
Olano, "Hash Functions for GPU Rendering" (JCGT 2020).
