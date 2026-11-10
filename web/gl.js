// WebGL2 port of the Android engine (FilmPipeline / FilmRenderer / StillProcessor / StampRenderer).
import { SHADERS } from './data.js';

const BORDER_ID = { NONE: 0, INSTANT: 1, BLACK: 2, SLIDE: 3, SPROCKET: 4, GATE8: 5, GATE16: 6, NEGATIVE: 7, TOY: 8 };
export const INSTANT_FRAMES = { WHITE: '#F4F2EC', CREAM: '#EFE6D2', BLACK: '#141414', PINK: '#F2CDD3', BLUE: '#C9DCEB', MINT: '#CFE8D8' };
const LOOK_DEFAULTS = {
  lut: null, lutMix: 1, exposure: 0, temp: 0, tint: 0, contrast: 1, saturation: 1, fade: 0, lift: [0, 0, 0], gain: [1, 1, 1],
  shadowTint: [0, 0, 0], highTint: [0, 0, 0], mono: 0, halation: 0, halationColor: [1, 0.3, 0.12], bloom: 0, soft: 0, softAll: 0,
  grain: 0, grainSize: 1, noise: 0, vignette: 0, leak: 0, leakColor: [1, 0.42, 0.12], chroma: 0, sharpen: 0, pixel: 0,
  scan: 0, vhs: 0, weave: 0, flicker: 0, dust: 0, interlace: 0, flash: 0.85,
};
const hex = h => [1, 3, 5].map(i => parseInt(h.slice(i, i + 2), 16) / 255);

export function frameSpec(border, imgAspect, borderOn) {
  let b = border;
  if (!borderOn && !['GATE8', 'GATE16', 'SPROCKET', 'TOY'].includes(border)) b = 'NONE';
  if (b === 'SLIDE') {
    const winW = 1, winH = 1 / imgAspect, size = Math.max(winW, winH) * 1.36;
    const x0 = (size - winW) / 2 / size, y0 = (size - winH) / 2 / size;
    return { outAspect: 1, rect: [x0, y0, 1 - x0, 1 - y0], imgAspect, border: b };
  }
  let m = [0, 0, 0];
  if (b === 'INSTANT') m = imgAspect < 0.85 ? [0.087, 0.15, 0.37] : imgAspect < 1.2 ? [0.08, 0.11, 0.27] : [0.045, 0.07, 0.17];
  else m = { BLACK: [0.05, 0.05, 0.05], NEGATIVE: [0.03, 0.15, 0.15], GATE8: [0.035, 0.035, 0.035], GATE16: [0.02, 0.02, 0.02],
    TOY: [0.03, 0.03, 0.03], SPROCKET: [0.012, 0.012, 0.012] }[b] || [0, 0, 0];
  const [side, top, bottom] = m;
  const w = 1 + 2 * side, h = 1 / imgAspect + top + bottom;
  return { outAspect: w / h, rect: [side / w, bottom / h, (side + 1) / w, (bottom + 1 / imgAspect) / h], imgAspect, border: b };
}

function frameColor(b, instant) {
  if (b === 'INSTANT') return hex(INSTANT_FRAMES[instant] || INSTANT_FRAMES.WHITE);
  if (b === 'SLIDE') return [0.93, 0.91, 0.86];
  if (b === 'NONE') return [0, 0, 0];
  return [0.035, 0.032, 0.03];
}

export function crop(src, dst, zoom = 1) {
  let c = dst < src ? [(1 - dst / src) / 2, 0, dst / src, 1] : [0, (1 - src / dst) / 2, 1, src / dst];
  if (zoom > 1) { const sw = c[2] / zoom, sh = c[3] / zoom; c = [c[0] + (c[2] - sw) / 2, c[1] + (c[3] - sh) / 2, sw, sh]; }
  return c;
}

// ------------------------------------------------------------------ overlays (port of StampRenderer)
const pad = n => String(n).padStart(2, '0');
const MON = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];
const SEGS = { 0: 'abcdef', 1: 'bc', 2: 'abged', 3: 'abgcd', 4: 'fgbc', 5: 'afgcd', 6: 'afgedc', 7: 'abc', 8: 'abcdefg', 9: 'abcdfg' };

