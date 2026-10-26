# YOnLab speech models and runtimes

The application performs actual inference using the selected bundled model. No output is replaced with a reference transcript. Model ranking requires a measured test set and physical-device timings.

## Vosk Korean and English

- Runtime: Vosk Android 0.3.75 / JNA 5.18.1; existing runtime notices remain bundled.
- Korean: original `vosk-model-small-ko-0.22.zip` and exact per-file manifest retained.
- English: original `vosk-model-small-en-us-0.15.zip` downloaded from https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip . The official https://alphacephei.com/vosk/models page lists Apache 2.0 for this model. Archive README is retained, not renamed as a different model.
- New English archive SHA-256: `30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498`.
- Vosk API Apache license obtained from the official repository's master branch: https://github.com/alphacep/vosk-api/blob/master/COPYING . This source is a license reference, not a claim that the branch is the exact 0.3.75 binary source revision.

## Whisper Tiny multilingual INT8

- One multilingual weight set is shared by Korean/global workspaces; native language detection is automatic. It is an alternative alongside Vosk Korean and Vosk English, not two fictitious language models.
- Original OpenAI Whisper code and weights: MIT, https://github.com/openai/whisper/blob/main/LICENSE . The verbatim MIT license is included in one/licenses/MIT-Whisper.txt and the model archive.
- Encoder, decoder and tokens are original pinned supplier files; offlinevoice/whisper-model-files.json records their size, SHA-256 and origin. The flattened ZIP changes packaging only, not weights.
- Runtime: official sherpa-onnx-static-link-onnxruntime 2.0.0 AAR, SHA256 59366e37cf2760b419e895d2872aa1664027fd721f6e4c827cdb0e805088fcbe (publisher digest matched). Apache-2.0 source: https://github.com/k2-fsa/sherpa-onnx/tree/v2.0.0 .
- This AAR statically links ONNX Runtime 1.28.2 (MIT plus third-party notices), separately from the vision module's ONNX Runtime Android 1.24.3. Both original version-specific notices are included.
- The synchronous native decode is worker-owned. Stop requests suppress further work and late UI updates; cleanup follows the native call returning.
- Legacy Moonshine model weights have been removed from this distribution and are rejected by the public model policy. The shared Sherpa runtime remains for Whisper.

## Input and metrics

Files use the existing bounded decoder/resampler: maximum 32 MiB / 30 seconds, mono signed PCM16 at 16 kHz. A comparison converts the selected file once and provides the same PCM bytes/hash to every selected engine. Reference text is eligible for CER only when the owned demo catalog's verified reference and encoded input SHA-256 match the actual copied file. Intent-supplied reference text alone is never trusted.

CER policy: NFC, remove Unicode whitespace, retain case/punctuation, Unicode codepoints; Levenshtein edits / reference characters. CER can exceed 100%. It is absent for unverified or empty reference text. It is not model confidence.

Automatic journals contain metadata/counts only. Microphone transcripts are kept for the current result UI, with view-state text persistence disabled. JSON/CSV/HTML containing recognized text are created only by the user's report action. Reports label the unmodified model output and user-edited text separately; CSV formula prefixes and HTML special characters are escaped.
