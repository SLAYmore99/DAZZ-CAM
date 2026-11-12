// FUN CAM · GLASS — interface logic. Translucent controls over a wash of the live scene.
import { createCore, CAT, shutterLabel, haptic } from '../core.js';
import { $, h, icon, Dial, dialParams, bindDial, mountPanels, viewfinderGestures, drawGrid, flyToGallery } from '../kit.js';

const canvas = $('#gl'), core = createCore(canvas), S = core.S;
const panels = mountPanels(core, 'glass');
$('#bFlip').innerHTML = icon('flip'); $('#bTray').innerHTML = icon('chevron');
$('#island').insertAdjacentHTML('beforeend', icon('tune', 'i-tune'));

// ---- looks strip: every camera rendered on the live scene
const looks = $('#looks');
let stripMode = '', stripFavs = '', settleT, selfScroll = false, lastMid = null;
const order = () => { const l = core.list(); return [...l.filter(c => core.isFav(c.id)), ...l.filter(c => !core.isFav(c.id))]; };
function buildLooks() {
  stripMode = core.mode(); stripFavs = S.favs.join();
  looks.replaceChildren(
    h('button', { class: 'look all', onclick: panels.openPicker }, h('span', { html: icon('cams') }), h('em', {}, 'All')),
    ...order().map(r => h('button', { class: 'look' + (core.isFav(r.id) ? ' fav' : ''), 'data-id': r.id, onclick: () => core.select(r.id) },
      h('span', {}, h('canvas', { width: 4, height: 4 })), h('em', {}, r.name))));
  centre(false); refreshLooks();
}
function refreshLooks() {
  if (!S.started || S.recording || document.hidden) return;
  core.previews(order().map(r => r.id), (id, c) => {
    const pv = looks.querySelector(`[data-id="${id}"] canvas`);
    if (pv) { pv.width = c.width; pv.height = c.height; pv.getContext('2d').drawImage(c, 0, 0); pv.classList.add('in'); }
  }, 1, 120);
}
setInterval(() => { if (!document.body.classList.contains('tray') && !document.body.classList.contains('sheet-open') && !S.busy) refreshLooks(); }, 6000);
function centre(smooth = true) {
  const b = looks.querySelector(`[data-id="${S.recipe.id}"]`);
  if (!b) return;
  selfScroll = true;
  looks.scrollTo({ left: b.offsetLeft + b.offsetWidth / 2 - looks.clientWidth / 2, behavior: smooth ? 'smooth' : 'auto' });
  setTimeout(() => (selfScroll = false), 450);
}
looks.addEventListener('scroll', () => {
  const mid = looks.scrollLeft + looks.clientWidth / 2;
  let best = null, bd = 1e9;
  for (const b of looks.children) { const d = Math.abs(b.offsetLeft + b.offsetWidth / 2 - mid); if (d < bd) { bd = d; best = b; } }
  if (best !== lastMid) { lastMid = best; if (!selfScroll) haptic(4); }
  clearTimeout(settleT);
  if (!selfScroll) settleT = setTimeout(() => { const id = best?.dataset.id; if (id && id !== S.recipe.id) core.select(id); }, 170);
}, { passive: true });

// ---- control tray
const dial = new Dial($('#dial'), { tick: 'line', accent: '#ffd60a', dim: 'rgba(255,255,255,.4)', gap: 10, font: '600 12px -apple-system, system-ui, sans-serif', fontSel: '700 14px -apple-system, system-ui, sans-serif', labels: false });
let pkey = null, plist = [];
const cur = () => plist.find(p => p.key === pkey);
function setParam(k) {
  pkey = pkey === k ? null : k; haptic(6);
  document.body.classList.toggle('dial', !!pkey);
  if (pkey) { bindDial(dial, core, cur()); requestAnimationFrame(() => dial.draw()); }
  paint();
}
function buildTools() {
  plist = dialParams(core);
  if (pkey && !cur()) { pkey = null; document.body.classList.remove('dial'); }
  const tool = (id, ic, label, onclick) => h('button', { class: 'tool', 'data-t': id, onclick }, h('i', { html: ic }), h('em', {}, label));
  $('#tools').replaceChildren(
    ...plist.map(p => tool('p:' + p.key, icon(p.icon), p.name, () => setParam(p.key))),
    tool('timer', icon('timer'), 'Timer', () => core.cycleTimer()),
    tool('ratio', '', 'Shape', () => core.cycleRatio()),
    tool('grid', icon('grid'), 'Grid', () => { haptic(6); core.toggle('grid'); }),
    tool('lens', icon('lens'), 'Lenses', panels.openLens),
    tool('tune', icon('tune'), 'Customize', panels.openTune),
    tool('auto', icon('auto'), 'Auto', () => core.auto()),
    tool('settings', icon('settings'), 'Settings', panels.openSettings),
  );
}

