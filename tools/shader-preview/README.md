# Shader preview

This tool renders every camera look on a synthetic test scene using headless Chromium's WebGL2.
WebGL2 uses the same GLSL ES 3.00 language as the app, so it catches shader compile errors and
look regressions without a phone.

```bash
python3 extract.py          # writes data.json from Shaders.kt + Cameras.kt
node run.js data.json sheet.png
```

You need Node with `playwright` and a Chromium browser.
