package com.funcam.app.gl

/**
 * FUN CAM render engine v2 (shared verbatim with the web app via tools/gen_web_data.py).
 *
 *  INPUT      camera OES -> upright, frame accumulation (slow shutter)
 *  GRADE      lens + flash + exposure/WB (linear) -> film LUT -> tone/colour   (picture size)
 *  DOWN       4x box downsample                                                (quarter size)
 *  BLUR       separable Gaussian, run H then V                                 (quarter size)
 *  COMPOSITE  glow, halation, softness, leaks, vignette, grain, video artefacts,
 *             date stamp, frames/gates                                         (output size)
 *
 * Noise uses an integer PCG hash (Jarzynski & Olano 2020), which has no lattice patterns on mobile
 * GPUs. The grade/bloom/composite split follows the approach of FilmCamera-Android (MIT).
 */
object Shaders {
    const val VERTEX = """#version 300 es
layout(location = 0) in vec2 aPos;
out vec2 vUv;
void main() {
    vUv = aPos * 0.5 + 0.5;
    gl_Position = vec4(aPos, 0.0, 1.0);
}
"""

    const val INPUT = """#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision highp float;
in vec2 vUv;
out vec4 o;
uniform samplerExternalOES uCam;
uniform mat4 uSt;
uniform int uRot;
uniform float uMirror;
uniform sampler2D uPrev;
uniform float uAlpha;
void main() {
    vec2 u = vUv;
    if (uMirror > 0.5) u.x = 1.0 - u.x;
    vec2 b;
    if (uRot == 90) b = vec2(1.0 - u.y, u.x);
    else if (uRot == 180) b = vec2(1.0 - u.x, 1.0 - u.y);
    else if (uRot == 270) b = vec2(u.y, 1.0 - u.x);
    else b = u;
    vec3 c = texture(uCam, (uSt * vec4(b, 0.0, 1.0)).xy).rgb;
    vec3 p = texture(uPrev, vUv).rgb;
    o = vec4(mix(p, c, uAlpha), 1.0);
}
"""

    const val COPY = """#version 300 es
precision mediump float;
in vec2 vUv;
out vec4 o;
uniform sampler2D uTex;
uniform float uFlipY;
void main() {
    vec2 uv = vUv;
    if (uFlipY > 0.5) uv.y = 1.0 - uv.y;
    o = texture(uTex, uv);
}
"""

    private const val COMMON = """
precision highp float;
precision highp int;
float luma(vec3 c) { return dot(c, vec3(0.2126, 0.7152, 0.0722)); }
uvec3 pcg3d(uvec3 v) {
    v = v * 1664525u + 1013904223u;
    v.x += v.y * v.z; v.y += v.z * v.x; v.z += v.x * v.y;
    v ^= v >> 16u;
    v.x += v.y * v.z; v.y += v.z * v.x; v.z += v.x * v.y;
    return v;
}
// three uniform randoms in [0,1) for an integer cell
vec3 rand3(ivec2 c, uint seed) {
    return vec3(pcg3d(uvec3(uvec2(c + 32768), seed))) * (1.0 / 4294967296.0);
}
float rand1(float x, uint seed) { return rand3(ivec2(int(floor(x)), 7), seed).x; }
// smooth value noise
float vnoise(vec2 p, uint seed) {
    vec2 i = floor(p); vec2 f = p - i; f = f * f * (3.0 - 2.0 * f);
    ivec2 c = ivec2(i);
    float a = rand3(c, seed).x, b = rand3(c + ivec2(1, 0), seed).x;
    float d = rand3(c + ivec2(0, 1), seed).x, e = rand3(c + ivec2(1, 1), seed).x;
    return mix(mix(a, b, f.x), mix(d, e, f.x), f.y);
}
float fbm(vec2 p, uint seed) {
    float s = 0.0, a = 0.5;
    for (int i = 0; i < 4; i++) { s += a * vnoise(p, seed + uint(i) * 101u); p = p * 2.03 + 11.7; a *= 0.5; }
    return s;
}
vec3 srgbToLinear(vec3 c) { return pow(max(c, 0.0), vec3(2.2)); }
vec3 linearToSrgb(vec3 c) { return pow(max(c, 0.0), vec3(1.0 / 2.2)); }
"""

