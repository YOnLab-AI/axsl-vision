# Model-centered testing implementation plan

> **For agentic workers:** Use subagent-driven-development for the independent guidance and model-preservation changes, with root integration and physical verification.

**Goal:** Let developers choose an execution model, understand its role, and test that same ID with diverse compatible inputs.

**Architecture:** Add a native model test entry using the existing typed registries and production controllers. Guidance is generated from the documented catalogue; explicit test selections remain separate from dataset language/group. No parallel inference scheduler is introduced.

**Tech stack:** Existing Android Java17 native UI, JDK21/Gradle9.1.0, current local model/runtime assets.

## Design and constraints

The user requested consistent feature/model guidance and model-centered testing. A copy-only change would leave silent model replacement intact; a new batch scheduler would add lifecycle and memory risks. The chosen implementation adds a model test page and fixes selection preservation while reusing existing cancellable controllers.

- AXSL Vision remains the only product brand; versionName stays0.1 and versionCode becomes21.
- Preserve existing package, signing identity, local data, models, asset hashes and the public v0.1 release.
- Use the native Ui tokens. Display feature, model/engine ID, runtime, input conditions and interpretation consistently.
- Default choices are not accuracy recommendations. Code/playback engines are identified explicitly.
- Real input hashes, reference provenance and task-specific metric limits remain intact.

## Tasks

- [x] Guidance: derive all68 ID entries from the documented catalogue; add one consistent native guide and clarify feature descriptions.
- [x] Preservation: regression tests for explicit model identity through input/group changes; implement keep_model test navigation for image/audio controllers.
- [x] Model test screen: unique model selector, compatible feature selector, condition/group/media filters, diverse six-case selection, explicit run actions and model-filtered history.
- [x] Integration: home/tab/settings entry points, current-model guide in workspaces, versionCode21.
- [x] Host verification: 259 Android unit tests,20 Python tests,release build/signature/16KB alignment and lint completed.
- [ ] Device verification: update installed(code21); two normal UI observations passed; final regression and diverse production executions require exclusive phone access.
- [ ] Documentation and Git: update usage/validation evidence; inspect the diff and secret/asset boundaries; publish source and an additive build21 release without replacing v0.1.

## Review focus

- KO/GO data changes must not replace an explicit model. Language mismatch is a test condition.
- No input choice may silently select additional models or start expensive work before explicit action in the model test page.
- Unknown sample difficulty is not labelled easy; input conditions and reference status are preserved.
- Rotation, returning from a controller and new input choices retain model identity and discard stale asynchronous UI results.
- History viewing and export use the same model filter. A single-case metric is not a corpus benchmark.
