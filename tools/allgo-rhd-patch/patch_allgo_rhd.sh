#!/bin/sh
# Patch the car's stock AllGo CarPlay APK so the CarPlay bar is always on the right.
# Usage: tools/allgo-rhd-patch/patch_allgo_rhd.sh /path/to/com.allgo.carplay.service.apk
# Output: dist/AllgoCarplay-rhd-v<versionCode>.apk (driving side and cluster directions, signed with the platform key)
# See tools/allgo-rhd-patch/README.md.
set -eu

HERE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT=$(CDPATH= cd -- "$HERE/../.." && pwd)

STOCK="${1:-}"
[ -f "$STOCK" ] || { echo "Usage: $0 /path/to/stock-allgo-carplay.apk" >&2; exit 1; }

command -v apktool >/dev/null 2>&1 || { echo "Error: apktool not found (brew install apktool)" >&2; exit 1; }

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK" ]; then
  if [ -d "$HOME/Library/Android/sdk" ]; then
    SDK="$HOME/Library/Android/sdk"
  elif [ -d "$HOME/Android/Sdk" ]; then
    SDK="$HOME/Android/Sdk"
  fi
fi
if [ -n "${ANDROID_BUILD_TOOLS:-}" ]; then
  BT="$ANDROID_BUILD_TOOLS"
elif [ -n "$SDK" ] && [ -d "$SDK/build-tools" ]; then
  BT=$(ls -d "$SDK"/build-tools/* 2>/dev/null | tail -n1)
else
  echo "Error: Android SDK or build-tools not found. Set ANDROID_HOME or ANDROID_BUILD_TOOLS." >&2
  exit 1
fi

KEYS="${MG4_PLATFORM_KEYS_DIR:-$ROOT/tools}"
if [ ! -f "$KEYS/platform.pk8" ] || [ ! -f "$KEYS/platform.x509.pem" ]; then
  echo "Error: platform.pk8 / platform.x509.pem not found in $KEYS. Set MG4_PLATFORM_KEYS_DIR." >&2
  exit 1
fi

WORK="${ALLGO_RHD_WORK_DIR:-$(mktemp -d)}"
rm -rf "$WORK/dec"
mkdir -p "$WORK"

echo "Decoding $STOCK"
apktool d -f -o "$WORK/dec" "$STOCK" >/dev/null

echo "Applying smali patches and adding ClusterNavBridge"
patch -p1 -d "$WORK/dec" -i "$HERE/rhd.patch"
patch -p1 -d "$WORK/dec" -i "$HERE/cluster.patch"
cp -R "$HERE/smali/." "$WORK/dec/smali_classes4/"

# Bump versionCode by two (105 was the driving-side-only build) and tag the versionName, so it installs over the stock app.
YML="$WORK/dec/apktool.yml"
CODE=$(sed -n "s/^  versionCode: *'\{0,1\}\([0-9][0-9]*\)'\{0,1\}$/\1/p" "$YML")
NAME=$(sed -n "s/^  versionName: *\(.*\)$/\1/p" "$YML" | tr -d "'\"")
[ -n "$CODE" ] || { echo "Error: versionCode not found in apktool.yml" >&2; exit 1; }
NEWCODE=$((CODE + 2))
sed -i.bak "s/^  versionCode: .*/  versionCode: $NEWCODE/; s/^  versionName: .*/  versionName: '${NAME}-rhd-cluster'/" "$YML"
rm -f "$YML.bak"
echo "versionCode $CODE -> $NEWCODE, versionName ${NAME}-rhd-cluster"

echo "Rebuilding"
apktool b -o "$WORK/unsigned.apk" "$WORK/dec" >/dev/null
"$BT/zipalign" -f 4 "$WORK/unsigned.apk" "$WORK/aligned.apk"

mkdir -p "$ROOT/dist"
OUT="$ROOT/dist/AllgoCarplay-rhd-v$NEWCODE.apk"
"$BT/apksigner" sign \
  --key "$KEYS/platform.pk8" \
  --cert "$KEYS/platform.x509.pem" \
  --min-sdk-version 24 --v1-signing-enabled true --v2-signing-enabled true \
  --out "$OUT" "$WORK/aligned.apk"
"$BT/apksigner" verify --verbose --print-certs "$OUT"
echo "$OUT"
