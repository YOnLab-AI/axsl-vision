#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
python3 tools/fetch_assets.py --all --verify-only
./gradlew :vision:testDebugUnitTest :offlinevoice:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon --console=plain
