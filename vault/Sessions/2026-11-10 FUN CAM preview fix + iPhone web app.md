# 2026-11-10: FUN CAM preview fix + iPhone web app

- The user tested the APK. The preview was sideways.
  - Cause: the SurfaceTexture matrix already included the camera rotation, so ours doubled it.
  - Fixed, and a manual "Rotate preview" setting was added.
- The user said the UI looked cluttered. It was redesigned (see [[FUN CAM]] → Update 2026-11-10).
- The user asked for an iPhone version to test. Built a Safari web app (PWA) in `web/` with the
  same looks.
  - Tested end to end in headless Chrome with a fake camera: photo, instant frame, VHS video,
    roll and viewer all work.
- Explained that the Dazz camera looks are approximations, not copies. To match them exactly,
  we need reference photos from the user.
- **Open:** where to host the web app (the repo is private). Device testing of the rotation fix.
