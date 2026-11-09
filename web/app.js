import { CAMERAS, SHUTTER_SPEEDS, FPS_VALUES } from './data.js';
import { FilmGL, frameSpec } from './gl.js';
import { drawCameraIcon } from './icons.js';
import * as roll from './roll.js';

const $ = s => document.querySelector(s);
const CAT = { DIGITAL: 'Digital', VIDEO: 'Video', V135: 'Vintage 135', V120: 'Vintage 120', INST: 'Inst' };
const GELS = { WHITE: [1, 1, 1], RED: [1, 0.25, 0.3], BLUE: [0.3, 0.5, 1], GREEN: [0.35, 1, 0.45], YELLOW: [1, 0.85, 0.3], PURPLE: [0.75, 0.35, 1] };
const byId = id => CAMERAS.find(c => c.id === id) || CAMERAS.find(c => c.id === 'funsaver');
const shutterLabel = s => s >= 1 ? `${Math.round(s)}"` : `1/${Math.round(1 / s)}`;

// ------------------------------------------------------------------ state
const saved = JSON.parse(localStorage.getItem('funcam') || '{}');
const S = {
  recipe: byId(saved.recipe), ratioIdx: saved.ratioIdx || {}, flash: false, timer: 0, grid: !!saved.grid,
  exposure: 0, temp: 0, tint: 0, shutter: 0, fps: 30, zoom: 1, front: false,
  stampOn: saved.stampOn ?? true, borderOn: saved.borderOn ?? true, sound: saved.sound ?? true, audio: saved.audio ?? true,
  acc: { fisheye: false, prism: false, star: false, nd: false, doubleExposure: false, lightLeaks: saved.leaks ?? true, gelName: 'WHITE' },
  busy: false, recording: false, panel: null, adjustOpen: false,
};
const persist = () => localStorage.setItem('funcam', JSON.stringify({
  recipe: S.recipe.id, ratioIdx: S.ratioIdx, grid: S.grid, stampOn: S.stampOn, borderOn: S.borderOn,
  sound: S.sound, audio: S.audio, leaks: S.acc.lightLeaks,
}));
const ratio = () => { const r = S.recipe.ratios; const [w, h] = r[Math.min(S.ratioIdx[S.recipe.id] || 0, r.length - 1)]; return { w, h, v: w / h, label: `${w}:${h}` }; };
const params = (flashFired = S.flash && !S.recipe.isVideo, recipe = S.recipe) => ({
  recipe, borderOn: S.borderOn, stampOn: S.stampOn, exposure: S.exposure, temp: S.temp, tint: S.tint, flashFired,
  acc: { ...S.acc, gel: GELS[S.acc.gelName] },
});

let film, video, stream;
const canvas = $('#gl');

// ------------------------------------------------------------------ camera
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

function loop() {
  const step = () => {
    if (video.readyState >= 2) { layout(); film.frame(video); }
    if (video.requestVideoFrameCallback) video.requestVideoFrameCallback(step); else requestAnimationFrame(step);
  };
  step();
}

// ------------------------------------------------------------------ layout / sync
let lastLayout = '';
function layout() {
  const wrap = $('#vfwrap'), vf = $('#vf');
  const spec = frameSpec(S.recipe.border, ratio().v, S.borderOn);
  const W = wrap.clientWidth - 24, H = wrap.clientHeight;
  let w = W, h = W / spec.outAspect;
  if (h > H) { h = H; w = H * spec.outAspect; }
  const dpr = Math.min(window.devicePixelRatio || 1, 2);
  const key = [w, h, dpr, spec.rect.join()].join();
  if (key === lastLayout) return;
  lastLayout = key;
  vf.style.width = w + 'px'; vf.style.height = h + 'px';
  canvas.width = Math.round(w * dpr); canvas.height = Math.round(h * dpr);
  drawGrid(spec, w, h);
}