    const val GRADE = """#version 300 es
$COMMON
precision highp sampler3D;
in vec2 vUv;
out vec4 fragColor;
uniform sampler2D uSrc;
uniform vec4 uSrcCrop;       // image uv -> source uv: offset.xy, scale.zw
uniform vec2 uSrcTexel;      // 1 / source texture size
uniform sampler2D uSrc2;     // double exposure
uniform vec4 uSrc2Crop;
uniform float uDouble;
uniform highp sampler3D uLut;
uniform float uLutMix;
uniform float uAspect;       // picture w / h
uniform float uTime;
uniform uint uSeed;
uniform float uExposure, uTemp, uTint;
uniform float uContrast, uSaturation, uFade, uMono;
uniform vec3 uLift, uGain, uShadowTint, uHighTint;
uniform float uFlash;
uniform vec3 uFlashColor;
uniform int uFisheye;        // 0 none, 1 wide barrel, 2 circular
uniform float uChroma, uPixel, uSharpen, uPrism, uVhs, uWeave;

vec3 src(vec2 p) {
    return texture(uSrc, uSrcCrop.xy + clamp(p, 0.0, 1.0) * uSrcCrop.zw).rgb;
}

void main() {
    vec2 p = vUv;   // picture uv, y up
    float frame = floor(uTime * 24.0);

    if (uWeave > 0.0) {
        p += uWeave * (vec2(vnoise(vec2(uTime * 3.1, 1.0), uSeed), vnoise(vec2(2.0, uTime * 2.3), uSeed)) - 0.5) * vec2(0.005, 0.008);
    }
    if (uVhs > 0.0) {
        float line = floor(p.y * 240.0);
        float j = (rand3(ivec2(int(line), int(frame)), uSeed).x - 0.5) * 0.0025;
        float band = smoothstep(0.08, 0.0, p.y);
        j += band * (rand3(ivec2(int(line * 3.0), int(frame)), uSeed + 3u).x - 0.5) * 0.04;
        j += sin(p.y * 9.0 + uTime * 1.7) * 0.0012;
        p.x += j * uVhs;
    }

    vec2 cq = (p - 0.5) * vec2(uAspect, 1.0);
    float r2 = dot(cq, cq);
    float fishMask = 1.0;
    if (uFisheye == 1) {
        float k = 0.55;
        vec2 s = cq * (1.0 - k * 0.35 + k * r2 * 0.9);
        p = s / vec2(uAspect, 1.0) + 0.5;
    } else if (uFisheye == 2) {
        float lim = 0.5 * min(uAspect, 1.0);
        float rr = sqrt(r2);
        fishMask = smoothstep(lim, lim - 0.004, rr);
        float t = rr / lim;
        float theta = t * 1.25;                     // equisolid-ish mapping
        float rs = tan(theta * 0.62) / tan(1.25 * 0.62) * lim;
        vec2 s = rr > 0.0 ? cq / rr * rs : cq;
        p = s / vec2(uAspect, 1.0) + 0.5;
    }
    if (uPixel > 0.0) {
        float cells = mix(1200.0, 260.0, uPixel);
        vec2 n = vec2(cells, cells / uAspect);
        p = (floor(p * n) + 0.5) / n;
    }

    vec3 c;
    if (uChroma > 0.0) {
        vec2 d = (p - 0.5) * uChroma * 0.006 * (0.4 + r2 * 3.0);
        c = vec3(src(p + d).r, src(p).g, src(p - d).b);
    } else {
        c = src(p);
    }
    if (uSharpen > 0.0) {
        vec2 t = uSrcTexel / uSrcCrop.zw * 1.5;
        vec3 b = (src(p + vec2(t.x, 0.0)) + src(p - vec2(t.x, 0.0)) + src(p + vec2(0.0, t.y)) + src(p - vec2(0.0, t.y))) * 0.25;
        c += (c - b) * uSharpen * 1.6;
    }
    if (uVhs > 0.0) {
        // composite video: luma keeps detail, chroma smears to the right
        vec3 cs = (src(p + vec2(0.004, 0.0)) + src(p + vec2(0.009, 0.0)) + src(p + vec2(0.015, 0.0)) + src(p + vec2(0.022, 0.0))) * 0.25;
        vec3 ls = (src(p + vec2(0.0015, 0.0)) + src(p - vec2(0.0015, 0.0))) * 0.5;
        float y = mix(luma(c), luma(ls), 0.6);
        c = mix(c, vec3(y) + (cs - vec3(luma(cs))) * 1.15, uVhs);
    }
    if (uPrism > 0.0) {
        vec3 ghost = vec3(src(p + vec2(0.06, 0.02)).r, src(p + vec2(-0.045, 0.05)).g, src(p + vec2(0.025, -0.06)).b);
        vec3 ghost2 = src(vec2(1.0 - p.x, p.y) + vec2(0.0, 0.03));
        c = c * (1.0 - 0.3 * uPrism) + (ghost * 0.38 + ghost2 * 0.12 * vec3(1.0, 0.8, 1.2)) * uPrism;
    }
    if (uDouble > 0.0) {
        vec3 c2 = texture(uSrc2, uSrc2Crop.xy + clamp(p, 0.0, 1.0) * uSrc2Crop.zw).rgb;
        c = mix(c, 1.0 - (1.0 - c) * (1.0 - c2), uDouble * 0.85);
    }

    // ---- scene light (linear): exposure, white balance, flash
    vec3 lin = srgbToLinear(c) * exp2(uExposure);
    lin *= vec3(1.0 + 0.18 * uTemp, 1.0 - 0.1 * uTint, 1.0 - 0.18 * uTemp);
    if (uFlash > 0.0) {
        vec2 fc = (p - vec2(0.5, 0.56)) * vec2(uAspect, 1.0);
        float fall = 1.0 / (1.0 + dot(fc, fc) * 7.0);            // on-camera flash falloff
        float near = smoothstep(0.2, 0.7, luma(lin));             // close bright subjects pop
        lin = lin * (1.0 - 0.4 * uFlash) + uFlash * uFlashColor * (lin * 1.25 * fall + 0.03 * fall + near * 0.08 * fall);
    }
    c = clamp(linearToSrgb(lin), 0.0, 1.0);

    // ---- film / sensor response
    if (uLutMix > 0.0) {
        vec3 coord = c * (32.0 / 33.0) + 0.5 / 33.0;
        c = mix(c, texture(uLut, coord).rgb, uLutMix);
    }
    c = mix(uLift, uGain, c);
    float l = luma(c);
    c = mix(c, (c - 0.5) * uContrast + 0.5, smoothstep(0.0, 0.15, l) * (1.0 - smoothstep(0.85, 1.0, l)) * 0.5 + 0.5);
    l = luma(c);
    c = mix(vec3(l), c, uSaturation);
    l = clamp(luma(c), 0.0, 1.0);
    c += uShadowTint * (1.0 - l) * (1.0 - l) * 1.5 + uHighTint * l * l * 1.5;
    c = mix(c, vec3(luma(c)), uMono);
    c = mix(c, c * (1.0 - uFade * 0.25) + uFade * 0.18, 1.0);

    fragColor = vec4(clamp(c, 0.0, 1.0) * fishMask, 1.0);
}
"""