// ---- paint
let shownId = '', islandT;
function paint() {
  const r = S.recipe;
  if (stripMode !== core.mode() || stripFavs !== S.favs.join()) { buildLooks(); buildTools(); }
  if (r.id !== shownId) {
    shownId = r.id;
    const isl = $('#island');
    $('.i-name').textContent = r.name; $('.i-sub').textContent = CAT[r.category];
    isl.classList.add('wide'); clearTimeout(islandT); islandT = setTimeout(() => isl.classList.remove('wide'), 2200);
  }
  $('#island').classList.toggle('rec', S.recording);
  $('#bFlash').innerHTML = icon(S.flash ? 'flash' : 'flashOff'); $('#bFlash').classList.toggle('on', S.flash);
  looks.querySelectorAll('.look').forEach(b => b.classList.toggle('sel', b.dataset.id === r.id));
  const t = id => $(`[data-t="${id}"]`);
  for (const p of plist) { const el = t('p:' + p.key); el.classList.toggle('sel', p.key === pkey); el.classList.toggle('on', p.key !== pkey && p.def != null && S[p.key] !== p.def); }
  t('timer').classList.toggle('on', S.timer > 0); t('timer').lastChild.textContent = S.timer ? `${S.timer} s` : 'Timer';
  t('ratio').firstChild.textContent = core.ratio().label; t('ratio').style.opacity = r.ratios.length > 1 ? 1 : 0.4;
  t('grid').classList.toggle('on', S.grid); t('lens').classList.toggle('on', core.lensOn()); t('tune').classList.toggle('on', core.tuned());
  t('auto').style.opacity = core.dirty() ? 1 : 0.4;
  const p = cur();
  if (p) { $('#dname').textContent = p.name; $('#dval').textContent = p.fmt(S[p.key]); }
  const slow = core.slow();
  $('#slow').classList.toggle('on', slow); $('#slow').innerHTML = slow ? `${icon('shutter')}<span>Slow ${shutterLabel(S.shutter)}</span>${icon('close')}` : '';
  $('#slow').querySelectorAll('.ic').forEach(i => { i.style.width = i.style.height = '14px'; });
  const z = $('#zoom');
  z.style.display = S.recording ? 'none' : 'flex';
  z.replaceChildren(...[1, 2, 3].map(v => {
    const sel = Math.abs(S.zoom - v) < 0.5 && (v < 3 || S.zoom >= 2.5);
    return h('button', { class: sel ? 'sel' : '', onclick: () => { haptic(8); core.set('zoom', v); } }, sel ? `${S.zoom % 1 ? S.zoom.toFixed(1) : S.zoom}×` : String(v));
  }));
  $('#mode').classList.toggle('video', r.isVideo);
  $('#mode').querySelectorAll('button').forEach(b => b.classList.toggle('sel', b.dataset.m === core.mode()));
  $('#shutter').className = [r.isVideo && 'video', S.recording && 'recording', S.busy && !S.recording && !S.countdown && 'busy', slow && 'slow'].filter(Boolean).join(' ');
  const big = $('#big'); big.className = S.countdown ? '' : 'small'; big.textContent = S.countdown || (S.progress ? S.progress[0] + S.progress.slice(1).toLowerCase() : '');
  $('#hist').classList.toggle('on', S.histogram); $('#level').classList.toggle('on', S.level);
  $('#bTray').classList.toggle('on', document.body.classList.contains('tray'));
  $('#bTray').firstChild.style.transform = document.body.classList.contains('tray') ? 'rotate(180deg)' : '';
  $('#bTray').firstChild.style.transition = 'transform .4s cubic-bezier(.3,1.45,.45,1)';
}
core.on('change', reason => {
  if (reason === 'camera') { if (stripMode === core.mode()) centre(); buildTools(); }
  else if (cur() && reason !== pkey && !['tweak', 'acc', 'fav', 'busy', 'count', 'rec'].includes(reason)) bindDial(dial, core, cur());
  paint();
});
core.on('frame', () => {
  const l = core.layout($('#vfwrap'), $('#vf'), 6, 0);
  if (l) drawGrid($('#grid'), l.spec, l.w, l.h, S.grid, 'rgba(255,255,255,.45)'); else if (($('#grid').style.display === 'block') !== S.grid) core.relayout();
  if (S.recording) $('.i-name').textContent = core.recTime();
});
const amb = $('#ambient'), ag = amb.getContext('2d');
core.on('meter', () => {
  if (core.drawScene(ag, 24, 52)) amb.classList.add('on');
  if (!S.histogram) return;
  const c = $('#hist'), g = c.getContext('2d');
  g.clearRect(0, 0, 96, 36); g.fillStyle = 'rgba(255,255,255,.85)';
  g.beginPath(); g.moveTo(4, 32);
  for (let x = 0; x < 24; x++) g.lineTo(4 + x * (88 / 23), 32 - core.hist[x] * 26);
  g.lineTo(92, 32); g.fill();
});
core.on('tilt', t => { const l = $('#level'); l.firstChild.style.transform = `rotate(${-t}deg)`; l.classList.toggle('flat', Math.abs(t) < 1); });
core.on('click', () => { for (const [el, c] of [[$('#blink'), 'go'], [$('#vf'), 'snap']]) { el.classList.remove(c); void el.offsetWidth; el.classList.add(c); } });
core.on('shot', s => { flyToGallery(s.url, $('#vf'), $('#thumb')); });
core.on('thumb', url => { $('#thumb').style.backgroundImage = `url(${url})`; });
let msgT;
core.on('message', t => { const m = $('#msg'); m.textContent = t; m.classList.add('on'); clearTimeout(msgT); msgT = setTimeout(() => m.classList.remove('on'), 2600); });

