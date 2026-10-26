# Asset bootstrap

`fetch_assets.py` uses only Python 3.10+ standard-library modules. Run it from any
directory; its default repository root is the parent of `tools/`.

```console
python tools/fetch_assets.py --all
python tools/fetch_assets.py --group models --verify-only
python -m unittest discover -s tools/tests -v
```

See [the asset guide](../assets/README.md) for groups, sizes, offline ZIP use,
integrity checks, and individual license boundaries. Network downloads require
no account token. `--root` and `--lock` support isolated reproducibility checks;
both default to this checkout. `--local-packs` reads the exact locked release ZIP
filenames from an existing directory and applies the same checks as HTTPS.

The tests exercise actual temporary files and ZIPs, including preservation on
pack/member hash failure, late-member failure before any replacement, exact
member allowlists, duplicate names, traversal/absolute/platform-alias paths,
oversized or missing members, archive symlinks, destination/parent symlinks,
group selection, and verification without downloading. Operating systems that
forbid creating test symlinks explicitly skip those two tests.