export function stampKey(style, t) {
  const d = new Date(t);
  if (style === 'CAMCORDER' || style === 'VHS') return style + Math.floor(t / 1000);
  if (style === 'NONE') return '';
  return style + d.toDateString();
}

function segDigit(g, ch, x, y, h, draw) {
  const w = h * 0.52, t = h * 0.12;
  if (ch === "'") { if (draw) { g.beginPath(); g.moveTo(x + t * 0.6, y); g.lineTo(x + t * 1.6, y); g.lineTo(x + t * 0.8, y + h * 0.3); g.lineTo(x + t * 0.2, y + h * 0.3); g.fill(); } return t * 2.4; }
  if (ch === ' ') return w * 0.7;
  const segs = SEGS[ch]; if (!segs) return w;
  if (!draw) return w + t * 0.9;
  const half = h / 2;
  const hSeg = (sx, sy) => { g.beginPath(); g.moveTo(sx + t * 0.5, sy); g.lineTo(sx + t, sy - t / 2); g.lineTo(sx + w - t, sy - t / 2); g.lineTo(sx + w - t * 0.5, sy); g.lineTo(sx + w - t, sy + t / 2); g.lineTo(sx + t, sy + t / 2); g.fill(); };
  const vSeg = (sx, sy, len) => { g.beginPath(); g.moveTo(sx, sy + t * 0.5); g.lineTo(sx + t / 2, sy + t); g.lineTo(sx + t / 2, sy + len - t); g.lineTo(sx, sy + len - t * 0.5); g.lineTo(sx - t / 2, sy + len - t); g.lineTo(sx - t / 2, sy + t); g.fill(); };
  const l = x + t / 2, r = x + w - t / 2;
  for (const s of segs) ({ a: () => hSeg(x, y + t / 2), g: () => hSeg(x, y + half), d: () => hSeg(x, y + h - t / 2),
    f: () => vSeg(l, y + t / 2, half - t / 2), b: () => vSeg(r, y + t / 2, half - t / 2), e: () => vSeg(l, y + half, half - t / 2), c: () => vSeg(r, y + half, half - t / 2) })[s]();
  return w + t * 0.9;
}

function segText(g, text, right, bottom, h) {
  let width = 0; for (const ch of text) width += segDigit(g, ch, 0, 0, h, false);
  let x = right - width; const y = bottom - h;
  g.save(); g.transform(1, 0, -0.08, 1, y * 0.08, 0);
  for (const ch of text) x += segDigit(g, ch, x, y, h, true);
  g.restore();
}

