#!/usr/bin/env bash
# Publishes the shared core, then builds, installs and launches native-ios on an iOS simulator (macOS + Xcode).
# Usage: scripts/run-native-ios.sh ["<simulator name>"]   (default: iPhone 17)
set -euo pipefail
cd "$(dirname "$0")/.."

SIMULATOR="${1:-iPhone 17}"
BUNDLE_ID="com.banhmilabs.tmdb.nativeapp"
DERIVED_DATA="build/native-ios-derived-data"
APP="$DERIVED_DATA/Build/Products/Debug-iphonesimulator/TmdbNative.app"

./gradlew publishSharedCore
# Keep the simulator's ad-hoc signing: without it the app has no Keychain access and the settings store fails.
xcodebuild -project native-ios/TmdbNative.xcodeproj -scheme TmdbNative -configuration Debug \
    -destination "platform=iOS Simulator,name=$SIMULATOR" -derivedDataPath "$DERIVED_DATA" -quiet build

# Booting an already booted simulator fails; that is fine.
xcrun simctl boot "$SIMULATOR" 2> /dev/null || true
open -a Simulator
xcrun simctl install "$SIMULATOR" "$APP"
xcrun simctl launch "$SIMULATOR" "$BUNDLE_ID"
