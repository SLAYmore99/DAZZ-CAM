// Canvas port of the Android CameraIcon (shaded original camera-body artwork).
function mix(a, b, t) {
  const p = h => [1, 3, 5].map(i => parseInt(h.slice(i, i + 2), 16));
  const x = p(a), y = p(b);
  return `rgb(${x.map((v, i) => Math.round(v + (y[i] - v) * t)).join(',')})`;
}
const toHex = c => c.startsWith('#') ? c : '#' + c.match(/\d+/g).slice(0, 3).map(v => (+v).toString(16).padStart(2, '0')).join('');

export function drawCameraIcon(canvas, r) {
  const g = canvas.getContext('2d'), w = canvas.width, h = canvas.height;
  g.clearRect(0, 0, w, h);
  const body = r.bodyColor, accent = r.accentColor;
  const dark = mix(body, '#000000', 0.5), light = mix(body, '#ffffff', 0.3);
  const rr = (x, y, bw, bh, rad) => { g.beginPath(); g.roundRect(x * w, y * h, bw * w, bh * h, rad * w); };
  const shade = (c, y, bh) => { const hc = toHex(c); const gr = g.createLinearGradient(0, y * h, 0, (y + bh) * h); gr.addColorStop(0, mix(hc, '#ffffff', 0.22)); gr.addColorStop(0.5, hc); gr.addColorStop(1, mix(hc, '#000000', 0.22)); return gr; };
  const box = (x, y, bw, bh, c, rad = 0.06, flat = false) => { rr(x, y, bw, bh, rad); g.fillStyle = flat ? c : shade(c, y, bh); g.fill(); };
  const shell = (x, y, bw, bh, c = body, rad = 0.07) => {
    rr(x + 0.012, y + 0.03, bw, bh, rad); g.fillStyle = 'rgba(0,0,0,0.35)'; g.fill();
    box(x, y, bw, bh, c, rad);
    g.strokeStyle = 'rgba(255,255,255,0.35)'; g.lineWidth = w * 0.01; g.beginPath(); g.moveTo((x + rad) * w, y * h + 1.5); g.lineTo((x + bw - rad) * w, y * h + 1.5); g.stroke();
  };
  const leather = (x, y, bw, bh, c = '#1b1b1b') => {
    box(x, y, bw, bh, c, 0.02); g.fillStyle = 'rgba(255,255,255,0.06)';
    for (let yy = y + 0.02; yy < y + bh - 0.01; yy += 0.05) for (let xx = x + 0.015; xx < x + bw - 0.01; xx += 0.03) { g.beginPath(); g.arc(xx * w, yy * h, w * 0.004, 0, 7); g.fill(); }
  };
  const circle = (cx, cy, rad, fill) => { g.beginPath(); g.arc(cx, cy, rad, 0, Math.PI * 2); g.fillStyle = fill; g.fill(); };
  const lens = (cx, cy, r0, ring = accent) => {
    const x = cx * w, y = cy * h, R = r0 * w;
    circle(x + w * 0.01, y + h * 0.02, R * 1.04, 'rgba(0,0,0,0.35)');
    let gr = g.createLinearGradient(x - R, y - R, x + R, y + R); gr.addColorStop(0, '#4a4a4a'); gr.addColorStop(1, '#121212'); circle(x, y, R, gr);
    g.beginPath(); g.arc(x, y, R * 0.84, 0, 7); g.strokeStyle = ring; g.lineWidth = R * 0.09; g.stroke();
    gr = g.createRadialGradient(x, y, 0, x, y, R * 0.7); gr.addColorStop(0, '#3b5b8a'); gr.addColorStop(0.5, '#0e1626'); gr.addColorStop(1, '#03050a'); circle(x, y, R * 0.68, gr);
    g.beginPath(); g.arc(x, y, R * 0.4, 0, 7); g.strokeStyle = 'rgba(127,166,214,0.25)'; g.lineWidth = R * 0.05; g.stroke();
    circle(x - R * 0.25, y - R * 0.25, R * 0.12, 'rgba(255,255,255,0.75)'); circle(x + R * 0.22, y + R * 0.18, R * 0.06, 'rgba(255,255,255,0.3)');
  };
  const flash = (x, y, fw, fh) => {
    box(x, y, fw, fh, '#d9d9d9', 0.015);
    const gr = g.createLinearGradient(0, y * h, 0, (y + fh) * h); gr.addColorStop(0, '#fff'); gr.addColorStop(1, '#bfc6cc');
    rr(x + 0.008, y + 0.015, fw - 0.016, fh - 0.03, 0.01); g.fillStyle = gr; g.fill();
    g.strokeStyle = '#9fa7ae'; g.lineWidth = w * 0.004;
    for (let lx = x + 0.02; lx < x + fw - 0.01; lx += 0.025) { g.beginPath(); g.moveTo(lx * w, (y + 0.02) * h); g.lineTo(lx * w, (y + fh - 0.02) * h); g.stroke(); }
  };
  const windowR = (x, y, ww, wh) => { const gr = g.createLinearGradient(x * w, y * h, (x + ww) * w, (y + wh) * h); gr.addColorStop(0, '#8fb3d9'); gr.addColorStop(1, '#1e2c3d'); rr(x, y, ww, wh, 0.012); g.fillStyle = gr; g.fill(); };
  const reel = (cx, cy, r0) => {
    const gr = g.createRadialGradient(cx * w, cy * h, 0, cx * w, cy * h, r0 * w); gr.addColorStop(0, light); gr.addColorStop(1, dark);
    circle(cx * w, cy * h, r0 * w, gr); circle(cx * w, cy * h, r0 * w * 0.3, accent);
    for (let i = 0; i < 3; i++) { const a = (i * 120 + 30) * Math.PI / 180; circle(cx * w + Math.cos(a) * r0 * w * 0.6, cy * h + Math.sin(a) * r0 * w * 0.6, r0 * w * 0.22, dark); }
  };
  const poly = (c, ...pts) => { g.fillStyle = c; g.beginPath(); for (let i = 0; i < pts.length; i += 2) i ? g.lineTo(pts[i] * w, pts[i + 1] * h) : g.moveTo(pts[i] * w, pts[i + 1] * h); g.closePath(); g.fill(); };

  switch (r.body) {
    case 'COMPACT': shell(0.05, 0.2, 0.9, 0.62); flash(0.1, 0.3, 0.2, 0.13); windowR(0.71, 0.29, 0.14, 0.1); lens(0.55, 0.56, 0.18); box(0.73, 0.13, 0.13, 0.07, accent, 0.02); break;
    case 'DIGICAM': shell(0.07, 0.22, 0.86, 0.58, body, 0.1); flash(0.12, 0.29, 0.17, 0.1); lens(0.62, 0.52, 0.16); box(0.14, 0.64, 0.22, 0.04, accent, 0.02); box(0.68, 0.15, 0.14, 0.07, dark, 0.02); break;
    case 'SLR': case 'MEDIUM': {
      const big = r.body === 'MEDIUM';
      poly(dark, 0.34, 0.25, 0.41, 0.06, 0.59, 0.06, 0.66, 0.25); shell(0.04, 0.22, 0.92, big ? 0.66 : 0.6);
      leather(0.06, 0.42, 0.88, big ? 0.42 : 0.36); box(0.09, 0.12, 0.12, 0.1, '#9e9e9e', 0.02); box(0.78, 0.13, 0.1, 0.09, accent, 0.02);
      lens(0.5, 0.58, big ? 0.25 : 0.22); break;
    }
    case 'RANGEFINDER': shell(0.04, 0.2, 0.92, 0.62, '#cfcfcf'); leather(0.05, 0.4, 0.9, 0.4, body); windowR(0.1, 0.26, 0.12, 0.08); windowR(0.3, 0.26, 0.08, 0.08); box(0.78, 0.12, 0.1, 0.08, accent, 0.02); lens(0.6, 0.58, 0.19); break;
    case 'DISPOSABLE': shell(0.05, 0.22, 0.9, 0.6); box(0.05, 0.48, 0.9, 0.26, accent, 0); g.fillStyle = 'rgba(255,255,255,0.6)'; g.fillRect(0.05 * w, 0.56 * h, 0.9 * w, 0.03 * h);
      flash(0.1, 0.29, 0.22, 0.14); lens(0.6, 0.43, 0.12); windowR(0.72, 0.27, 0.12, 0.08); circle(0.86 * w, 0.17 * h, w * 0.04, '#2a2a2a'); break;
    case 'HALF': shell(0.12, 0.14, 0.76, 0.72); leather(0.13, 0.4, 0.74, 0.42); windowR(0.17, 0.22, 0.13, 0.09); box(0.68, 0.06, 0.12, 0.08, accent, 0.02); lens(0.5, 0.56, 0.17); break;
    case 'TOY': {
      shell(0.14, 0.12, 0.72, 0.78, body, 0.05); box(0.3, 0.04, 0.4, 0.1, dark, 0.02); circle(0.5 * w, 0.57 * h, 0.29 * w, '#2b2b2b');
      g.strokeStyle = '#444'; g.lineWidth = w * 0.012;
      for (let i = 0; i < 24; i++) { const a = i * 15 * Math.PI / 180; g.beginPath(); g.moveTo(0.5 * w + Math.cos(a) * 0.24 * w, 0.57 * h + Math.sin(a) * 0.24 * w); g.lineTo(0.5 * w + Math.cos(a) * 0.29 * w, 0.57 * h + Math.sin(a) * 0.29 * w); g.stroke(); }
      lens(0.5, 0.57, 0.22); box(0.2, 0.17, 0.1, 0.06, accent, 0.02); break;
    }
    case 'TLR': shell(0.24, 0.08, 0.52, 0.86, body, 0.04); box(0.26, 0.02, 0.48, 0.08, dark, 0.02); leather(0.26, 0.12, 0.48, 0.78); box(0.3, 0.2, 0.4, 0.66, '#2e2e2e', 0.03);
      lens(0.5, 0.34, 0.12, '#bdbdbd'); lens(0.5, 0.68, 0.13); box(0.33, 0.47, 0.34, 0.05, accent, 0.01); break;
    case 'QUAD': case 'MULTI16': {
      shell(0.05, 0.16, 0.9, 0.7);
      const n = r.body === 'QUAD' ? 2 : 4, step = n === 2 ? 0.24 : 0.12, rad = n === 2 ? 0.085 : 0.045;
      const x0 = 0.5 - step * (n - 1) / 2 + 0.06, y0 = 0.51 - step * 1.3 * (n - 1) / 2;
      for (let i = 0; i < n; i++) for (let j = 0; j < n; j++) lens(x0 + i * step, y0 + j * step * 1.3, rad);
      box(0.08, 0.2, 0.1, 0.08, accent, 0.02); break;
    }
    case 'STEREO': shell(0.02, 0.24, 0.96, 0.54); box(0.02, 0.24, 0.96, 0.1, accent, 0.03); for (let i = 0; i < 4; i++) lens(0.2 + i * 0.2, 0.56, 0.075); break;
    case 'PANORAMA': shell(0.02, 0.24, 0.96, 0.54); leather(0.03, 0.44, 0.94, 0.3); circle(0.12 * w, 0.2 * h, 0.06 * w, dark); circle(0.88 * w, 0.2 * h, 0.06 * w, dark); lens(0.5, 0.55, 0.15); box(0.7, 0.17, 0.08, 0.07, accent, 0.02); break;
    case 'INSTANT': case 'INSTANT_SQ': {
      const sq = r.body === 'INSTANT_SQ';
      shell(sq ? 0.16 : 0.1, 0.1, sq ? 0.68 : 0.8, 0.8, body, 0.12);
      if (sq) box(0.16, 0.62, 0.68, 0.28, light, 0.1); else circle(0.47 * w, 0.52 * h, 0.3 * w, mix(toHex(body), '#000000', 0.12));
      lens(sq ? 0.5 : 0.47, sq ? 0.4 : 0.52, sq ? 0.15 : 0.17); flash(sq ? 0.6 : 0.68, 0.17, 0.17, 0.11); windowR(sq ? 0.21 : 0.15, 0.17, 0.12, 0.09);
      box(sq ? 0.24 : 0.18, 0.82, sq ? 0.52 : 0.6, 0.03, '#151515', 0.01); break;
    }
    case 'PHONE':
      shell(0.2, 0.06, 0.6, 0.88, body, 0.12); box(0.25, 0.12, 0.32, 0.4, mix(toHex(body), '#ffffff', 0.08), 0.08);
      lens(0.35, 0.22, 0.065, '#555'); lens(0.35, 0.42, 0.065, '#555'); lens(0.49, 0.32, 0.065, '#555');
      circle(0.49 * w, 0.17 * h, 0.022 * w, '#fff3c4'); circle(0.32 * w, 0.78 * h, 0.07 * w, accent); poly('#fff', 0.305, 0.745, 0.305, 0.815, 0.345, 0.78); break;
    case 'SUPER8': shell(0.22, 0.16, 0.66, 0.46); poly(dark, 0.46, 0.6, 0.62, 0.6, 0.58, 0.96, 0.44, 0.96); box(0.04, 0.24, 0.2, 0.3, '#2b2b2b', 0.02); lens(0.12, 0.39, 0.1); box(0.7, 0.22, 0.12, 0.1, accent, 0.02); break;
    case 'CINE': reel(0.34, 0.24, 0.17); reel(0.66, 0.24, 0.17); shell(0.14, 0.36, 0.72, 0.46); lens(0.3, 0.6, 0.13); box(0.58, 0.48, 0.2, 0.08, accent, 0.02); break;
    case 'VHS': case 'HANDYCAM':
      shell(0.18, 0.3, 0.76, 0.46, body, 0.1); box(0.3, 0.13, 0.5, 0.1, dark, 0.04); box(0.34, 0.18, 0.06, 0.14, dark, 0.02); box(0.72, 0.18, 0.06, 0.14, dark, 0.02);
      box(0.03, 0.37, 0.24, 0.32, '#2b2b2b', 0.06); lens(0.12, 0.53, 0.105); circle(0.86 * w, 0.4 * h, w * 0.025, '#e5383b'); box(0.5, 0.6, 0.32, 0.06, accent, 0.02); break;
    case 'PROJECTOR': reel(0.3, 0.22, 0.16); reel(0.62, 0.22, 0.16); shell(0.1, 0.38, 0.72, 0.42); lens(0.86, 0.58, 0.1);
      box(0.18, 0.82, 0.08, 0.09, dark, 0.02); box(0.66, 0.82, 0.08, 0.09, dark, 0.02); box(0.2, 0.5, 0.32, 0.07, accent, 0.02); break;
  }
}
