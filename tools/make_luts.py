#!/usr/bin/env python3
"""Builds FUN CAM's colour LUTs.

Film stocks come from the RawTherapee Film Simulation HaldCLUT collection (CC BY-SA 4.0,
Pat David, Pavlov Dmitry, Michael Ezra). Digital/video looks are FUN CAM's own formulas below.

Output: 33x33x33 LUT stored as a 33 x 1089 RGB PNG where pixel (x=r, y=g + b*33) holds the
output colour. That row order equals GL's 3D texture order, so it uploads directly.

usage: make_luts.py /path/to/HaldCLUT
"""
import os, sys
import numpy as np
from PIL import Image

N = 33
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUTS = [os.path.join(ROOT, 'app/src/main/assets/luts'), os.path.join(ROOT, 'web/luts')]

FILMS = {
    'portra160': 'Color/Kodak/Kodak Portra 160 2.png',
    'portra400': 'Color/Kodak/Kodak Portra 400 2.png',
    'portra800': 'Color/Kodak/Kodak Portra 800 2.png',
    'elite200': 'Color/Kodak/Kodak Elite Color 200.png',
    'elite400': 'Color/Kodak/Kodak Elite Color 400.png',
    'ektar100': 'Color/Kodak/Kodak Ektar 100.png',
    'ektachrome': 'Color/Kodak/Kodak Ektachrome 100 VS.png',
    'kodachrome64': 'Color/Kodak/Kodak Kodachrome 64.png',
    'superia200': 'Color/Fuji/Fuji Superia 200.png',
    'superia400': 'Color/Fuji/Fuji Superia 400 2.png',
    'superia800': 'Color/Fuji/Fuji Superia X-Tra 800.png',
    'fuji400h': 'Color/Fuji/Fuji 400H 2.png',
    'fuji160c': 'Color/Fuji/Fuji 160C 2.png',
    'velvia50': 'Color/Fuji/Fuji Velvia 50.png',
    'provia100f': 'Color/Fuji/Fuji Provia 100F.png',
    'astia': 'Color/Fuji/Fuji Astia 100F.png',
    'fp100c': 'Color/Fuji/Fuji FP-100c 3.png',
    'agfavista': 'Color/Agfa/Agfa Vista 200.png',
    'polaroid669': 'Color/Polaroid/Polaroid 669 3.png',
    'polaroid690': 'Color/Polaroid/Polaroid 690 3.png',
    'xpro': 'Color/Lomography/Lomography X-Pro Slide 200.png',
    'tealorange': 'Color/CreativePack-1/TealOrange.png',
    'candlelight': 'Color/CreativePack-1/CandleLight.png',
    'trix': 'Black-and-White/Kodak/Kodak TRI-X 400 2.png',
    'fp3000b': 'Black-and-White/Fuji/Fuji FP-3000b HC.png',
}


def grid():
    v = np.linspace(0.0, 1.0, N)
    b, g, r = np.meshgrid(v, v, v, indexing='ij')  # index order [b][g][r] -> r fastest
    return np.stack([r, g, b], -1)


def hald(path):
    im = np.asarray(Image.open(path).convert('RGB'), dtype=np.float32) / 255.0
    flat = im.reshape(-1, 3)
    level = round(round(len(flat) ** (1 / 3)) ** 0.5)
    n = level * level  # cube size (144 for level 12)
    cube = flat.reshape(n, n, n, 3)  # [b][g][r]
    # trilinear resample to N
    x = np.linspace(0, n - 1, N)
    i0 = np.floor(x).astype(int); i1 = np.minimum(i0 + 1, n - 1); f = (x - i0)
    def lerp_axis(c, axis):
        a = np.take(c, i0, axis=axis); b = np.take(c, i1, axis=axis)
        shape = [1, 1, 1, 1]; shape[axis] = N
        return a + (b - a) * f.reshape(shape)
    for ax in (0, 1, 2):
        cube = lerp_axis(cube, ax)
    return cube


def luma(c):
    return c[..., 0] * 0.299 + c[..., 1] * 0.587 + c[..., 2] * 0.114


def sat(c, s):
    l = luma(c)[..., None]
    return l + (c - l) * s


