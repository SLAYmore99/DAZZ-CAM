// Shared interface parts for the DOT, GLASS and FIELD editions: icons, the ruler dial, sliders, sheets,
// viewfinder gestures and the panels (cameras, lenses, customize, settings, roll). They carry behaviour only;
// every edition styles them with its own CSS.
import { CAMERAS, CAT, GELS, FRAMES, LENSES, TWEAKS, ratioLabel, shutterLabel, fmtEv, haptic } from './core.js';
import { PUBLISHED } from './published.js';

export const $ = (s, el = document) => el.querySelector(s);
export const h = (tag, props = {}, ...kids) => {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(props)) {
    if (k === 'class') el.className = v; else if (k === 'html') el.innerHTML = v; else if (k.startsWith('on')) el.addEventListener(k.slice(2), v);
    else if (k === 'style') el.style.cssText = v; else if (v !== false && v != null) el.setAttribute(k, v);
  }
  for (const kid of kids.flat()) if (kid != null && kid !== false) el.append(kid);
  return el;
};

// ------------------------------------------------------------------ icons (FUN CAM's own line set, 24 px grid)
const I = {
  flash: '<path d="M13 2 5 13.5h6L10 22l9-12.5h-6.2z"/>',
  flashOff: '<path d="M13 2 9.6 6.9M7.4 10 5 13.5h6L10 22l4.6-6.4M16.8 12.6 19 9.5h-6.2L13 8"/><path d="M4 3l16 18"/>',
  timer: '<circle cx="12" cy="13.5" r="7.5"/><path d="M12 9.5v4l2.6 1.6M9.5 2.5h5M12 2.5V6"/>',
  grid: '<rect x="3.5" y="3.5" width="17" height="17" rx="2"/><path d="M9.2 3.5v17M14.8 3.5v17M3.5 9.2h17M3.5 14.8h17"/>',
  flip: '<path d="M4.5 9.5A8 8 0 0 1 19 8M19.5 14.5A8 8 0 0 1 5 16"/><path d="M19.5 3.5V8H15M4.5 20.500V16H9"/>',
  more: '<circle cx="5" cy="12" r="1.3" fill="currentColor"/><circle cx="12" cy="12" r="1.3" fill="currentColor"/><circle cx="19" cy="12" r="1.3" fill="currentColor"/>',
  tune: '<path d="M4 7h9M17 7h3M4 17h3M11 17h9"/><circle cx="15" cy="7" r="2"/><circle cx="9" cy="17" r="2"/>',
  lens: '<circle cx="12" cy="12" r="8.5"/><circle cx="12" cy="12" r="3.5"/><path d="M12 3.500l2.200 5.300M20.500 12l-5.300 2.200M12 20.500l-2.200-5.300M3.500 12l5.300-2.200"/>',
  close: '<path d="M6 6l12 12M18 6 6 18"/>',
  star: '<path d="m12 3.500 2.600 5.400 5.900.8-4.300 4.100 1.050 5.900L12 16.900l-5.250 2.800 1.050-5.900-4.300-4.100 5.900-.8z"/>',
  level: '<path d="M3 12h4.500M16.500 12H21"/><rect x="7.500" y="9" width="9" height="6" rx="3"/>',
  importPhoto: '<rect x="3.500" y="5" width="17" height="14" rx="2.500"/><path d="m3.500 15.500 4.500-4 4 3.500 3-2.500 5.500 4.500"/><circle cx="15.500" cy="9" r="1.300"/>',
  settings: '<circle cx="12" cy="12" r="3"/><path d="M12 2.500v3M12 18.500v3M2.500 12h3M18.500 12h3M5.300 5.300l2.100 2.100M16.600 16.600l2.100 2.100M5.300 18.700l2.100-2.100M16.600 7.400l2.100-2.100"/>',
  chevron: '<path d="m6 15 6-6 6 6"/>',
  photos: '<rect x="6.500" y="3.500" width="14" height="14" rx="2.500"/><path d="M3.500 7.500v10.500a2.500 2.500 0 0 0 2.500 2.500h10.500"/>',
  hist: '<path d="M4 20V13M8 20V8M12 20V4M16 20v-9M20 20v-5"/>',
  ratio: '<rect x="3.500" y="6" width="17" height="12" rx="2"/><path d="M7 9.500v5M17 9.500v5"/>',
  cams: '<rect x="3" y="7" width="18" height="12.500" rx="2.500"/><circle cx="12" cy="13.200" r="3.400"/><path d="M8 7l1.200-2.500h5.600L16 7"/>',
  sun: '<circle cx="12" cy="12" r="3.600"/><path d="M12 2.500v3M12 18.500v3M2.500 12h3M18.500 12h3M5.300 5.300l2.100 2.100M16.600 16.600l2.100 2.100M5.300 18.700l2.100-2.100M16.600 7.400l2.100-2.100"/>',
  temp: '<path d="M10 4.500a2 2 0 0 1 4 0v8.800a4 4 0 1 1-4 0z"/><path d="M12 9v7"/>',
  drop: '<path d="M12 3.500c3.500 4.200 5.500 7 5.500 10a5.500 5.500 0 0 1-11 0c0-3 2-5.800 5.500-10z"/>',
  shutter: '<circle cx="12" cy="12" r="8.500"/><path d="M12 3.500 15 9M20.500 12 15 15M12 20.500 9 15M3.500 12 9 9M9 9h6l0 6H9z"/>',
  fps: '<rect x="3.500" y="5" width="17" height="14" rx="2"/><path d="M7.500 5v14M16.500 5v14M3.500 9.700h4M3.500 14.300h4M16.500 9.700h4M16.500 14.300h4"/>',
  save: '<path d="M12 3.500v11M7.500 10.500 12 15l4.500-4.500M4.500 19.500h15"/>',
  trash: '<path d="M4.500 7h15M9.500 7V4.500h5V7M6.500 7l1 13h9l1-13"/>',
  auto: '<path d="m6.500 18 5.500-13 5.500 13M8.600 13.500h6.800"/>',
};
export const icon = (name, cls = '') => `<svg class="ic ${cls}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">${I[name] || ''}</svg>`;

