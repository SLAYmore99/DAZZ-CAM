// FUN CAM · DOT — interface logic. Monochrome dot-matrix instrument with one red accent.
import { createCore, CAT, shutterLabel, haptic } from '../core.js';
import { $, h, icon, Dial, dialParams, bindDial, mountPanels, viewfinderGestures, drawGrid, flyToGallery } from '../kit.js';

const canvas = $('#gl'), core = createCore(canvas), S = core.S;
const panels = mountPanels(core, 'dot');
$('#bSettings').innerHTML = icon('settings'); $('#bAll').innerHTML = icon('cams'); $('#bFlip').innerHTML = icon('flip');

// ---- shutter glyph ring: 28 dots that count down, chase while developing and pulse while recording
const ring = $('#ring'), DOTS = 28;
for (let i = 0; i < DOTS; i++) {
  const a = (i / DOTS) * Math.PI * 2 - Math.PI / 2;
  ring.insertAdjacentHTML('beforeend', `<circle cx="${(Math.cos(a) * 48).toFixed(2)}" cy="${(Math.sin(a) * 48).toFixed(2)}" r="1.9"/>`);
}
const dots = [...ring.children];
const sweep = () => dots.forEach((d, i) => setTimeout(() => { d.classList.add('lit'); setTimeout(() => d.classList.remove('lit'), 260); }, i * 9));

// ---- the camera name resolves out of random glyphs, like a split-flap board made of dots
const GLYPHS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
let scrambleT;
function scramble(el, text) {
  clearInterval(scrambleT);
  let n = 0;
  scrambleT = setInterval(() => {
    n += 1;
    el.textContent = [...text].map((ch, i) => (i < n || ch === ' ' ? ch : GLYPHS[(Math.random() * GLYPHS.length) | 0])).join('');
    if (n >= text.length) clearInterval(scrambleT);
  }, 28);
}

// ---- quick row
const quick = [
  ['FLASH', () => S.flash, () => core.toggle('flash'), () => 'FLASH'],
  ['TIMER', () => S.timer > 0, () => core.cycleTimer(), () => (S.timer ? `TIMER ${S.timer}S` : 'TIMER')],
  ['RATIO', () => false, () => core.cycleRatio(), () => core.ratio().label, () => S.recipe.ratios.length < 2],
  ['GRID', () => S.grid, () => core.toggle('grid'), () => 'GRID'],
  ['HIST', () => S.histogram, () => core.toggle('histogram'), () => 'HIST'],
].map(([id, on, act, label, off]) => {
  const b = h('button', { class: 'q', onclick: () => { haptic(6); act(); } });
  $('#quick').append(b);
  return () => { b.textContent = label(); b.classList.toggle('on', on()); b.classList.toggle('off', !!off?.()); };
});

// ---- parameter tabs + ruler
const dial = new Dial($('#dial'), { tick: 'dot', accent: '#e1202a', dim: 'rgba(255,255,255,.3)', gap: 12,
  font: '15px Doto, ui-monospace, monospace', fontSel: '19px Doto, ui-monospace, monospace', labels: false });
let pkey = 'exposure', plist = [];
function buildTabs() {
  plist = dialParams(core);
  if (!plist.some(p => p.key === pkey)) pkey = 'exposure';
  $('#ptabs').replaceChildren(...plist.map(p => h('button', { class: 'pt', 'data-k': p.key, onclick: () => { pkey = p.key; haptic(6); bindDial(dial, core, cur()); paint(); } }, p.short)));
  bindDial(dial, core, cur());
}
const cur = () => plist.find(p => p.key === pkey);

// ---- camera ticker (names, not pictures)
const ticker = $('#ticker');
let tickMode = '', settleT, selfScroll = false;
function buildTicker() {
  tickMode = core.mode();
  ticker.replaceChildren(h('div', { class: 'pad' }), ...core.list().map(r => h('button', { 'data-id': r.id, onclick: () => core.select(r.id) }, r.name)), h('div', { class: 'pad' }));
  centre(false);
}
function centre(smooth = true) {
  const b = ticker.querySelector(`[data-id="${S.recipe.id}"]`);
  if (!b) return;
  selfScroll = true;
  ticker.scrollTo({ left: b.offsetLeft + b.offsetWidth / 2 - ticker.clientWidth / 2, behavior: smooth ? 'smooth' : 'auto' });
  setTimeout(() => (selfScroll = false), 420);
}
let lastMid = null;
ticker.addEventListener('scroll', () => {
  const mid = ticker.scrollLeft + ticker.clientWidth / 2;
  let best = null, bd = 1e9;
  for (const b of ticker.querySelectorAll('button')) { const d = Math.abs(b.offsetLeft + b.offsetWidth / 2 - mid); if (d < bd) { bd = d; best = b; } }
  if (best && best !== lastMid) { lastMid = best; ticker.querySelectorAll('.sel').forEach(x => x.classList.remove('sel')); best.classList.add('sel'); if (!selfScroll) haptic(4); }
  clearTimeout(settleT);
  if (!selfScroll) settleT = setTimeout(() => { if (best && best.dataset.id !== S.recipe.id) core.select(best.dataset.id); }, 160);
}, { passive: true });

