# Optional runtime, model, and demo payloads

The Git repository contains source, tests, small metadata, notices, and the
original AXSL Vision UI graphics. Large payloads are separate `v0.1` release
assets. `asset-lock.json` fixes every ZIP's size/SHA-256 and every member's
repository-relative path, size/SHA-256, individual terms, and source binding.

From the repository root, using Python 3.10 or newer:

```console
python tools/fetch_assets.py --all
python tools/fetch_assets.py --all --verify-only
```

The groups may also be fetched separately:

```console
python tools/fetch_assets.py --group runtime
python tools/fetch_assets.py --group models
python tools/fetch_assets.py --group demos
```

| Group | Files | Compressed ZIP bytes | Contents |
|---|---:|---:|---|
| runtime | 1 | 38,296,452 | Official Sherpa ONNX 2.0.0 AAR |
| models | 49 | 833,502,416 | Model weights, model archives, and PANNs frontend tensors |
| demos | 611 | 46,825,603 | Media and thumbnails serving 312 admitted demo IDs |

All three are needed to reproduce the full reference app. Fetching only a subset
does not make the other models or demos available. The script reuses existing
files only when their exact size and SHA-256 match.

For an offline copy of the exact release ZIPs:

```console
python tools/fetch_assets.py --all --local-packs /path/to/downloaded/release
```

`--verify-only` reads existing files and does not download or install anything.
The tool accepts only this project's pinned HTTPS release URLs, exact member
allowlists, bounded sizes, and regular files. Traversal, platform path aliases,
duplicate ZIP members, extra members, hash mismatches, symlinks, and junctions
are rejected. Every member is staged and checked before existing files in that
pack are replaced. Replacement is atomic per file. If a filesystem failure
interrupts placement, rerunning completes the verified pack. Failed staging is
removed automatically; external originals are never followed or edited.

## Individual terms remain separate

The source-code Apache-2.0 license does not relicense SDKs, model weights, or
demo media. Inspect each entry's `license`, `license_file`, and `source_bindings`.
In particular, 33 project-authored demo fixtures retain their original permission
for distribution with this free app; that statement is not a general open-source
or Creative Commons grant. CC BY/CC BY-SA media retain their attribution,
license, and modification requirements. ESC audio is from the separately
licensed ESC-10 subset, with per-clip notices preserved.

`asset-exclusions.json` records items omitted from the ZIPs: the former ambiguous
`go-coco-86` media and its unique thumbnail are omitted from public distribution;
the three original signage guides and five original category illustrations stay
in Git with their sources and notices. Previous AXSL site photographs and the
corporate logo are not distributed. Moonshine and YOLO26 weights are not in these
packs. The old `one/models/provenance.json` is not an asset allowlist.

Asset hash checks establish file identity, not model accuracy, dataset ground
truth, or the results of any physical-device test.
