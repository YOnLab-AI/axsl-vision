import hashlib
import importlib.util
import json
import os
from pathlib import Path
import stat
import sys
import tempfile
import unittest
import warnings
import zipfile

TOOL = Path(__file__).resolve().parents[1] / "fetch_assets.py"
spec = importlib.util.spec_from_file_location("fetch_assets", TOOL)
fetch = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = fetch
spec.loader.exec_module(fetch)


def digest(value):
    return hashlib.sha256(value).hexdigest()


class AssetFetchTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.base = Path(self.temp.name)
        self.root = self.base / "repo"
        self.root.mkdir()
        self.packs = self.base / "packs"
        self.packs.mkdir()
        self.target = "vision/src/main/assets/one/media/KO/demo.png"
        self.payload = b"known verified asset"

    def fixture(self, members=None, entry_path=None, entry_size=None, expected_sha=None, group="demos"):
        pack_file = self.packs / "axsl-vision-v0.1-demos.zip"
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", UserWarning)
            with zipfile.ZipFile(pack_file, "w", zipfile.ZIP_DEFLATED) as archive:
                for path, data in (members if members is not None else [(self.target, self.payload)]):
                    archive.writestr(path, data)
        entry = {"path": entry_path or self.target, "sha256": digest(self.payload),
                 "bytes": len(self.payload) if entry_size is None else entry_size,
                 "license": "CC-BY-4.0", "license_file": "licenses/example.txt",
                 "source_url": "https://example.org/asset"}
        manifest = {"schema": "axsl-vision-asset-lock-v1", "packs": [{
            "id": "demos", "group": group,
            "url": "https://github.com/YOnLab-AI/axsl-vision/releases/download/v0.1/" + pack_file.name,
            "sha256": expected_sha or digest(pack_file.read_bytes()),
            "bytes": pack_file.stat().st_size, "entries": [entry]}]}
        lock = self.root / "asset-lock.json"
        lock.write_text(json.dumps(manifest), encoding="utf-8")
        return lock, manifest, pack_file

    def run_fetch(self, lock, **kwargs):
        return fetch.sync_assets(self.root, lock, local_packs=self.packs, **kwargs)

    def assert_no_staging(self):
        self.assertEqual([], list(self.root.glob(".axsl-assets-*")))
        self.assertEqual([], list(self.root.rglob("*.part")))

    def test_verified_pack_installs_exact_file_and_verify_only_needs_no_pack(self):
        lock, _, pack = self.fixture()
        result = self.run_fetch(lock)
        self.assertEqual(self.payload, (self.root / self.target).read_bytes())
        self.assertEqual(1, result["verified_files"])
        pack.unlink()
        result = fetch.sync_assets(self.root, lock, verify_only=True)
        self.assertEqual(1, result["verified_files"])
        self.assert_no_staging()

    def test_pack_sha_mismatch_leaves_existing_file_unchanged(self):
        lock, _, _ = self.fixture(expected_sha="0" * 64)
        target = self.root / self.target
        target.parent.mkdir(parents=True)
        target.write_bytes(b"keep existing")
        with self.assertRaises(fetch.AssetError):
            self.run_fetch(lock)
        self.assertEqual(b"keep existing", target.read_bytes())
        self.assert_no_staging()

    def test_member_sha_mismatch_cannot_replace_existing_file(self):
        lock, _, _ = self.fixture(members=[(self.target, b"x" * len(self.payload))])
        target = self.root / self.target
        target.parent.mkdir(parents=True)
        target.write_bytes(b"original")
        with self.assertRaises(fetch.AssetError):
            self.run_fetch(lock)
        self.assertEqual(b"original", target.read_bytes())
        self.assert_no_staging()

    def test_late_member_corruption_does_not_commit_earlier_valid_member(self):
        second = "vision/src/main/assets/one/media/KO/second.png"
        lock, manifest, _ = self.fixture(members=[(self.target, self.payload), (second, b"bad")])
        manifest["packs"][0]["entries"].append(dict(path=second, bytes=3, sha256=digest(b"YES"),
            license="CC0-1.0", license_file="licenses/example.txt"))
        lock.write_text(json.dumps(manifest), encoding="utf-8")
        target = self.root / self.target
        target.parent.mkdir(parents=True)
        target.write_bytes(b"preserved until whole archive validates")
        with self.assertRaises(fetch.AssetError):
            self.run_fetch(lock)
        self.assertEqual(b"preserved until whole archive validates", target.read_bytes())
        self.assertFalse((self.root / second).exists())
        self.assert_no_staging()

    def test_unexpected_archive_entry_rejects_whole_pack_before_install(self):
        lock, _, _ = self.fixture(members=[(self.target, self.payload), ("vision/src/main/assets/extra.png", b"extra")])
        with self.assertRaises(fetch.AssetError):
            self.run_fetch(lock)
        self.assertFalse((self.root / self.target).exists())
        self.assertFalse((self.root / "vision/src/main/assets/extra.png").exists())
        self.assert_no_staging()

    def test_duplicate_zip_member_is_rejected(self):
        lock, _, _ = self.fixture(members=[(self.target, self.payload), (self.target, self.payload)])
        with self.assertRaises(fetch.AssetError):
            self.run_fetch(lock)
        self.assertFalse((self.root / self.target).exists())
        self.assert_no_staging()

    def test_oversized_member_and_missing_member_are_rejected(self):
        for members in [[(self.target, self.payload + b"extra")], []]:
            with self.subTest(members=members):
                lock, _, _ = self.fixture(members=members)
                with self.assertRaises(fetch.AssetError):
                    self.run_fetch(lock)
                self.assertFalse((self.root / self.target).exists())
                self.assert_no_staging()

    def test_manifest_paths_cannot_traverse_write_absolute_or_escape_asset_roots(self):
        for path in ["../outside.png", "/tmp/outside.png", "C:/outside.png", "C:outside.png",
                     "vision/src/main/assets/../../../../outside.png", "vision\\src\\main\\assets\\x.png",
                     "vision/src/main/assets/NUL.png", "vision/src/main/assets/x.png:stream", "tools/evil.py"]:
            with self.subTest(path=path):
                lock, _, _ = self.fixture(entry_path=path)
                with self.assertRaises(fetch.AssetError):
                    self.run_fetch(lock)
                self.assert_no_staging()

    def test_zip_symlink_is_not_written_as_a_file(self):
        link = zipfile.ZipInfo(self.target)
        link.create_system = 3
        link.external_attr = (stat.S_IFLNK | 0o777) << 16
        lock, _, _ = self.fixture(members=[(link, self.payload)])
        with self.assertRaises(fetch.AssetError):
            self.run_fetch(lock)
        self.assert_no_staging()

    def test_parent_symlink_cannot_write_outside_repo(self):
        outside = self.base / "outside"
        outside.mkdir()
        linked = self.root / "vision"
        try:
            linked.symlink_to(outside, target_is_directory=True)
        except (OSError, NotImplementedError) as error:
            self.skipTest("OS does not permit test symlink: " + str(error))
        lock, _, _ = self.fixture()
        with self.assertRaises(fetch.AssetError):
            self.run_fetch(lock)
        self.assertEqual([], list(outside.rglob("*")))
        self.assert_no_staging()

    def test_destination_symlink_preserves_the_link_and_external_original(self):
        outside = self.base / "original.png"
        outside.write_bytes(b"external original")
        target = self.root / self.target
        target.parent.mkdir(parents=True)
        try:
            target.symlink_to(outside)
        except (OSError, NotImplementedError) as error:
            self.skipTest("OS does not permit test symlink: " + str(error))
        lock, _, _ = self.fixture()
        with self.assertRaises(fetch.AssetError):
            self.run_fetch(lock)
        self.assertTrue(target.is_symlink())
        self.assertEqual(b"external original", outside.read_bytes())
        self.assert_no_staging()

    def test_manifest_size_limit_rejects_before_download_or_install(self):
        lock, manifest, _ = self.fixture()
        manifest["packs"][0]["entries"][0]["bytes"] = fetch.MAX_ENTRY_BYTES + 1
        lock.write_text(json.dumps(manifest), encoding="utf-8")
        with self.assertRaises(fetch.AssetError):
            self.run_fetch(lock)
        self.assert_no_staging()

    def test_malformed_manifest_shapes_fail_with_a_clear_asset_error(self):
        lock, manifest, _ = self.fixture()
        for broken in [[], {"schema": "axsl-vision-asset-lock-v1", "packs": [None]}]:
            lock.write_text(json.dumps(broken), encoding="utf-8")
            with self.assertRaises(fetch.AssetError):
                self.run_fetch(lock)
        manifest["packs"][0]["entries"] = [None]
        lock.write_text(json.dumps(manifest), encoding="utf-8")
        with self.assertRaises(fetch.AssetError):
            self.run_fetch(lock)

    def test_group_selection_does_not_fetch_other_packs(self):
        lock, _, pack = self.fixture()
        pack.unlink()
        result = self.run_fetch(lock, group="models")
        self.assertEqual(0, result["verified_files"])
        self.assertEqual(0, result["packs"])

    def test_existing_correct_files_skip_download(self):
        lock, _, pack = self.fixture()
        target = self.root / self.target
        target.parent.mkdir(parents=True)
        target.write_bytes(self.payload)
        pack.unlink()
        self.assertEqual(1, self.run_fetch(lock)["verified_files"])

    def test_case_alias_destinations_and_nonrelease_urls_are_rejected(self):
        lock, manifest, _ = self.fixture()
        entry = dict(manifest["packs"][0]["entries"][0])
        entry["path"] = entry["path"].replace("demo.png", "DEMO.PNG")
        manifest["packs"][0]["entries"].append(entry)
        lock.write_text(json.dumps(manifest), encoding="utf-8")
        with self.assertRaises(fetch.AssetError):
            self.run_fetch(lock)
        manifest["packs"][0]["entries"].pop()
        manifest["packs"][0]["url"] = "http://example.org/pack.zip"
        lock.write_text(json.dumps(manifest), encoding="utf-8")
        with self.assertRaises(fetch.AssetError):
            self.run_fetch(lock)


if __name__ == "__main__":
    unittest.main()
