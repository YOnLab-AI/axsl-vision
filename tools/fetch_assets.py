#!/usr/bin/env python3
"""Fetch the versioned, SHA-256 locked AXSL Vision asset packs (Python 3.10+).

Only exact manifest members under the three Android asset roots or offlinevoice/libs
may be installed. All members are checked before any existing file is replaced.
Each final replacement is atomic; a filesystem error during placement may leave
an already-verified subset installed. Rerunning safely completes that pack.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import stat
import struct
import sys
import tempfile
import time
import urllib.parse
import urllib.request
import zipfile

MAX_PACK_BYTES = 2_000_000_000
MAX_ENTRY_BYTES = 400_000_000
MAX_UNPACKED_BYTES = 2_500_000_000
MAX_MANIFEST_BYTES = 10_000_000
MAX_ENTRIES = 5000
MAX_PACKS = 16
CHUNK = 1024 * 1024
DOWNLOAD_SECONDS = 1800
RELEASE_PREFIX = "https://github.com/YOnLab-AI/axsl-vision/releases/download/v0.1/"
REDIRECT_HOSTS = {"github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com"}
ASSET_PREFIXES = ("app/src/main/assets/", "vision/src/main/assets/", "offlinevoice/src/main/assets/")
MODEL_SUFFIXES = {".onnx", ".tflite", ".zip", ".bin", ".npy", ".npz", ".task"}
MEDIA_SUFFIXES = {".jpg", ".jpeg", ".png", ".webp", ".gif", ".wav", ".mp4", ".webm", ".ogg", ".mp3", ".flac"}
SHA = re.compile(r"[0-9a-f]{64}\Z")
WINDOWS_RESERVED = {"CON", "PRN", "AUX", "NUL"} | {f"{p}{n}" for p in ("COM", "LPT") for n in range(1, 10)}


class AssetError(Exception):
    """A manifest, integrity, path, or bounded-download check failed."""


def file_sha(path: Path) -> str:
    value = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(CHUNK), b""):
            value.update(chunk)
    return value.hexdigest()


def safe_relative(value: str) -> str:
    if not isinstance(value, str) or not value or len(value) > 512:
        raise AssetError("Invalid asset path")
    if "\\" in value or ":" in value or any(ord(c) < 32 for c in value):
        raise AssetError("Invalid asset path: " + value)
    parts = value.split("/")
    if PurePosixPath(value).is_absolute() or any(p in ("", ".", "..") for p in parts):
        raise AssetError("Asset path must be a canonical relative path: " + value)
    if any(len(p) > 255 or p[-1:] in (" ", ".") or p.split(".")[0].upper() in WINDOWS_RESERVED for p in parts):
        raise AssetError("Asset path has a platform alias: " + value)
    return value


def is_link(path: Path) -> bool:
    try:
        info = path.lstat()
    except FileNotFoundError:
        return False
    return stat.S_ISLNK(info.st_mode) or bool(getattr(info, "st_file_attributes", 0) & getattr(stat, "FILE_ATTRIBUTE_REPARSE_POINT", 0x400))


def destination(root: Path, relative: str) -> Path:
    safe_relative(relative)
    current = root
    if is_link(current):
        raise AssetError("Repository root cannot be a symlink or junction")
    for part in relative.split("/"):
        current = current / part
        if is_link(current):
            raise AssetError("Refusing symlink or junction asset path: " + relative)
    try:
        current.resolve().relative_to(root)
    except ValueError as error:
        raise AssetError("Asset path escapes repository: " + relative) from error
    if current.exists() and not current.is_file():
        raise AssetError("Asset destination is not a file: " + relative)
    return current


def integer(value, low: int, high: int, name: str) -> int:
    if type(value) is not int or not low <= value <= high:
        raise AssetError("Out-of-bounds " + name)
    return value


def valid_sha(value, name: str):
    if not isinstance(value, str) or not SHA.fullmatch(value):
        raise AssetError("Invalid SHA-256 for " + name)


def load_lock(path: Path) -> dict:
    if path.stat().st_size > MAX_MANIFEST_BYTES:
        raise AssetError("Asset manifest exceeds size bound")
    lock = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(lock, dict) or lock.get("schema") != "axsl-vision-asset-lock-v1":
        raise AssetError("Unsupported asset manifest schema")
    packs = lock.get("packs")
    if not isinstance(packs, list) or not 1 <= len(packs) <= MAX_PACKS:
        raise AssetError("Invalid pack count")
    seen, ids, total_entries = set(), set(), 0
    for pack in packs:
        if not isinstance(pack, dict):
            raise AssetError("Pack record must be an object")
        pack_id = pack.get("id", "")
        if not isinstance(pack_id, str) or not re.fullmatch(r"[a-z0-9-]{1,64}", pack_id) or pack_id in ids:
            raise AssetError("Invalid or duplicate pack ID")
        ids.add(pack_id)
        group = pack.get("group", pack_id)
        if group not in ("runtime", "models", "demos"):
            raise AssetError("Invalid asset group")
        pack["group"] = group
        url = pack.get("url", "")
        if not isinstance(url, str) or not url.startswith(RELEASE_PREFIX) or not re.fullmatch(r"[a-zA-Z0-9._-]+[.]zip", url[len(RELEASE_PREFIX):]):
            raise AssetError("Pack URL must be the pinned AXSL Vision v0.1 GitHub release")
        valid_sha(pack.get("sha256"), pack_id)
        integer(pack.get("bytes"), 1, MAX_PACK_BYTES, "pack bytes")
        entries = pack.get("entries")
        if not isinstance(entries, list) or not 1 <= len(entries) <= MAX_ENTRIES:
            raise AssetError("Invalid pack entry count")
        total_entries += len(entries)
        if total_entries > MAX_ENTRIES:
            raise AssetError("Manifest entry count exceeds bound")
        unpacked = 0
        for entry in entries:
            if not isinstance(entry, dict):
                raise AssetError("Asset record must be an object")
            name = safe_relative(entry.get("path"))
            suffix = PurePosixPath(name).suffix.lower()
            allowed = (group == "runtime" and name.startswith("offlinevoice/libs/") and suffix == ".aar") or (
                any(name.startswith(prefix) for prefix in ASSET_PREFIXES) and
                ((group == "models" and suffix in MODEL_SUFFIXES) or (group == "demos" and suffix in MEDIA_SUFFIXES)))
            if not allowed:
                raise AssetError("Asset path is outside its group's allowed roots/types: " + name)
            if name.casefold() in seen:
                raise AssetError("Duplicate or case-alias destination: " + name)
            seen.add(name.casefold())
            valid_sha(entry.get("sha256"), name)
            unpacked += integer(entry.get("bytes"), 0, MAX_ENTRY_BYTES, "entry bytes")
            if not isinstance(entry.get("license"), str) or not entry["license"].strip():
                raise AssetError("Missing individual asset license: " + name)
            safe_relative(entry.get("license_file"))
        if unpacked > MAX_UNPACKED_BYTES:
            raise AssetError("Pack uncompressed size exceeds bound")
    return lock


def matches(path: Path, entry: dict) -> bool:
    return path.is_file() and path.stat().st_size == entry["bytes"] and file_sha(path) == entry["sha256"]


class HttpsReleaseRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, message, headers, new_url):
        url = urllib.parse.urlsplit(new_url)
        if url.scheme != "https" or url.hostname not in REDIRECT_HOSTS or url.username or url.password:
            raise AssetError("Release download redirected outside the allowed HTTPS hosts")
        return super().redirect_request(request, fp, code, message, headers, new_url)


def obtain_pack(pack: dict, target: Path, local_packs: Path | None):
    if local_packs is not None:
        source = local_packs / pack["url"].rsplit("/", 1)[1]
        if is_link(local_packs) or is_link(source) or not source.is_file():
            raise AssetError("Local pack is missing or is a symlink/junction: " + source.name)
        if source.stat().st_size != pack["bytes"]:
            raise AssetError("Local pack size mismatch: " + pack["id"])
        with source.open("rb") as incoming:
            copy_bounded(incoming, target, pack)
    else:
        request = urllib.request.Request(pack["url"], headers={"User-Agent": "AXSL-Vision-Asset-Bootstrap/0.1"})
        opener = urllib.request.build_opener(HttpsReleaseRedirect())
        with opener.open(request, timeout=30) as incoming:
            final_url = urllib.parse.urlsplit(incoming.geturl())
            if final_url.scheme != "https" or final_url.hostname not in REDIRECT_HOSTS:
                raise AssetError("Unexpected release download origin")
            copy_bounded(incoming, target, pack)


def copy_bounded(incoming, target: Path, pack: dict):
    value, count, deadline = hashlib.sha256(), 0, time.monotonic() + DOWNLOAD_SECONDS
    with target.open("xb") as out:
        while True:
            if time.monotonic() > deadline:
                raise AssetError("Pack download exceeded time bound")
            chunk = incoming.read(min(CHUNK, pack["bytes"] - count + 1))
            if not chunk:
                break
            count += len(chunk)
            if count > pack["bytes"]:
                raise AssetError("Pack download exceeded exact byte bound")
            out.write(chunk)
            value.update(chunk)
        out.flush()
        os.fsync(out.fileno())
    if count != pack["bytes"] or value.hexdigest() != pack["sha256"]:
        raise AssetError("Pack byte count/SHA-256 mismatch: " + pack["id"])


def bounded_central_directory(path: Path):
    # Reject oversized central directories before ZipFile allocates ZipInfo objects.
    with path.open("rb") as stream:
        size = path.stat().st_size
        stream.seek(max(0, size - 65557))
        tail = stream.read(65557)
    offset = tail.rfind(b"PK\x05\x06")
    if offset < 0 or len(tail) - offset < 22:
        raise AssetError("ZIP end record missing")
    _, disk, central_disk, disk_count, count, central_size, central_offset, comment = struct.unpack("<4s4H2LH", tail[offset:offset + 22])
    if disk or central_disk or disk_count != count or not 1 <= count <= MAX_ENTRIES or central_size > 8_000_000:
        raise AssetError("ZIP central directory count/size exceeds bound")
    if count == 65535 or central_offset == 0xFFFFFFFF or central_size == 0xFFFFFFFF or offset + 22 + comment != len(tail):
        raise AssetError("Unsupported ZIP64 or trailing ZIP data")


def stage_pack(archive_path: Path, pack: dict, staging: Path) -> list[tuple[dict, Path]]:
    bounded_central_directory(archive_path)
    expected = {entry["path"]: entry for entry in pack["entries"]}
    staged = []
    with zipfile.ZipFile(archive_path) as archive:
        members = archive.infolist()
        names = [info.filename for info in members]
        if len(names) != len(set(names)) or set(names) != set(expected):
            raise AssetError("ZIP members must match the exact manifest allowlist without duplicates")
        for index, info in enumerate(members):
            if info.orig_filename != info.filename:
                raise AssetError("ZIP member name contains a hidden alias")
            safe_relative(info.orig_filename)
            entry = expected[info.filename]
            mode = info.external_attr >> 16
            file_type = stat.S_IFMT(mode)
            if info.is_dir() or file_type not in (0, stat.S_IFREG) or info.flag_bits & 1:
                raise AssetError("ZIP member must be an unencrypted regular file: " + info.filename)
            if info.file_size != entry["bytes"] or info.file_size > MAX_ENTRY_BYTES:
                raise AssetError("ZIP member size differs from manifest: " + info.filename)
            path = staging / (str(index) + ".verified")
            count, value = 0, hashlib.sha256()
            with archive.open(info) as incoming, path.open("xb") as out:
                while True:
                    chunk = incoming.read(min(CHUNK, entry["bytes"] - count + 1))
                    if not chunk:
                        break
                    count += len(chunk)
                    if count > entry["bytes"]:
                        raise AssetError("ZIP member exceeds exact byte bound")
                    out.write(chunk)
                    value.update(chunk)
                out.flush()
                os.fsync(out.fileno())
            if count != entry["bytes"] or value.hexdigest() != entry["sha256"]:
                raise AssetError("ZIP member SHA-256 mismatch: " + info.filename)
            staged.append((entry, path))
    return staged


def sync_assets(root: Path, lock_path: Path, *, group: str = "all", verify_only: bool = False,
                local_packs: Path | None = None) -> dict:
    root = Path(os.path.abspath(root))
    if is_link(root) or not root.is_dir():
        raise AssetError("Repository root must be an existing real directory")
    root = root.resolve()
    lock = load_lock(Path(lock_path))
    if group not in ("all", "runtime", "models", "demos"):
        raise AssetError("Unknown asset group")
    packs = [p for p in lock["packs"] if group == "all" or p["group"] == group]
    result = {"packs": len(packs), "verified_files": 0, "installed_files": 0}
    for pack in packs:
        destinations = [(entry, destination(root, entry["path"])) for entry in pack["entries"]]
        missing = [(entry, path) for entry, path in destinations if not matches(path, entry)]
        if verify_only and missing:
            raise AssetError("Missing or mismatched assets: " + ", ".join(e["path"] for e, _ in missing[:10]))
        if missing:
            needed = pack["bytes"] + sum(e["bytes"] for e in pack["entries"]) + 16_000_000
            if shutil.disk_usage(root).free < needed:
                raise AssetError("Insufficient free space for verified staging: " + pack["id"])
            with tempfile.TemporaryDirectory(prefix=".axsl-assets-", dir=root) as temporary:
                staging = Path(temporary)
                archive_path = staging / "download.zip"
                try:
                    obtain_pack(pack, archive_path, Path(local_packs) if local_packs is not None else None)
                    staged = stage_pack(archive_path, pack, staging)
                    # Check every destination before replacing even the first existing file.
                    for entry, _ in staged:
                        destination(root, entry["path"])
                    for entry, verified in staged:
                        final = destination(root, entry["path"])
                        final.parent.mkdir(parents=True, exist_ok=True)
                        destination(root, entry["path"])
                        os.replace(verified, final)
                        result["installed_files"] += 1
                except (zipfile.BadZipFile, EOFError, RuntimeError) as error:
                    raise AssetError("Invalid or damaged asset pack: " + pack["id"]) from error
        result["verified_files"] += len(pack["entries"])
    return result


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    choice = parser.add_mutually_exclusive_group()
    choice.add_argument("--group", choices=("runtime", "models", "demos", "all"), default="all")
    choice.add_argument("--all", action="store_true", help="Fetch/verify every group (default)")
    parser.add_argument("--verify-only", action="store_true", help="Hash existing files without downloading")
    parser.add_argument("--local-packs", type=Path, help="Use exact release ZIP filenames from this local directory")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--lock", type=Path, help="Default: ROOT/assets/asset-lock.json")
    args = parser.parse_args(argv)
    try:
        result = sync_assets(args.root, args.lock or args.root / "assets/asset-lock.json", group=args.group,
                             verify_only=args.verify_only, local_packs=args.local_packs)
    except (AssetError, OSError, ValueError) as error:
        print("Asset bootstrap failed: " + str(error), file=sys.stderr)
        return 1
    print(f"Verified {result['verified_files']} files in {result['packs']} packs; installed {result['installed_files']} files.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
