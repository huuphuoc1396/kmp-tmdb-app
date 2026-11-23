#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

SIMULATOR="${1:-iPhone 17}"
BUNDLE_ID="com.banhmilabs.tmdb.nativeapp"
DERIVED_DATA="build/native-ios-derived-data"
APP="$DERIVED_DATA/Build/Products/Debug-iphonesimulator/TmdbNative.app"

./gradlew publishSharedCore
xcodebuild -project native-ios/TmdbNative.xcodeproj -scheme TmdbNative -configuration Debug \
    -destination "platform=iOS Simulator,name=$SIMULATOR" -derivedDataPath "$DERIVED_DATA" -quiet build

xcrun simctl boot "$SIMULATOR" 2> /dev/null || true
open -a Simulator
xcrun simctl install "$SIMULATOR" "$APP"
xcrun simctl launch "$SIMULATOR" "$BUNDLE_ID"
