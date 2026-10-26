# Build21 verification status

VersionName remains **0.1**; versionCode is **21**. This development branch adds model-centered testing and consistent feature/model guidance. The previous public `v0.1` release and its asset packs remain available.

## Completed

- Signed release build and installation as an update on SM-F956N / Android17(API37), with the existing package and certificate.
- **259 Android unit tests** in43 suites: zero failures/errors/skips.
- **20 Python tests** for asset handling and model-guide contracts: passed.
- Release lint: zero errors, two existing `MonochromeLauncherIcon` fallback warnings.
- APK signature and16KB ZIP alignment checks: passed.
- Two normal UI observations at fontScale1.5: the five consistent guidance fields are reachable; all-list/diverse-six input selection is reachable and displays six compatible cases.
- [Remote source CI](https://github.com/YOnLab-AI/axsl-vision/actions/runs/40323411899) passed for implementation commit `02897725c629bed9ff3aaa61764a28625980fabd`. It runs Python tests, runtime bootstrap, Android unit tests and lint. Documentation-only follow-ups do not change the compiled application.
- Independent code review completed; saved-report model bindings and model-preserving empty-history navigation were corrected and regression-tested.

## Remaining physical acceptance

The connected phone repeatedly moved to other applications during normal UI automation. The test stopped rather than treating another application's UI as AXSL Vision. No target-app crash was found in the diagnostic crash buffer. Final rotation/return assertions and the prepared diverse-input production-model matrix remain **not completed** while exclusive phone access is unavailable.

The planned17-input matrix covers classification, detection, Korean OCR, QR, Vosk noise conditions and Whisper Korean/English inputs. Additional selection-preservation and model-scoped report-export checks are prepared. These are test plans, not passed inference or accuracy results.

The source is on `feat/model-testing-build21`. Main integration, tag `v0.1-build21` and the new public APK release wait for the remaining physical acceptance. This document must be updated with actual results before claiming completion.

## Exact local APK

- File name: `AXSL-Vision-0.1-build21.apk`
- Bytes: `1583020342`
- SHA-256: `fe0fed606618084d6cd153d7925ccdd26d87ab1a2fb263258780ca2869a9067e`

The APK has been built and installed locally; this status document does not claim that it is already a public Release asset. Signing keys, private helpers, raw device logs and identifiers are excluded from the repository. No execution video was produced.