function drawGrid(spec, w, h) {
  const g = $('#grid');
  g.style.display = S.grid ? 'block' : 'none';
  g.setAttribute('viewBox', `0 0 ${w} ${h}`);
  const [r0, r1, r2, r3] = spec.rect;
  const x0 = r0 * w, x1 = r2 * w, y0 = (1 - r3) * h, y1 = (1 - r1) * h;
  let s = '';
  for (let i = 1; i <= 2; i++) {
    const x = x0 + (x1 - x0) * i / 3, y = y0 + (y1 - y0) * i / 3;
    s += `<line x1="${x}" y1="${y0}" x2="${x}" y2="${y1}"/><line x1="${x0}" y1="${y}" x2="${x1}" y2="${y}"/>`;
  }
  g.innerHTML = `<g stroke="rgba(255,255,255,.35)" stroke-width="1">${s}</g>`;
}

function sync() {
  if (film) {
    film.params = params();
    film.imgAspect = ratio().v;
    film.shutter = S.recipe.manual ? S.shutter : 0;
    film.zoom = S.zoom;
  }
  lastLayout = '';
  $('#bFlash use').setAttribute('href', S.flash ? '#i-flash' : '#i-flashoff');
  $('#bFlash').classList.toggle('on', S.flash);
  $('#bTimer').classList.toggle('on', S.timer > 0);
  $('#bTimer').dataset.t = S.timer;
  $('#bTimer').title = S.timer ? `${S.timer}s` : 'Off';
  const r = ratio();
  $('#bRatio').textContent = r.label;
  $('#bRatio').classList.toggle('fixed', S.recipe.ratios.length < 2);
  $('#bAdjust').classList.toggle('on', S.adjustOpen || !!(S.exposure || S.temp || S.tint));
  $('#bAcc').classList.toggle('on', ['fisheye', 'prism', 'star', 'nd', 'doubleExposure'].some(k => S.acc[k]));
  $('#camName').textContent = S.recipe.name;
  $('#camCat').textContent = '  ·  ' + CAT[S.recipe.category];
  $('#shutter').className = (S.recipe.isVideo ? 'video' : '') + (S.recording ? ' recording' : '') + (S.busy && !S.recording ? ' busy' : '');
  $('#mGrid').textContent = S.grid ? 'Hide grid' : 'Show grid';
  document.querySelectorAll('#strip button').forEach(b => b.classList.toggle('sel', b.dataset.id === S.recipe.id));
  renderZoom();
  renderAdjust();
}

function message(text) {
  const m = $('#msg');
  m.textContent = text; m.style.display = 'block';
  clearTimeout(message.t); message.t = setTimeout(() => (m.style.display = 'none'), 2600);
}

function toast() {
  const t = $('#toast');
  t.querySelector('b').textContent = S.recipe.name.toUpperCase();
  t.querySelector('span').textContent = S.recipe.description;
  t.style.opacity = 1;
  clearTimeout(toast.t); toast.t = setTimeout(() => (t.style.opacity = 0), 1600);
}

// ------------------------------------------------------------------ cameras
function selectRecipe(r) {
  if (S.recording) stopRecording();
  if (r.id === S.recipe.id) return;
  resetMulti();
  S.recipe = r;
  if (r.id === 'dreamblur') { S.shutter = 1 / 4; S.fps = 12; } else { S.shutter = 0; S.fps = r.videoFps; }
  persist(); sync(); toast();
  film?.resetAccumulation();
  const b = document.querySelector(`#strip button[data-id="${r.id}"]`);
  b?.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' });
}

function buildStrip() {
  const strip = $('#strip');
  for (const r of CAMERAS) {
    const b = document.createElement('button');
    b.dataset.id = r.id; b.setAttribute('aria-label', r.name);
    const c = document.createElement('canvas'); c.width = 108; c.height = 78;
    drawCameraIcon(c, r);
    b.appendChild(c);
    b.onclick = () => selectRecipe(r);
    strip.appendChild(b);
  }
}

