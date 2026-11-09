// Canvas port of the Android CameraIcon: original camera-body artwork drawn from primitives.
function mix(a, b, t) {
  const p = h => [1, 3, 5].map(i => parseInt(h.slice(i, i + 2), 16));
  const x = p(a), y = p(b);
  return `rgb(${x.map((v, i) => Math.round(v + (y[i] - v) * t)).join(',')})`;
}

export function drawCameraIcon(canvas, r) {
  const g = canvas.getContext('2d');
  const w = canvas.width, h = canvas.height;
  g.clearRect(0, 0, w, h);
  const body = r.bodyColor, accent = r.accentColor;
  const dark = mix(body, '#000000', 0.55), light = mix(body, '#ffffff', 0.35);
  const box = (x, y, bw, bh, c, rad = 0.08) => {
    g.fillStyle = c; g.beginPath();
    g.roundRect ? g.roundRect(x * w, y * h, bw * w, bh * h, rad * w) : g.rect(x * w, y * h, bw * w, bh * h);
    g.fill();
  };
  const circle = (cx, cy, rad, c, stroke = 0) => {
    g.beginPath(); g.arc(cx, cy, rad, 0, Math.PI * 2);
    if (stroke) { g.strokeStyle = c; g.lineWidth = stroke; g.stroke(); } else { g.fillStyle = c; g.fill(); }
  };
  const lens = (cx, cy, rr) => {
    const x = cx * w, y = cy * h, R = rr * w;
    circle(x, y, R, '#1a1a1a'); circle(x, y, R * 0.86, accent, R * 0.1);
    circle(x, y, R * 0.66, '#0b0f18'); circle(x, y, R * 0.42, '#2b3a55');
    circle(x - R * 0.22, y - R * 0.22, R * 0.13, 'rgba(255,255,255,0.55)');
  };
  const flash = (x, y, fw, fh) => {
    box(x, y, fw, fh, '#e8e8e8', 0.02);
    g.strokeStyle = '#bdbdbd'; g.lineWidth = w * 0.006; g.beginPath();
    g.moveTo(x * w, (y + fh / 2) * h); g.lineTo((x + fw) * w, (y + fh / 2) * h); g.stroke();
  };
  const reel = (cx, cy, rr) => {
    circle(cx * w, cy * h, rr * w, dark); circle(cx * w, cy * h, rr * w * 0.35, accent);
    for (let i = 0; i < 3; i++) {
      const a = (i * 120 + 30) * Math.PI / 180;
      circle(cx * w + Math.cos(a) * rr * w * 0.62, cy * h + Math.sin(a) * rr * w * 0.62, rr * w * 0.22, body);
    }
  };
  const poly = (pts, c) => { g.fillStyle = c; g.beginPath(); pts.forEach(([x, y], i) => i ? g.lineTo(x * w, y * h) : g.moveTo(x * w, y * h)); g.closePath(); g.fill(); };

  switch (r.body) {
    case 'COMPACT':
      box(0.06, 0.2, 0.88, 0.62, body); box(0.06, 0.2, 0.88, 0.1, light, 0.04);
      flash(0.12, 0.36, 0.2, 0.14); box(0.7, 0.33, 0.14, 0.1, '#223344', 0.02);
      lens(0.55, 0.55, 0.17); box(0.74, 0.13, 0.12, 0.08, accent, 0.02); break;
    case 'DIGICAM':
      box(0.08, 0.22, 0.84, 0.58, body, 0.1); flash(0.14, 0.3, 0.16, 0.1); lens(0.62, 0.52, 0.15);
      box(0.16, 0.62, 0.2, 0.04, accent, 0.02); box(0.68, 0.15, 0.14, 0.07, dark, 0.02); break;
    case 'SLR': case 'MANUAL':
      poly([[0.36, 0.24], [0.42, 0.08], [0.58, 0.08], [0.64, 0.24]], dark);
      box(0.05, 0.22, 0.9, 0.6, body); box(0.05, 0.22, 0.9, 0.12, light, 0.04);
      box(0.05, 0.5, 0.9, 0.32, 'rgba(0,0,0,0.3)', 0.06); box(0.1, 0.12, 0.12, 0.1, accent, 0.03); lens(0.5, 0.56, 0.21); break;
    case 'DISPOSABLE':
      box(0.06, 0.22, 0.88, 0.6, body); box(0.06, 0.5, 0.88, 0.22, accent, 0); flash(0.12, 0.3, 0.22, 0.14);
      lens(0.6, 0.44, 0.12); circle(0.86 * w, 0.17 * h, w * 0.035, '#333'); box(0.7, 0.28, 0.12, 0.08, '#223344', 0.02); break;
    case 'HALF':
      box(0.1, 0.16, 0.8, 0.68, body); box(0.1, 0.16, 0.8, 0.12, dark, 0.04); lens(0.5, 0.56, 0.18);
      box(0.16, 0.34, 0.14, 0.09, '#223344', 0.02); box(0.7, 0.34, 0.1, 0.09, accent, 0.02); break;
    case 'TOY': case 'MEDIUM':
      box(0.14, 0.1, 0.72, 0.8, body, 0.06); box(0.14, 0.1, 0.72, 0.14, light, 0.04);
      if (r.body === 'MEDIUM') box(0.3, 0.02, 0.4, 0.1, dark, 0.02);
      lens(0.5, 0.58, r.body === 'TOY' ? 0.25 : 0.22); box(0.2, 0.16, 0.1, 0.06, accent, 0.02); break;
    case 'QUAD':
      box(0.06, 0.18, 0.88, 0.66, body);
      for (let i = 0; i < 2; i++) for (let j = 0; j < 2; j++) lens(0.36 + i * 0.28, 0.4 + j * 0.3, 0.09);
      box(0.08, 0.2, 0.12, 0.08, accent, 0.02); break;
    case 'STEREO':
      box(0.04, 0.24, 0.92, 0.56, body); box(0.04, 0.24, 0.92, 0.1, accent, 0.03);
      for (let i = 0; i < 3; i++) lens(0.26 + i * 0.24, 0.56, 0.09); break;
    case 'INSTANT':
      box(0.1, 0.12, 0.8, 0.76, body, 0.1); box(0.1, 0.64, 0.8, 0.24, light, 0.08); box(0.22, 0.8, 0.56, 0.03, '#111', 0.01);
      g.fillStyle = accent; g.fillRect(0.47 * w, 0.12 * h, 0.06 * w, 0.52 * h);
      lens(0.5, 0.4, 0.16); flash(0.16, 0.18, 0.18, 0.1); box(0.68, 0.18, 0.14, 0.1, '#223344', 0.02); break;
    case 'SUPER8':
      box(0.2, 0.18, 0.66, 0.44, body, 0.06); poly([[0.46, 0.6], [0.62, 0.6], [0.58, 0.95], [0.44, 0.95]], dark);
      box(0.04, 0.26, 0.18, 0.28, dark, 0.02); lens(0.12, 0.4, 0.1); box(0.7, 0.24, 0.12, 0.1, accent, 0.02); break;
    case 'CINE':
      reel(0.34, 0.24, 0.17); reel(0.66, 0.24, 0.17); box(0.16, 0.36, 0.68, 0.44, body, 0.06);
      lens(0.3, 0.6, 0.12); box(0.6, 0.48, 0.18, 0.08, accent, 0.02); break;
    case 'VHS': case 'HANDYCAM':
      box(0.18, 0.3, 0.74, 0.46, body, 0.1); box(0.3, 0.14, 0.5, 0.1, dark, 0.04);
      box(0.34, 0.18, 0.06, 0.14, dark, 0.02); box(0.72, 0.18, 0.06, 0.14, dark, 0.02);
      box(0.04, 0.38, 0.22, 0.3, dark, 0.06); lens(0.12, 0.53, 0.1);
      circle(0.84 * w, 0.4 * h, w * 0.025, '#e5383b'); box(0.5, 0.6, 0.3, 0.06, accent, 0.02); break;
    case 'PROJECTOR':
      reel(0.3, 0.22, 0.16); reel(0.62, 0.22, 0.16); box(0.12, 0.38, 0.7, 0.42, body, 0.06); lens(0.86, 0.58, 0.1);
      box(0.2, 0.82, 0.08, 0.08, dark, 0.02); box(0.66, 0.82, 0.08, 0.08, dark, 0.02); box(0.22, 0.5, 0.3, 0.07, accent, 0.02); break;
  }
}
