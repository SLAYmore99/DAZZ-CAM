// FUN CAM · FIELD — interface logic. A pocket instrument: keys, LCD, thumbwheel, light meter, orange shutter.
import { createCore, CAT, shutterLabel, fmtEv, haptic } from '../core.js';
import { $, h, icon, Dial, dialParams, bindDial, mountPanels, viewfinderGestures, drawGrid, flyToGallery, toggleRow } from '../kit.js';
import { drawCameraIcon } from '../icons.js';

const canvas = $('#gl'), core = createCore(canvas), S = core.S;
const panels = mountPanels(core, 'field');
$('#bAll').innerHTML = icon('cams'); $('#bFlip').innerHTML = icon('flip');

// body colour: bone or graphite
const setDark = on => { document.body.classList.toggle('dark', on); localStorage.setItem('field_dark', on ? '1' : ''); document.querySelector('meta[name=theme-color]').content = on ? '#232427' : '#e9e6df'; };
setDark(!!localStorage.getItem('field_dark'));
$('#sSettings .sheet-body .pills').before(toggleRow('Graphite body', 'Dark body for shooting at night', () => document.body.classList.contains('dark'), v => { setDark(v); dial.style.dim = '#bbb'; }));

// light-meter scale
const mt = $('#mticks');
for (let i = -4; i <= 4; i++) {
  const a = (i * 14 - 90) * Math.PI / 180, r1 = 40, r2 = i % 2 ? 36.5 : 34;
  mt.insertAdjacentHTML('beforeend', `<line x1="${50 + Math.cos(a) * r1}" y1="${56 + Math.sin(a) * r1}" x2="${50 + Math.cos(a) * r2}" y2="${56 + Math.sin(a) * r2}"/>`);
}

// ---- key row
const keyDefs = [
  ['FLASH', () => S.flash, () => core.toggle('flash')],
  ['TIMER', () => S.timer > 0, () => core.cycleTimer(), () => (S.timer ? `${S.timer} SEC` : 'TIMER')],
  ['SHAPE', () => false, () => core.cycleRatio(), () => core.ratio().label, () => S.recipe.ratios.length < 2],
  ['LENS', () => core.lensOn(), () => panels.openLens()],
  ['TUNE', () => core.tuned(), () => panels.openTune()],
  ['MENU', () => false, () => panels.openSettings()],
].map(([name, on, act, label, off]) => {
  const text = h('span', {}, name), b = h('button', { class: 'key', onclick: () => { haptic(10); act(); } }, h('i', { class: 'led' }), text);
  $('#keys').append(b);
  return () => { text.textContent = label ? label() : name; b.classList.toggle('on', on()); b.classList.toggle('off', !!off?.()); };
});

// ---- thumbwheel
const dial = new Dial($('#dial'), { tick: 'knurl', accent: '#ff5a1f', dim: '#bbb', gap: 12, font: '700 11px ui-monospace, monospace', fontSel: '700 13px ui-monospace, monospace', color: '#eee', labels: false });
let pkey = 'exposure', plist = [];
const cur = () => plist.find(p => p.key === pkey);
function buildTabs() {
  plist = dialParams(core);
  if (!cur()) pkey = 'exposure';
  $('#ptabs').replaceChildren(...plist.map(p => h('button', { class: 'pt', 'data-k': p.key, onclick: () => { pkey = p.key; haptic(8); bindDial(dial, core, cur()); paint(); } }, p.short)));
  bindDial(dial, core, cur());
}

// ---- camera drum (FUN CAM's own drawn camera bodies)
const drum = $('#drum');
let drumMode = '', settleT, selfScroll = false, lastMid = null;
function buildDrum() {
  drumMode = core.mode();
  drum.replaceChildren(h('div', { class: 'pad' }), ...core.list().map(r => {
    const c = h('canvas', { width: 116, height: 84 }); drawCameraIcon(c, r);
    return h('button', { 'data-id': r.id, 'aria-label': r.name, onclick: () => core.select(r.id) }, c);
  }), h('div', { class: 'pad' }));
  centre(false);
}
function centre(smooth = true) {
  const b = drum.querySelector(`[data-id="${S.recipe.id}"]`);
  if (!b) return;
  selfScroll = true;
  drum.scrollTo({ left: b.offsetLeft + b.offsetWidth / 2 - drum.clientWidth / 2, behavior: smooth ? 'smooth' : 'auto' });
  setTimeout(() => (selfScroll = false), 420);
}
drum.addEventListener('scroll', () => {
  const mid = drum.scrollLeft + drum.clientWidth / 2;
  let best = null, bd = 1e9;
  for (const b of drum.querySelectorAll('button')) { const d = Math.abs(b.offsetLeft + b.offsetWidth / 2 - mid); if (d < bd) { bd = d; best = b; } }
  if (best && best !== lastMid) {
    lastMid = best; drum.querySelectorAll('.sel').forEach(x => x.classList.remove('sel')); best.classList.add('sel');
    if (!selfScroll) { haptic(7); $('#camName').textContent = core.CAMERAS.find(c => c.id === best.dataset.id).name; }
  }
  clearTimeout(settleT);
  if (!selfScroll) settleT = setTimeout(() => { if (best && best.dataset.id !== S.recipe.id) core.select(best.dataset.id); }, 160);
}, { passive: true });