function buildPicker() {
  const sheet = $('#sPicker'), tabs = sheet.querySelector('.tabs'), cards = sheet.querySelector('.cards');
  let tab = S.recipe.category;
  const render = () => {
    tabs.innerHTML = ''; cards.innerHTML = '';
    for (const [k, label] of Object.entries(CAT)) {
      const t = document.createElement('button'); t.textContent = label; t.className = k === tab ? 'sel' : '';
      t.onclick = () => { tab = k; render(); };
      tabs.appendChild(t);
    }
    for (const r of CAMERAS.filter(c => c.category === tab)) {
      const d = document.createElement('button'); d.className = 'card' + (r.id === S.recipe.id ? ' sel' : '');
      const c = document.createElement('canvas'); c.width = 152; c.height = 110; drawCameraIcon(c, r);
      d.append(c);
      d.insertAdjacentHTML('beforeend', `<b>${r.name}</b><small>${r.description}</small>`);
      d.onclick = () => { selectRecipe(r); closeSheets(); };
      cards.appendChild(d);
    }
  };
  return () => { tab = S.recipe.category; render(); openSheet(sheet); };
}

// ------------------------------------------------------------------ sheets
function openSheet(el) { $('#scrim').style.display = 'block'; requestAnimationFrame(() => el.classList.add('open')); }
function closeSheets() {
  document.querySelectorAll('.sheet.open').forEach(s => s.classList.remove('open'));
  $('#scrim').style.display = 'none'; $('#menu').style.display = 'none';
}

function toggleRow(title, sub, get, set) {
  const row = document.createElement('div'); row.className = 'toggle';
  row.innerHTML = `<div><b>${title}</b><small>${sub}</small></div><span class="sw"></span>`;
  const sw = row.querySelector('.sw');
  const paint = () => sw.classList.toggle('on', !!get());
  row.onclick = () => { set(!get()); paint(); persist(); sync(); };
  paint();
  return row;
}

function buildAcc() {
  const sheet = $('#sAcc'), list = sheet.querySelector('.list'), gels = sheet.querySelector('.gels');
  const items = [
    ['fisheye', 'Fisheye', 'Circular ultra-wide lens'], ['prism', 'Prism', 'Rainbow ghost reflections'],
    ['star', 'Star filter', 'Starbursts on bright lights'], ['nd', 'ND filter', 'Darker exposure for slow shutters in daylight'],
    ['doubleExposure', 'Double exposure', 'Layer two shots in one frame'], ['lightLeaks', 'Light leaks', 'Random warm leaks on film bodies'],
  ];
  for (const [k, t, s] of items) list.appendChild(toggleRow(t, s, () => S.acc[k], v => { if (k === 'doubleExposure') resetMulti(); S.acc[k] = v; }));
  const paintGels = () => gels.querySelectorAll('button').forEach(b => b.classList.toggle('sel', b.dataset.g === S.acc.gelName));
  for (const [name, c] of Object.entries(GELS)) {
    const b = document.createElement('button'); b.dataset.g = name;
    b.style.background = `rgb(${c.map(v => v * 255).join(',')})`;
    b.onclick = () => { S.acc.gelName = name; if (!S.flash) S.flash = true; paintGels(); sync(); };
    gels.appendChild(b);
  }
  paintGels();
}

function buildSettings() {
  const list = $('#sSettings .list');
  list.append(
    toggleRow('Date stamp', 'Burn the date / camcorder overlay into shots', () => S.stampOn, v => (S.stampOn = v)),
    toggleRow('Frames', 'Instant prints, film rebates and slide mounts', () => S.borderOn, v => (S.borderOn = v)),
    toggleRow('Shutter sound', 'Click when you take a photo', () => S.sound, v => (S.sound = v)),
    toggleRow('Record audio', 'Microphone sound in videos', () => S.audio, v => (S.audio = v)),
  );
}