// ------------------------------------------------------------------ ruler dial (drag left/right, detents, inertia)
/**
 * A horizontal ruler you drag. `items` is a list of {label, value} stops, or use min/max/step for a continuous scale.
 * style.tick: 'dot' | 'line' | 'knurl'.
 */
export class Dial {
  constructor(canvas, style = {}) {
    this.c = canvas; this.g = canvas.getContext('2d');
    this.style = { tick: 'line', color: '#fff', dim: 'rgba(255,255,255,.35)', accent: '#ffcc00', gap: 11, font: '600 10px system-ui', labels: true, ...style };
    this.pos = 0; this.n = 1; this.stops = null; this.onChange = () => {}; this.label = () => '';
    this.major = 5; this.vel = 0; this.last = -1;
    let drag = null;
    canvas.style.touchAction = 'none';
    canvas.addEventListener('pointerdown', e => { canvas.setPointerCapture(e.pointerId); drag = { x: e.clientX, p: this.pos, t: performance.now(), lx: e.clientX }; this.vel = 0; cancelAnimationFrame(this.raf); });
    canvas.addEventListener('pointermove', e => {
      if (!drag) return;
      const now = performance.now();
      this.vel = (drag.lx - e.clientX) / this.style.gap / Math.max(now - drag.t, 1) * 16; drag.lx = e.clientX; drag.t = now;
      this.move(drag.p + (drag.x - e.clientX) / this.style.gap);
    });
    const end = () => { if (!drag) return; drag = null; this.fling(); };
    canvas.addEventListener('pointerup', end); canvas.addEventListener('pointercancel', end);
    new ResizeObserver(() => this.draw()).observe(canvas);
  }
  /** Continuous scale. */
  range(min, max, step, value, label, major = 5) {
    this.stops = null; this.min = min; this.step = step; this.n = Math.round((max - min) / step) + 1; this.major = major;
    this.label = label; this.pos = (value - min) / step; this.last = Math.round(this.pos); this.zero = Math.round((0 - min) / step); this.draw();
  }
  /** Discrete stops: [{label, value}]. */
  list(stops, value) {
    this.stops = stops; this.n = stops.length; this.major = 1; this.zero = 0;
    this.pos = Math.max(0, stops.findIndex(s => s.value === value)); this.last = this.pos; this.draw();
  }
  value(i = Math.round(this.pos)) { return this.stops ? this.stops[i].value : +(this.min + i * this.step).toFixed(4); }
  move(p) {
    this.pos = Math.max(0, Math.min(this.n - 1, p));
    const i = Math.round(this.pos);
    if (i !== this.last) { this.last = i; haptic(this.stops || i % this.major === 0 ? 8 : 3); this.onChange(this.value(i)); }
    this.draw();
  }
  fling() {
    const tick = () => {
      this.vel *= 0.92;
      if (Math.abs(this.vel) > 0.04 && this.pos > 0 && this.pos < this.n - 1) { this.move(this.pos + this.vel); this.raf = requestAnimationFrame(tick); return; }
      const target = Math.round(this.pos), d = target - this.pos;
      if (Math.abs(d) < 0.01) { this.pos = target; this.draw(); return; }
      this.pos += d * 0.3; this.draw(); this.raf = requestAnimationFrame(tick);
    };
    tick();
  }
  draw() {
    const c = this.c, dpr = Math.min(devicePixelRatio || 1, 3), w = c.clientWidth, hh = c.clientHeight;
    if (!w) return;
    if (c.width !== Math.round(w * dpr)) { c.width = Math.round(w * dpr); c.height = Math.round(hh * dpr); }
    const g = this.g, s = this.style;
    g.setTransform(dpr, 0, 0, dpr, 0, 0); g.clearRect(0, 0, w, hh);
    const cx = w / 2, gap = this.stops ? s.gap * 4.4 : s.gap, base = s.tick === 'knurl' ? hh / 2 : hh * 0.36;
    const from = Math.max(0, Math.floor(this.pos - cx / gap) - 1), to = Math.min(this.n - 1, Math.ceil(this.pos + cx / gap) + 1);
    g.textAlign = 'center'; g.textBaseline = 'middle'; g.font = s.font;
    if (s.tick === 'knurl') {
      // a thumbwheel: closely spaced grooves that travel with your finger
      const off = (this.pos * gap) % 6;
      for (let x = -off; x < w; x += 6) {
        const k = Math.cos(((x - cx) / (w / 2)) * 1.35);
        g.fillStyle = s.dim; g.globalAlpha = Math.max(0, k);
        g.fillRect(x, 5, 1.6 * Math.max(0.3, k), hh - 10);
      }
      g.globalAlpha = 1;
    }
    for (let i = from; i <= to; i++) {
      const x = cx + (i - this.pos) * gap, fade = Math.max(0, 1 - Math.abs(x - cx) / (cx * 1.02));
      const big = this.stops || i % this.major === 0, near = Math.abs(i - this.pos) < 0.5;
      g.globalAlpha = fade * (s.tick === 'knurl' ? 0.95 : 1);
      g.fillStyle = near ? s.accent : (i === this.zero && !this.stops ? s.color : big ? s.color : s.dim);
      if (this.stops) {
        g.font = near ? s.fontSel || s.font : s.font;
        g.fillText(this.stops[i].label, x, hh / 2);
      } else if (s.tick === 'dot') {
        const r = big ? 2.1 : 1.2;
        g.beginPath(); g.arc(x, base, near ? r + 0.9 : r, 0, 7); g.fill();
      } else if (s.tick === 'line') {
        const len = big ? hh * 0.34 : hh * 0.2;
        g.fillRect(x - (near ? 1 : 0.6), base - len / 2, near ? 2 : 1.2, len);
      }
      if (!this.stops && big && s.labels && s.tick !== 'knurl') {
        g.fillStyle = s.dim; g.globalAlpha = fade * 0.9;
        g.fillText(this.label(this.value(i), true), x, hh * 0.8);
      }
    }
    g.globalAlpha = 1;
    if (s.tick === 'knurl') { g.fillStyle = s.accent; g.fillRect(cx - 1.2, 2, 2.4, hh - 4); }
    else if (!this.stops) { g.fillStyle = s.accent; g.beginPath(); g.moveTo(cx - 4, 0); g.lineTo(cx + 4, 0); g.lineTo(cx, 5.5); g.fill(); }
  }
}