// ---- wiring
$('#shutter').onclick = () => core.shutter();
$('#bFlip').onclick = () => core.flip();
$('#bFlash').onclick = () => { haptic(8); core.toggle('flash'); };
$('#bTray').onclick = () => { haptic(8); document.body.classList.toggle('tray'); paint(); };
$('#island').onclick = panels.openTune;
$('#mode').querySelectorAll('button').forEach(b => (b.onclick = () => core.setMode(b.dataset.m)));
$('#thumb').onclick = panels.openLast;
$('#slow').onclick = () => { haptic(10); core.set('shutter', 0); };
$('#dval').onclick = () => { const p = cur(); if (p?.def != null) { haptic(10); core.set(p.key, p.def); } };
// swipe up on the dock opens the tray, down closes it
let sy = null;
$('.dock').addEventListener('touchstart', e => (sy = e.touches[0].clientY), { passive: true });
$('.dock').addEventListener('touchend', e => { const d = e.changedTouches[0].clientY - sy; if (Math.abs(d) > 30) { document.body.classList.toggle('tray', d < 0); paint(); } });
viewfinderGestures(core, $('#vf'), $('#reticle'));
buildLooks(); buildTools(); paint();

$('#bStart').onclick = async () => {
  $('#startErr').textContent = ''; $('#bStart').textContent = 'Loading film…';
  try { await core.start(); $('#start').classList.add('gone'); centre(false); refreshLooks(); }
  catch (e) { console.error(e); $('#startErr').textContent = e.message || String(e); $('#bStart').textContent = 'Open Camera'; }
};