// ------------------------------------------------------------------ adjust panel
function renderAdjust() {
  const box = $('#adjust');
  box.style.display = S.adjustOpen ? 'block' : 'none';
  if (!S.adjustOpen) return;
  const r = S.recipe;
  const chips = [['exposure', `EXP ${fmt(S.exposure)}`], ['temp', `TEMP ${fmt(S.temp)}`], ['tint', `TINT ${fmt(S.tint)}`]];
  if (r.manual) {
    chips.push(['shutter', 'S ' + (S.shutter ? shutterLabel(S.shutter) : 'AUTO')]);
    if (r.isVideo) chips.push(['fps', `${S.fps} FPS`]);
  }
  if (!chips.some(c => c[0] === S.panel)) S.panel = 'exposure';
  const c = box.querySelector('.chips'); c.innerHTML = '';
  for (const [k, label] of chips) {
    const b = document.createElement('button'); b.className = 'chip' + (k === S.panel ? ' sel' : ''); b.textContent = label;
    b.onclick = () => { S.panel = k; renderAdjust(); };
    c.appendChild(b);
  }
  const range = box.querySelector('input'), val = box.querySelector('.val');
  if (['exposure', 'temp', 'tint'].includes(S.panel)) {
    Object.assign(range, { min: -2, max: 2, step: 0.05 }); range.value = S[S.panel]; val.textContent = fmt(S[S.panel]);
    range.oninput = () => { S[S.panel] = +range.value; film.params = params(); val.textContent = fmt(S[S.panel]); c.querySelector('.sel').textContent = c.querySelector('.sel').textContent.replace(/[-+][\d.]+$/, fmt(S[S.panel])); };
    range.onchange = () => sync();
  } else if (S.panel === 'shutter') {
    const opts = [0, ...SHUTTER_SPEEDS];
    Object.assign(range, { min: 0, max: opts.length - 1, step: 1 }); range.value = Math.max(0, opts.indexOf(S.shutter));
    val.textContent = S.shutter ? shutterLabel(S.shutter) : 'AUTO';
    range.oninput = () => { S.shutter = opts[+range.value]; film.resetAccumulation(); sync(); };
  } else if (S.panel === 'fps') {
    Object.assign(range, { min: 0, max: FPS_VALUES.length - 1, step: 1 }); range.value = Math.max(0, FPS_VALUES.indexOf(S.fps));
    val.textContent = `${S.fps}`;
    range.oninput = () => { S.fps = FPS_VALUES[+range.value]; sync(); };
  }
}
const fmt = v => (v >= 0 ? '+' : '') + v.toFixed(1);

function renderZoom() {
  const z = $('#zoom'); z.innerHTML = '';
  z.style.visibility = S.recording ? 'hidden' : 'visible';
  for (const v of [1, 2, 3]) {
    const b = document.createElement('button');
    const sel = Math.abs(S.zoom - v) < 0.05;
    b.className = sel ? 'sel' : ''; b.textContent = sel ? `${v}×` : v;
    b.onclick = () => { S.zoom = v; sync(); };
    z.appendChild(b);
  }
}

// ------------------------------------------------------------------ capture
let ctx;
function click() {
  const blink = $('#blink');
  blink.style.transition = 'none'; blink.style.opacity = 1;
  requestAnimationFrame(() => { blink.style.transition = 'opacity .22s'; blink.style.opacity = 0; });
  navigator.vibrate?.(20);
  if (!S.sound) return;
  try {
    ctx ||= new AudioContext();
    const len = ctx.sampleRate * 0.06, buf = ctx.createBuffer(1, len, ctx.sampleRate), d = buf.getChannelData(0);
    for (let i = 0; i < len; i++) d[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / len, 3) * (i < len * 0.3 ? 1 : 0.5);
    const s = ctx.createBufferSource(); s.buffer = buf; s.connect(ctx.destination); s.start();
  } catch { /* ignore */ }
}