// ---- paint
let shownId = '';
function paint() {
  const r = S.recipe, list = core.list(), i = list.findIndex(c => c.id === r.id);
  if (r.id !== shownId) { shownId = r.id; scramble($('#camName'), r.name.toUpperCase()); }
  $('#camMeta').innerHTML = `${CAT[r.category]} · ${String(i + 1).padStart(2, '0')}/${list.length}${core.tuned() ? ' · <b>TUNED</b>' : ''}${core.lensOn() ? ' · <b>LENS</b>' : ''}`;
  quick.forEach(f => f());
  $('#bAuto').classList.toggle('on', core.dirty());
  if (tickMode !== core.mode()) { buildTicker(); buildTabs(); }
  ticker.querySelectorAll('button').forEach(b => { b.classList.toggle('sel', b.dataset.id === r.id); b.classList.toggle('fav', core.isFav(b.dataset.id)); });
  $('#bMode').classList.toggle('video', r.isVideo);
  const p = cur();
  if (p) {
    $('#pval').textContent = p.fmt(S[p.key]);
    document.querySelectorAll('.pt').forEach(t => {
      const q = plist.find(x => x.key === t.dataset.k);
      t.classList.toggle('sel', q.key === pkey); t.classList.toggle('set', q.def != null && S[q.key] !== q.def && q.key !== pkey);
    });
  }
  const slow = core.slow();
  $('#slow').classList.toggle('on', slow); $('#slow').textContent = slow ? `SLOW ${shutterLabel(S.shutter)}  ×` : '';
  $('#zoom').textContent = S.zoom.toFixed(1) + '×'; $('#zoom').style.display = S.recording ? 'none' : 'flex';
  const sh = $('#shutter');
  sh.className = [r.isVideo && 'video', S.recording && 'recording', S.busy && !S.recording && !S.countdown && 'busy spin', slow && 'slow'].filter(Boolean).join(' ');
  const big = $('#big');
  big.className = S.countdown ? '' : 'small'; big.textContent = S.countdown || S.progress || '';
  if (S.countdown) dots.forEach((d, k) => d.classList.toggle('lit', k < DOTS * (1 - (S.countdown - 1) / S.timer)));
  else if (!S.busy) dots.forEach(d => d.classList.remove('lit'));
  $('#rec').classList.toggle('on', S.recording);
  $('#bTune').classList.toggle('on', core.tuned()); $('#bLens').classList.toggle('on', core.lensOn());
  $('#hist').classList.toggle('on', S.histogram); $('#level').classList.toggle('on', S.level);
}
core.on('change', reason => {
  if (reason === 'camera') { if (tickMode === core.mode()) centre(); buildTabs(); }
  else if (!['tweak', 'acc', 'fav', 'busy', 'count', 'rec'].includes(reason) && cur() && reason !== pkey) bindDial(dial, core, cur());
  paint();
});
core.on('frame', () => {
  const l = core.layout($('#vfwrap'), $('#vf'), 12, 0);
  if (l) drawGrid($('#grid'), l.spec, l.w, l.h, S.grid); else if (($('#grid').style.display === 'block') !== S.grid) core.relayout();
  if (S.recording) $('#rec span').textContent = core.recTime();
});
core.on('meter', () => {
  if (!S.histogram) return;
  const c = $('#hist'), g = c.getContext('2d');
  g.clearRect(0, 0, 96, 28);
  for (let x = 0; x < 24; x++) for (let y = 0; y < 7; y++) {
    const lit = core.hist[x] * 7 > y;
    g.fillStyle = lit ? (x > 21 ? '#e1202a' : '#fff') : 'rgba(255,255,255,.16)';
    g.beginPath(); g.arc(2 + x * 4, 26 - y * 4, lit ? 1.3 : 0.8, 0, 7); g.fill();
  }
});
core.on('tilt', t => { const l = $('#level'); l.firstChild.style.transform = `rotate(${-t}deg)`; l.classList.toggle('flat', Math.abs(t) < 1); });
core.on('click', () => { const b = $('#blink'); b.classList.remove('go'); void b.offsetWidth; b.classList.add('go'); sweep(); });
core.on('shot', s => flyToGallery(s.url, $('#vf'), $('#thumb')));
core.on('thumb', url => { $('#thumb').style.backgroundImage = `url(${url})`; $('#thumb').classList.add('has'); });
let msgT;
core.on('message', t => { const m = $('#msg'); m.textContent = t; m.classList.add('on'); clearTimeout(msgT); msgT = setTimeout(() => m.classList.remove('on'), 2600); });

// ---- wiring
$('#shutter').onclick = () => core.shutter();
$('#bFlip').onclick = () => core.flip();
$('#bMode').onclick = () => core.setMode(S.recipe.isVideo ? 'photo' : 'video');
$('#bAll').onclick = panels.openPicker; $('#bTune').onclick = panels.openTune; $('#bLens').onclick = panels.openLens;
$('#bSettings').onclick = panels.openSettings; $('#thumb').onclick = panels.openLast;
$('#bAuto').onclick = () => core.auto();
$('#slow').onclick = () => { haptic(10); core.set('shutter', 0); };
$('#zoom').onclick = () => { haptic(8); core.set('zoom', S.zoom < 1.95 ? 2 : S.zoom < 2.95 ? 3 : 1); };
viewfinderGestures(core, $('#vf'), $('#reticle'));
buildTicker(); buildTabs(); paint();

$('#bStart').onclick = async () => {
  $('#startErr').textContent = ''; $('#bStart').textContent = 'LOADING FILM';
  try { await core.start(); $('#start').classList.add('gone'); centre(false); }
  catch (e) { console.error(e); $('#startErr').textContent = e.message || String(e); $('#bStart').textContent = 'OPEN CAMERA'; }
};