    /** 4x box downsample. */
    const val DOWN = """#version 300 es
$COMMON
in vec2 vUv;
out vec4 fragColor;
uniform sampler2D uSrc;
uniform vec2 uTexel;
void main() {
    vec3 c = texture(uSrc, vUv + uTexel * vec2(-0.25, -0.25)).rgb + texture(uSrc, vUv + uTexel * vec2(0.25, -0.25)).rgb
           + texture(uSrc, vUv + uTexel * vec2(-0.25, 0.25)).rgb + texture(uSrc, vUv + uTexel * vec2(0.25, 0.25)).rgb;
    fragColor = vec4(c * 0.25, 1.0);
}
"""

    /** One direction of a separable Gaussian (paired bilinear taps). */
    const val BLUR = """#version 300 es
$COMMON
in vec2 vUv;
out vec4 fragColor;
uniform sampler2D uSrc;
uniform vec2 uStep;
uniform float uSigma;
void main() {
    vec3 sum = texture(uSrc, vUv).rgb;
    float total = 1.0;
    float k = -0.5 / (uSigma * uSigma);
    int radius = int(ceil(uSigma * 2.5));
    for (int i = 1; i <= 40; i += 2) {
        if (i > radius) break;
        float a = float(i);
        float wa = exp(a * a * k), wb = exp((a + 1.0) * (a + 1.0) * k);
        float w = wa + wb;
        vec2 off = uStep * ((a * wa + (a + 1.0) * wb) / w);
        sum += (texture(uSrc, vUv + off).rgb + texture(uSrc, vUv - off).rgb) * w;
        total += 2.0 * w;
    }
    fragColor = vec4(sum / total, 1.0);
}
"""

