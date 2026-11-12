// FUN CAM core for the engine-v4 editions (DOT, GLASS, FIELD): camera, state, capture, video, roll.
// It has no look of its own. Each edition draws its own interface and talks to this module.
import { CAMERAS4, SHUTTER_SPEEDS, FPS_VALUES } from './data.js';
import { FilmGL, frameSpec } from './gl.js';
import * as roll from './roll.js';

export const CAMERAS = CAMERAS4;
export { FPS_VALUES, frameSpec };
export const SHUTTERS = [0, ...SHUTTER_SPEEDS];
export const CAT = { DIGITAL: 'Digital', V135: 'Vintage 135', V120: 'Vintage 120', INST: 'Instant', VIDEO: 'Video' };
export const GELS = { WHITE: [1, 1, 1], RED: [1, 0.25, 0.3], BLUE: [0.3, 0.5, 1], GREEN: [0.35, 1, 0.45], YELLOW: [1, 0.85, 0.3], PURPLE: [0.75, 0.35, 1] };
export const FRAMES = { WHITE: '#F4F2EC', CREAM: '#EFE6D2', BLACK: '#141414', PINK: '#F2CDD3', BLUE: '#C9DCEB', MINT: '#CFE8D8' };
export const LENSES = [
  ['mist', 'Mist', 'Soft glowing highlights, gentler contrast'],
  ['streak', 'Streak', 'Blue anamorphic flares through lights'],
  ['star', 'Star', 'Starbursts on bright lights'],
  ['prism', 'Prism', 'Rainbow ghost reflections'],
  ['fisheyeW', 'Fisheye', 'Bulging ultra-wide'],
  ['fisheyeC', 'Circle', 'Circular fisheye in black'],
  ['nd', 'ND', 'Darker, for slow shutter in daylight'],
  ['doubleExposure', 'Double', 'Two shots in one frame'],
  ['lightLeaks', 'Leaks', 'Warm light leaks on film cameras'],
];
export const TWEAKS = [
  ['intensity', 'Intensity', 1.5], ['grain', 'Grain', 2], ['leaks', 'Leaks', 2], ['glow', 'Glow', 2], ['vignette', 'Vignette', 2], ['soft', 'Softness', 2],
];
export const shutterLabel = s => !s ? 'AUTO' : s >= 1 ? `${Math.round(s)}"` : `1/${Math.round(1 / s)}`;
export const ratioLabel = ([w, h]) => w === 239 ? '2.39' : `${w}:${h}`;
export const fmtEv = v => (v > 0.04 ? '+' : v < -0.04 ? '−' : '±') + Math.abs(v).toFixed(1);
export const haptic = (ms = 6) => { try { navigator.vibrate?.(ms); } catch { /* not supported */ } };
const byId = id => CAMERAS.find(c => c.id === id);
const TW0 = { intensity: 1, grain: 1, leaks: 1, glow: 1, vignette: 1, soft: 1 };
const wait = ms => new Promise(r => setTimeout(r, ms));
const newSeed = () => (Math.random() * 2 ** 31) | 0;
const toBlob = (c, type = 'image/jpeg', q = 0.94) => new Promise(r => c.toBlob(r, type, q));
const native = () => !!window.Capacitor?.isNativePlatform?.();

