# FUN CAM for iPhone

This is a native iOS app made with [Capacitor](https://capacitorjs.com). It wraps the shared
FUN CAM camera engine in `../web`: the same shaders, film LUTs, 42 cameras, wheel, Customize
and slow shutter as the Android app.

GitHub Actions builds `FUN-CAM.ipa` on every push (`.github/workflows/ios.yml`). It's attached
to the **"latest" release** on GitHub and to the workflow run as an artifact.

## Installing the IPA

Apple only lets signed apps run. The CI build is **unsigned**, so whoever installs it signs it
with their own Apple ID. That's free and takes a couple of minutes.

**AltStore (easiest; works from a Windows or Mac computer)**
1. Install AltServer on your computer from altstore.io, and AltStore on the iPhone through it.
2. Download `FUN-CAM.ipa` to the iPhone. In AltStore, go to **My Apps → +** and pick the IPA.
3. On the iPhone, go to **Settings → General → VPN & Device Management** and trust your Apple ID.
4. Free Apple IDs need the app refreshed in AltStore every 7 days. AltStore can do this
   automatically over Wi-Fi.

**Sideloadly (Windows/Mac):** plug the iPhone in, drag the IPA into Sideloadly, enter your
Apple ID and click Start. Then trust the developer as in step 3.

## Sharing with lots of people (no 7-day limit)
Join the Apple Developer Program ($99/year). Then:
- add the signing certificate and provisioning profile as GitHub secrets, and switch the
  workflow to a signed `xcodebuild archive` + `-exportArchive`; or
- upload builds to **TestFlight**, so testers just install the TestFlight app and tap your
  invite link. Up to 10,000 testers.

Until then, the free web version works on any iPhone in Safari (see `../web/README.md`).

## Building locally (Mac with Xcode)
```bash
cd ios-app && npm ci && npm run copy-web && npx cap add ios && bash scripts/configure-ios.sh && npx cap sync ios
npx cap open ios   # then Run on your iPhone with your Apple ID as the team
```
