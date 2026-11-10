#!/usr/bin/env python3
"""Match a camera look from example photo pairs (measuring outputs, never using another app's files).

Give it pairs of the SAME scene: an untouched original and the same scene rendered by the look you
want to match. Best: import one iPhone photo into the other app (pixel-aligned). Also works with two
separate shots from the same spot (aligned automatically with feature matching).

    pip install opencv-python-headless scipy numpy
    python3 tools/match_look.py --name sure85 orig1.jpg target1.jpg orig2.jpg target2.jpg ...

Outputs:
  app/src/main/assets/luts/m_<name>.png and web/luts/m_<name>.png  (33^3 LUT, same format as make_luts.py)
  docs/matches/<name>.json  (fitted LUT error, vignette, grain, sharpness suggestions)
  docs/matches/<name>_check.jpg  (original | target | our match, for eyeballing)
"""
import argparse, json, os
import numpy as np
import cv2
from scipy.ndimage import gaussian_filter

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
N = 33
WORK = 1200


def load(path):
    im = cv2.imread(path, cv2.IMREAD_COLOR)
    if im is None:
        raise SystemExit(f'cannot read {path}')
    im = cv2.cvtColor(im, cv2.COLOR_BGR2RGB).astype(np.float32) / 255.0
    s = WORK / max(im.shape[:2])
    return cv2.resize(im, (round(im.shape[1] * s), round(im.shape[0] * s)), interpolation=cv2.INTER_AREA) if s < 1 else im


def align(src, dst):
    """Warp dst onto src. Returns (warped dst, valid mask, method)."""
    if src.shape == dst.shape and np.abs(gaussian_filter(src.mean(2), 3) - gaussian_filter(dst.mean(2), 3)).mean() < 0.25:
        # might already be aligned: check correlation of edges
        e1 = cv2.Laplacian(cv2.GaussianBlur(src.mean(2), (0, 0), 2), cv2.CV_32F)
        e2 = cv2.Laplacian(cv2.GaussianBlur(dst.mean(2), (0, 0), 2), cv2.CV_32F)
        if np.corrcoef(e1.ravel(), e2.ravel())[0, 1] > 0.6:
            return dst, np.ones(src.shape[:2], bool), 'aligned'
    g1 = cv2.equalizeHist((src.mean(2) * 255).astype(np.uint8))
    g2 = cv2.equalizeHist((dst.mean(2) * 255).astype(np.uint8))
    orb = cv2.ORB_create(6000)
    k1, d1 = orb.detectAndCompute(g1, None)
    k2, d2 = orb.detectAndCompute(g2, None)
    if d1 is None or d2 is None:
        raise SystemExit('could not find features to align the pair; use the import method')
    m = cv2.BFMatcher(cv2.NORM_HAMMING, crossCheck=True).match(d1, d2)
    m = sorted(m, key=lambda x: x.distance)[:1500]
    p1 = np.float32([k1[x.queryIdx].pt for x in m]); p2 = np.float32([k2[x.trainIdx].pt for x in m])
    H, inl = cv2.findHomography(p2, p1, cv2.RANSAC, 3.0)
    if H is None or inl.sum() < 40:
        raise SystemExit('alignment failed (too few matches); take both shots from the same spot or use import')
    h, w = src.shape[:2]
    warped = cv2.warpPerspective(dst, H, (w, h), flags=cv2.INTER_LINEAR)
    valid = cv2.warpPerspective(np.ones(dst.shape[:2], np.float32), H, (w, h)) > 0.99
    return warped, valid, f'homography ({int(inl.sum())} inliers)'


def poly_features(c):
    r, g, b = c[:, 0], c[:, 1], c[:, 2]
    f = [np.ones_like(r), r, g, b, r * g, r * b, g * b, r * r, g * g, b * b, r * g * b,
         r ** 3, g ** 3, b ** 3, r * r * g, r * r * b, g * g * r, g * g * b, b * b * r, b * b * g]
    return np.stack(f, 1)


