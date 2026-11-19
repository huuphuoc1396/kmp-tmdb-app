#!/usr/bin/env bash
# Publishes the shared core, then builds, installs and launches native-android on the connected device or emulator.
set -euo pipefail
cd "$(dirname "$0")/.."

APP_ID="com.banhmilabs.tmdb.nativeapp"

if ! adb get-state > /dev/null 2>&1; then
    echo "No Android device found: start an emulator or connect a device, then run again." >&2
    exit 1
fi

./gradlew publishSharedCore
(cd native-android && ./gradlew :app:installDebug)
adb shell monkey -p "$APP_ID" -c android.intent.category.LAUNCHER 1 > /dev/null
echo "Launched $APP_ID"
