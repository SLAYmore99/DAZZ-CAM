// WebGL2 port of the Android renderer (FilmRenderer + StillProcessor + FilmProgram + FrameSpec).
import { SHADERS } from './data.js';

const BORDER_ID = { NONE: 0, INSTANT: 1, FILM: 2, SLIDE: 3, SPROCKET: 4 };
const BORDER_COLOR = {
  NONE: [0, 0, 0], INSTANT: [0.95, 0.94, 0.91], FILM: [0.07, 0.06, 0.055],
  SPROCKET: [0.07, 0.06, 0.055], SLIDE: [0.93, 0.91, 0.86],
};
const LOOK_DEFAULTS = {
  exposure: 0, contrast: 1, saturation: 1, temp: 0, tint: 0, fade: 0, gamma: 1, lift: [0, 0, 0], gain: [1, 1, 1],
  shadowTint: [0, 0, 0], highTint: [0, 0, 0], mono: 0, cross: 0, halation: 0, halationColor: [1, 0.25, 0.1], bloom: 0,
  grain: 0, grainSize: 1, vignette: 0, leak: 0, leakColor: [1, 0.35, 0.1], chroma: 0, soft: 0, sharpen: 0, pixel: 0,
  scan: 0, vhs: 0, weave: 0, flicker: 0, dust: 0, flash: 0.7,
};

export function frameSpec(border, imgAspect, borderOn) {
  const b = borderOn ? border : 'NONE';
  if (b === 'SLIDE') {
    const winW = 1, winH = 1 / imgAspect, size = Math.max(winW, winH) * 1.36;
    const x0 = (size - winW) / 2 / size, y0 = (size - winH) / 2 / size;
    return { outAspect: 1, rect: [x0, y0, 1 - x0, 1 - y0], imgAspect, border: b };
  }
  const [side, top, bottom] = {
    NONE: [0, 0, 0], INSTANT: [0.06, 0.07, 0.26], FILM: [0.05, 0.05, 0.05], SPROCKET: [0.03, 0.12, 0.12],
  }[b];
  const w = 1 + 2 * side, h = 1 / imgAspect + top + bottom;
  return { outAspect: w / h, rect: [side / w, bottom / h, (side + 1) / w, (bottom + 1 / imgAspect) / h], imgAspect, border: b };
}

/** Centre crop from aspect src to dst, then digital zoom. Returns [offX, offY, scaleX, scaleY]. */
export function crop(src, dst, zoom = 1) {
  let c = dst < src ? [(1 - dst / src) / 2, 0, dst / src, 1] : [0, (1 - src / dst) / 2, 1, src / dst];
  if (zoom > 1) {
    const sw = c[2] / zoom, sh = c[3] / zoom;
    c = [c[0] + (c[2] - sw) / 2, c[1] + (c[3] - sh) / 2, sw, sh];
  }
  return c;
}

// ------------------------------------------------------------------ date stamps (port of StampRenderer)
const pad = n => String(n).padStart(2, '0');
const MON = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];

export function stampKey(style, t) {
  const d = new Date(t);
  if (style === 'CAMCORDER' || style === 'VHS') return style + d.toISOString().slice(0, 19);
  if (style === 'NONE' || style === 'PROJECTOR') return '';
  return style + d.toDateString();
}