/** The adjustable values every edition offers on its dial. */
export function dialParams(core) {
  const S = core.S;
  const P = [
    { key: 'exposure', short: 'EV', name: 'Exposure', icon: 'sun', min: -2, max: 2, step: 0.1, fmt: fmtEv, def: 0 },
    { key: 'temp', short: 'WB', name: 'Warmth', icon: 'temp', min: -2, max: 2, step: 0.1, fmt: fmtEv, def: 0 },
    { key: 'tint', short: 'TINT', name: 'Tint', icon: 'drop', min: -2, max: 2, step: 0.1, fmt: fmtEv, def: 0 },
    { key: 'shutter', short: 'S', name: 'Shutter', icon: 'shutter', stops: () => core.SHUTTERS.map(v => ({ value: v, label: shutterLabel(v) })), fmt: shutterLabel, def: 0 },
    { key: 'zoom', short: 'ZOOM', name: 'Zoom', icon: 'lens', min: 1, max: 4, step: 0.1, fmt: v => v.toFixed(1) + '×', def: 1 },
    { key: 'fps', short: 'FPS', name: 'Frame rate', icon: 'fps', video: true, stops: () => core.FPS.map(v => ({ value: v, label: String(v) })), fmt: v => `${v}`, def: null },
  ];
  return P.filter(p => !p.video || S.recipe.isVideo);
}
export function bindDial(dial, core, p) {
  const S = core.S;
  if (p.stops) dial.list(p.stops(), S[p.key]); else dial.range(p.min, p.max, p.step, S[p.key], v => p.fmt(v), p.key === 'zoom' ? 10 : 5);
  dial.onChange = v => core.set(p.key, v);
}