const wait = ms => new Promise(r => setTimeout(r, ms));
async function countdown() {
  if (!S.timer) return;
  S.busy = true; sync();
  for (let i = S.timer; i > 0; i--) { $('#count').textContent = i; await wait(1000); }
  $('#count').textContent = ''; S.busy = false; sync();
}

/** Full-resolution, upright (and mirrored for selfies, like the preview) copy of the current video frame. */
function snapshot() {
  const c = document.createElement('canvas');
  c.width = video.videoWidth; c.height = video.videoHeight;
  const g = c.getContext('2d');
  if (S.front) { g.translate(c.width, 0); g.scale(-1, 1); }
  g.drawImage(video, 0, 0);
  return c;
}

let pending = [], doubleFirst = null;
function resetMulti() { pending = []; doubleFirst = null; film?.setDouble(null); status(null); }
function status(t) { const s = $('#status'); s.textContent = t || ''; s.style.display = t ? 'block' : 'none'; }

function compose(frames, cols, rows, cell) {
  const cellH = Math.min(...frames.map(f => f.height), ...frames.map(f => Math.floor(f.width / cell)), Math.floor(2400 / rows));
  const cellW = Math.round(cellH * cell), gap = Math.round(cellH * 0.03);
  const out = document.createElement('canvas');
  out.width = cols * cellW + (cols - 1) * gap; out.height = rows * cellH + (rows - 1) * gap;
  const g = out.getContext('2d'); g.fillStyle = 'rgb(12,11,10)'; g.fillRect(0, 0, out.width, out.height);
  frames.forEach((f, i) => {
    const fa = f.width / f.height;
    let sx = 0, sy = 0, sw = f.width, sh = f.height;
    if (fa > cell) { sw = f.height * cell; sx = (f.width - sw) / 2; } else { sh = f.width / cell; sy = (f.height - sh) / 2; }
    g.drawImage(f, sx, sy, sw, sh, (i % cols) * (cellW + gap), Math.floor(i / cols) * (cellH + gap), cellW, cellH);
  });
  return out;
}

async function shoot() {
  await countdown();
  S.busy = true; sync();
  const r = S.recipe, P = params(S.flash), seed = film.seed, imgAspect = ratio().v;
  try {
    if (r.capture === 'SINGLE') {
      click();
      let raw;
      if (r.manual && S.shutter > 1 / 30) { film.resetAccumulation(); await wait(S.shutter * 1000 + 120); raw = film.grabRaw(); }
      else raw = snapshot();
      if (S.acc.doubleExposure) {
        if (!doubleFirst) { doubleFirst = raw; film.setDouble(raw); status('1/2'); message('Double exposure: take the second shot'); return; }
        const first = doubleFirst; doubleFirst = null; film.setDouble(null); status(null);
        await save(film.develop(raw, P, imgAspect, seed, Date.now(), first, S.zoom), r);
      } else {
        await save(film.develop(raw, P, imgAspect, seed, Date.now(), null, S.zoom), r);
      }
    } else if (r.capture === 'HALF') {
      click(); pending.push(snapshot());
      if (pending.length < 2) { status('1/2'); return; }
      const comp = compose(pending, 2, 1, imgAspect); resetMulti();
      await save(film.develop(comp, P, comp.width / comp.height, seed, Date.now()), r);
    } else if (r.capture === 'QUAD') {
      const frames = [];
      for (let i = 0; i < 4; i++) { status(`${i + 1}/4`); click(); frames.push(film.grabRaw()); await wait(280); }
      status(null);
      const comp = compose(frames, 2, 2, imgAspect);
      await save(film.develop(comp, P, comp.width / comp.height, seed, Date.now()), r);
    } else if (r.capture === 'WIGGLE') {
      click();
      const frames = [];
      for (let i = 0; i < 3; i++) { frames.push(film.grabRaw()); await wait(110); }
      await saveWiggle(frames.map((f, i) => film.develop(f, P, imgAspect, seed, Date.now(), null, 1.08)), r);
    }
  } catch (e) {
    console.error(e); message('Capture failed: ' + e.message);
  } finally {
    S.busy = false; film.seed = Math.random() * 100; sync();
  }
}