export function renderStamp(style, w, h, t) {
  if (style === 'NONE' || style === 'PROJECTOR') return null;
  const c = document.createElement('canvas');
  c.width = w; c.height = h;
  const g = c.getContext('2d');
  const d = new Date(t);
  const unit = Math.min(w, h);
  const outlined = (text, x, y, size, font) => {
    g.font = `bold ${size}px ${font}`;
    g.lineWidth = size * 0.12; g.strokeStyle = 'rgba(0,0,0,0.8)'; g.strokeText(text, x, y);
    g.fillStyle = 'rgb(245,245,240)'; g.fillText(text, x, y);
  };
  if (style === 'FILM_ORANGE' || style === 'DIGI_ORANGE') {
    const text = style === 'FILM_ORANGE'
      ? `'${String(d.getFullYear()).slice(2)}  ${d.getMonth() + 1}  ${d.getDate()}`
      : `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())}`;
    const size = unit * 0.046;
    g.font = `bold ${size}px "Courier New", monospace`;
    g.setTransform(1, 0, -0.12, 1, 0, 0);
    const tw = g.measureText(text).width;
    const x = w - tw - unit * 0.07 + (h - unit * 0.07) * 0.12, y = h - unit * 0.07;
    g.shadowColor = 'rgba(255,90,0,0.86)'; g.shadowBlur = size * 0.45;
    g.fillStyle = 'rgb(255,156,52)'; g.fillText(text, x, y);
    g.shadowBlur = size * 0.12; g.shadowColor = 'rgb(255,120,20)';
    g.fillStyle = 'rgba(255,196,120,0.6)'; g.fillText(text, x, y);
  } else if (style === 'CAMCORDER') {
    const size = unit * 0.055;
    const h12 = d.getHours() % 12 || 12;
    outlined(`${MON[d.getMonth()]}. ${d.getDate()} ${d.getFullYear()}`, unit * 0.07, h - unit * 0.07 - size * 1.25, size, 'sans-serif');
    outlined(`${h12}:${pad(d.getMinutes())}:${pad(d.getSeconds())} ${d.getHours() < 12 ? 'AM' : 'PM'}`, unit * 0.07, h - unit * 0.07, size, 'sans-serif');
  } else if (style === 'VHS') {
    const size = unit * 0.062, f = '"Courier New", monospace';
    outlined('PLAY ▶', unit * 0.07, unit * 0.07 + size, size, f);
    outlined('SP', w - unit * 0.07 - size * 1.4, unit * 0.07 + size, size, f);
    const h12 = d.getHours() % 12 || 12;
    outlined(`${pad(h12)}:${pad(d.getMinutes())} ${d.getHours() < 12 ? 'AM' : 'PM'}`, unit * 0.07, h - unit * 0.07 - size * 1.3, size, f);
    outlined(`${MON[d.getMonth()]}.${pad(d.getDate())} ${d.getFullYear()}`, unit * 0.07, h - unit * 0.07, size, f);
  }
  return c;
}

// ------------------------------------------------------------------ GL helpers
function compile(gl, type, src) {
  const s = gl.createShader(type);
  gl.shaderSource(s, src); gl.compileShader(s);
  if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) throw new Error(gl.getShaderInfoLog(s));
  return s;
}
function program(gl, vs, fs) {
  const p = gl.createProgram();
  gl.attachShader(p, compile(gl, gl.VERTEX_SHADER, vs));
  gl.attachShader(p, compile(gl, gl.FRAGMENT_SHADER, fs));
  gl.bindAttribLocation(p, 0, 'aPos');
  gl.linkProgram(p);
  if (!gl.getProgramParameter(p, gl.LINK_STATUS)) throw new Error(gl.getProgramInfoLog(p));
  const locs = {};
  return { p, u: n => (n in locs ? locs[n] : (locs[n] = gl.getUniformLocation(p, n))) };
}

class Fbo {
  constructor(gl, w, h, half) {
    this.gl = gl; this.width = w; this.height = h;
    this.tex = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D, this.tex);
    for (const [k, v] of [[gl.TEXTURE_MIN_FILTER, gl.LINEAR], [gl.TEXTURE_MAG_FILTER, gl.LINEAR],
      [gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE], [gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE]]) gl.texParameteri(gl.TEXTURE_2D, k, v);
    if (half) gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA16F, w, h, 0, gl.RGBA, gl.HALF_FLOAT, null);
    else gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, w, h, 0, gl.RGBA, gl.UNSIGNED_BYTE, null);
    this.fbo = gl.createFramebuffer();
    gl.bindFramebuffer(gl.FRAMEBUFFER, this.fbo);
    gl.framebufferTexture2D(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0, gl.TEXTURE_2D, this.tex, 0);
    this.ok = gl.checkFramebufferStatus(gl.FRAMEBUFFER) === gl.FRAMEBUFFER_COMPLETE;
    gl.bindFramebuffer(gl.FRAMEBUFFER, null);
  }
  bind() { this.gl.bindFramebuffer(this.gl.FRAMEBUFFER, this.fbo); this.gl.viewport(0, 0, this.width, this.height); }
  release() { this.gl.deleteFramebuffer(this.fbo); this.gl.deleteTexture(this.tex); }
}

/**
 * Live renderer: video element -> accumulation (slow shutter) -> film look -> canvas.
 * Also develops stills offscreen with exactly the same shader.
 */