// ------------------------------------------------------------------ slider (drag, double-tap to reset)
export function slider(min, max, value, onInput, def = null) {
  const el = h('div', { class: 'sl' }, h('i', { class: 'sl-fill' }), h('b', { class: 'sl-thumb' }));
  const fill = el.firstChild, thumb = el.lastChild;
  let v = value, lastTap = 0;
  const paint = () => { const k = (v - min) / (max - min); fill.style.width = k * 100 + '%'; thumb.style.left = k * 100 + '%'; el.classList.toggle('at-def', def != null && Math.abs(v - def) < 1e-6); };
  const at = e => { const r = el.getBoundingClientRect(); let k = Math.max(0, Math.min(1, (e.clientX - r.left - 10) / (r.width - 20))); let nv = min + k * (max - min); nv = Math.round(nv * 20) / 20;
    if (def != null && Math.abs(nv - def) < (max - min) * 0.025) nv = def;
    if (nv !== v) { if (nv === def) haptic(8); v = nv; paint(); onInput(v); } };
  el.style.touchAction = 'none';
  el.addEventListener('pointerdown', e => {
    const now = performance.now();
    if (def != null && now - lastTap < 300) { v = def; paint(); onInput(v); haptic(10); return; }
    lastTap = now; el.setPointerCapture(e.pointerId); el.classList.add('drag'); at(e);
  });
  el.addEventListener('pointermove', e => { if (el.hasPointerCapture(e.pointerId)) at(e); });
  const up = () => el.classList.remove('drag');
  el.addEventListener('pointerup', up); el.addEventListener('pointercancel', up);
  el.set = nv => { v = nv; paint(); };
  paint();
  return el;
}

// ------------------------------------------------------------------ sheets (slide up, drag down to close)
let scrim;
export function sheet(id, title, sub) {
  scrim ||= document.body.appendChild(h('div', { class: 'scrim', onclick: closeSheets }));
  const body = h('div', { class: 'sheet-body' });
  const head = h('div', { class: 'sheet-head' }, h('div', { class: 'grab' }),
    h('div', { class: 'sheet-title' }, h('h2', {}, title), h('button', { class: 'x', 'aria-label': 'Close', html: icon('close'), onclick: closeSheets })),
    sub ? h('p', { class: 'sub' }, sub) : null);
  const el = document.body.appendChild(h('div', { class: 'sheet', id }, head, body));
  let y0 = null;
  head.style.touchAction = 'none';
  head.addEventListener('pointerdown', e => { if (e.target.closest('button')) return; y0 = e.clientY; head.setPointerCapture(e.pointerId); el.style.transition = 'none'; });
  head.addEventListener('pointermove', e => { if (y0 != null) el.style.transform = `translateY(${Math.max(0, e.clientY - y0)}px)`; });
  const up = e => { if (y0 == null) return; const d = e.clientY - y0; y0 = null; el.style.transition = ''; el.style.transform = ''; if (d > 90) closeSheets(); };
  head.addEventListener('pointerup', up); head.addEventListener('pointercancel', up);
  el.body = body; el.titleEl = $('h2', head); el.subEl = $('.sub', head);
  return el;
}
export function openSheet(el) { closeSheets(); scrim.classList.add('on'); el.classList.add('open'); document.body.classList.add('sheet-open'); haptic(4); el.onopen?.(); }
export function closeSheets() {
  document.querySelectorAll('.sheet.open').forEach(s => { s.classList.remove('open'); s.onclose?.(); });
  scrim?.classList.remove('on'); document.body.classList.remove('sheet-open');
}

