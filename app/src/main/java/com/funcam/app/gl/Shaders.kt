package com.funcam.app.gl

object Shaders {
    const val VERTEX = """#version 300 es
layout(location = 0) in vec2 aPos;
out vec2 vUv;
void main() {
    vUv = aPos * 0.5 + 0.5;
    gl_Position = vec4(aPos, 0.0, 1.0);
}
"""

    /**
     * Camera input pass: rotates/mirrors the external camera texture upright and blends it into
     * an accumulation buffer. uAlpha < 1 simulates a shutter slower than the frame interval.
     */
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

    /** Plain copy, used for showing snapshots and compositing. */
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

    /** The film look. Everything a camera body does to light lives here. */
    const val FILM = """#version 300 es
precision highp float;
in vec2 vUv;
out vec4 fragColor;

uniform sampler2D uTex;
uniform sampler2D uTex2;
uniform float uDouble;
uniform sampler2D uStamp;
uniform float uStampOn;
uniform vec2 uRes;
uniform float uTime;
uniform float uSeed;
uniform float uFlipY;
uniform vec4 uImgRect;
uniform float uAspect;
uniform vec3 uBorderColor;
uniform int uBorder;
uniform vec4 uSrcCrop;
uniform vec4 uSrc2Crop;

uniform float uExposure, uContrast, uSaturation, uTemp, uTint, uFade, uGamma;
uniform vec3 uLift, uGain, uShadowTint, uHighTint;
uniform float uMono, uCross, uHalation, uBloom, uGrain, uGrainSize, uVignette, uLeak;
uniform vec3 uHalationColor, uLeakColor;
uniform float uChroma, uSoft, uSharpen, uPixel, uScan, uVhs, uWeave, uFlicker, uDust;
uniform float uFisheye, uPrism, uStar, uFlash;
uniform vec3 uFlashColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}
float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}
float luma(vec3 c) { return dot(c, vec3(0.299, 0.587, 0.114)); }

vec3 src(vec2 p) {
    p = clamp(p, 0.0, 1.0);
    return texture(uTex, uSrcCrop.xy + p * uSrcCrop.zw).rgb;
}

vec3 borderColor(vec2 uv) {
    vec3 b = uBorderColor;
    if (uBorder == 1) {
        b *= 0.965 + 0.035 * vnoise(uv * uRes * 0.35);
    } else if (uBorder == 2) {
        // film rebate: dark base with sprocket-like windows in the long margins
        float hu = uv.x * uRes.x / uRes.y;
        bool top = uv.y > uImgRect.w;
        bool bottom = uv.y < uImgRect.y;
        if (top || bottom) {
            float m0 = top ? uImgRect.w : 0.0;
            float m1 = top ? 1.0 : uImgRect.y;
            float my = (uv.y - m0) / max(m1 - m0, 1e-4);
            float cell = fract(hu / 0.075);
            float hole = step(0.3, cell) * step(cell, 0.7) * step(0.32, my) * step(my, 0.68);
            b = mix(b, vec3(0.78, 0.72, 0.6), hole * 0.85);
        }
        b *= 0.9 + 0.1 * vnoise(uv * uRes * 0.2);
    } else if (uBorder == 3) {
        b *= 0.94 + 0.06 * vnoise(uv * uRes * 0.15);
    }
    return b;
}

void main() {
    vec2 uv = vUv;
    if (uFlipY > 0.5) uv.y = 1.0 - uv.y;
    vec2 r0 = uImgRect.xy;
    vec2 r1 = uImgRect.zw;
    if (uv.x < r0.x || uv.y < r0.y || uv.x > r1.x || uv.y > r1.y) {
        fragColor = vec4(borderColor(uv), 1.0);
        return;
    }
    vec2 p = (uv - r0) / (r1 - r0);

    if (uBorder == 3) {
        // rounded slide-mount window
        vec2 q = abs(p - 0.5) * vec2(uAspect, 1.0);
        vec2 hs = vec2(uAspect, 1.0) * 0.5 - 0.05;
        float d = length(max(q - hs, 0.0));
        if (d > 0.05) { fragColor = vec4(borderColor(uv), 1.0); return; }
    }

    vec2 p0 = p;
    float frame = floor(uTime * 24.0);

    // gate weave (projector / cine)
    if (uWeave > 0.0) {
        p += uWeave * vec2(vnoise(vec2(uTime * 3.1, uSeed)) - 0.5,
                           vnoise(vec2(uSeed, uTime * 2.3)) - 0.5) * vec2(0.006, 0.008);
    }
    // VHS line jitter and bottom tracking band
    if (uVhs > 0.0) {
        float line = floor(p.y * 240.0);
        float j = (hash(vec2(line, frame)) - 0.5) * 0.003;
        float band = smoothstep(0.07, 0.0, p.y);
        j += band * (hash(vec2(line * 1.7, frame)) - 0.5) * 0.05;
        p.x += j * uVhs;
    }
    // fisheye: magnify centre, circular image
    float fishMask = 1.0;
    if (uFisheye > 0.0) {
        vec2 c = (p - 0.5) * vec2(uAspect, 1.0);
        float rr = length(c);
        float lim = 0.5 * min(uAspect, 1.0);
        fishMask = smoothstep(lim, lim - 0.006, rr);
        float k = 0.9 * uFisheye;
        vec2 s = c * (1.0 - k * 0.5 + k * rr * rr * 2.0);
        p = s / vec2(uAspect, 1.0) + 0.5;
    }
    // low-res digicam
    if (uPixel > 0.0) {
        float cells = mix(900.0, 200.0, uPixel);
        vec2 n = vec2(cells, cells / uAspect);
        p = (floor(p * n) + 0.5) / n;
    }

    vec2 px = vec2(0.001, 0.001 * uAspect);
    vec3 c;
    if (uChroma > 0.0) {
        vec2 d = (p - 0.5) * uChroma * 0.012;
        c = vec3(src(p + d).r, src(p).g, src(p - d).b);
    } else {
        c = src(p);
    }

    if (uSoft > 0.0 || uSharpen > 0.0) {
        float r = 1.5 + uSoft * 3.0;
        vec3 b = (src(p + px * vec2(r, 0.0)) + src(p - px * vec2(r, 0.0)) +
                  src(p + px * vec2(0.0, r)) + src(p - px * vec2(0.0, r)) +
                  src(p + px * r * 0.7) + src(p - px * r * 0.7) +
                  src(p + px * vec2(r, -r) * 0.7) + src(p + px * vec2(-r, r) * 0.7)) * 0.125;
        c = mix(c, b, clamp(uSoft, 0.0, 1.0) * 0.85);
        c += (c - b) * uSharpen * 1.5;
    }

    if (uVhs > 0.0) {
        vec3 cs = (src(p + vec2(0.004, 0.0)) + src(p + vec2(0.009, 0.0)) + src(p + vec2(0.014, 0.0))) / 3.0;
        vec3 bled = vec3(luma(c)) + (cs - vec3(luma(cs))) * 1.1;
        c = mix(c, bled, uVhs);
    }

    if (uBloom > 0.0 || uHalation > 0.0) {
        vec3 glow = vec3(0.0);
        float rad = 0.014;
        for (int i = 0; i < 8; i++) {
            float a = float(i) * 0.785398 + 0.3;
            vec2 o = vec2(cos(a), sin(a) * uAspect) * rad;
            glow += max(src(p + o) - 0.62, 0.0) + max(src(p + o * 2.2) - 0.62, 0.0) * 0.6;
        }
        glow /= 8.0;
        c += glow * uBloom * 1.6;
        c += luma(glow) * uHalationColor * uHalation * 2.2;
    }

    if (uPrism > 0.0) {
        vec3 ghost = vec3(src(p + vec2(0.05, 0.02)).r, src(p + vec2(-0.035, 0.04)).g, src(p + vec2(0.02, -0.05)).b);
        c = c * (1.0 - 0.25 * uPrism) + ghost * 0.45 * uPrism;
    }

    if (uStar > 0.0) {
        vec3 s = vec3(0.0);
        for (int d = 0; d < 4; d++) {
            float a = float(d) * 0.785398 + 0.392699;
            vec2 dir = vec2(cos(a), sin(a) * uAspect);
            for (int k = 1; k <= 7; k++) {
                float t = float(k) * 0.009;
                float w = 1.0 - float(k) / 8.0;
                s += (max(src(p + dir * t) - 0.82, 0.0) + max(src(p - dir * t) - 0.82, 0.0)) * w;
            }
        }
        c += s * uStar * 0.9;
    }

    if (uDouble > 0.0) {
        vec3 c2 = texture(uTex2, uSrc2Crop.xy + clamp(p, 0.0, 1.0) * uSrc2Crop.zw).rgb;
        vec3 scr = 1.0 - (1.0 - c) * (1.0 - c2);
        c = mix(c, mix((c + c2) * 0.5, scr, 0.5), uDouble);
    }

    // ---- colour ----
    c *= exp2(uExposure);
    c *= vec3(1.0 + 0.12 * uTemp, 1.0 - 0.06 * uTint, 1.0 - 0.14 * uTemp);

    if (uFlash > 0.0) {
        vec2 fc = (p - vec2(0.5, 0.55)) * vec2(uAspect, 1.0);
        float lit = exp(-dot(fc, fc) * 4.5);
        c = c * (1.0 - 0.35 * uFlash) + lit * uFlash * (c * 0.9 + 0.12) * uFlashColor;
    }

    c = mix(uLift, uGain, c);
    c = (c - 0.5) * uContrast + 0.5;
    if (uCross > 0.0) {
        vec3 cc = clamp(c, 0.0, 1.0);
        vec3 x = vec3(smoothstep(0.05, 0.95, cc.r), pow(cc.g, 0.85), cc.b * 0.75 + 0.15);
        c = mix(c, x, uCross);
    }
    float l = luma(c);
    c = mix(vec3(l), c, uSaturation);
    l = clamp(luma(c), 0.0, 1.0);
    c += uShadowTint * (1.0 - l) * (1.0 - l) * 2.0 + uHighTint * l * l * 2.0;
    c = mix(c, vec3(luma(c)), uMono);
    c = mix(vec3(uFade), vec3(1.0 - uFade * 0.35), clamp(c, 0.0, 1.0));
    c = pow(max(c, 0.0), vec3(1.0 / uGamma));

    // vignette
    vec2 vq = (p - 0.5) * 1.414;
    c *= 1.0 - uVignette * smoothstep(0.25, 1.05, dot(vq, vq));

    // light leaks
    if (uLeak > 0.0) {
        float s = uSeed;
        vec2 lp = vec2(hash(vec2(s, 1.3)) > 0.5 ? -0.05 : 1.05, hash(vec2(s, 2.7)));
        float d = distance(p * vec2(uAspect, 1.0), lp * vec2(uAspect, 1.0));
        float shape = exp(-d * d * (3.0 + hash(vec2(s, 3.1)) * 5.0));
        shape *= 0.65 + 0.35 * vnoise(p * 3.0 + s * 10.0);
        vec2 lp2 = vec2(hash(vec2(s, 4.4)), hash(vec2(s, 5.5)) > 0.5 ? -0.05 : 1.05);
        float d2 = distance(p, lp2);
        shape += 0.5 * exp(-d2 * d2 * 9.0) * step(0.55, hash(vec2(s, 6.6)));
        vec3 lc = mix(uLeakColor, vec3(1.0, 0.85, 0.45), clamp(shape - 0.3, 0.0, 1.0));
        c = 1.0 - (1.0 - clamp(c, 0.0, 1.0)) * (1.0 - lc * clamp(shape * uLeak, 0.0, 1.0));
    }

    // projector flicker
    if (uFlicker > 0.0) c *= 1.0 + (hash(vec2(frame, 5.0)) - 0.5) * 0.12 * uFlicker;

    // dust and scratches
    if (uDust > 0.0) {
        vec2 g = p * vec2(70.0, 70.0 / uAspect);
        vec2 id = floor(g);
        float h = hash(id + frame * 1.37);
        if (h > 0.996) {
            float dd = length(fract(g) - 0.5);
            c = mix(c, vec3(0.06), smoothstep(0.35, 0.05, dd) * uDust);
        }
        if (hash(vec2(frame, 7.0)) > 0.6) {
            float x = hash(vec2(frame, 3.0));
            c = mix(c, vec3(0.92), smoothstep(0.0018, 0.0, abs(p.x - x)) * uDust * 0.5);
        }
    }

    // scanlines
    if (uScan > 0.0) c *= 1.0 - uScan * 0.16 * (0.5 + 0.5 * sin(p.y * 480.0 * 3.14159));
    if (uVhs > 0.0) {
        float band = smoothstep(0.06, 0.0, p.y);
        float n = hash(vec2(floor(p.x * 300.0), floor(p.y * 200.0) + frame));
        c = mix(c, vec3(n), band * 0.6 * uVhs);
    }

    // grain (image-relative so preview matches the saved photo)
    if (uGrain > 0.0) {
        float gs = 1400.0 / uGrainSize;
        vec2 gp = p * vec2(gs, gs / uAspect) + uTime * vec2(37.0, 91.0) + uSeed * 13.0;
        float g = vnoise(gp) + vnoise(gp * 1.9 + 7.0) - 1.0;
        float lg = clamp(luma(c), 0.0, 1.0);
        float w = 0.45 + 0.55 * (1.0 - pow(abs(lg - 0.5) * 2.0, 2.0));
        float cg = (vnoise(gp * 1.3 + 21.0) - 0.5) * (1.0 - uMono);
        c += (g + vec3(cg, -cg * 0.5, cg * 0.3) * 0.4) * uGrain * 0.22 * w;
    }

    c *= fishMask;

    if (uStampOn > 0.5) {
        vec4 s = texture(uStamp, p0);
        c = c * (1.0 - s.a) + s.rgb;
    }

    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
"""
}
