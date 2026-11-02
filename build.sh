#!/usr/bin/env bash
# Builds release/PSSXLAUNCHER.apk without the Android SDK:
#   javac (against small API stubs) -> dx (dex2jar's jar2dex) -> apktool (manifest, resources, assets) -> uber-apk-signer.
# Needs: Java 11+ (JDK), unzip, zip, curl.
set -euo pipefail
cd "$(dirname "$0")"
T=.tools; B=build
mkdir -p "$T" "$B"

get() { [ -f "$T/$2" ] || curl -fL --retry 3 -o "$T/$2" "$1"; }
get https://github.com/iBotPeaches/Apktool/releases/download/v2.10.0/apktool_2.10.0.jar apktool.jar
get https://github.com/patrickfav/uber-apk-signer/releases/download/v1.3.0/uber-apk-signer-1.3.0.jar uber.jar
get https://github.com/ThexXTURBOXx/dex2jar/releases/download/2.4.38/dex-tools-2.4.38.zip dex-tools.zip
[ -d "$T/dex-tools-2.4.38" ] || unzip -q -o "$T/dex-tools.zip" -d "$T"

# 1. Java -> classes.dex
rm -rf "$B/stub" "$B/cls"; mkdir -p "$B/stub" "$B/cls"
javac -nowarn -Xlint:-options --release 8 -d "$B/stub" $(find android/stub -name '*.java')
javac -Xlint:-options --release 8 -cp "$B/stub" -d "$B/cls" $(find android/java -name '*.java')
(cd "$B/cls" && rm -f ../app.jar && jar cf ../app.jar com)
sh "$T/dex-tools-2.4.38/d2j-jar2dex.sh" -f -o "$B/classes.dex" "$B/app.jar"

# 2. APK project = manifest + res + web assets
rm -rf "$B/apk"; mkdir -p "$B/apk"
cp -r android/AndroidManifest.xml android/apktool.yml android/res "$B/apk/"
cp -r web "$B/apk/assets"
java -jar "$T/apktool.jar" b "$B/apk" -o "$B/unsigned.apk"
(cd "$B" && zip -j unsigned.apk classes.dex >/dev/null)

# 3. Align + sign (debug key built into uber-apk-signer: same key every build, so updates install over the old one)
rm -rf "$B/signed"
java -jar "$T/uber.jar" -a "$B/unsigned.apk" -o "$B/signed"
mkdir -p release
cp "$B"/signed/*-aligned-debugSigned.apk release/PSSXLAUNCHER.apk
echo "OK -> release/PSSXLAUNCHER.apk"