export function renderStamp(style, w, h, t) {
  if (style === 'NONE') return null;
  const c = document.createElement('canvas'); c.width = w; c.height = h;
  const g = c.getContext('2d'); const d = new Date(t); const unit = Math.min(w, h);
  const outlined = (text, x, y, size, font) => {
    g.font = `bold ${size}px ${font}`; g.lineJoin = 'round'; g.lineWidth = size * 0.12; g.strokeStyle = 'rgba(0,0,0,0.8)'; g.strokeText(text, x, y);
    g.fillStyle = 'rgb(245,245,240)'; g.fillText(text, x, y);
  };
  if (style === 'FILM_ORANGE') {
    const text = `'${pad(d.getFullYear() % 100)} ${String(d.getMonth() + 1).padStart(2, ' ')} ${String(d.getDate()).padStart(2, ' ')}`;
    const hgt = unit * 0.042, right = w - unit * 0.075, bottom = h - unit * 0.07;
    g.filter = `blur(${hgt * 0.18}px)`; g.fillStyle = 'rgba(255,80,0,0.8)'; segText(g, text, right, bottom, hgt);
    g.filter = 'none'; g.fillStyle = 'rgb(255,150,45)'; segText(g, text, right, bottom, hgt);
    g.fillStyle = 'rgba(255,225,170,0.35)'; segText(g, text, right, bottom, hgt);
  } else if (style === 'DIGI_ORANGE') {
    const text = `${d.getFullYear()}/${pad(d.getMonth() + 1)}/${pad(d.getDate())}`;
    const size = unit * 0.042; g.font = `bold ${size}px sans-serif`;
    g.shadowColor = 'rgba(255,100,0,0.8)'; g.shadowBlur = size * 0.25; g.fillStyle = 'rgb(255,170,40)';
    g.fillText(text, w - g.measureText(text).width - unit * 0.06, h - unit * 0.06);
  } else if (style === 'CAMCORDER') {
    const size = unit * 0.05, h12 = d.getHours() % 12 || 12;
    outlined(`${MON[d.getMonth()]}. ${d.getDate()} ${d.getFullYear()}`, unit * 0.07, h - unit * 0.07 - size * 1.25, size, 'sans-serif');
    outlined(`${h12}:${pad(d.getMinutes())}:${pad(d.getSeconds())} ${d.getHours() < 12 ? 'AM' : 'PM'}`, unit * 0.07, h - unit * 0.07, size, 'sans-serif');
  } else if (style === 'VHS') {
    const size = unit * 0.058, f = '"Courier New", monospace', h12 = d.getHours() % 12 || 12;
    outlined('PLAY ▶', unit * 0.07, unit * 0.07 + size, size, f);
    outlined('SP', w - unit * 0.07 - size * 1.4, unit * 0.07 + size, size, f);
    outlined(`${d.getHours() < 12 ? 'AM' : 'PM'} ${pad(h12)}:${pad(d.getMinutes())}`, unit * 0.07, h - unit * 0.07 - size * 1.3, size, f);
    outlined(`${MON[d.getMonth()]}.${pad(d.getDate())} ${d.getFullYear()}`, unit * 0.07, h - unit * 0.07, size, f);
  }
  return c;
}

export function renderMarks(border, edgeText, w, h, rect, frameNo = 12) {
  if (!edgeText || (border !== 'NEGATIVE' && border !== 'SPROCKET')) return null;
  const c = document.createElement('canvas'); c.width = w; c.height = h;
  const g = c.getContext('2d');
  g.fillStyle = 'rgba(255,166,60,0.84)'; g.shadowColor = 'rgba(255,120,0,0.5)'; g.shadowBlur = 2;
  const top = (1 - rect[3]) * h, bottom = (1 - rect[1]) * h;
  if (border === 'NEGATIVE') {
    g.font = `bold ${top * 0.32}px sans-serif`;
    g.fillText(edgeText, w * 0.06, top * 0.62); g.fillText(`▶ ${frameNo}`, w * 0.62, top * 0.62);
    g.fillText(`${frameNo}`, w * 0.08, bottom + (h - bottom) * 0.62); g.fillText(`${frameNo}A  ▶`, w * 0.7, bottom + (h - bottom) * 0.62);
  } else {
    const band = (bottom - top) * 0.13; g.font = `bold ${band * 0.26}px sans-serif`; g.fillStyle = 'rgba(255,170,70,0.6)';
    g.fillText(`${edgeText}  ▶ ${frameNo}`, w * 0.2, top + band * 0.96); g.fillText(`${frameNo}A`, w * 0.66, bottom - band * 0.04);
  }
  return c;
}

// ------------------------------------------------------------------ GL helpers
function compile(gl, type, src) {
  const s = gl.createShader(type); gl.shaderSource(s, src); gl.compileShader(s);
  if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) throw new Error(gl.getShaderInfoLog(s));
  return s;
}
function program(gl, vs, fs) {
  const p = gl.createProgram();
  gl.attachShader(p, compile(gl, gl.VERTEX_SHADER, vs)); gl.attachShader(p, compile(gl, gl.FRAGMENT_SHADER, fs));
  gl.bindAttribLocation(p, 0, 'aPos'); gl.linkProgram(p);
  if (!gl.getProgramParameter(p, gl.LINK_STATUS)) throw new Error(gl.getProgramInfoLog(p));
  const locs = {};
  const u = n => (n in locs ? locs[n] : (locs[n] = gl.getUniformLocation(p, n)));
  return {
    p, u, use: () => gl.useProgram(p),
    f: (n, v) => gl.uniform1f(u(n), v), i: (n, v) => gl.uniform1i(u(n), v), ui: (n, v) => gl.uniform1ui(u(n), v >>> 0),
    v2: (n, a, b) => gl.uniform2f(u(n), a, b), v3: (n, v) => gl.uniform3f(u(n), v[0], v[1], v[2]), v4: (n, v) => gl.uniform4f(u(n), v[0], v[1], v[2], v[3]),
    tex: (n, unit, target, t) => { gl.activeTexture(gl.TEXTURE0 + unit); gl.bindTexture(target, t); gl.uniform1i(u(n), unit); },
  };
}