    const val COMPOSITE = """#version 300 es
$COMMON
in vec2 vUv;
out vec4 fragColor;
uniform sampler2D uGraded;
uniform sampler2D uBlur;
uniform sampler2D uStamp;     // date stamp, picture space
uniform float uStampOn;
uniform sampler2D uFrameTex;  // frame markings, output space
uniform float uFrameTexOn;
uniform vec2 uRes;            // output px
uniform float uFlipY;
uniform vec4 uImgRect;        // picture rect in output uv (y up)
uniform float uAspect;        // picture aspect
uniform int uBorder;          // see Border ids
uniform vec3 uFrameColor;
uniform float uTime;
uniform uint uSeed;           // per shot / per frame
uniform uint uLeakSeed;
uniform float uBloom, uHalation, uSoft, uSoftAll, uStar;
uniform vec3 uHalationColor;
uniform float uVignette, uLeak, uLeakAnim;
uniform vec3 uLeakColor;
uniform float uGrain, uGrainSize, uNoise;
uniform float uScan, uVhs, uFlicker, uDust, uInterlace;

// rounded-rect signed distance in picture space (units of picture height)
float roundRect(vec2 p, vec2 half_, float r) {
    vec2 q = abs(p) - half_ + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

vec3 frameColorAt(vec2 uv) {
    vec3 b = uFrameColor;
    float n = vnoise(uv * uRes * 0.25, 77u);
    if (uBorder == 1) return b * (0.97 + 0.03 * n);                      // instant paper
    if (uBorder == 3) return b * (0.93 + 0.07 * vnoise(uv * uRes * 0.08, 5u)); // slide card
    return b * (0.85 + 0.15 * n);                                        // film base
}

void main() {
    vec2 uv = vUv;
    if (uFlipY > 0.5) uv.y = 1.0 - uv.y;
    float frame = floor(uTime * 24.0);
    uint fseed = uSeed + uint(frame) * 7919u;

    // overlay markings sit on top of everything (edge print, sprocket text)
    vec4 marks = uFrameTexOn > 0.5 ? texture(uFrameTex, uv) : vec4(0.0);

    vec2 r0 = uImgRect.xy, r1 = uImgRect.zw;
    bool outside = uv.x < r0.x || uv.y < r0.y || uv.x > r1.x || uv.y > r1.y;
    vec2 p = (uv - r0) / (r1 - r0);
    vec2 q = (p - 0.5) * vec2(uAspect, 1.0);

    // frame / gate masks
    float inside = outside ? 0.0 : 1.0;
    if (!outside) {
        if (uBorder == 3) {                                   // slide window
            inside = 1.0 - smoothstep(-0.002, 0.002, roundRect(q, vec2(uAspect, 1.0) * 0.5, 0.04));
        } else if (uBorder == 5) {                            // 8mm gate: rounded corners, soft edge
            float d = roundRect(q, vec2(uAspect, 1.0) * 0.5, 0.09);
            inside = 1.0 - smoothstep(-0.012, 0.004, d);
        } else if (uBorder == 6) {                            // 16mm gate
            float d = roundRect(q, vec2(uAspect, 1.0) * 0.5, 0.03);
            inside = 1.0 - smoothstep(-0.006, 0.002, d);
        } else if (uBorder == 2 || uBorder == 7) {            // black film frame / negative: rough inner edge
            vec2 e = min(p, 1.0 - p) * vec2(uAspect, 1.0);
            float edge = min(e.x, e.y);
            float rough = (vnoise(p * vec2(140.0, 140.0 / uAspect), 9u) - 0.5) * 0.006;
            inside = smoothstep(0.0, 0.004, edge + rough);
        } else if (uBorder == 4) {                            // exposed over the sprocket holes
            float band = 0.13;
            float yy = p.y < band ? p.y / band : p.y > 1.0 - band ? (1.0 - p.y) / band : -1.0;
            if (yy >= 0.0) {
                float hx = fract(p.x * uAspect / 0.105) - 0.5;            // hole pitch
                vec2 hq = vec2(hx * 0.105, (yy - 0.5) * band);
                float hole = 1.0 - smoothstep(-0.002, 0.002, roundRect(hq, vec2(0.028, band * 0.32), 0.008));
                inside *= 1.0 - hole * 0.97;
            }
            vec2 e = min(p, 1.0 - p) * vec2(uAspect, 1.0);
            inside *= smoothstep(0.0, 0.003, min(e.x, e.y));
        } else if (uBorder == 8) {                            // toy camera: uneven cut-off edges
            vec2 e = min(p, 1.0 - p) * vec2(uAspect, 1.0);
            float edge = min(e.x, e.y);
            float rough = (fbm(p * 9.0, uLeakSeed) - 0.5) * 0.05;
            inside = smoothstep(0.0, 0.03, edge + rough - 0.006);
        }
    }

    vec3 c = texture(uGraded, p).rgb;
    vec3 B = texture(uBlur, p).rgb;
    float r2 = dot(q, q) / (0.25 * (uAspect * uAspect + 1.0));   // 0 centre .. 1 corner

    // lens softness (corners) and overall diffusion
    c = mix(c, B, clamp(uSoft * smoothstep(0.15, 1.0, r2) + uSoftAll * 0.6, 0.0, 0.95));
    // glow (bloom) and halation from the blurred highlights
    float bl = luma(B);
    vec3 glow = B * smoothstep(0.45, 1.0, bl);
    c = 1.0 - (1.0 - c) * (1.0 - clamp(glow * uBloom * 1.4, 0.0, 1.0));
    c += uHalationColor * max(bl - luma(c) * 0.55 - 0.18, 0.0) * uHalation * 2.2;
    if (uStar > 0.0) {
        vec3 s = vec3(0.0);
        for (int d = 0; d < 4; d++) {
            float a = float(d) * 0.785398 + 0.3927;
            vec2 dir = vec2(cos(a) / uAspect, sin(a));
            for (int k = 1; k <= 8; k++) {
                float t = float(k) * 0.012;
                float w = pow(1.0 - float(k) / 9.0, 2.0);
                s += (max(texture(uGraded, p + dir * t).rgb - 0.82, 0.0) + max(texture(uGraded, p - dir * t).rgb - 0.82, 0.0)) * w;
            }
        }
        c += s * uStar * 0.55;
    }

    // vignette (natural falloff)
    c *= 1.0 - uVignette * smoothstep(0.2, 1.1, r2) * 0.9;

    // light leaks: soft, organic glow bleeding in from a film edge
    if (uLeak > 0.0) {
        float t = uTime * uLeakAnim;
        vec3 h = rand3(ivec2(3, 9), uLeakSeed);
        int side = int(h.x * 4.0);
        float along = side < 2 ? p.y : p.x;
        float across = side == 0 ? p.x : side == 1 ? 1.0 - p.x : side == 2 ? p.y : 1.0 - p.y;
        float centre = 0.2 + h.y * 0.6 + sin(t * 0.7) * 0.1;
        float warp = fbm(vec2(along * 2.5 + t * 0.2, across * 1.5), uLeakSeed) - 0.5;
        float reach = 0.18 + h.z * 0.3;
        float shape = exp(-pow(max(across + warp * 0.25, 0.0) / reach, 1.6) * 2.0)
                    * exp(-pow((along - centre) / (0.35 + h.z * 0.3), 2.0) * 1.5);
        shape += 0.35 * exp(-pow(across / 0.05, 2.0)) * smoothstep(0.3, 0.7, fbm(vec2(along * 6.0, 1.0), uLeakSeed + 5u));
        vec3 lc = mix(uLeakColor, vec3(1.0, 0.82, 0.45), smoothstep(0.55, 1.0, shape));
        lc = mix(lc, vec3(1.0, 0.15, 0.08), smoothstep(0.4, 0.0, shape) * 0.5);
        c = 1.0 - (1.0 - clamp(c, 0.0, 1.0)) * (1.0 - clamp(lc * shape * uLeak * 1.3, 0.0, 1.0));
    }

    // projector / cine flicker
    if (uFlicker > 0.0) c *= 1.0 + (rand1(frame, uSeed + 13u) - 0.5) * 0.1 * uFlicker;

    // dust, hairs and scratches (video bodies)
    if (uDust > 0.0) {
        vec2 g = p * vec2(80.0 * uAspect, 80.0);
        ivec2 id = ivec2(floor(g));
        vec3 h = rand3(id, fseed + 99u);
        if (h.x > 0.9975) {
            float d = length(fract(g) - h.yz);
            c = mix(c, vec3(h.y > 0.5 ? 0.92 : 0.05), smoothstep(0.18, 0.02, d) * uDust);
        }
        vec3 hs = rand3(ivec2(int(frame), 1), uSeed + 5u);
        if (hs.x > 0.55) c = mix(c, vec3(0.9), smoothstep(0.0012, 0.0, abs(p.x - hs.y - sin(p.y * 6.0) * 0.003)) * uDust * 0.5);
        vec3 hh = rand3(ivec2(int(frame), 2), uSeed + 7u);
        if (hh.x > 0.85) {
            vec2 hc = hh.yz;
            float hair = abs(length((p - hc) * vec2(uAspect, 1.0)) - 0.04 - 0.02 * sin(atan(p.y - hc.y, p.x - hc.x) * 3.0));
            c = mix(c, vec3(0.04), smoothstep(0.0015, 0.0, hair) * uDust * 0.8);
        }
    }

    // video signal artefacts
    if (uScan > 0.0) c *= 1.0 - uScan * 0.1 * (0.5 + 0.5 * sin(p.y * 960.0 * 3.14159));
    if (uInterlace > 0.0) c *= 1.0 - uInterlace * 0.05 * step(0.5, fract(p.y * 240.0));
    if (uVhs > 0.0) {
        float band = smoothstep(0.045, 0.0, p.y);
        vec3 nz = rand3(ivec2(int(p.x * 400.0), int(p.y * 300.0)) + ivec2(0, int(frame) * 300), fseed);
        c = mix(c, vec3(nz.x), band * 0.75 * uVhs);
        float tl = rand1(frame * 0.25, uSeed + 21u);
        float ty = fract(tl * 7.3 + uTime * 0.05);
        c += vec3(0.25) * smoothstep(0.004, 0.0, abs(p.y - ty)) * step(0.7, tl) * uVhs * nz.y;
    }

    // grain: two rotated octaves of gaussian-ish value noise, strongest in the midtones
    if (uGrain > 0.0) {
        float scale = 1100.0 / uGrainSize;
        vec2 gp = p * vec2(uAspect, 1.0) * scale;
        mat2 ra = mat2(0.866, 0.5, -0.5, 0.866);
        mat2 rb = mat2(0.342, -0.94, 0.94, 0.342);
        float g1 = vnoise(ra * gp, fseed) + vnoise(ra * gp + 37.0, fseed + 1u) - 1.0;
        float g2 = vnoise(rb * gp * 0.45 + 17.0, fseed + 2u) + vnoise(rb * gp * 0.45 + 51.0, fseed + 3u) - 1.0;
        float g = g1 * 0.8 + g2 * 0.5;
        vec3 resp = pow(clamp(4.0 * c * (1.0 - c), 0.0, 1.0), vec3(0.7));
        float lc = luma(c);
        float chroma = clamp(length(c - lc) * 4.0, 0.0, 1.0);
        float dye = vnoise(ra * gp * 0.8 + 91.0, fseed + 4u) - 0.5;
        c += (g + vec3(dye, -dye * 0.5, -dye) * 0.3 * chroma) * uGrain * 0.2 * resp;
    }
    // digital sensor noise: fine, mostly chroma, stronger in shadows
    if (uNoise > 0.0) {
        vec3 n = rand3(ivec2(uv * uRes), fseed + 11u) - 0.5;
        c += (n * vec3(1.0, 0.7, 1.2) + (n.x - 0.0) * 0.5) * uNoise * 0.08 * (1.0 - luma(c) * 0.7);
    }

    if (uStampOn > 0.5) {
        vec4 s = texture(uStamp, p);
        c = c * (1.0 - s.a) + s.rgb;
    }

    c = clamp(c, 0.0, 1.0);
    vec3 outc = mix(frameColorAt(uv), c, inside);
    outc = outc * (1.0 - marks.a) + marks.rgb;
    fragColor = vec4(outc, 1.0);
}
"""
}