export class FilmGL {
  constructor(canvas) {
    const gl = canvas.getContext('webgl2', { alpha: false, antialias: false, premultipliedAlpha: false, preserveDrawingBuffer: true });
    if (!gl) throw new Error('WebGL2 is not available on this browser');
    this.gl = gl; this.canvas = canvas;
    this.film = program(gl, SHADERS.VERTEX, SHADERS.FILM);
    this.input = program(gl, SHADERS.VERTEX, SHADERS.INPUT);
    this.copy = program(gl, SHADERS.VERTEX, SHADERS.COPY);
    this.half = !!gl.getExtension('EXT_color_buffer_float') || !!gl.getExtension('EXT_color_buffer_half_float');
    this.maxTex = gl.getParameter(gl.MAX_TEXTURE_SIZE);
    const buf = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, buf);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 1, -1, -1, 1, 1, 1]), gl.STATIC_DRAW);
    this.vao = gl.createVertexArray();
    gl.bindVertexArray(this.vao);
    gl.enableVertexAttribArray(0);
    gl.vertexAttribPointer(0, 2, gl.FLOAT, false, 0, 0);
    this.blank = this.texture();
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, 1, 1, 0, gl.RGBA, gl.UNSIGNED_BYTE, new Uint8Array(4));
    this.camTex = this.texture();
    this.acc = [null, null]; this.cur = 0; this.accFresh = true;
    this.stampTex = null; this.stampKeyStr = '';
    this.doubleTex = null; this.doubleAspect = 1;
    this.start = performance.now(); this.lastFrame = 0; this.frameDt = 1 / 30;
    this.shutter = 0; this.mirror = false; this.zoom = 1;
    this.params = null; this.imgAspect = 0.75; this.seed = Math.random() * 100;
  }

  texture() {
    const gl = this.gl, t = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D, t);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    return t;
  }

  /** Upload an image/canvas/video so that v = 1 is the top row (same convention as Android). */
  upload(src, tex) {
    const gl = this.gl;
    const t = tex || this.texture();
    gl.bindTexture(gl.TEXTURE_2D, t);
    gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, true);
    gl.pixelStorei(gl.UNPACK_PREMULTIPLY_ALPHA_WEBGL, true);
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, src);
    gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, false);
    gl.pixelStorei(gl.UNPACK_PREMULTIPLY_ALPHA_WEBGL, false);
    return t;
  }

  quad() { this.gl.bindVertexArray(this.vao); this.gl.drawArrays(this.gl.TRIANGLE_STRIP, 0, 4); }

  get cameraAspect() { return this.camW ? this.camW / this.camH : 0.75; }

  resetAccumulation() { this.accFresh = true; }

  setDouble(img) {
    if (!img) { if (this.doubleTex) this.gl.deleteTexture(this.doubleTex); this.doubleTex = null; return; }
    this.doubleTex = this.upload(img, this.doubleTex);
    this.doubleAspect = img.width / img.height;
  }

  /** Called for each new video frame. */
  frame(video) {
    const gl = this.gl;
    const w = video.videoWidth, h = video.videoHeight;
    if (!w || !h) return;
    const now = performance.now();
    if (this.lastFrame) this.frameDt = this.frameDt * 0.9 + Math.min(Math.max((now - this.lastFrame) / 1000, 0.005), 0.5) * 0.1;
    this.lastFrame = now;
    // cap the working resolution for speed; stills are taken from the full video frame
    const s = Math.min(1, 1440 / Math.max(w, h));
    const aw = Math.round(w * s), ah = Math.round(h * s);
    if (!this.acc[0] || this.acc[0].width !== aw || this.acc[0].height !== ah) {
      this.acc.forEach(f => f && f.release());
      this.acc = [new Fbo(gl, aw, ah, this.half), new Fbo(gl, aw, ah, this.half)];
      if (!this.acc[0].ok) { this.half = false; this.acc.forEach(f => f.release()); this.acc = [new Fbo(gl, aw, ah, false), new Fbo(gl, aw, ah, false)]; }
      this.accFresh = true;
    }
    this.camW = w; this.camH = h;
    this.upload(video, this.camTex);
    const dst = this.acc[1 - this.cur], prev = this.acc[this.cur];
    dst.bind();
    const p = this.input;
    gl.useProgram(p.p);
    gl.activeTexture(gl.TEXTURE0); gl.bindTexture(gl.TEXTURE_2D, this.camTex); gl.uniform1i(p.u('uCam'), 0);
    gl.activeTexture(gl.TEXTURE1); gl.bindTexture(gl.TEXTURE_2D, this.accFresh ? this.blank : prev.tex); gl.uniform1i(p.u('uPrev'), 1);
    gl.uniformMatrix4fv(p.u('uSt'), false, [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]);
    gl.uniform1i(p.u('uRot'), 0);
    gl.uniform1f(p.u('uMirror'), this.mirror ? 1 : 0);
    const alpha = (this.accFresh || this.shutter <= this.frameDt) ? 1 : Math.min(Math.max(this.frameDt / this.shutter, 0.02), 1);
    gl.uniform1f(p.u('uAlpha'), alpha);
    this.quad();
    this.cur = 1 - this.cur; this.accFresh = false;

    if (this.params) {
      gl.bindFramebuffer(gl.FRAMEBUFFER, null);
      gl.viewport(0, 0, this.canvas.width, this.canvas.height);
      const t = (now - this.start) / 1000;
      const seed = this.params.recipe.isVideo ? this.seed + Math.floor(t / 3.5) : this.seed;
      this.drawFilm(dst.tex, this.cameraAspect, this.params, this.imgAspect, this.canvas.width, this.canvas.height, t, seed, false,
        this.liveStamp(this.params, this.imgAspect), this.doubleTex, this.doubleAspect, this.zoom);
    }
  }

  liveStamp(params, imgAspect) {
    const now = Date.now();
    const style = params.stampOn ? params.recipe.stamp : 'NONE';
    const k = stampKey(style, now) + imgAspect;
    if (k !== this.stampKeyStr) {
      this.stampKeyStr = k;
      const c = renderStamp(style, 1080, Math.round(1080 / imgAspect), now);
      if (!c) { if (this.stampTex) this.gl.deleteTexture(this.stampTex); this.stampTex = null; }
      else this.stampTex = this.upload(c, this.stampTex);
    }
    return this.stampTex;
  }

  drawFilm(src, srcAspect, P, imgAspect, w, h, time, seed, flipY, stampTex, tex2, tex2Aspect, zoom = 1) {
    const gl = this.gl, f = this.film, u = f.u;
    const L = Object.assign({}, LOOK_DEFAULTS, P.recipe.look);
    const a = P.acc;
    const spec = frameSpec(P.recipe.border, imgAspect, P.borderOn);
    const c = crop(srcAspect, imgAspect, zoom);
    gl.useProgram(f.p);
    gl.activeTexture(gl.TEXTURE0); gl.bindTexture(gl.TEXTURE_2D, src); gl.uniform1i(u('uTex'), 0);
    gl.activeTexture(gl.TEXTURE1); gl.bindTexture(gl.TEXTURE_2D, tex2 || this.blank); gl.uniform1i(u('uTex2'), 1);
    gl.activeTexture(gl.TEXTURE2); gl.bindTexture(gl.TEXTURE_2D, stampTex || this.blank); gl.uniform1i(u('uStamp'), 2);
    const f1 = (n, v) => gl.uniform1f(u(n), v), v3 = (n, v) => gl.uniform3f(u(n), v[0], v[1], v[2]);
    f1('uDouble', tex2 ? 1 : 0);
    const c2 = tex2 ? crop(tex2Aspect, imgAspect) : [0, 0, 1, 1];
    gl.uniform4f(u('uSrc2Crop'), ...c2);
    f1('uStampOn', stampTex ? 1 : 0);
    gl.uniform2f(u('uRes'), w, h);
    f1('uTime', time); f1('uSeed', seed); f1('uFlipY', flipY ? 1 : 0);
    gl.uniform4f(u('uImgRect'), ...spec.rect);
    f1('uAspect', imgAspect);
    v3('uBorderColor', BORDER_COLOR[spec.border]);
    gl.uniform1i(u('uBorder'), BORDER_ID[spec.border]);
    gl.uniform4f(u('uSrcCrop'), ...c);
    f1('uExposure', L.exposure + P.exposure - (a.nd ? 1.3 : 0));
    f1('uContrast', L.contrast); f1('uSaturation', L.saturation); f1('uTemp', L.temp + P.temp); f1('uTint', L.tint + P.tint);
    f1('uFade', L.fade); f1('uGamma', L.gamma); v3('uLift', L.lift); v3('uGain', L.gain);
    v3('uShadowTint', L.shadowTint); v3('uHighTint', L.highTint); f1('uMono', L.mono); f1('uCross', L.cross);
    f1('uHalation', L.halation); v3('uHalationColor', L.halationColor); f1('uBloom', L.bloom);
    f1('uGrain', L.grain); f1('uGrainSize', L.grainSize); f1('uVignette', L.vignette);
    f1('uLeak', a.lightLeaks ? L.leak : 0); v3('uLeakColor', L.leakColor);
    f1('uChroma', L.chroma); f1('uSoft', L.soft); f1('uSharpen', L.sharpen); f1('uPixel', L.pixel);
    f1('uScan', L.scan); f1('uVhs', L.vhs); f1('uWeave', L.weave); f1('uFlicker', L.flicker); f1('uDust', L.dust);
    f1('uFisheye', a.fisheye ? 1 : 0); f1('uPrism', a.prism ? 1 : 0); f1('uStar', a.star ? 1 : 0);
    f1('uFlash', P.flashFired ? Math.max(L.flash, 0.6) : 0);
    v3('uFlashColor', a.gel);
    this.quad();
    return spec;
  }

  /** The current accumulated (pre-film) frame as a canvas. */
  grabRaw() {
    const gl = this.gl, src = this.acc[this.cur];
    if (!src) return null;
    const out = new Fbo(gl, src.width, src.height, false);
    out.bind();
    gl.useProgram(this.copy.p);
    gl.activeTexture(gl.TEXTURE0); gl.bindTexture(gl.TEXTURE_2D, src.tex);
    gl.uniform1i(this.copy.u('uTex'), 0); gl.uniform1f(this.copy.u('uFlipY'), 1);
    this.quad();
    const c = this.readToCanvas(src.width, src.height);
    out.release();
    gl.bindFramebuffer(gl.FRAMEBUFFER, null);
    return c;
  }

  readToCanvas(w, h) {
    const gl = this.gl;
    const px = new Uint8ClampedArray(w * h * 4);
    gl.readPixels(0, 0, w, h, gl.RGBA, gl.UNSIGNED_BYTE, px);
    const c = document.createElement('canvas');
    c.width = w; c.height = h;
    c.getContext('2d').putImageData(new ImageData(px, w, h), 0, 0);
    return c;
  }

  /** Full-quality develop of a still source (canvas/image/video frame). Returns a canvas. */
  develop(src, P, imgAspect, seed, stampTime, double = null, zoom = 1, maxLong = 4096) {
    const gl = this.gl;
    const sw = src.videoWidth || src.width, sh = src.videoHeight || src.height;
    const srcTex = this.upload(src);
    const dTex = double ? this.upload(double) : null;
    const spec = frameSpec(P.recipe.border, imgAspect, P.borderOn);
    const srcAspect = sw / sh;
    const imgW = (imgAspect < srcAspect ? sh * imgAspect : sw) / zoom;
    let outW = imgW / (spec.rect[2] - spec.rect[0]), outH = outW / spec.outAspect;
    const k = Math.min(1, Math.min(maxLong, this.maxTex) / Math.max(outW, outH));
    const w = Math.max(16, Math.round(outW * k)), h = Math.max(16, Math.round(outH * k));
    const imgPx = Math.min(2048, Math.max(64, Math.round(w * (spec.rect[2] - spec.rect[0]))));
    const style = P.stampOn ? P.recipe.stamp : 'NONE';
    const sc = renderStamp(style, imgPx, Math.round(imgPx / imgAspect), stampTime);
    const sTex = sc ? this.upload(sc) : null;
    const fbo = new Fbo(gl, w, h, false);
    fbo.bind();
    this.drawFilm(srcTex, srcAspect, P, imgAspect, w, h, seed * 3.7, seed, true, sTex, dTex, double ? double.width / double.height : 1, zoom);
    const out = this.readToCanvas(w, h);
    fbo.release();
    [srcTex, dTex, sTex].forEach(t => t && gl.deleteTexture(t));
    gl.bindFramebuffer(gl.FRAMEBUFFER, null);
    return out;
  }
}