class Fbo {
  constructor(gl, w, h, half) {
    this.gl = gl; this.width = w; this.height = h;
    this.tex = gl.createTexture(); gl.bindTexture(gl.TEXTURE_2D, this.tex);
    for (const [k, v] of [[gl.TEXTURE_MIN_FILTER, gl.LINEAR], [gl.TEXTURE_MAG_FILTER, gl.LINEAR], [gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE], [gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE]]) gl.texParameteri(gl.TEXTURE_2D, k, v);
    if (half) gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA16F, w, h, 0, gl.RGBA, gl.HALF_FLOAT, null);
    else gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, w, h, 0, gl.RGBA, gl.UNSIGNED_BYTE, null);
    this.fbo = gl.createFramebuffer(); gl.bindFramebuffer(gl.FRAMEBUFFER, this.fbo);
    gl.framebufferTexture2D(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0, gl.TEXTURE_2D, this.tex, 0);
    this.ok = gl.checkFramebufferStatus(gl.FRAMEBUFFER) === gl.FRAMEBUFFER_COMPLETE;
    gl.bindFramebuffer(gl.FRAMEBUFFER, null);
  }
  bind() { this.gl.bindFramebuffer(this.gl.FRAMEBUFFER, this.fbo); this.gl.viewport(0, 0, this.width, this.height); }
  release() { this.gl.deleteFramebuffer(this.fbo); this.gl.deleteTexture(this.tex); }
}