// ---- paint
function paint() {
  const r = S.recipe;
  if (drumMode !== core.mode()) { buildDrum(); buildTabs(); }
  $('#camName').textContent = r.name;
  $('#count').textContent = String((S.shots % 36) + 1).padStart(2, '0');
  $('#lcdInfo').textContent = [`EV ${fmtEv(S.exposure)}`, `S ${shutterLabel(S.shutter)}`, core.ratio().label, r.isVideo ? `${S.fps} FPS` : CAT[r.category].toUpperCase(), S.flash ? 'FLASH' : ''].filter(Boolean).join(' · ');
  keyDefs.forEach(f => f());
  drum.querySelectorAll('button').forEach(b => b.classList.toggle('sel', b.dataset.id === r.id));
  $('#bMode').classList.toggle('video', r.isVideo);
  const p = cur();
  if (p) {
    $('#pval').textContent = p.fmt(S[p.key]);
    document.querySelectorAll('.pt').forEach(t => { const q = plist.find(x => x.key === t.dataset.k); t.classList.toggle('sel', q.key === pkey); t.classList.toggle('set', q.def != null && S[q.key] !== q.def); });
  }
  const slow = core.slow();
  $('#slow').classList.toggle('on', slow); $('#slow').textContent = slow ? `SLOW ${shutterLabel(S.shutter)} ✕` : '';
  $('.well').classList.toggle('slow', slow);
  $('#shutter').className = [r.isVideo && 'video', S.recording && 'recording', S.busy && !S.recording && !S.countdown && 'busy'].filter(Boolean).join(' ');
  const big = $('#big'); big.className = S.countdown ? '' : 'small'; big.textContent = S.countdown || S.progress || '';
  $('#rec').classList.toggle('on', S.recording); $('#level').classList.toggle('on', S.level);
}
core.on('change', reason => {
  if (reason === 'camera') { if (drumMode === core.mode()) centre(); buildTabs(); }
  else if (cur() && reason !== pkey && !['tweak', 'acc', 'fav', 'busy', 'count', 'rec'].includes(reason)) bindDial(dial, core, cur());
  paint();
});
core.on('frame', () => {
  const l = core.layout($('#vfwrap'), $('#vf'), 19, 7);
  if (l) drawGrid($('#grid'), l.spec, l.w, l.h, S.grid); else if (($('#grid').style.display === 'block') !== S.grid) core.relayout();
  if (S.recording) $('#rec span').textContent = core.recTime();
});
core.on('meter', () => { $('#needle').style.transform = `rotate(${Math.max(-2.2, Math.min(2.2, core.meterEv)) * 28}deg)`; });
core.on('tilt', t => { const l = $('#level'); l.firstChild.style.transform = `rotate(${-t}deg)`; l.classList.toggle('flat', Math.abs(t) < 1); });
core.on('click', () => { const c = $('#curtain'); c.classList.remove('go'); void c.offsetWidth; c.classList.add('go'); });
const setPrint = url => { const t = $('#thumb'); t.firstChild.style.backgroundImage = `url(${url})`; t.classList.add('has'); t.classList.remove('print'); void t.offsetWidth; t.classList.add('print'); };
core.on('shot', s => setPrint(s.url));   // the print slides out of the slot
core.on('thumb', url => { $('#thumb i').style.backgroundImage = `url(${url})`; $('#thumb').classList.add('has'); });
let msgT;
core.on('message', t => { const m = $('#msg'); m.textContent = t; m.classList.add('on'); clearTimeout(msgT); msgT = setTimeout(() => m.classList.remove('on'), 2600); });

// ---- wiring
$('#shutter').onclick = () => core.shutter();
$('#bFlip').onclick = () => core.flip();
$('#bMode').onclick = () => core.setMode(S.recipe.isVideo ? 'photo' : 'video');
$('#bAll').onclick = panels.openPicker; $('#lcd').onclick = panels.openTune; $('#thumb').onclick = panels.openLast;
$('#pval').onclick = () => { const p = cur(); if (p?.def != null) { haptic(12); core.set(p.key, p.def); } };
$('#slow').onclick = () => { haptic(10); core.set('shutter', 0); };
viewfinderGestures(core, $('#vf'), $('#reticle'));
buildDrum(); buildTabs(); paint();

$('#bStart').onclick = async () => {
  $('#startErr').textContent = ''; $('#bStart').textContent = 'Loading film';
  try { await core.start(); $('#start').classList.add('gone'); centre(false); dial.draw(); }
  catch (e) { console.error(e); $('#startErr').textContent = e.message || String(e); $('#bStart').textContent = 'Open camera'; }
};
