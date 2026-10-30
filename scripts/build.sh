#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export ANDROID_HOME="${ANDROID_HOME:-/workspace/.android-sdk}"
if [ -z "${JAVA_HOME:-}" ]; then
  candidates=(/workspace/.jdk/jdk-*/bin/javac)
  export JAVA_HOME="${candidates[0]%/bin/javac}"
fi
export PATH="$JAVA_HOME/bin:$PATH"
tools="$ANDROID_HOME/build-tools/35.0.0"
android="$ANDROID_HOME/platforms/android-35/android.jar"
mkdir -p build/classes build/dex .local
rm -rf build/classes build/dex
mkdir -p build/classes build/dex
"$tools/aapt2" compile --dir app/src/main/res -o build/resources.zip
"$tools/aapt2" link -I "$android" --manifest app/src/main/AndroidManifest.xml --version-code 1 --version-name 0.1.0 -o build/base.apk build/resources.zip
find app/src/main/java -name '*.java' -print > build/sources.txt
javac --release 8 -Xlint:-options -encoding UTF-8 -cp "$android" -d build/classes @build/sources.txt
python3 - <<'PY'
import pathlib,zipfile
with zipfile.ZipFile('build/classes.jar','w') as z:
    for p in sorted(pathlib.Path('build/classes').rglob('*.class')):
        z.write(p,p.relative_to('build/classes'))
PY
"$tools/d8" --min-api 26 --lib "$android" --output build/dex build/classes.jar
cp build/base.apk build/unsigned.apk
python3 - <<'PY'
import zipfile
with zipfile.ZipFile('build/unsigned.apk','a') as z:
    z.write('build/dex/classes.dex','classes.dex')
PY
"$tools/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk
if [ ! -f .local/debug.keystore ]; then
  keytool -genkeypair -keystore .local/debug.keystore -storepass android -keypass android -alias androiddebugkey -dname "CN=Android Debug" -keyalg RSA -keysize 2048 -validity 10000 -noprompt
fi
"$tools/apksigner" sign --ks .local/debug.keystore --ks-pass pass:android --key-pass pass:android --out build/firetv-clock-debug.apk build/aligned.apk
"$tools/apksigner" verify --verbose build/firetv-clock-debug.apk
"$tools/aapt" dump badging build/firetv-clock-debug.apk
echo "APK: $PWD/build/firetv-clock-debug.apk"
