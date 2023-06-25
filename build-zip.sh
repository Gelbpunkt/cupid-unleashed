#!/bin/env bash
cd cupid-unleashed-backend
cargo build --release
cp target/aarch64-linux-android/release/cupid-unleashed-backend ../zip-src/system/bin/cupid-unleashed
cd ../cupid-unleashed-manager
./gradlew assembleRelease
zipalign -v -p 4 app/build/outputs/apk/release/app-release-unsigned.apk app/build/outputs/apk/release/app-release-unsigned-aligned.apk
apksigner sign  --ks ../unleashed.jks --ks-pass=file:../unleashed.jkpass --out ../zip-src/system/priv-app/UnleashedManager/UnleashedManager.apk app/build/outputs/apk/release/app-release-unsigned-aligned.apk
apksigner verify ../zip-src/system/priv-app/UnleashedManager/UnleashedManager.apk
cd ../zip-src
zip -r9 ../cupid-unleashed.zip ./ -x system/bin/.gitkeep -x system/priv-app/UnleashedManager/.gitkeep -x system/priv-app/UnleashedManager/UnleashedManager.apk.idsig