def fit_lut(src, dst, w):
    """Smooth 3D LUT: polynomial prior + trilinear-splatted local correction."""
    X = poly_features(src)
    sw = np.sqrt(w)[:, None]
    coef, *_ = np.linalg.lstsq(X * sw, dst * sw, rcond=None)
    grid = np.linspace(0, 1, N)
    B, G, R = np.meshgrid(grid, grid, grid, indexing='ij')
    nodes = np.stack([R, G, B], -1).reshape(-1, 3)
    cubic = np.clip(poly_features(nodes) @ coef, 0, 1).reshape(N, N, N, 3)
    # a 3x4 affine fit extrapolates sanely into colours the photos never contained
    Xa = np.concatenate([src, np.ones((len(src), 1))], 1)
    A, *_ = np.linalg.lstsq(Xa * sw, dst * sw, rcond=None)
    affine = np.clip(np.concatenate([nodes, np.ones((len(nodes), 1))], 1) @ A, 0, 1).reshape(N, N, N, 3)
    # data density on the grid decides which prior to trust
    dens = np.zeros((N, N, N))
    xi = np.clip(np.round(np.clip(src, 0, 1) * (N - 1)).astype(int), 0, N - 1)
    np.add.at(dens, (xi[:, 2], xi[:, 1], xi[:, 0]), w)
    dens = gaussian_filter(dens, 2.5)
    trust = np.clip(dens / (np.percentile(dens[dens > 0], 30) + 1e-9), 0, 1)[..., None]
    prior = cubic * trust + affine * (1 - trust)
    # splat residuals onto the grid (relative to the prior)
    resid = dst - apply_lut(prior, src)
    acc = np.zeros((N, N, N, 3)); cnt = np.zeros((N, N, N))
    x = np.clip(src, 0, 1) * (N - 1)
    i0 = np.floor(x).astype(int); f = x - i0; i1 = np.minimum(i0 + 1, N - 1)
    for dr in (0, 1):
        for dg in (0, 1):
            for db in (0, 1):
                ir = i1[:, 0] if dr else i0[:, 0]; ig = i1[:, 1] if dg else i0[:, 1]; ib = i1[:, 2] if db else i0[:, 2]
                ww = (f[:, 0] if dr else 1 - f[:, 0]) * (f[:, 1] if dg else 1 - f[:, 1]) * (f[:, 2] if db else 1 - f[:, 2]) * w
                np.add.at(cnt, (ib, ig, ir), ww)
                np.add.at(acc, (ib, ig, ir), resid * ww[:, None])
    k = 3.0  # confidence needed before local data overrides the smooth prior
    corr = acc / (cnt[..., None] + k)
    corr = np.stack([gaussian_filter(corr[..., c], 1.0) for c in range(3)], -1)
    return np.clip(prior + corr, 0, 1), coef


def apply_lut(lut, img):
    x = np.clip(img, 0, 1) * (N - 1)
    i0 = np.floor(x).astype(int); f = x - i0; i1 = np.minimum(i0 + 1, N - 1)
    out = 0
    for dr in (0, 1):
        for dg in (0, 1):
            for db in (0, 1):
                ir = i1[..., 0] if dr else i0[..., 0]; ig = i1[..., 1] if dg else i0[..., 1]; ib = i1[..., 2] if db else i0[..., 2]
                ww = (f[..., 0] if dr else 1 - f[..., 0]) * (f[..., 1] if dg else 1 - f[..., 1]) * (f[..., 2] if db else 1 - f[..., 2])
                out = out + lut[ib, ig, ir] * ww[..., None]
    return out


def radius(h, w):
    y, x = np.mgrid[0:h, 0:w]
    a = w / h
    q = np.stack([(x / w - 0.5) * a, (y / h - 0.5)], -1)
    return (q ** 2).sum(-1) / (0.25 * (a * a + 1))   # 0 centre .. 1 corner, same as the shader