const toBlob = (c, type = 'image/jpeg', q = 0.94) => new Promise(r => c.toBlob(r, type, q));
async function save(canvasOut, r) {
  const blob = await toBlob(canvasOut);
  const id = Date.now();
  await roll.add({ id, blob, type: 'image/jpeg', name: `FUNCAM_${id}.jpg`, camera: r.name });
  setThumb(blob);
}

function pickMime() {
  for (const m of ['video/mp4;codecs=avc1', 'video/mp4', 'video/webm;codecs=vp9', 'video/webm'])
    if (window.MediaRecorder && MediaRecorder.isTypeSupported?.(m)) return m;
  return '';
}

async function saveWiggle(frames, r) {
  // play 0,1,2,1 on a canvas and record it as a looping clip
  const c = document.createElement('canvas'); c.width = frames[0].width; c.height = frames[0].height;
  const g = c.getContext('2d');
  const mime = pickMime();
  const rec = new MediaRecorder(c.captureStream(10), mime ? { mimeType: mime } : undefined);
  const chunks = []; rec.ondataavailable = e => e.data.size && chunks.push(e.data);
  const done = new Promise(res => (rec.onstop = res));
  rec.start();
  const order = [0, 1, 2, 1];
  for (let i = 0; i < 32; i++) {
    const f = frames[order[i % 4]];
    const dx = (order[i % 4] - 1) * c.width * 0.02;
    g.drawImage(f, dx, 0);
    await wait(100);
  }
  rec.stop(); await done;
  const blob = new Blob(chunks, { type: mime || 'video/webm' });
  const id = Date.now(), ext = (mime || 'webm').includes('mp4') ? 'mp4' : 'webm';
  const thumb = await toBlob(frames[1]);
  await roll.add({ id, blob, type: blob.type, name: `FUNCAM_${id}.${ext}`, camera: r.name, thumb });
  setThumb(thumb);
}