export function toggleRow(title, sub, get, set) {
  const sw = h('span', { class: 'sw' });
  const row = h('button', { class: 'row' }, h('span', { class: 'row-text' }, h('b', {}, title), sub ? h('small', {}, sub) : null), sw);
  const paint = () => { sw.classList.toggle('on', !!get()); row.classList.toggle('on', !!get()); };
  row.onclick = async () => { await set(!get()); haptic(6); paint(); };
  row.paint = paint; paint();
  return row;
}
const pills = (items, isSel, onPick) => {
  const row = h('div', { class: 'pills' });
  const paint = () => [...row.children].forEach((b, i) => b.classList.toggle('sel', isSel(items[i][1])));
  items.forEach(([label, v]) => row.append(h('button', { class: 'pill', onclick: () => { onPick(v); haptic(6); paint(); } }, label)));
  paint();
  return row;
};
const section = t => h('h3', { class: 'sec' }, t);

// ------------------------------------------------------------------ panels
/** Builds every panel once. Returns functions that open them. `links` lists the other editions. */
export function mountPanels(core, edition) {
  const S = core.S;
  document.body.append(h('input', { type: 'file', id: 'file', accept: 'image/*', hidden: '' }));
  $('#file').onchange = e => { const f = e.target.files[0]; e.target.value = ''; if (f) core.importFile(f); };

  // ---- cameras: every look previewed on what the lens sees right now
  const picker = sheet('sPicker', 'Cameras', 'Each one is shown on your scene, live.');
  const tabs = h('div', { class: 'seg' }), cams = h('div', { class: 'cams' });
  picker.body.append(tabs, cams);
  let pickMode = 'photo';
  const renderPicker = () => {
    tabs.replaceChildren(...['photo', 'video'].map(m => h('button', { class: m === pickMode ? 'sel' : '', onclick: () => { pickMode = m; haptic(6); renderPicker(); } }, m === 'photo' ? 'Photo' : 'Video')));
    cams.replaceChildren();
    const list = core.list(pickMode), order = [];
    const groups = [['★ Favourites', list.filter(c => core.isFav(c.id))], ...Object.keys(CAT).map(k => [CAT[k], list.filter(c => c.category === k)])];
    const canv = {};
    for (const [name, items] of groups) {
      if (!items.length) continue;
      cams.append(h('h3', { class: 'sec' }, name, h('span', {}, String(items.length).padStart(2, '0'))));
      const grid = h('div', { class: 'cam-grid' });
      for (const r of items) {
        const pv = h('canvas', { class: 'cam-pv', width: 4, height: 4 });
        (canv[r.id] ||= []).push(pv); if (!order.includes(r.id)) order.push(r.id);
        const fav = h('span', { class: 'fav' + (core.isFav(r.id) ? ' on' : ''), html: icon('star'), role: 'button', 'aria-label': 'Favourite',
          onclick: e => { e.stopPropagation(); core.toggleFav(r.id); renderPicker(); } });
        grid.append(h('button', { class: 'cam' + (r.id === S.recipe.id ? ' sel' : ''), onclick: () => { core.select(r.id); closeSheets(); } },
          h('span', { class: 'cam-img' }, pv, fav, r.v4 ? h('em', { class: 'new' }, 'NEW') : null),
          h('b', {}, r.name), h('small', {}, r.description)));
      }
      cams.append(grid);
    }
    core.previews(order, (id, c) => canv[id].forEach(pv => { pv.width = c.width; pv.height = c.height; pv.getContext('2d').drawImage(c, 0, 0); pv.classList.add('in'); }), 0.8, 220);
  };
  const openPicker = () => { pickMode = core.mode(); renderPicker(); openSheet(picker); cams.parentElement.scrollTop = 0; };

  // ---- lenses and effects
  const lens = sheet('sLens', 'Lenses', 'Stack these on any camera.');
  const tiles = h('div', { class: 'tiles' }), gels = h('div', { class: 'swatches' });
  lens.body.append(tiles, section('Flash colour'), gels);
  const paintLens = () => {
    tiles.replaceChildren(...LENSES.map(([k, name, sub]) => h('button', { class: 'tile' + (S.acc[k] ? ' on' : ''), onclick: () => { core.setAcc(k, !S.acc[k]); haptic(8); paintLens(); } },
      h('i', { class: 'led' }), h('b', {}, name), h('small', {}, sub))));
    gels.replaceChildren(...Object.entries(GELS).map(([n, c]) => h('button', { class: 'swatch' + (S.acc.gelName === n ? ' sel' : ''), 'aria-label': n,
      style: `background:rgb(${c.map(v => v * 255).join(',')})`, onclick: () => { core.setGel(n); haptic(6); paintLens(); } })));
  };
  const openLens = () => { paintLens(); openSheet(lens); };

  // ---- customize this camera
  const tune = sheet('sTune', 'Customize', ' ');
  const openTune = () => {
    const r = S.recipe, t = core.tweak(), b = tune.body;
    tune.titleEl.textContent = r.name; tune.subEl.textContent = r.description;
    b.replaceChildren();
    const sliders = {};
    b.append(h('div', { class: 'sec-row' }, section('Look'), h('button', { class: 'link', onclick: () => { core.resetTweak(); haptic(10); openTune(); } }, 'Reset')));
    for (const [key, name, max] of TWEAKS) {
      const val = h('em', {}, Math.round(t[key] * 100) + '%');
      sliders[key] = slider(0, max, t[key], v => { core.setTweak(key, v); val.textContent = Math.round(v * 100) + '%'; }, 1);
      b.append(h('div', { class: 'tw' }, h('label', {}, name, val), sliders[key]));
    }
    if (r.ratios.length > 1) b.append(section('Frame shape'), pills(r.ratios.map((x, i) => [ratioLabel(x), i]), i => core.ratio().i === i, i => core.setRatio(i)));
    b.append(section('Shutter'), pills([0, 1 / 15, 1 / 8, 1 / 4, 1 / 2, 1].map(v => [shutterLabel(v), v]), v => S.shutter === v, v => core.set('shutter', v)));
    b.append(h('p', { class: 'note' }, 'A slow shutter smears motion into trails. It stays on across cameras until you turn it off.'));
    b.append(toggleRow('Date stamp', 'Burn the date into shots', () => S.stampOn, v => core.set('stampOn', v)),
      toggleRow('Frame', 'Film frame or instant border', () => S.borderOn, v => core.set('borderOn', v)));
    if (r.border === 'INSTANT') b.append(section('Print colour'), h('div', { class: 'swatches' }, ...Object.entries(FRAMES).map(([n, c]) =>
      h('button', { class: 'swatch sq' + (S.instantFrame === n ? ' sel' : ''), style: `background:${c}`, 'aria-label': n, onclick: () => { core.set('instantFrame', n); openTune(); } }))));
    if (!tune.classList.contains('open')) openSheet(tune);
  };

  // ---- settings
  const settings = sheet('sSettings', 'Settings');
  const editions = [['dot', 'DOT'], ['glass', 'GLASS'], ['field', 'FIELD']].filter(([k]) => PUBLISHED.includes(k));
  settings.body.append(
    section('Viewfinder'),
    toggleRow('Grid', 'Rule-of-thirds lines', () => S.grid, v => core.set('grid', v)),
    toggleRow('Level', 'Shows when the horizon is straight', () => S.level, v => core.enableLevel(v)),
    toggleRow('Histogram', 'Live brightness graph', () => S.histogram, v => core.set('histogram', v)),
    section('Capture'),
    toggleRow('Date stamp', 'Burn the date or camcorder overlay into shots', () => S.stampOn, v => core.set('stampOn', v)),
    toggleRow('Frames', 'Instant prints, film edges and slide mounts', () => S.borderOn, v => core.set('borderOn', v)),
    toggleRow('Shutter sound', 'Click when you take a photo', () => S.sound, v => core.set('sound', v)),
    toggleRow('Record audio', 'Microphone sound in videos', () => S.audio, v => core.set('audio', v)),
    h('button', { class: 'row', onclick: () => { closeSheets(); $('#file').click(); } }, h('span', { class: 'row-text' }, h('b', {}, 'Develop a photo'), h('small', {}, 'Pick one from your library and run it through this camera')), h('span', { class: 'go', html: icon('importPhoto') })),
    section('Interface'),
    h('div', { class: 'pills' }, ...editions.map(([k, n]) => h('a', { class: 'pill' + (k === edition ? ' sel' : ''), href: `../${k}/` }, n)), h('a', { class: 'pill', href: '../' }, 'CLASSIC')),
    h('p', { class: 'note' }, 'FUN CAM is free and open source (MIT). 51 cameras, no ads, no tracking. Shots stay on this device until you save them.'),
  );
  const openSettings = () => { settings.body.querySelectorAll('.row').forEach(r => r.paint?.()); openSheet(settings); };

  // ---- roll + viewer
  const rollSheet = sheet('sRoll', 'Roll', 'Tap a shot to view, save or delete it.');
  const rollGrid = h('div', { class: 'roll' }); rollSheet.body.append(rollGrid);
  const media = h('div', { class: 'viewer-media' }), meta = h('div', { class: 'viewer-meta' });
  let viewing = null;
  const closeViewer = () => { viewer.classList.remove('open'); media.replaceChildren(); };
  const viewer = document.body.appendChild(h('div', { class: 'viewer' }, meta, media, h('div', { class: 'viewer-bar' },
    h('button', { onclick: closeViewer }, 'Close'),
    h('button', { class: 'danger', onclick: async () => { await core.roll.remove(viewing.id); closeViewer(); openRoll(); } }, 'Delete'),
    h('button', { class: 'primary', onclick: () => core.saveToDevice(viewing) }, 'Save'))));
  const view = it => {
    viewing = it;
    const url = URL.createObjectURL(it.blob), vid = it.type.startsWith('video');
    media.replaceChildren(vid ? Object.assign(document.createElement('video'), { src: url, controls: true, loop: true, playsInline: true, autoplay: true })
      : Object.assign(document.createElement('img'), { src: url, alt: it.name }));
    const d = new Date(it.id);
    meta.replaceChildren(h('b', {}, it.camera || 'FUN CAM'), h('span', {}, d.toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' }) + ' · ' + d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })));
    viewer.classList.add('open');
  };
  const openRoll = async () => {
    const items = await core.roll.all();
    rollGrid.replaceChildren(...(items.length ? items.map(it => {
      const vid = it.type.startsWith('video'), pic = vid ? it.thumb : it.blob;
      return h('button', { class: 'shot' + (vid ? ' vid' : ''), style: pic ? `background-image:url(${URL.createObjectURL(pic)})` : '', onclick: () => view(it) });
    }) : [h('p', { class: 'note' }, 'Nothing here yet. Take a photo and it lands in this roll.')]));
    if (!rollSheet.classList.contains('open')) openSheet(rollSheet);
  };
  const openLast = async () => { const items = await core.roll.all(); if (items[0]) view(items[0]); else openRoll(); };
  return { openPicker, openLens, openTune, openSettings, openRoll, openLast, importPhoto: () => $('#file').click() };
}

// ------------------------------------------------------------------ viewfinder gestures
/**
 * Tap: focus reticle with an exposure sun. Drag up/down: exposure. Swipe left/right: next/previous camera. Pinch: zoom.
 * The edition supplies the reticle element; this adds/removes classes and positions it.
 */
export function viewfinderGestures(core, vf, reticle) {
  const S = core.S, pts = new Map();
  let start = null, mode = null, hideT, pinch = null;
  const show = (x, y) => {
    reticle.style.left = x + 'px'; reticle.style.top = y + 'px';
    reticle.classList.remove('on'); void reticle.offsetWidth; reticle.classList.add('on');
    clearTimeout(hideT); hideT = setTimeout(() => reticle.classList.remove('on', 'ev'), 2600);
  };
  const paintSun = () => reticle.style.setProperty('--ev', (-S.exposure / 2).toFixed(3));
  core.on('change', paintSun);
  vf.style.touchAction = 'none';
  vf.addEventListener('pointerdown', e => {
    if (e.target.closest('button')) return;
    pts.set(e.pointerId, e); vf.setPointerCapture(e.pointerId);
    if (pts.size === 2) { const [a, b] = [...pts.values()]; pinch = { d: Math.hypot(a.clientX - b.clientX, a.clientY - b.clientY), z: S.zoom }; mode = 'pinch'; return; }
    start = { x: e.clientX, y: e.clientY, ev: S.exposure, t: performance.now() }; mode = null;
  });
  vf.addEventListener('pointermove', e => {
    if (!pts.has(e.pointerId)) return;
    pts.set(e.pointerId, e);
    if (mode === 'pinch' && pts.size === 2) {
      const [a, b] = [...pts.values()], d = Math.hypot(a.clientX - b.clientX, a.clientY - b.clientY);
      core.set('zoom', Math.round(Math.min(4, Math.max(1, pinch.z * d / pinch.d)) * 10) / 10); return;
    }
    if (!start) return;
    const dx = e.clientX - start.x, dy = e.clientY - start.y;
    if (!mode && Math.hypot(dx, dy) > 12) mode = Math.abs(dy) > Math.abs(dx) ? 'ev' : 'swipe';
    if (mode === 'ev') {
      const v = Math.round(Math.max(-2, Math.min(2, start.ev - dy / 110)) * 10) / 10;
      if (v !== S.exposure) { haptic(v === 0 ? 10 : 2); core.set('exposure', v); }
      reticle.classList.add('on', 'ev'); clearTimeout(hideT); hideT = setTimeout(() => reticle.classList.remove('on', 'ev'), 1800);
    }
  });
  const up = e => {
    if (!pts.has(e.pointerId)) return;
    pts.delete(e.pointerId);
    if (mode === 'pinch') { if (pts.size < 2) { mode = 'done'; start = null; } return; }
    if (!start) return;
    const dx = e.clientX - start.x, r = vf.getBoundingClientRect();
    if (!mode && performance.now() - start.t < 400) { show(e.clientX - r.left, e.clientY - r.top); haptic(6); }
    else if (mode === 'swipe' && Math.abs(dx) > 50) core.step(dx < 0 ? 1 : -1);
    start = null; mode = null;
  };
  vf.addEventListener('pointerup', up); vf.addEventListener('pointercancel', up);
  paintSun();
}

/** Rule-of-thirds grid inside the picture area (not over the frame). */
export function drawGrid(svg, spec, w, hh, on, stroke = 'rgba(255,255,255,.4)') {
  svg.style.display = on ? 'block' : 'none';
  if (!on) return;
  svg.setAttribute('viewBox', `0 0 ${w} ${hh}`);
  const [r0, r1, r2, r3] = spec.rect, x0 = r0 * w, x1 = r2 * w, y0 = (1 - r3) * hh, y1 = (1 - r1) * hh;
  let s = '';
  for (let i = 1; i <= 2; i++) { const x = x0 + (x1 - x0) * i / 3, y = y0 + (y1 - y0) * i / 3; s += `<line x1="${x}" y1="${y0}" x2="${x}" y2="${y1}"/><line x1="${x0}" y1="${y}" x2="${x1}" y2="${y}"/>`; }
  svg.innerHTML = `<g stroke="${stroke}" stroke-width="0.8">${s}</g>`;
}

/** A shot's thumbnail flies from the viewfinder into the gallery button. */
export function flyToGallery(url, from, to) {
  const a = from.getBoundingClientRect(), b = to.getBoundingClientRect();
  const el = document.body.appendChild(h('div', { class: 'fly', style: `left:${a.left}px;top:${a.top}px;width:${a.width}px;height:${a.height}px;background-image:url(${url})` }));
  const sx = b.width / a.width, sy = b.height / a.height;
  el.animate([{ transform: 'none', opacity: 1, borderRadius: '12px' },
    { transform: `translate(${b.left - a.left}px,${b.top - a.top}px) scale(${sx},${sy})`, opacity: 0.9, borderRadius: '40px' }],
  { duration: 520, easing: 'cubic-bezier(.5,0,.2,1)' }).onfinish = () => { el.remove(); to.style.backgroundImage = `url(${url})`; to.classList.add('has'); to.animate([{ transform: 'scale(1.18)' }, { transform: 'none' }], { duration: 260, easing: 'ease-out' }); };
}