def scurve(x, k):
    """Smooth S-curve around 0.5 with strength k (0 = identity)."""
    s = x * x * (3 - 2 * x)
    return np.clip(x + (s - x) * k, 0, 1)


def shoulder(x, start=0.8):
    """Digital-style highlight clip with a short shoulder."""
    over = np.clip((x - start) / (1 - start), 0, 1)
    return np.where(x > start, start + (1 - start) * (1 - (1 - over) ** 2.2), x)


# ---------------------------------------------------------------- FUN CAM's own looks
def ccd(c):
    """Early-2000s CCD compact: punchy, cool-cyan whites, yellowish greens, hard highlight clip."""
    c = scurve(c, 0.35)
    c = sat(c, 1.22)
    l = luma(c)[..., None]
    c = c + np.array([-0.015, 0.0, 0.03]) * (l ** 2) * 2       # cool highlights
    g = c[..., 1:2]; r = c[..., 0:1]
    c[..., 0:1] = r + 0.04 * np.clip(g - r, 0, 1)                # greens lean yellow
    return shoulder(np.clip(c, 0, 1), 0.82)


def grd(c):
    """Ricoh GR Digital high-contrast: deep blacks, muted, slightly cool."""
    c = np.clip((c - 0.06) / 0.94, 0, 1)
    c = scurve(c, 0.75)
    c = sat(c, 0.82)
    c = c * np.array([0.97, 1.0, 1.04])
    return np.clip(c, 0, 1)


def infra(c):
    """Colour-infrared film (false colour): foliage turns magenta/red, sky deep aqua-blue, skin warm."""
    r, g, b = c[..., 0], c[..., 1], c[..., 2]
    l = luma(c)
    veg = np.clip((g - b + 0.05) / 0.15, 0, 1)                       # only green-over-blue reflects IR strongly
    ir = np.clip(0.2 * l + (1.6 * np.clip(g - b, 0, 1) + 0.6 * np.clip(g - r, 0, 1)) * veg + 0.3 * np.clip(r - b, 0, 1), 0, 1)
    out = np.stack([ir, r * 0.9 + 0.05, np.clip(g * 0.7 + b * 0.45, 0, 1)], -1)
    out = scurve(sat(np.clip(out, 0, 1), 1.12), 0.3)
    return np.clip(out, 0, 1)


def vhs(c):
    """Consumer VHS: hot reds/oranges, lifted video blacks, slight green cast, clipped whites."""
    c = sat(c, 1.18)
    r = c[..., 0:1]; g = c[..., 1:2]
    c[..., 0:1] = r + 0.08 * np.clip(r - g, 0, 1)
    c = c * np.array([1.0, 1.03, 0.96])
    c = 0.06 + c * 0.9
    return shoulder(np.clip(c, 0, 1), 0.85)


def dcr(c):
    """Late-90s camcorder: video gamma, punchy saturation, cool highlights, warm skin."""
    c = np.clip(c, 0, 1) ** 0.9
    c = scurve(c, 0.3)
    c = sat(c, 1.25)
    l = luma(c)[..., None]
    c = c + np.array([0.02, 0.0, -0.01]) * (1 - l) + np.array([-0.01, 0.0, 0.03]) * l * l
    return shoulder(np.clip(c, 0, 1), 0.86)


CUSTOM = {'ccd': ccd, 'grd': grd, 'infra': infra, 'vhs': vhs, 'dcr': dcr}


def save(name, cube):
    img = (np.clip(cube, 0, 1) * 255 + 0.5).astype(np.uint8)  # [b][g][r][3]
    img = img.reshape(N * N, N, 3)  # rows = g + b*N, cols = r
    for d in OUTS:
        os.makedirs(d, exist_ok=True)
        Image.fromarray(img, 'RGB').save(os.path.join(d, name + '.png'), optimize=True)


def main():
    src = sys.argv[1]
    for name, rel in FILMS.items():
        save(name, hald(os.path.join(src, rel)))
    for name, fn in CUSTOM.items():
        save(name, fn(grid()))
    print(f'{len(FILMS) + len(CUSTOM)} LUTs written')


if __name__ == '__main__':
    main()
