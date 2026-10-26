# Contributing

AXSL Vision is a reference application for learning and evaluating on-device AI.
Keep examples reproducible and distinguish measurements from accuracy claims.

1. Read the feature, model, and architecture guides in `docs/`.
2. Use JDK 21, Android SDK 36, and the checked-in Gradle wrapper.
3. Run the asset downloader for the groups needed by your change. Do not add
   model weights, datasets, APKs, signing keys, or local SDK paths to Git.
4. Run the relevant unit tests and lint. For runtime changes, record the app
   version, model ID, input hash, device/API, and actual result.
5. Open a pull request explaining the problem, behavior after the change, and
   validation. A screenshot is useful for UI changes; include no personal data.

New model adapters need an immutable upstream reference, exact artifact hash,
license and attribution, tensor/input contract, pre/postprocessing description,
failure handling, and a reproducible test. A model loading successfully is not
evidence of general accuracy or hardware acceleration.

Preserve existing attribution and add notices for new dependencies. Contributors
must have the right to submit their changes under the repository's code license.
