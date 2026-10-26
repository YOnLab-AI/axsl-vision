# Build and reproduce

## Environment

The current model-testing build is **0.1/code21**. [Build21 results](BUILD21_STATUS.md) record279 host checks and scoped physical observations; earlier code20 evidence remains version-labelled.

| Item | Pinned project contract |
|---|---|
| Gradle runtime | JDK 21 |
| Java language/bytecode | Java 17 |
| Android SDK | compile/target 36, min 24 |
| Build Tools | 36.0.0 |
| Android Gradle Plugin | 9.0.1 |
| Gradle | Wrapper 9.1.0 with distribution SHA-256 |
| Python asset tool | Standard library, Python 3.10 or newer |
| Packaged ABIs | arm64-v8a, armeabi-v7a, x86_64 |

Set `JAVA_HOME` and `ANDROID_HOME`. A developer-specific `local.properties`
file is optional and must not be committed. Accept the Android SDK licenses
through the SDK manager for your own environment.

## Restore the exact assets

```sh
python3 tools/fetch_assets.py --all
python3 tools/fetch_assets.py --all --verify-only
```

The three pack groups are `runtime`, `models`, and `demos`. Their exact release
URLs, sizes, hashes, members, source bindings, and license files are in
[`asset-lock.json`](../assets/asset-lock.json). The source tree includes the
small original interface illustrations and three guide layouts directly.

For an offline machine, download the three release ZIP files and supply their
directory without changing their names:

```sh
python3 tools/fetch_assets.py --all --local-packs /path/to/downloaded-packs
```

The tool checks an archive before replacing any installed member. Replacements
are atomic per file. A filesystem failure during placement can leave a verified
subset installed; rerunning completes it. Do not treat a partial command or a
file name match as a successful bootstrap. Use `--verify-only` afterwards.

## Test and build

```sh
python3 -m unittest discover -s tools/tests -v
./gradlew :vision:testDebugUnitTest :offlinevoice:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Windows uses `python` and `.\gradlew.bat`. `build.ps1` and `build.sh` wrap the
full-asset check and the same Android tasks. Android Studio opens the repository
root; `offlinevoice/` is a real module, not a temporary staging folder.

Source-only CI fetches the runtime AAR and runs host tests/lint. It does not
prove model inference, camera/microphone behavior, long-running playback, or
physical device acceptance. For a real inference APK, restore every asset group.

## Release signing

Debug builds use the Android development environment's debug certificate.
Release signing is optional and comes only from the following environment
variables. No release key or password is distributed here.

```text
AXSL_SIGNING_STORE_FILE
AXSL_SIGNING_STORE_PASSWORD
AXSL_SIGNING_KEY_ALIAS
AXSL_SIGNING_KEY_PASSWORD
```

Set all four or leave all four unset, then run `:app:assembleRelease`.
Without signing variables, Gradle produces an unsigned release APK. An unsigned
APK cannot be installed until it is signed. The published reference APK uses
the maintainer's development certificate and has `debuggable=false`.

Android requires the same package and certificate for an update. VersionName is
`0.1`; versionCode is `21`, preserving upgrade order from previous
code19 and public code20 builds. The legacy package and persisted schema identifiers remain for
data compatibility. A fork that changes its applicationId must review provider
authorities, intent routes, and integrations. Avoid uninstalling an app with
valuable local data merely to resolve a signing mismatch.

## Record a reproducible experiment

Record the commit/APK hash, feature and model ID, actual encoded input hash,
source attribution, device model/API/ABI, CPU execution settings, output, and
whether engine/input cleanup completed. Separate model load, inference, and
total processing time. Use validated reference annotations only for the metrics
they support; a single example is not an accuracy benchmark.

Use public demo IDs for issue reports. Redact device identifiers and avoid
posting private images, face templates, recordings, or transcripts.

## Troubleshooting

- **Missing model or demo:** run the full bootstrap and verify it. Similar
  upstream model names do not guarantee the same tensors or preprocessing.
- **Certificate mismatch:** use the compatible maintainer APK or a distinct
  development package. Back up data before any intentional uninstall.
- **Memory or initialization error:** check the model size, ABI and device
  limits in the model catalogue. Try a smaller supported model.
- **Network unavailable on the build machine:** use verified local packs and
  a prepared Gradle cache. The app itself has no INTERNET permission.
- **Restricted temporary directory:** choose a writable `TMP`/`TEMP` for Python
  tests in managed Windows environments. Ordinary developer/CI environments use
  the platform temporary directory.

Official references: [Android signing](https://developer.android.com/studio/publish/app-signing),
[GitHub large files](https://docs.github.com/en/repositories/working-with-files/managing-large-files/about-large-files-on-github),
[GitHub Releases](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases).
