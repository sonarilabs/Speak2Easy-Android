#!/usr/bin/env bash
set -euo pipefail

DEVICE_ID="${1:-R5CX20Z8VZA}"
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
AAB="$PROJECT_DIR/app/build/outputs/bundle/release/app-release.aab"
APKS="/tmp/speak2easy.apks"
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
KEYSTORE="$HOME/.android/speak2easy-upload.jks"
KEY_ALIAS="speak2easy-upload"

if [[ -z "${KS_PASS:-}" ]]; then
  echo "KS_PASS is not set. Export the upload keystore password first." >&2
  exit 1
fi

cd "$PROJECT_DIR"

./gradlew :app:bundleRelease

bundletool build-apks \
  --bundle="$AAB" \
  --output="$APKS" \
  --overwrite \
  --connected-device \
  --device-id="$DEVICE_ID" \
  --ks="$KEYSTORE" \
  --ks-key-alias="$KEY_ALIAS" \
  --ks-pass="pass:$KS_PASS" \
  --key-pass="pass:$KS_PASS" \
  --adb="$ADB"

bundletool install-apks \
  --apks="$APKS" \
  --device-id="$DEVICE_ID" \
  --adb="$ADB"
