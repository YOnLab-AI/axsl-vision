# AXSL Vision

**An Android reference app for developing and evaluating on-device AI.**

AXSL Vision connects model selection, local media inputs, actual inference,
side-by-side comparison, and inspectable results in one native application.
It is intended for developers learning how to integrate models and reproduce
their behavior on a real device. The application UI is currently Korean.

[한국어](README.ko.md) · [Build21 APK](https://github.com/YOnLab-AI/axsl-vision/releases/tag/v0.1-build21) · [Model testing](docs/MODEL_TESTING.md) · [Feature guide](docs/FEATURES.md) · [Model catalogue](docs/MODEL_CATALOG.md)

The model-testing update keeps versionName **0.1** and uses Android **code21**.
Its completed build/device evidence is in [Build21 results](docs/BUILD21_STATUS.md)
and [Validation](docs/VALIDATION.md). The code20 results below remain separately
labelled evidence for the previous public APK.

## What you can explore

| Area | Reference workflows |
|---|---|
| Images and documents | Object detection, image classification, segmentation, OCR, QR/barcodes, document extraction, background editing, image retrieval |
| People and motion | Face analysis/comparison, local face enrollment, driver observations, people flow, visitor sessions, pose landmarks, geometric exercise feedback |
| Audio | Local speech recognition and environmental sound classification |
| Presentation | A photo/video playlist for shop, event, and exhibition information, with full-screen playback |

The registry contains **19 features, 87 feature-specific model/engine selection
slots, and 68 distinct execution IDs**. Slots reuse weights and include code
engines such as Android Media and ZXing. They are not 87 independent neural
networks. The [model catalogue](docs/MODEL_CATALOG.md) explains every selection,
shared weights, input contracts, preprocessing, outputs, provenance, and limits.

The default public dataset has **312 examples**: 235 images, 66 audio clips,
and 11 videos. Korean, global, and shared tags describe the materials, not a
person's nationality. Each item retains its own attribution and license.

## Try the app

1. Install a reference APK from [Releases](https://github.com/YOnLab-AI/axsl-vision/releases) after checking its `SHA256SUMS.txt` and build number.
2. Open **모델 시험**, choose one execution ID, then choose a supported feature.
3. Vary the input using metadata-based sets of up to six diverse examples, or
   the complete list with twelve more items per expansion. Filter by data group,
   input type, and declared difficulty, or use your own file. Changing the input
   preserves the chosen model instead of selecting another by data language.
4. Open **이 모델의 시험 기록** to inspect input-specific results for the same ID.
   Model comparison on one input remains available. Inspect outputs, hashes,
   status, and timing scope before exporting a report.
5. For presentation playback, open **매장·행사 안내판**, add media, choose a
   playlist, and enter full-screen mode. Its playback engine is not an AI model.

The release is a development reference build. It retains the historical Android
package `com.yonlab.one` to support updates without discarding existing app data.
No signing key is included in this repository. A local debug build uses a
different certificate; do not uninstall an existing app containing data merely
to bypass a certificate mismatch.

## Build from source

Use **JDK 21**, Android SDK **Platform 36 / Build Tools 36.0.0**, and Python
**3.10+**. Java source compatibility is 17; the checked-in wrapper pins Gradle
9.1.0 and its distribution hash. The app declares minSdk 24 and targetSdk 36;
model compatibility and memory requirements still depend on the device.

```sh
git clone https://github.com/YOnLab-AI/axsl-vision.git
cd axsl-vision
python3 tools/fetch_assets.py --all
./gradlew :vision:testDebugUnitTest :offlinevoice:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

On Windows, use `python` and `.\gradlew.bat`, or run `./build.ps1` after fetching
the assets. Set `JAVA_HOME` and `ANDROID_HOME` to your local installations.
The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

Large weights, runtime AARs, and demo media are distributed as separately
versioned [asset packs](assets/README.md). The downloader checks exact sizes,
SHA-256 hashes, and archive members before installing files. It does not replace
the current weights with a similarly named upstream model. Runtime-only
bootstrap is available for source checks:

```sh
python3 tools/fetch_assets.py --group runtime
python3 -m unittest discover -s tools/tests -v
./gradlew :vision:testDebugUnitTest :offlinevoice:testDebugUnitTest :app:lintDebug
```

See [build and reproduction](docs/BUILD_AND_REPRODUCE.md) for release signing,
asset verification, and troubleshooting. The release manifest does not request
the `INTERNET` permission; asset downloading happens on the development machine.

## Source map

```text
app/           Android application manifest, version, icons, packaging
vision/        Native workspace, vision/media adapters, results, tests
offlinevoice/  Vosk and Whisper speech pipelines, audio lifecycle, tests
assets/        Versioned asset lock and distribution exclusions
tools/         Verified asset bootstrap and its adversarial tests
docs/          Feature/model analysis, architecture, data terms, validation
```

| Documentation | Contents |
|---|---|
| [Model testing](docs/MODEL_TESTING.md) | Model-first input variation, shared guidance, exact-ID history, component versus workflow tests |
| [Features](docs/FEATURES.md) | All 19 workflows: inputs, models, processing, outputs, use cases, limitations |
| [Model catalogue](docs/MODEL_CATALOG.md) | All 68 IDs, 87 slots, runtimes, tensor/pipeline contracts, weights, source and license bindings |
| [Architecture](docs/ARCHITECTURE.md) | Registries, input ownership, engine lifecycle, cancellation, records and exports |
| [Validation](docs/VALIDATION.md) | Versioned local/physical evidence and the boundaries of each observation |
| [Data and licenses](docs/DATA_AND_LICENSES.md) | Model, SDK, dataset, media and authored-fixture conditions |
| [Limitations](docs/LIMITATIONS.md) | Accuracy, demographic estimates, temporal metrics, memory, device and SDK limits |

## Current build21 verification

The signed 0.1/code21 APK passed **279 host tests** (259 Android, 20 Python) and
**six normal UI observations** on SM-F956N/API37 at font scale 1.5. The diverse
matrix completed **17 model/input pairs across six IDs and 13 distinct encoded
inputs**. Including selection-preservation and image/sound export regressions,
**24 controller executions covered eight IDs and 16 distinct encoded inputs**.
Two strict model-filter saved-history checks passed separately. These counts do
not imply independent accuracy samples or validation of every model.

Image/sound report JSON membership and model-bound saved events were checked.
Normal UI actions, independent controller runs, and privately instrumented
input-return regressions are distinguished in [the results](docs/BUILD21_STATUS.md)
and [sanitized JSON](docs/validation-0.1-build21.json). Initial private test-tool
errors were corrected and the exact same APK was re-tested. The private helper
was removed and temporary phone settings were restored.

APK: **1,583,020,342 bytes**; SHA-256:

`fe0fed606618084d6cd153d7925ccdd26d87ab1a2fb263258780ca2869a9067e`

[Source CI](https://github.com/YOnLab-AI/axsl-vision/actions/runs/40323411899) passed at the recorded implementation revision.
The release identifier is [v0.1-build21](https://github.com/YOnLab-AI/axsl-vision/releases/tag/v0.1-build21).

### Previous public baseline: 0.1/code20

The **0.1 / code20 release APK** passed **260 local host tests**: 244 Android
unit tests and 16 asset-bootstrap tests. Release assembly succeeded; lint reported
zero errors and two `MonochromeLauncherIcon` warnings. On an SM-F956N / API37
with font scale 1.5, **five selected engine executions** and **five UI checks**
passed. These device observations are separate from the 260 host tests.

The five single-input runs exercised MobileNetV4 classification, EfficientDet
Lite0 detection, ML Kit Korean OCR, ZXing QR, and Vosk Korean ASR. The first three
are output observations without an accuracy evaluation. QR matched the exact
payload and symbology for one raster. Vosk recorded **CER 0.15789 on one
9.9-second clip**, using NFC normalization, removal of Unicode whitespace,
preserved case/punctuation, and codepoint edit distance. This is not corpus
accuracy or a repeated benchmark. Predecoded-PCM engine timing is recorded
separately from input preparation and decoding.

APK size: **1,583,002,073 bytes**. SHA-256:

`cde2536127ea067c0c4b2ff143e3113f7f051565b2338043747f144d7a7721f7`

[Validation](docs/VALIDATION.md) and the [sanitized 0.1 evidence summary](docs/validation-0.1.json)
retain model/input hashes and measurement scope without private paths, device
serials, transcripts, or run IDs. Local host results do not assert a remote
GitHub Actions result. The earlier **4.4.0 / code19** records—244 host tests,
22 physical checks, and four representative runs—remain explicitly historical.

These observations do not validate every feature/model, long-term unattended
playback, other devices/boards, mechanical folding, or power/thermal/NPU
performance. See the feature-specific limits before interpreting an output.

## License and contributions

Project-authored application code is licensed under **[Apache-2.0](LICENSE)**.
Model weights, SDKs, media, and other third-party materials retain their own
terms; downloading them does not make them Apache-2.0. Historical authored
fixtures with a free-app distribution grant retain that narrower grant.
See [NOTICE](NOTICE) and [data/license details](docs/DATA_AND_LICENSES.md).

Contributions should include reproducible inputs, provenance, and meaningful
validation. Read [CONTRIBUTING](CONTRIBUTING.md) and [SECURITY](SECURITY.md) before
opening an issue or submitting a model adapter. Do not publish personal media,
face embeddings, transcripts, credentials, or unredacted device dumps.