// ------------------------------------------------------------------ video
let recorder, recTimer, audioStream;
async function startRecording() {
  await countdown();
  const r = S.recipe, fps = r.manual ? S.fps : r.videoFps;
  const out = canvas.captureStream(fps);
  if (S.audio) {
    try {
      audioStream ||= await navigator.mediaDevices.getUserMedia({ audio: true });
      audioStream.getAudioTracks().forEach(t => out.addTrack(t));
    } catch { message('Recording without sound (microphone blocked)'); }
  }
  const mime = pickMime();
  const rec = new MediaRecorder(out, { ...(mime ? { mimeType: mime } : {}), videoBitsPerSecond: 8_000_000 });
  recorder = rec;
  const chunks = [];
  rec.ondataavailable = e => e.data.size && chunks.push(e.data);
  rec.onstop = async () => {
    const blob = new Blob(chunks, { type: (rec.mimeType || mime || 'video/mp4').split(';')[0] });
    const id = Date.now(), ext = blob.type.includes('mp4') ? 'mp4' : 'webm';
    await roll.add({ id, blob, type: blob.type, name: `FUNCAM_${id}.${ext}`, camera: r.name, thumb: posterBlob });
    setThumb(posterBlob);
    S.busy = false; sync();
  };
  // poster frame for the roll, grabbed as recording starts
  let posterBlob = null;
  requestAnimationFrame(async () => {
    const t = document.createElement('canvas'); t.width = 240; t.height = Math.round(240 * canvas.height / canvas.width);
    t.getContext('2d').drawImage(canvas, 0, 0, t.width, t.height);
    posterBlob = await toBlob(t);
  });
  rec.start(1000);
  S.recording = true; sync();
  const start = Date.now(), recEl = $('#rec');
  recEl.style.display = 'flex';
  recTimer = setInterval(() => {
    const s = Math.floor((Date.now() - start) / 1000);
    recEl.querySelector('span').textContent = `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;
  }, 250);
}

function stopRecording() {
  if (!recorder) return;
  clearInterval(recTimer); $('#rec').style.display = 'none';
  S.recording = false; S.busy = true; sync();
  recorder.stop(); recorder = null;
}

// ------------------------------------------------------------------ roll / viewer
let thumbUrl;
function setThumb(blob) {
  if (thumbUrl) URL.revokeObjectURL(thumbUrl);
  thumbUrl = URL.createObjectURL(blob);
  const t = $('#thumb'); t.style.backgroundImage = `url(${thumbUrl})`; t.querySelector('svg').style.display = 'none';
}

async function openRoll() {
  const grid = $('#sRoll .rollgrid'); grid.innerHTML = '';
  const items = await roll.all();
  if (!items.length) grid.innerHTML = '<p class="sub">No shots yet.</p>';
  for (const it of items) {
    const b = document.createElement('button');
    if (it.type.startsWith('video')) b.classList.add('vid');
    const pic = it.type.startsWith('video') ? it.thumb : it.blob;
    if (pic) b.style.backgroundImage = `url(${URL.createObjectURL(pic)})`;
    b.onclick = () => openViewer(it);
    grid.appendChild(b);
  }
  openSheet($('#sRoll'));
}

let viewing;
function openViewer(it) {
  viewing = it;
  const m = $('#viewer .media'); m.innerHTML = '';
  const url = URL.createObjectURL(it.blob);
  const el = it.type.startsWith('video') ? Object.assign(document.createElement('video'), { src: url, controls: true, loop: true, playsInline: true, autoplay: true, muted: false })
    : Object.assign(document.createElement('img'), { src: url, alt: it.name });
  m.appendChild(el);
  $('#viewer').style.display = 'flex';
}

async function saveViewing() {
  const it = viewing; if (!it) return;
  const file = new File([it.blob], it.name, { type: it.type });
  if (navigator.canShare?.({ files: [file] })) {
    try { await navigator.share({ files: [file] }); return; } catch (e) { if (e.name === 'AbortError') return; }
  }
  const a = Object.assign(document.createElement('a'), { href: URL.createObjectURL(it.blob), download: it.name });
  document.body.appendChild(a); a.click(); a.remove();
}

// ------------------------------------------------------------------ import
async function importPhoto(file) {
  const r = S.recipe;
  if (r.isVideo || !r.canImport) { message(`${r.name} can't develop imported photos — pick a photo camera`); return; }
  S.busy = true; sync();
  try {
    const bmp = await createImageBitmap(file, { imageOrientation: 'from-image' });
    const c = document.createElement('canvas'); c.width = bmp.width; c.height = bmp.height; c.getContext('2d').drawImage(bmp, 0, 0);
    const t = file.lastModified || Date.now();
    await save(film.develop(c, params(false), ratio().v, Math.random() * 100, t), r);
    message(`Developed with ${r.name} — see Your photos`);
  } catch (e) { message('Import failed: ' + e.message); }
  S.busy = false; sync();
}

// ------------------------------------------------------------------ wiring
function wire() {
  $('#bFlash').onclick = () => { S.flash = !S.flash; sync(); };
  $('#bTimer').onclick = () => { S.timer = S.timer === 0 ? 3 : S.timer === 3 ? 10 : 0; sync(); message(S.timer ? `Timer ${S.timer}s` : 'Timer off'); };
  $('#bRatio').onclick = () => {
    if (S.recipe.ratios.length < 2) return;
    S.ratioIdx[S.recipe.id] = ((S.ratioIdx[S.recipe.id] || 0) + 1) % S.recipe.ratios.length;
    resetMulti(); persist(); sync();
  };
  $('#bAdjust').onclick = () => { S.adjustOpen = !S.adjustOpen; sync(); };
  $('#bAcc').onclick = () => openSheet($('#sAcc'));
  $('#bMore').onclick = e => { e.stopPropagation(); const m = $('#menu'); m.style.display = m.style.display === 'block' ? 'none' : 'block'; };
  document.addEventListener('click', e => { if (!e.target.closest('#menu') && e.target.id !== 'bMore') $('#menu').style.display = 'none'; });
  $('#mGrid').onclick = () => { S.grid = !S.grid; persist(); sync(); $('#menu').style.display = 'none'; };
  $('#mImport').onclick = () => { $('#menu').style.display = 'none'; $('#file').click(); };
  $('#mRoll').onclick = () => { $('#menu').style.display = 'none'; openRoll(); };
  $('#mSettings').onclick = () => { $('#menu').style.display = 'none'; openSheet($('#sSettings')); };
  $('#file').onchange = e => { const f = e.target.files[0]; e.target.value = ''; if (f) importPhoto(f); };
  $('#scrim').onclick = closeSheets;
  const openPicker = buildPicker();
  $('#bAll').onclick = openPicker;
  $('#thumb').onclick = openRoll;
  $('#flip').onclick = async () => { if (S.recording) return; S.front = !S.front; S.zoom = 1; await openCamera(); sync(); };
  $('#shutter').onclick = () => {
    if (S.recipe.isVideo) { if (S.recording) stopRecording(); else if (!S.busy) startRecording(); return; }
    if (!S.busy) shoot();
  };
  $('#vClose').onclick = () => { $('#viewer').style.display = 'none'; $('#viewer .media').innerHTML = ''; };
  $('#vSave').onclick = saveViewing;
  $('#vDelete').onclick = async () => { await roll.remove(viewing.id); $('#vClose').onclick(); openRoll(); };
  // pinch to zoom (digital)
  let pinch = null;
  const vf = $('#vf');
  vf.addEventListener('touchstart', e => { if (e.touches.length === 2) pinch = { d: dist(e.touches), z: S.zoom }; }, { passive: true });
  vf.addEventListener('touchmove', e => {
    if (pinch && e.touches.length === 2) { S.zoom = Math.min(4, Math.max(1, pinch.z * dist(e.touches) / pinch.d)); film.zoom = S.zoom; renderZoom(); }
  }, { passive: true });
  vf.addEventListener('touchend', () => (pinch = null));
  window.addEventListener('resize', () => (lastLayout = ''));
  document.addEventListener('visibilitychange', () => { if (document.hidden && S.recording) stopRecording(); });
}
const dist = t => Math.hypot(t[0].clientX - t[1].clientX, t[0].clientY - t[1].clientY);

async function start() {
  $('#startErr').textContent = '';
  try {
    if (!window.isSecureContext) throw new Error('Open this page over https:// so the camera can be used.');
    if (!navigator.mediaDevices?.getUserMedia) throw new Error('This browser has no camera access. On iPhone use Safari.');
    film = new FilmGL(canvas);
    video = Object.assign(document.createElement('video'), { muted: true, playsInline: true, autoplay: true });
    video.setAttribute('playsinline', '');
    await openCamera();
    $('#start').style.display = 'none';
    sync(); toast(); loop();
    const items = await roll.all();
    const latest = items[0] && (items[0].type.startsWith('image') ? items[0].blob : items[0].thumb);
    if (latest) setThumb(latest);
  } catch (e) {
    console.error(e);
    $('#startErr').textContent = e.message || String(e);
  }
}

buildStrip(); buildAcc(); buildSettings(); wire(); sync();
$('#bStart').onclick = start;
if (navigator.serviceWorker && location.protocol === 'https:') navigator.serviceWorker.register('sw.js').catch(() => {});