export class FilmGL {
  constructor(canvas) {
    const gl = canvas.getContext('webgl2', { alpha: false, antialias: false, premultipliedAlpha: false, preserveDrawingBuffer: true });
    if (!gl) throw new Error('WebGL2 is not available on this browser');
    this.gl = gl; this.canvas = canvas;
    this.input = program(gl, SHADERS.VERTEX, SHADERS.INPUT);
    this.copyP = program(gl, SHADERS.VERTEX, SHADERS.COPY);
    this.gradeP = program(gl, SHADERS.VERTEX, SHADERS.GRADE);
    this.downP = program(gl, SHADERS.VERTEX, SHADERS.DOWN);
    this.blurP = program(gl, SHADERS.VERTEX, SHADERS.BLUR);
    this.compP = program(gl, SHADERS.VERTEX, SHADERS.COMPOSITE);
    this.half = !!gl.getExtension('EXT_color_buffer_float') || !!gl.getExtension('EXT_color_buffer_half_float');
    this.maxTex = gl.getParameter(gl.MAX_TEXTURE_SIZE);
    const buf = gl.createBuffer(); gl.bindBuffer(gl.ARRAY_BUFFER, buf);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 1, -1, -1, 1, 1, 1]), gl.STATIC_DRAW);
    this.vao = gl.createVertexArray(); gl.bindVertexArray(this.vao);
    gl.enableVertexAttribArray(0); gl.vertexAttribPointer(0, 2, gl.FLOAT, false, 0, 0);
    this.blank = this.texture(); gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, 1, 1, 0, gl.RGBA, gl.UNSIGNED_BYTE, new Uint8Array(4));
    this.luts = {}; this.lutIdentity = this.lut3d(null);
    this.camTex = this.texture();
    this.acc = [null, null]; this.cur = 0; this.accFresh = true;
    this.stampTex = null; this.stampKeyStr = ''; this.marksTex = null; this.marksKey = '';
    this.doubleTex = null; this.doubleAspect = 1;
    this.start = performance.now(); this.lastFrame = 0; this.frameDt = 1 / 30; this.frameCount = 0;
    this.shutter = 0; this.mirror = false; this.zoom = 1;
    this.params = null; this.imgAspect = 0.75; this.seed = (Math.random() * 2 ** 31) | 0;
  }

  texture() {
    const gl = this.gl, t = gl.createTexture(); gl.bindTexture(gl.TEXTURE_2D, t);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR); gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE); gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    return t;
  }

  /** Upload so that v = 1 is the top row (same convention as Android). */
  upload(src, tex) {
    const gl = this.gl, t = tex || this.texture();
    gl.bindTexture(gl.TEXTURE_2D, t);
    gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, true); gl.pixelStorei(gl.UNPACK_PREMULTIPLY_ALPHA_WEBGL, true);
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, src);
    gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, false); gl.pixelStorei(gl.UNPACK_PREMULTIPLY_ALPHA_WEBGL, false);
    return t;
  }

  lut3d(img) {
    const gl = this.gl, n = 33, data = new Uint8Array(n * n * n * 3);
    if (img) {
      const c = document.createElement('canvas'); c.width = n; c.height = n * n;
      const g = c.getContext('2d', { willReadFrequently: true }); g.drawImage(img, 0, 0);
      const px = g.getImageData(0, 0, n, n * n).data;
      for (let i = 0; i < n * n * n; i++) { data[i * 3] = px[i * 4]; data[i * 3 + 1] = px[i * 4 + 1]; data[i * 3 + 2] = px[i * 4 + 2]; }
    } else {
      let i = 0;
      for (let b = 0; b < n; b++) for (let g = 0; g < n; g++) for (let r = 0; r < n; r++) {
        data[i++] = Math.round(r * 255 / (n - 1)); data[i++] = Math.round(g * 255 / (n - 1)); data[i++] = Math.round(b * 255 / (n - 1));
      }
    }
    const t = gl.createTexture(); gl.bindTexture(gl.TEXTURE_3D, t);
    gl.pixelStorei(gl.UNPACK_ALIGNMENT, 1);
    gl.texImage3D(gl.TEXTURE_3D, 0, gl.RGB8, n, n, n, 0, gl.RGB, gl.UNSIGNED_BYTE, data);
    gl.pixelStorei(gl.UNPACK_ALIGNMENT, 4);
    for (const p of [gl.TEXTURE_WRAP_S, gl.TEXTURE_WRAP_T, gl.TEXTURE_WRAP_R]) gl.texParameteri(gl.TEXTURE_3D, p, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_3D, gl.TEXTURE_MIN_FILTER, gl.LINEAR); gl.texParameteri(gl.TEXTURE_3D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
    return t;
  }

  /** Load all LUTs used by the roster (call once before rendering). */
  async loadLuts(names, base = 'luts/') {
    await Promise.all([...new Set(names.filter(Boolean))].map(n => new Promise(res => {
      const img = new Image();
      img.onload = () => { this.luts[n] = this.lut3d(img); res(); };
      img.onerror = () => res();
      img.src = `${base}${n}.png`;
    })));
  }

  quad() { this.gl.bindVertexArray(this.vao); this.gl.drawArrays(this.gl.TRIANGLE_STRIP, 0, 4); }
  get cameraAspect() { return this.camW ? this.camW / this.camH : 0.75; }
  resetAccumulation() { this.accFresh = true; }

  setDouble(img) {
    if (!img) { if (this.doubleTex) this.gl.deleteTexture(this.doubleTex); this.doubleTex = null; return; }
    this.doubleTex = this.upload(img, this.doubleTex); this.doubleAspect = img.width / img.height;
  }

  ensure(w, h) {
    const gl = this.gl;
    if (!this.gradeF || this.gradeF.width !== w || this.gradeF.height !== h) { this.gradeF?.release(); this.gradeF = new Fbo(gl, w, h, false); }
    const qw = Math.max(1, w >> 2), qh = Math.max(1, h >> 2);
    if (!this.qa || this.qa.width !== qw || this.qa.height !== qh) {
      this.qa?.release(); this.qb?.release(); this.qa = new Fbo(gl, qw, qh, false); this.qb = new Fbo(gl, qw, qh, false);
    }
  }

  /** The four-pass film look (port of FilmPipeline.render). */
  render(src, srcW, srcH, srcCrop, P, spec, outW, outH, bindTarget, time, seed, leakSeed, flipY, stampTex, marksTex, tex2 = null, tex2Crop = null) {
    const gl = this.gl, L = Object.assign({}, LOOK_DEFAULTS, P.recipe.look), a = P.acc;
    const t = Object.assign({ intensity: 1, grain: 1, leaks: 1, glow: 1, vignette: 1, soft: 1 }, P.tweak || {}), k = t.intensity;
    const dev = (v, n) => n + (v - n) * k, dev3 = (v, n) => v.map(x => dev(x, n));
    const imgW = Math.max(16, Math.round(outW * (spec.rect[2] - spec.rect[0]))), imgH = Math.max(16, Math.round(outH * (spec.rect[3] - spec.rect[1])));
    this.ensure(imgW, imgH);
    const g = this.gradeF, qa = this.qa, qb = this.qb;
    const G = this.gradeP; g.bind(); G.use();
    G.tex('uSrc', 0, gl.TEXTURE_2D, src); G.v4('uSrcCrop', srcCrop); G.v2('uSrcTexel', 1 / srcW, 1 / srcH);
    G.tex('uSrc2', 1, gl.TEXTURE_2D, tex2 || this.blank); G.v4('uSrc2Crop', tex2Crop || [0, 0, 1, 1]); G.f('uDouble', tex2 ? 1 : 0);
    G.tex('uLut', 2, gl.TEXTURE_3D, (L.lut && this.luts[L.lut]) || this.lutIdentity); G.f('uLutMix', L.lut ? Math.min(1, Math.max(0, L.lutMix * k)) : 0);
    G.f('uAspect', spec.imgAspect); G.f('uTime', time); G.ui('uSeed', seed);
    G.f('uExposure', dev(L.exposure, 0) + P.exposure - (a.nd ? 1.3 : 0)); G.f('uTemp', dev(L.temp, 0) + P.temp); G.f('uTint', dev(L.tint, 0) + P.tint);
    G.f('uContrast', dev(L.contrast, 1)); G.f('uSaturation', Math.max(0, dev(L.saturation, 1))); G.f('uFade', Math.max(0, dev(L.fade, 0))); G.f('uMono', L.mono);
    G.v3('uLift', dev3(L.lift, 0)); G.v3('uGain', dev3(L.gain, 1)); G.v3('uShadowTint', dev3(L.shadowTint, 0)); G.v3('uHighTint', dev3(L.highTint, 0));
    G.f('uFlash', P.flashFired ? L.flash : 0); G.v3('uFlashColor', a.gel || [1, 1, 1]);
    G.i('uFisheye', a.fisheyeC ? 2 : a.fisheyeW ? 1 : 0); G.f('uChroma', L.chroma * k); G.f('uPixel', L.pixel); G.f('uSharpen', L.sharpen);
    G.f('uPrism', a.prism ? 1 : 0); G.f('uVhs', L.vhs); G.f('uWeave', L.weave);
    this.quad();
    qa.bind(); this.downP.use(); this.downP.tex('uSrc', 0, gl.TEXTURE_2D, g.tex); this.downP.v2('uTexel', 1 / qa.width, 1 / qa.height); this.quad();
    const sigma = Math.max(1.5, 0.018 * Math.min(qa.width, qa.height));
    const B = this.blurP;
    qb.bind(); B.use(); B.tex('uSrc', 0, gl.TEXTURE_2D, qa.tex); B.v2('uStep', 1 / qa.width, 0); B.f('uSigma', sigma); this.quad();
    qa.bind(); B.tex('uSrc', 0, gl.TEXTURE_2D, qb.tex); B.v2('uStep', 0, 1 / qa.height); this.quad();
    bindTarget();
    const C = this.compP; C.use();
    C.tex('uGraded', 0, gl.TEXTURE_2D, g.tex); C.tex('uBlur', 1, gl.TEXTURE_2D, qa.tex);
    C.tex('uStamp', 2, gl.TEXTURE_2D, stampTex || this.blank); C.f('uStampOn', stampTex ? 1 : 0);
    C.tex('uFrameTex', 3, gl.TEXTURE_2D, marksTex || this.blank); C.f('uFrameTexOn', marksTex ? 1 : 0);
    C.v2('uRes', outW, outH); C.f('uFlipY', flipY ? 1 : 0); C.v4('uImgRect', spec.rect); C.f('uAspect', spec.imgAspect);
    C.i('uBorder', BORDER_ID[spec.border]); C.v3('uFrameColor', frameColor(spec.border, P.instantFrame));
    C.f('uTime', time); C.ui('uSeed', seed); C.ui('uLeakSeed', leakSeed);
    C.f('uBloom', L.bloom * t.glow); C.f('uHalation', L.halation * t.glow); C.v3('uHalationColor', L.halationColor); C.f('uSoft', L.soft * t.soft); C.f('uSoftAll', L.softAll * t.soft);
    C.f('uStar', a.star ? 1 : 0); C.f('uVignette', Math.min(1.2, L.vignette * t.vignette)); C.f('uLeak', a.lightLeaks ? L.leak * t.leaks : 0); C.f('uLeakAnim', P.recipe.isVideo ? 1 : 0);
    C.v3('uLeakColor', L.leakColor); C.f('uGrain', L.grain * t.grain); C.f('uGrainSize', L.grainSize); C.f('uNoise', L.noise);
    C.f('uScan', L.scan); C.f('uVhs', L.vhs); C.f('uFlicker', L.flicker); C.f('uDust', L.dust); C.f('uInterlace', L.interlace);
    this.quad();
  }

  /** Called for each new video frame. */
  frame(video) {
    const gl = this.gl, w = video.videoWidth, h = video.videoHeight;
    if (!w || !h) return;
    const now = performance.now();
    if (this.lastFrame) this.frameDt = this.frameDt * 0.9 + Math.min(Math.max((now - this.lastFrame) / 1000, 0.005), 0.5) * 0.1;
    this.lastFrame = now;
    const s = Math.min(1, 1440 / Math.max(w, h)), aw = Math.round(w * s), ah = Math.round(h * s);
    if (!this.acc[0] || this.acc[0].width !== aw || this.acc[0].height !== ah) {
      this.acc.forEach(f => f && f.release());
      this.acc = [new Fbo(gl, aw, ah, this.half), new Fbo(gl, aw, ah, this.half)];
      if (!this.acc[0].ok) { this.half = false; this.acc.forEach(f => f.release()); this.acc = [new Fbo(gl, aw, ah, false), new Fbo(gl, aw, ah, false)]; }
      this.accFresh = true;
    }
    this.camW = w; this.camH = h;
    this.upload(video, this.camTex);
    const dst = this.acc[1 - this.cur], prev = this.acc[this.cur];
    dst.bind(); const I = this.input; I.use();
    I.tex('uCam', 0, gl.TEXTURE_2D, this.camTex); I.tex('uPrev', 1, gl.TEXTURE_2D, this.accFresh ? this.blank : prev.tex);
    gl.uniformMatrix4fv(I.u('uSt'), false, [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]);
    I.i('uRot', 0); I.f('uMirror', this.mirror ? 1 : 0);
    I.f('uAlpha', (this.accFresh || this.shutter <= this.frameDt) ? 1 : Math.min(Math.max(this.frameDt / this.shutter, 0.02), 1));
    this.quad();
    this.cur = 1 - this.cur; this.accFresh = false; this.frameCount++;
    if (!this.params) return;
    const P = this.params, spec = frameSpec(P.recipe.border, this.imgAspect, P.borderOn);
    const t = (now - this.start) / 1000;
    this.render(dst.tex, aw, ah, crop(this.cameraAspect, this.imgAspect, this.zoom), P, spec, this.canvas.width, this.canvas.height, () => {
      gl.bindFramebuffer(gl.FRAMEBUFFER, null); gl.viewport(0, 0, this.canvas.width, this.canvas.height);
    }, t, (this.seed + this.frameCount) | 0, this.seed, false, this.liveStamp(P), this.liveMarks(P, spec), this.doubleTex,
      this.doubleTex ? crop(this.doubleAspect, this.imgAspect) : null);
  }

  liveStamp(P) {
    const now = Date.now(), style = P.stampOn ? P.recipe.stamp : 'NONE';
    const k = stampKey(style, now) + this.imgAspect;
    if (k !== this.stampKeyStr) {
      this.stampKeyStr = k;
      const c = renderStamp(style, 1080, Math.round(1080 / this.imgAspect), now);
      if (!c) { if (this.stampTex) this.gl.deleteTexture(this.stampTex); this.stampTex = null; } else this.stampTex = this.upload(c, this.stampTex);
    }
    return this.stampTex;
  }

  liveMarks(P, spec) {
    const k = spec.border + P.recipe.edgeText + spec.outAspect;
    if (k !== this.marksKey) {
      this.marksKey = k;
      const c = renderMarks(spec.border, P.recipe.edgeText, 1080, Math.round(1080 / spec.outAspect), spec.rect);
      if (!c) { if (this.marksTex) this.gl.deleteTexture(this.marksTex); this.marksTex = null; } else this.marksTex = this.upload(c, this.marksTex);
    }
    return this.marksTex;
  }

  grabRaw() {
    const gl = this.gl, src = this.acc[this.cur];
    if (!src) return null;
    const out = new Fbo(gl, src.width, src.height, false); out.bind();
    this.copyP.use(); this.copyP.tex('uTex', 0, gl.TEXTURE_2D, src.tex); this.copyP.f('uFlipY', 1); this.quad();
    const c = this.readToCanvas(src.width, src.height);
    out.release(); gl.bindFramebuffer(gl.FRAMEBUFFER, null);
    return c;
  }

  readToCanvas(w, h) {
    const gl = this.gl, px = new Uint8ClampedArray(w * h * 4);
    gl.readPixels(0, 0, w, h, gl.RGBA, gl.UNSIGNED_BYTE, px);
    const c = document.createElement('canvas'); c.width = w; c.height = h;
    c.getContext('2d').putImageData(new ImageData(px, w, h), 0, 0);
    return c;
  }

  /** Full-quality develop of a still source. Returns a canvas. */
  develop(src, P, imgAspect, seed, stampTime, double = null, zoom = 1, maxLong = 4096) {
    const gl = this.gl, sw = src.videoWidth || src.width, sh = src.videoHeight || src.height;
    const srcTex = this.upload(src), dTex = double ? this.upload(double) : null;
    const spec = frameSpec(P.recipe.border, imgAspect, P.borderOn), srcAspect = sw / sh;
    const imgW = (imgAspect < srcAspect ? sh * imgAspect : sw) / zoom;
    let outW = imgW / (spec.rect[2] - spec.rect[0]), outH = outW / spec.outAspect;
    const k = Math.min(1, Math.min(maxLong, this.maxTex) / Math.max(outW, outH));
    const w = Math.max(16, Math.round(outW * k)), h = Math.max(16, Math.round(outH * k));
    const imgPx = Math.min(2400, Math.max(64, Math.round(w * (spec.rect[2] - spec.rect[0]))));
    const sc = renderStamp(P.stampOn ? P.recipe.stamp : 'NONE', imgPx, Math.round(imgPx / imgAspect), stampTime);
    const sTex = sc ? this.upload(sc) : null;
    const mc = renderMarks(spec.border, P.recipe.edgeText, Math.min(w, 2400), Math.round(Math.min(w, 2400) / spec.outAspect), spec.rect);
    const mTex = mc ? this.upload(mc) : null;
    const fbo = new Fbo(gl, w, h, false);
    this.render(srcTex, sw, sh, crop(srcAspect, imgAspect, zoom), P, spec, w, h, () => fbo.bind(), 0, seed, seed, true, sTex, mTex,
      dTex, double ? crop(double.width / double.height, imgAspect) : null);
    fbo.bind();
    const out = this.readToCanvas(w, h);
    fbo.release(); [srcTex, dTex, sTex, mTex].forEach(t => t && gl.deleteTexture(t));
    gl.bindFramebuffer(gl.FRAMEBUFFER, null);
    return out;
  }
}