def smoothstep(a, b, x):
    t = np.clip((x - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


def luma(c):
    return c[..., 0] * 0.2126 + c[..., 1] * 0.7152 + c[..., 2] * 0.0722


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--name', required=True)
    ap.add_argument('--margin', type=float, default=0.06, help='ignore this fraction at the edges (frames)')
    ap.add_argument('--stamp', type=float, default=0.22, help='ignore bottom-right corner this big (date stamp)')
    ap.add_argument('--no-write', action='store_true')
    ap.add_argument('files', nargs='+')
    a = ap.parse_args()
    if len(a.files) % 2:
        raise SystemExit('give files as pairs: original target original target ...')

    S, D, W, pairs = [], [], [], []
    vig_r, vig_ratio, grain_t, grain_s, sharp = [], [], [], [], []
    for sp, dp in zip(a.files[0::2], a.files[1::2]):
        src, dst = load(sp), load(dp)
        if dst.shape != src.shape and abs(dst.shape[1] / dst.shape[0] - src.shape[1] / src.shape[0]) < 0.02:
            dst = cv2.resize(dst, (src.shape[1], src.shape[0]), interpolation=cv2.INTER_AREA)
        dst, valid, how = align(src, dst)
        h, w = src.shape[:2]
        m = valid.copy()
        mx, my = int(w * a.margin), int(h * a.margin)
        m[:my] = m[-my:] = False; m[:, :mx] = m[:, -mx:] = False
        m[int(h * (1 - a.stamp)):, int(w * (1 - a.stamp * 1.6)):] = False
        # blurred colours (grain-free) and low-gradient weights (robust to small misalignment)
        sb = cv2.GaussianBlur(src, (0, 0), 2.0); db = cv2.GaussianBlur(dst, (0, 0), 2.0)
        grad = cv2.Laplacian(sb.mean(2), cv2.CV_32F) ** 2
        wgt = np.exp(-grad / (np.percentile(grad, 60) + 1e-6)) * m
        r2 = radius(h, w)
        centre = wgt * (r2 < 0.2)   # vignette-free core for the colour fit
        idx = np.flatnonzero(centre.ravel() > 0.05)
        if len(idx) > 60000: idx = np.random.default_rng(0).choice(idx, 60000, replace=False)
        S.append(sb.reshape(-1, 3)[idx]); D.append(db.reshape(-1, 3)[idx]); W.append(centre.ravel()[idx])
        pairs.append(dict(original=os.path.basename(sp), target=os.path.basename(dp), alignment=how))
        # keep for second-stage measurements
        pairs[-1]['_data'] = (src, dst, sb, db, m, wgt, r2)

    S = np.concatenate(S); D = np.concatenate(D); Wt = np.concatenate(W)
    lut, coef = fit_lut(S, D, Wt)
    lut_centre = lut
    err = np.abs(apply_lut(lut, S) - D).mean() * 255

    for p in pairs:
        src, dst, sb, db, m, wgt, r2 = p.pop('_data')
        pred = apply_lut(lut_centre, sb)
        lp, ld = luma(pred), luma(db)
        ok = (wgt > 0.05) & (lp > 0.08) & (lp < 0.92)
        vig_r.append(r2[ok]); vig_ratio.append((ld[ok] / lp[ok]))
        # grain: high-pass energy in flat areas
        flat = ok & (cv2.Laplacian(sb.mean(2), cv2.CV_32F) ** 2 < np.percentile(cv2.Laplacian(sb.mean(2), cv2.CV_32F) ** 2, 40))
        hp_t = luma(dst) - luma(cv2.GaussianBlur(dst, (0, 0), 1.5)); hp_s = luma(src) - luma(cv2.GaussianBlur(src, (0, 0), 1.5))
        grain_t.append(hp_t[flat].std()); grain_s.append(hp_s[flat].std())
        # sharpness: band-pass energy ratio in textured areas
        tex = (wgt < 0.3) & m
        bp_t = luma(cv2.GaussianBlur(dst, (0, 0), 1.0)) - luma(cv2.GaussianBlur(dst, (0, 0), 3.0))
        bp_s = luma(cv2.GaussianBlur(src, (0, 0), 1.0)) - luma(cv2.GaussianBlur(src, (0, 0), 3.0))
        if tex.sum() > 500: sharp.append(bp_t[tex].std() / (bp_s[tex].std() + 1e-6))

    r = np.concatenate(vig_r); ratio = np.concatenate(vig_ratio)
    sm = smoothstep(0.2, 1.1, r) * 0.9
    # ratio ≈ 1 - v * sm  -> least squares for v
    v = float(np.clip(((1 - ratio) * sm).sum() / ((sm * sm).sum() + 1e-6), 0, 1))
    g_excess = max(0.0, float(np.mean(grain_t)) ** 2 - float(np.mean(grain_s)) ** 2) ** 0.5
    grain = round(min(1.0, g_excess / 0.022), 2)     # shader grain 1.0 ~ 0.022 luma std after the 1.5px high-pass
    sh = float(np.mean(sharp)) if sharp else 1.0
    report = dict(
        name=a.name, pairs=pairs, lut_mean_abs_error_8bit=round(float(err), 2),
        suggested=dict(lut=f'm_{a.name}', lutMix=1.0, vignette=round(v, 2), grain=grain,
                       sharpen=round(max(0.0, (sh - 1) * 1.5), 2), softAll=round(max(0.0, (1 - sh) * 1.2), 2)),
        measured=dict(grain_std_target=round(float(np.mean(grain_t)), 4), grain_std_original=round(float(np.mean(grain_s)), 4),
                      detail_ratio=round(sh, 3)),
    )
    print(json.dumps(report, indent=1))
    if a.no_write:
        return report, lut
    img = (np.clip(lut, 0, 1) * 255 + 0.5).astype(np.uint8).reshape(N * N, N, 3)
    for d in ['app/src/main/assets/luts', 'web/luts']:
        os.makedirs(os.path.join(ROOT, d), exist_ok=True)
        cv2.imwrite(os.path.join(ROOT, d, f'm_{a.name}.png'), cv2.cvtColor(img, cv2.COLOR_RGB2BGR))
    os.makedirs(os.path.join(ROOT, 'docs/matches'), exist_ok=True)
    json.dump(report, open(os.path.join(ROOT, f'docs/matches/{a.name}.json'), 'w'), indent=1)
    src, dst = load(a.files[0]), load(a.files[1])
    dst, _, _ = align(src, dst) if dst.shape == src.shape else align(src, cv2.resize(dst, (src.shape[1], src.shape[0])))
    ours = apply_lut(lut, src) * (1 - v * smoothstep(0.2, 1.1, radius(*src.shape[:2])) * 0.9)[..., None]
    check = np.concatenate([src, dst, np.clip(ours, 0, 1)], 1)
    cv2.imwrite(os.path.join(ROOT, f'docs/matches/{a.name}_check.jpg'), cv2.cvtColor((check * 255).astype(np.uint8), cv2.COLOR_RGB2BGR))
    return report, lut


if __name__ == '__main__':
    main()
