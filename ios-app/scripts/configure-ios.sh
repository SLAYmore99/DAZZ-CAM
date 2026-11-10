#!/bin/bash
# Applies FUN CAM's iOS settings to the generated Capacitor project (run on macOS after `npx cap add ios`).
set -euo pipefail
cd "$(dirname "$0")/.."
PLIST=ios/App/App/Info.plist
pb() { /usr/libexec/PlistBuddy -c "$1" "$PLIST" 2>/dev/null || true; }
pb "Delete :NSCameraUsageDescription"; pb "Add :NSCameraUsageDescription string FUN CAM uses the camera to take photos and videos."
pb "Delete :NSMicrophoneUsageDescription"; pb "Add :NSMicrophoneUsageDescription string FUN CAM records sound with your videos."
pb "Delete :NSPhotoLibraryAddUsageDescription"; pb "Add :NSPhotoLibraryAddUsageDescription string FUN CAM saves your photos and videos to your library."
pb "Delete :NSPhotoLibraryUsageDescription"; pb "Add :NSPhotoLibraryUsageDescription string FUN CAM lets you import photos to develop with a camera."
pb "Delete :UISupportedInterfaceOrientations"; pb "Add :UISupportedInterfaceOrientations array"; pb "Add :UISupportedInterfaceOrientations:0 string UIInterfaceOrientationPortrait"
pb "Delete :UISupportedInterfaceOrientations~ipad"; pb "Add :UISupportedInterfaceOrientations~ipad array"; pb "Add :UISupportedInterfaceOrientations~ipad:0 string UIInterfaceOrientationPortrait"
pb "Delete :UIStatusBarStyle"; pb "Add :UIStatusBarStyle string UIStatusBarStyleLightContent"
pb "Delete :UIViewControllerBasedStatusBarAppearance"; pb "Add :UIViewControllerBasedStatusBarAppearance bool false"
pb "Delete :CFBundleDisplayName"; pb "Add :CFBundleDisplayName string FUN CAM"
cp resources/icon-1024.png ios/App/App/Assets.xcassets/AppIcon.appiconset/AppIcon-512@2x.png
echo "configured"
