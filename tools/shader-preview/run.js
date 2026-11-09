const { chromium } = require('playwright');
const fs = require('fs');
const data = JSON.parse(fs.readFileSync(process.argv[2]));
(async () => {
  const browser = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader'] });
  const page = await browser.newPage({ viewport: { width: 1400, height: 1000 } });
  page.on('console', m => console.log('PAGE:', m.text()));
  await page.setContent('<html><body style="margin:0;background:#111"><div id="out" style="display:flex;flex-wrap:wrap;gap:6px;padding:6px"></div></body></html>');
  const res = await page.evaluate((data) => {
    const errors = [];
    const D = { exposure:0, contrast:1, saturation:1, temp:0, tint:0, fade:0, gamma:1, lift:[0,0,0], gain:[1,1,1],
      shadowTint:[0,0,0], highTint:[0,0,0], mono:0, cross:0, halation:0, halationColor:[1,0.25,0.1], bloom:0, grain:0,
      grainSize:1, vignette:0, leak:0, leakColor:[1,0.35,0.1], chroma:0, soft:0, sharpen:0, pixel:0, scan:0, vhs:0,
      weave:0, flicker:0, dust:0, flash:0.7 };
    const RATIOS = { r34:3/4, r11:1, r23:2/3, r916:9/16, r43:4/3, r169:16/9, r32:3/2 };
    const ratioVal = r => RATIOS[r] ?? (() => { const m = r.match(/Ratio\((\d+), (\d+)\)/); return m[1]/m[2]; })();
    const BORDER = { NONE:0, INSTANT:1, FILM:2, SLIDE:3, SPROCKET:4 };
    const BCOL = { NONE:[0,0,0], INSTANT:[0.95,0.94,0.91], FILM:[0.07,0.06,0.055], SLIDE:[0.93,0.91,0.86], SPROCKET:[0.07,0.06,0.055] };
    function spec(border, ia) {
      const m = { NONE:[0,0,0], INSTANT:[0.06,0.07,0.26], FILM:[0.05,0.05,0.05], SLIDE:[0.16,0,0], SPROCKET:[0.03,0.12,0.12] }[border];
      if (border === 'SLIDE') { const ww=1, wh=1/ia, s=Math.max(ww,wh)*1.36, x0=(s-ww)/2/s, y0=(s-wh)/2/s; return {out:1, rect:[x0,y0,1-x0,1-y0]}; }
      const [side, top, bottom] = m; const w = 1+2*side, h = 1/ia+top+bottom;
      return { out: w/h, rect: [side/w, bottom/h, (side+1)/w, (bottom+1/ia)/h] };
    }
    function crop(src, dst) { if (dst < src) { const s = dst/src; return [(1-s)/2,0,s,1]; } const s = src/dst; return [0,(1-s)/2,1,s]; }

    // synthetic test scene
    const sc = document.createElement('canvas'); sc.width = 900; sc.height = 1200;
    const g = sc.getContext('2d');
    let gr = g.createLinearGradient(0,0,0,700); gr.addColorStop(0,'#3a78c8'); gr.addColorStop(1,'#bfd8ee');
    g.fillStyle = gr; g.fillRect(0,0,900,700);
    g.fillStyle = '#fffbe8'; g.beginPath(); g.arc(680,220,70,0,7); g.fill();
    g.fillStyle = '#4f7a3a'; g.fillRect(0,700,900,500);
    g.fillStyle = '#c0392b'; g.fillRect(80,560,220,300);
    g.fillStyle = '#f1c40f'; g.fillRect(330,620,160,240);
    g.fillStyle = '#e0ac8a'; g.beginPath(); g.arc(640,700,120,0,7); g.fill();
    g.fillStyle = '#222'; g.fillRect(560,820,160,300);
    for (let i=0;i<9;i++){ g.fillStyle = `hsl(${i*40},70%,50%)`; g.fillRect(i*100,1110,100,90); }
    g.fillStyle='#fff'; g.font='bold 60px sans-serif'; g.fillText('FUN CAM', 260, 1060);

    const cv = document.createElement('canvas');
    const gl = cv.getContext('webgl2', { preserveDrawingBuffer: true });
    if (!gl) return { errors: ['no webgl2'] };
    function compile(type, src) {
      const s = gl.createShader(type); gl.shaderSource(s, src); gl.compileShader(s);
      if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) errors.push(gl.getShaderInfoLog(s));
      return s;
    }
    function prog(vs, fs) {
      const p = gl.createProgram(); gl.attachShader(p, compile(gl.VERTEX_SHADER, vs)); gl.attachShader(p, compile(gl.FRAGMENT_SHADER, fs));
      gl.bindAttribLocation(p, 0, 'aPos'); gl.linkProgram(p);
      if (!gl.getProgramParameter(p, gl.LINK_STATUS)) errors.push('link: ' + gl.getProgramInfoLog(p));
      return p;
    }
    const S = data.shaders;
    const input = S.INPUT.replace('#extension GL_OES_EGL_image_external_essl3 : require\n','').replace('samplerExternalOES','sampler2D');
    prog(S.VERTEX, input); prog(S.VERTEX, S.COPY);
    const film = prog(S.VERTEX, S.FILM);
    if (errors.length) return { errors };
    const buf = gl.createBuffer(); gl.bindBuffer(gl.ARRAY_BUFFER, buf);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1,-1,1,-1,-1,1,1,1]), gl.STATIC_DRAW);
    gl.enableVertexAttribArray(0); gl.vertexAttribPointer(0,2,gl.FLOAT,false,0,0);
    function tex(img) {
      const t = gl.createTexture(); gl.bindTexture(gl.TEXTURE_2D, t);
      gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, true);
      gl.texImage2D(gl.TEXTURE_2D,0,gl.RGBA,gl.RGBA,gl.UNSIGNED_BYTE,img);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR); gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE); gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
      return t;
    }
    const srcTex = tex(sc);
    // date stamp overlay like StampRenderer FILM_ORANGE
    function stamp(ia) {
      const c = document.createElement('canvas'); c.width = 1080; c.height = Math.round(1080/ia);
      const x = c.getContext('2d'); const unit = Math.min(c.width, c.height);
      x.font = `bold ${unit*0.058}px monospace`; x.fillStyle = 'rgb(255,156,52)'; x.shadowColor='rgba(255,90,0,0.86)'; x.shadowBlur = unit*0.03;
      const t = "'26  11  9"; const tw = x.measureText(t).width; x.fillText(t, c.width - tw - unit*0.07, c.height - unit*0.07);
      return tex(c);
    }
    const blank = tex(document.createElement('canvas'));
    const out = document.getElementById('out');
    const U = n => gl.getUniformLocation(film, n);
    function render(cam, acc, label, flash) {
      const ia = ratioVal(cam.ratio); const sp = spec(cam.border, ia);
      const H = 420; const W = Math.round(H * sp.out);
      cv.width = W; cv.height = H; gl.viewport(0,0,W,H);
      gl.useProgram(film);
      const L = Object.assign({}, D, cam.look);
      gl.activeTexture(gl.TEXTURE0); gl.bindTexture(gl.TEXTURE_2D, srcTex); gl.uniform1i(U('uTex'),0);
      gl.activeTexture(gl.TEXTURE1); gl.bindTexture(gl.TEXTURE_2D, blank); gl.uniform1i(U('uTex2'),1);
      const st = ['vhsc','handycam99','super8','16mil','projector','slidee6','medium67','instasq','instamini','instawide','dreamblur','manualv','manualp'].includes(cam.id) ? 0 : stamp(ia);
      gl.activeTexture(gl.TEXTURE2); gl.bindTexture(gl.TEXTURE_2D, st || blank); gl.uniform1i(U('uStamp'),2);
      gl.uniform1f(U('uDouble'),0); gl.uniform4f(U('uSrc2Crop'),0,0,1,1); gl.uniform1f(U('uStampOn'), st?1:0);
      gl.uniform2f(U('uRes'),W,H); gl.uniform1f(U('uTime'),1.3); gl.uniform1f(U('uSeed'),42.7); gl.uniform1f(U('uFlipY'),0);
      gl.uniform4f(U('uImgRect'), ...sp.rect); gl.uniform1f(U('uAspect'), ia);
      gl.uniform3f(U('uBorderColor'), ...BCOL[cam.border]); gl.uniform1i(U('uBorder'), BORDER[cam.border]);
      gl.uniform4f(U('uSrcCrop'), ...crop(900/1200, ia));
      const f = (n,v) => gl.uniform1f(U(n), v), v3 = (n,v) => gl.uniform3f(U(n), ...v);
      f('uExposure', L.exposure - (acc.nd?1.3:0)); f('uContrast', L.contrast); f('uSaturation', L.saturation); f('uTemp', L.temp); f('uTint', L.tint);
      f('uFade', L.fade); f('uGamma', L.gamma); v3('uLift', L.lift); v3('uGain', L.gain); v3('uShadowTint', L.shadowTint); v3('uHighTint', L.highTint);
      f('uMono', L.mono); f('uCross', L.cross); f('uHalation', L.halation); v3('uHalationColor', L.halationColor); f('uBloom', L.bloom);
      f('uGrain', L.grain); f('uGrainSize', L.grainSize); f('uVignette', L.vignette); f('uLeak', L.leak); v3('uLeakColor', L.leakColor);
      f('uChroma', L.chroma); f('uSoft', L.soft); f('uSharpen', L.sharpen); f('uPixel', L.pixel); f('uScan', L.scan); f('uVhs', L.vhs);
      f('uWeave', L.weave); f('uFlicker', L.flicker); f('uDust', L.dust);
      f('uFisheye', acc.fisheye?1:0); f('uPrism', acc.prism?1:0); f('uStar', acc.star?1:0); f('uFlash', flash ? Math.max(L.flash,0.6) : 0); v3('uFlashColor', acc.gel || [1,1,1]);
      gl.drawArrays(gl.TRIANGLE_STRIP,0,4);
      const e = gl.getError(); if (e) errors.push(cam.id + ' glError ' + e);
      const wrap = document.createElement('div'); wrap.style.cssText='color:#ddd;font:11px sans-serif;text-align:center';
      const img = document.createElement('img'); img.src = cv.toDataURL(); img.style.height='210px'; img.style.display='block';
      wrap.appendChild(img); wrap.appendChild(document.createTextNode(label)); out.appendChild(wrap);
    }
    // original for reference
    const ow = document.createElement('div'); ow.style.cssText='color:#ddd;font:11px sans-serif;text-align:center';
    const oi = document.createElement('img'); oi.src = sc.toDataURL(); oi.style.height='210px'; oi.style.display='block'; ow.appendChild(oi); ow.appendChild(document.createTextNode('SOURCE')); out.appendChild(ow);
    for (const cam of data.cams) render(cam, {}, cam.name, false);
    const fs = data.cams.find(c => c.id === 'funsaver'), mp = data.cams.find(c=>c.id==='manualp');
    render(fs, {}, 'Fun Saver + flash', true);
    render(fs, { gel:[0.3,0.5,1] }, 'Fun Saver + blue gel', true);
    render(mp, { fisheye:true }, 'Fisheye', false);
    render(mp, { prism:true }, 'Prism', false);
    render(mp, { star:true }, 'Star filter', false);
    render(mp, { nd:true }, 'ND filter', false);
    return { errors };
  }, data);
  console.log(JSON.stringify(res.errors));
  await page.waitForTimeout(500);
  await page.screenshot({ path: process.argv[3], fullPage: true });
  await browser.close();
})();
