# FUN CAM for iPhone (web app)

This is a test version of FUN CAM that runs in Safari on iPhone. It uses the same film shader
and the same 28 camera recipes as the Android app. `data.js` is generated from the Android
sources by `tools/gen_web_data.py`, so the looks always match.

## Install on iPhone
1. Open **https://slaymore99.github.io/DAZZ-CAM/** in **Safari**.
2. Tap **Open camera** and allow camera access.
3. Tap **Share → Add to Home Screen**. FUN CAM then opens full-screen like a normal app.
4. Shots are kept inside the app. To put one in Photos, tap the thumbnail, pick a shot and
   tap **Save → Save Image**.

## Hosting
The camera only works over **https** (or on `localhost`).

- **GitHub Pages:** the `Deploy FUN CAM for iPhone` workflow publishes this folder. Turn it
  on under **Settings → Pages → Source: GitHub Actions**. Pages on a private repo needs a
  paid GitHub plan; on a public repo it's free.
- **Any static host** works too, for example Netlify Drop, Cloudflare Pages or Vercel.
  Upload the `web/` folder.
- **Local test:** run `python3 -m http.server -d web 8000` and open http://localhost:8000.

## Differences from the Android app
- **Resolution:** photos come from the camera's video stream (about 1440×1920), not the full
  sensor.
- **No hardware controls:** there's no real flash/torch, manual ISO or tap-to-focus. Safari
  doesn't expose them. The flash *look* is still applied.
- **Shutter and zoom:** slow shutter is simulated by blending frames, the same as Android.
  Zoom is digital.
- **No Darkroom yet.**