export function createCore(canvas) {
  const saved = JSON.parse(localStorage.getItem('funcam4') || '{}');
  const first = byId(saved.recipe) || byId('sure85');
  const S = {
    recipe: first, ratioIdx: saved.ratioIdx || {}, flash: first.flashDefault, timer: 0,
    grid: !!saved.grid, level: false, histogram: !!saved.histogram,
    exposure: 0, temp: 0, tint: 0, shutter: 0, fps: first.videoFps, zoom: 1, front: false,
    stampOn: saved.stampOn ?? true, borderOn: saved.borderOn ?? true, sound: saved.sound ?? true, audio: saved.audio ?? true,
    instantFrame: saved.instantFrame || 'WHITE', favs: saved.favs || [], lastPhoto: saved.lastPhoto || 'sure85', lastVideo: saved.lastVideo || 'classicv',
    acc: { fisheyeW: false, fisheyeC: false, prism: false, star: false, nd: false, mist: false, streak: false, doubleExposure: false,
      lightLeaks: saved.leaks ?? true, gelName: 'WHITE' },
    busy: false, recording: false, recStart: 0, countdown: 0, progress: null, started: false, shots: saved.shots || 0, tilt: 0,
  };
  const tweaks = JSON.parse(localStorage.getItem('funcam_tweaks') || '{}');
  const listeners = {};
  const core = { S, CAMERAS, SHUTTERS, FPS: FPS_VALUES };
  core.on = (evt, fn) => { (listeners[evt] ||= []).push(fn); return core; };
  const emit = (evt, a) => (listeners[evt] || []).forEach(f => f(a));
  const persist = () => localStorage.setItem('funcam4', JSON.stringify({
    recipe: S.recipe.id, ratioIdx: S.ratioIdx, grid: S.grid, histogram: S.histogram, stampOn: S.stampOn, borderOn: S.borderOn, sound: S.sound,
    audio: S.audio, leaks: S.acc.lightLeaks, instantFrame: S.instantFrame, favs: S.favs, lastPhoto: S.lastPhoto, lastVideo: S.lastVideo, shots: S.shots,
  }));

  let film, video, stream;
  core.mode = () => (S.recipe.isVideo ? 'video' : 'photo');
  core.slow = () => S.shutter > 1 / 30;
  core.ratio = () => { const r = S.recipe.ratios, i = Math.min(S.ratioIdx[S.recipe.id] || 0, r.length - 1); return { i, w: r[i][0], h: r[i][1], v: r[i][0] / r[i][1], label: ratioLabel(r[i]) }; };
  core.spec = () => frameSpec(S.recipe.border, core.ratio().v, S.borderOn);
  core.tweak = (id = S.recipe.id) => Object.assign({}, TW0, tweaks[id] || {});
  core.tuned = (id = S.recipe.id) => { const t = core.tweak(id); return Object.keys(TW0).some(k => Math.abs(t[k] - TW0[k]) > 1e-6); };
  core.lensOn = () => ['fisheyeW', 'fisheyeC', 'prism', 'star', 'nd', 'mist', 'streak', 'doubleExposure'].some(k => S.acc[k]);
  /** True when anything is off its automatic value, so the interface can offer one "back to auto". */
  core.dirty = () => !!(S.exposure || S.temp || S.tint || S.shutter);
  const params = (flashFired = S.flash && !S.recipe.isVideo, recipe = S.recipe) => ({
    recipe, borderOn: S.borderOn, stampOn: S.stampOn, exposure: S.exposure, temp: S.temp, tint: S.tint, flashFired, instantFrame: S.instantFrame,
    tweak: core.tweak(recipe.id), acc: { ...S.acc, gel: GELS[S.acc.gelName] },
  });

  function sync(reason) {
    if (film) { film.params = params(); film.imgAspect = core.ratio().v; film.shutter = S.shutter; film.zoom = S.zoom; }
    emit('change', reason);
  }
  core.sync = sync;
  const say = text => emit('message', text);

  // ---------------------------------------------------------------- camera
  async function openCamera() {
    stream?.getTracks().forEach(t => t.stop());
    stream = await navigator.mediaDevices.getUserMedia({
      audio: false,
      video: { facingMode: S.front ? 'user' : 'environment', width: { ideal: 1920 }, height: { ideal: 1440 }, aspectRatio: { ideal: 4 / 3 } },
    });
    video.srcObject = stream;
    await video.play();
    film.mirror = S.front;
    film.resetAccumulation();
  }

  core.start = async () => {
    if (!window.isSecureContext && !native()) throw new Error('Open this page over https:// so the camera can be used.');
    if (!navigator.mediaDevices?.getUserMedia) throw new Error('This browser has no camera access. On iPhone use Safari.');
    film = new FilmGL(canvas);
    await film.loadLuts(CAMERAS.map(c => c.look.lut), new URL('./luts/', import.meta.url).href);
    video = Object.assign(document.createElement('video'), { muted: true, playsInline: true, autoplay: true });
    video.setAttribute('playsinline', '');
    await openCamera();
    S.started = true;
    sync('start');
    const step = () => {
      if (video.readyState >= 2) { emit('frame'); film.frame(video); meter(); }
      if (video.requestVideoFrameCallback) video.requestVideoFrameCallback(step); else requestAnimationFrame(step);
    };
    step();
    const items = await roll.all();
    const latest = items[0] && (items[0].type.startsWith('image') ? items[0].blob : items[0].thumb);
    if (latest) emit('thumb', URL.createObjectURL(latest));
  };

  /** Sizes the viewfinder element to the exact output frame inside its wrapper. Call from the 'frame' event. */
  let lastLayout = '';
  core.layout = (wrap, vf, padX = 0, padY = 0) => {
    const spec = core.spec();
    const W = wrap.clientWidth - padX * 2, H = wrap.clientHeight - padY * 2;
    let w = W, h = W / spec.outAspect;
    if (h > H) { h = H; w = H * spec.outAspect; }
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const key = [w, h, dpr].join();
    if (key === lastLayout) return null;
    lastLayout = key;
    vf.style.width = w + 'px'; vf.style.height = h + 'px';
    canvas.width = Math.round(w * dpr); canvas.height = Math.round(h * dpr);
    return { spec, w, h };
  };
  core.relayout = () => { lastLayout = ''; };

  /** Draws the raw camera frame into a 2D context (for ambient backdrops). */
  core.drawScene = (g, w, h) => { if (video?.videoWidth) { try { g.drawImage(video, 0, 0, w, h); return true; } catch { /* not ready */ } } return false; };

  // ---------------------------------------------------------------- light meter + histogram (tiny 2D sample of the camera)
  const mc = document.createElement('canvas'); mc.width = 32; mc.height = 24;
  const mg = mc.getContext('2d', { willReadFrequently: true });
  let meterTick = 0;
  core.meterEv = 0; core.hist = new Float32Array(24);
  function meter() {
    if (++meterTick % 6) return;
    try {
      mg.drawImage(video, 0, 0, 32, 24);
      const d = mg.getImageData(0, 0, 32, 24).data, h = new Float32Array(24);
      let sum = 0;
      for (let i = 0; i < d.length; i += 4) {
        const l = (0.2126 * d[i] + 0.7152 * d[i + 1] + 0.0722 * d[i + 2]) / 255;
        sum += l * l; h[Math.min(23, (l * 24) | 0)]++;
      }
      const mean = Math.sqrt(sum / (d.length / 4));
      const ev = Math.log2(Math.max(mean, 0.02) / 0.42) + S.exposure;
      core.meterEv += (ev - core.meterEv) * 0.35;
      const mx = Math.max(...h, 1);
      for (let i = 0; i < 24; i++) core.hist[i] += (h[i] / mx - core.hist[i]) * 0.4;
      emit('meter');
    } catch { /* video not ready */ }
  }

  // ---------------------------------------------------------------- selections
  core.list = (mode = core.mode()) => CAMERAS.filter(c => c.isVideo === (mode === 'video'));
  core.isFav = id => S.favs.includes(id);
  core.toggleFav = id => { S.favs = core.isFav(id) ? S.favs.filter(x => x !== id) : [...S.favs, id]; persist(); haptic(); sync('fav'); };
  core.select = id => {
    const r = byId(id);
    if (!r) return;
    if (S.recording) stopRecording();
    if (r.id === S.recipe.id) return;
    resetMulti();
    S.recipe = r; S.flash = r.flashDefault;
    if (r.isVideo) S.lastVideo = r.id; else S.lastPhoto = r.id;
    // the shutter you set stays across cameras; the slow badge always shows when it is on
    if (r.id === 'dreamblur') { S.shutter = 1 / 4; S.fps = 12; } else S.fps = r.videoFps;
    persist(); core.relayout(); film?.resetAccumulation(); sync('camera'); emit('camera', r);
  };
  core.step = dir => { const l = core.list(), i = l.findIndex(c => c.id === S.recipe.id); core.select(l[(i + dir + l.length) % l.length].id); };
  core.setMode = mode => { if (mode !== core.mode()) { haptic(10); core.select(mode === 'video' ? S.lastVideo : S.lastPhoto); } };
  core.set = (key, value) => {
    if (S[key] === value) return;
    S[key] = value;
    if (key === 'shutter') film?.resetAccumulation();
    if (['grid', 'histogram', 'stampOn', 'borderOn', 'sound', 'audio', 'instantFrame'].includes(key)) persist();
    if (key === 'borderOn') core.relayout();
    sync(key);
  };
  core.toggle = key => core.set(key, !S[key]);
  core.cycleTimer = () => { core.set('timer', S.timer === 0 ? 3 : S.timer === 3 ? 10 : 0); haptic(); };
  core.setRatio = i => { S.ratioIdx[S.recipe.id] = i; resetMulti(); persist(); core.relayout(); sync('ratio'); };
  core.cycleRatio = () => { const n = S.recipe.ratios.length; if (n > 1) { haptic(); core.setRatio((core.ratio().i + 1) % n); } };
  core.setAcc = (k, v) => {
    if (k === 'doubleExposure') resetMulti();
    S.acc[k] = v;
    if (v && k === 'fisheyeW') S.acc.fisheyeC = false;
    if (v && k === 'fisheyeC') S.acc.fisheyeW = false;
    persist(); sync('acc');
  };
  core.setGel = name => { S.acc.gelName = name; if (!S.flash) S.flash = true; sync('acc'); };
  core.setTweak = (key, v) => { const t = core.tweak(); t[key] = v; tweaks[S.recipe.id] = t; localStorage.setItem('funcam_tweaks', JSON.stringify(tweaks)); sync('tweak'); };
  core.resetTweak = () => { delete tweaks[S.recipe.id]; localStorage.setItem('funcam_tweaks', JSON.stringify(tweaks)); sync('tweak'); };
  /** Everything back to automatic. */
  core.auto = () => { S.exposure = 0; S.temp = 0; S.tint = 0; S.shutter = 0; film?.resetAccumulation(); haptic(12); sync('auto'); };
  core.flip = async () => { if (S.recording) return; S.front = !S.front; S.zoom = 1; haptic(10); try { await openCamera(); } catch (e) { say(e.message); } sync('flip'); };

  // ---------------------------------------------------------------- horizon level
  core.enableLevel = async on => {
    if (on) {
      try {
        if (typeof DeviceOrientationEvent !== 'undefined' && DeviceOrientationEvent.requestPermission) {
          if ((await DeviceOrientationEvent.requestPermission()) !== 'granted') { say('Motion access was not allowed'); return; }
        }
        window.addEventListener('deviceorientation', onTilt);
      } catch { say('No motion sensor here'); return; }
    } else window.removeEventListener('deviceorientation', onTilt);
    S.level = on; sync('level');
  };
  function onTilt(e) {
    if (e.gamma == null) return;
    const t = Math.abs(e.beta) > 45 ? e.gamma : 0;
    S.tilt += (t - S.tilt) * 0.3; emit('tilt', S.tilt);
  }

  // ---------------------------------------------------------------- look previews of the scene in front of the lens
  core.snapshotSmall = (max = 220) => {
    if (!video?.videoWidth) return null;
    const k = max / Math.max(video.videoWidth, video.videoHeight), c = document.createElement('canvas');
    c.width = Math.round(video.videoWidth * k); c.height = Math.round(video.videoHeight * k);
    const g = c.getContext('2d');
    if (S.front) { g.translate(c.width, 0); g.scale(-1, 1); }
    g.drawImage(video, 0, 0, c.width, c.height);
    return c;
  };
  /** Renders each camera's look on a small copy of the live scene. onOne(id, canvas) is called as each is ready. */
  let previewRun = 0;
  core.previews = async (ids, onOne, aspect = 1, px = 200) => {
    const run = ++previewRun, src = core.snapshotSmall(px * 1.4);
    if (!src || !film) return;
    for (const id of ids) {
      if (run !== previewRun) return;
      const r = byId(id);
      const P = { ...params(false, r), borderOn: false, stampOn: false, exposure: 0, temp: 0, tint: 0 };
      onOne(id, film.develop(src, P, aspect, 4242, Date.now(), null, 1, px));
      await new Promise(requestAnimationFrame);
    }
  };

  // ---------------------------------------------------------------- capture
  let actx;
  function click() {
    emit('click'); haptic(18);
    if (!S.sound) return;
    try {
      actx ||= new AudioContext();
      const len = actx.sampleRate * 0.06, buf = actx.createBuffer(1, len, actx.sampleRate), d = buf.getChannelData(0);
      for (let i = 0; i < len; i++) d[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / len, 3) * (i < len * 0.3 ? 1 : 0.5);
      const s = actx.createBufferSource(); s.buffer = buf; s.connect(actx.destination); s.start();
    } catch { /* no audio */ }
  }
  async function countdown() {
    if (!S.timer) return;
    S.busy = true;
    for (let i = S.timer; i > 0; i--) { S.countdown = i; haptic(8); sync('count'); await wait(1000); }
    S.countdown = 0; S.busy = false; sync('count');
  }
  function snapshot() {
    const c = document.createElement('canvas'); c.width = video.videoWidth; c.height = video.videoHeight;
    const g = c.getContext('2d');
    if (S.front) { g.translate(c.width, 0); g.scale(-1, 1); }
    g.drawImage(video, 0, 0);
    return c;
  }
  let pending = [], doubleFirst = null;
  function resetMulti() { pending = []; doubleFirst = null; film?.setDouble(null); S.progress = null; }
  function compose(frames, cols, rows, cell, bg = 'rgb(12,11,10)', gapFrac = 0.03, padFrac = 0) {
    const cellH = Math.min(...frames.map(f => f.height), ...frames.map(f => Math.floor(f.width / cell)), Math.floor(2400 / rows));
    const cellW = Math.round(cellH * cell), gap = Math.round(cellH * gapFrac), pad = Math.round(cellH * padFrac);
    const out = document.createElement('canvas');
    out.width = cols * cellW + (cols - 1) * gap + 2 * pad; out.height = rows * cellH + (rows - 1) * gap + 2 * pad;
    const g = out.getContext('2d'); g.fillStyle = bg; g.fillRect(0, 0, out.width, out.height); g.translate(pad, pad);
    frames.forEach((f, i) => {
      const fa = f.width / f.height;
      let sx = 0, sy = 0, sw = f.width, sh = f.height;
      if (fa > cell) { sw = f.height * cell; sx = (f.width - sw) / 2; } else { sh = f.width / cell; sy = (f.height - sh) / 2; }
      g.drawImage(f, sx, sy, sw, sh, (i % cols) * (cellW + gap), Math.floor(i / cols) * (cellH + gap), cellW, cellH);
    });
    return out;
  }
  async function saveStill(out, r) {
    const blob = await toBlob(out), id = Date.now();
    await roll.add({ id, blob, type: 'image/jpeg', name: `FUNCAM_${id}.jpg`, camera: r.name });
    S.shots++; persist();
    emit('shot', { url: URL.createObjectURL(blob), aspect: out.width / out.height, video: false });
  }
  async function shoot() {
    await countdown();
    S.busy = true; sync('busy');
    const r = S.recipe, P = params(S.flash), seed = film.seed, a = core.ratio().v;
    try {
      if (r.capture === 'SINGLE') {
        click();
        let raw;
        if (core.slow()) { film.resetAccumulation(); S.progress = 'EXPOSING'; sync('busy'); await wait(S.shutter * 1000 + 120); raw = film.grabRaw(); S.progress = null; }
        else raw = snapshot();
        if (S.acc.doubleExposure) {
          if (!doubleFirst) { doubleFirst = raw; film.setDouble(raw); S.progress = '1/2'; say('Double exposure: take the second shot'); return; }
          const firstShot = doubleFirst; doubleFirst = null; film.setDouble(null); S.progress = null;
          await saveStill(film.develop(raw, P, a, seed, Date.now(), firstShot, S.zoom), r);
        } else await saveStill(film.develop(raw, P, a, seed, Date.now(), null, S.zoom), r);
      } else if (r.capture === 'HALF' || r.capture === 'COLLAGE') {
        const need = r.capture === 'HALF' ? 2 : 4;
        click(); pending.push(snapshot());
        if (pending.length < need) { S.progress = `${pending.length}/${need}`; return; }
        const comp = need === 2 ? compose(pending, 2, 1, a) : compose(pending, 2, 2, a, 'rgb(246,244,238)', 0.04, 0.06);
        resetMulti();
        await saveStill(film.develop(comp, P, comp.width / comp.height, seed, Date.now()), r);
      } else if (r.capture === 'GRID16') {
        const frames = []; click();
        for (let i = 0; i < 16; i++) { S.progress = `${i + 1}/16`; sync('busy'); frames.push(film.grabRaw()); await wait(110); }
        S.progress = null;
        const comp = compose(frames, 4, 4, a, 'rgb(12,11,10)', 0.02);
        await saveStill(film.develop(comp, P, comp.width / comp.height, seed, Date.now()), r);
      } else if (r.capture === 'WIGGLE') {
        click();
        const frames = [];
        for (let i = 0; i < 4; i++) { frames.push(film.grabRaw()); await wait(90); }
        await saveWiggle(frames.map(f => film.develop(f, P, a, seed, Date.now(), null, 1.08)), r);
      }
    } catch (e) {
      console.error(e); say('Capture failed: ' + e.message);
    } finally {
      S.busy = false; film.seed = newSeed(); sync('busy');
    }
  }
  function pickMime() {
    for (const m of ['video/mp4;codecs=avc1', 'video/mp4', 'video/webm;codecs=vp9', 'video/webm'])
      if (window.MediaRecorder && MediaRecorder.isTypeSupported?.(m)) return m;
    return '';
  }
  async function saveWiggle(frames, r) {
    const c = document.createElement('canvas'); c.width = frames[0].width; c.height = frames[0].height;
    const g = c.getContext('2d'), mime = pickMime();
    const rec = new MediaRecorder(c.captureStream(10), mime ? { mimeType: mime } : undefined);
    const chunks = []; rec.ondataavailable = e => e.data.size && chunks.push(e.data);
    const done = new Promise(res => (rec.onstop = res));
    rec.start();
    const order = [0, 1, 2, 1];
    for (let i = 0; i < 32; i++) { g.drawImage(frames[order[i % 4]], (order[i % 4] - 1) * c.width * 0.02, 0); await wait(100); }
    rec.stop(); await done;
    const blob = new Blob(chunks, { type: mime || 'video/webm' });
    const id = Date.now(), ext = (mime || 'webm').includes('mp4') ? 'mp4' : 'webm', thumb = await toBlob(frames[1]);
    await roll.add({ id, blob, type: blob.type, name: `FUNCAM_${id}.${ext}`, camera: r.name, thumb });
    emit('shot', { url: URL.createObjectURL(thumb), aspect: c.width / c.height, video: true });
  }

  // ---------------------------------------------------------------- video
  let recorder, audioStream;
  async function startRecording() {
    await countdown();
    const r = S.recipe, out = canvas.captureStream(S.fps);
    if (S.audio) {
      try {
        audioStream ||= await navigator.mediaDevices.getUserMedia({ audio: true });
        audioStream.getAudioTracks().forEach(t => out.addTrack(t));
      } catch { say('Recording without sound (microphone blocked)'); }
    }
    const mime = pickMime();
    const rec = new MediaRecorder(out, { ...(mime ? { mimeType: mime } : {}), videoBitsPerSecond: 8_000_000 });
    recorder = rec;
    const chunks = [];
    let posterBlob = null;
    rec.ondataavailable = e => e.data.size && chunks.push(e.data);
    rec.onstop = async () => {
      const blob = new Blob(chunks, { type: (rec.mimeType || mime || 'video/mp4').split(';')[0] });
      const id = Date.now(), ext = blob.type.includes('mp4') ? 'mp4' : 'webm';
      await roll.add({ id, blob, type: blob.type, name: `FUNCAM_${id}.${ext}`, camera: r.name, thumb: posterBlob });
      if (posterBlob) emit('shot', { url: URL.createObjectURL(posterBlob), aspect: canvas.width / canvas.height, video: true });
      S.busy = false; sync('busy');
    };
    requestAnimationFrame(async () => {
      const t = document.createElement('canvas'); t.width = 240; t.height = Math.round(240 * canvas.height / canvas.width);
      t.getContext('2d').drawImage(canvas, 0, 0, t.width, t.height);
      posterBlob = await toBlob(t);
    });
    rec.start(1000);
    S.recording = true; S.recStart = Date.now(); haptic(25); sync('rec');
  }
  function stopRecording() {
    if (!recorder) return;
    S.recording = false; S.busy = true; haptic(25); sync('rec');
    recorder.stop(); recorder = null;
  }
  core.recTime = () => { const s = Math.floor((Date.now() - S.recStart) / 1000); return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`; };
  core.shutter = () => {
    if (!S.started) return;
    if (S.recipe.isVideo) { if (S.recording) stopRecording(); else if (!S.busy) startRecording(); return; }
    if (!S.busy) shoot();
  };
  document.addEventListener('visibilitychange', () => { if (document.hidden && S.recording) stopRecording(); });
  window.addEventListener('resize', core.relayout);

  // ---------------------------------------------------------------- import + roll
  core.importFile = async file => {
    const r = S.recipe;
    if (r.isVideo || !r.canImport) { say(`${r.name} can't develop imported photos. Pick a photo camera.`); return; }
    S.busy = true; S.progress = 'DEVELOPING'; sync('busy');
    try {
      const bmp = await createImageBitmap(file, { imageOrientation: 'from-image' });
      const c = document.createElement('canvas'); c.width = bmp.width; c.height = bmp.height; c.getContext('2d').drawImage(bmp, 0, 0);
      await saveStill(film.develop(c, params(false), core.ratio().v, newSeed(), file.lastModified || Date.now()), r);
      say(`Developed with ${r.name}`);
    } catch (e) { say('Import failed: ' + e.message); }
    S.busy = false; S.progress = null; sync('busy');
  };
  core.roll = roll;
  core.saveToDevice = async it => {
    if (native()) {
      try {
        const { Filesystem, Share } = window.Capacitor.Plugins;
        const data = await new Promise((res, rej) => { const fr = new FileReader(); fr.onload = () => res(String(fr.result).split(',')[1]); fr.onerror = rej; fr.readAsDataURL(it.blob); });
        const res = await Filesystem.writeFile({ path: it.name, data, directory: 'CACHE' });
        await Share.share({ files: [res.uri], title: 'FUN CAM' });
      } catch (e) { if (!/cancel/i.test(String(e?.message || e))) say('Could not save: ' + (e?.message || e)); }
      return;
    }
    const file = new File([it.blob], it.name, { type: it.type });
    if (navigator.canShare?.({ files: [file] })) {
      try { await navigator.share({ files: [file] }); return; } catch (e) { if (e.name === 'AbortError') return; }
    }
    const a = Object.assign(document.createElement('a'), { href: URL.createObjectURL(it.blob), download: it.name });
    document.body.appendChild(a); a.click(); a.remove();
  };
  if (navigator.serviceWorker && location.protocol === 'https:' && !native()) navigator.serviceWorker.register(new URL('./sw.js', import.meta.url)).catch(() => {});
  return core;
}
