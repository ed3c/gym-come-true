#!/usr/bin/env python3
"""Compute stable semantic identities for Android APKs and Web artifact trees.

Whole-file artifact hashes are retained as transport identities. Semantic payload
identities are derived from sorted entry paths + sizes + SHA-256 values so that
APK signing-block / ZIP metadata drift does not masquerade as product payload drift.

This tool is standard-library only and network-free.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import tempfile
import zipfile
from pathlib import Path
from typing import Iterable

SCHEMA_VERSION = 1


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def canonical_digest(rows: Iterable[tuple[str, int, str]]) -> str:
    h = hashlib.sha256()
    for rel, size, digest in sorted(rows):
        h.update(rel.encode("utf-8"))
        h.update(b"\0")
        h.update(str(size).encode("ascii"))
        h.update(b"\0")
        h.update(digest.encode("ascii"))
        h.update(b"\n")
    return h.hexdigest()


def apk_identity(path: Path) -> dict[str, object]:
    if not path.is_file():
        raise ValueError(f"APK not found: {path}")
    rows: list[tuple[str, int, str]] = []
    names: set[str] = set()
    with zipfile.ZipFile(path) as zf:
        for info in zf.infolist():
            if info.is_dir():
                continue
            if info.filename in names:
                raise ValueError(f"duplicate APK entry: {info.filename}")
            names.add(info.filename)
            data = zf.read(info)
            rows.append((info.filename, len(data), sha256_bytes(data)))
    if not rows:
        raise ValueError("APK contains no files")
    return {
        "schemaVersion": SCHEMA_VERSION,
        "kind": "ANDROID_APK",
        "transportSha256": sha256_file(path),
        "semanticPayloadSha256": canonical_digest(rows),
        "entryCount": len(rows),
        "byteLength": path.stat().st_size,
        "identitySemantics": "transport hash includes signing/container bytes; semantic payload hash covers sorted ZIP entry path+size+content hash",
    }


def tree_identity(path: Path) -> dict[str, object]:
    if not path.is_dir():
        raise ValueError(f"tree not found: {path}")
    rows: list[tuple[str, int, str]] = []
    for item in sorted(p for p in path.rglob("*") if p.is_file()):
        rel = item.relative_to(path).as_posix()
        rows.append((rel, item.stat().st_size, sha256_file(item)))
    if not rows:
        raise ValueError("tree contains no files")
    return {
        "schemaVersion": SCHEMA_VERSION,
        "kind": "WEB_TREE",
        "semanticPayloadSha256": canonical_digest(rows),
        "entryCount": len(rows),
        "byteLength": sum(size for _, size, _ in rows),
        "identitySemantics": "semantic payload hash covers sorted relative path+size+content hash; archive wrapper metadata is excluded",
    }


def write_receipt(receipt: dict[str, object], output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(receipt, indent=2, sort_keys=True) + "\n"
    tmp = output.with_suffix(output.suffix + ".tmp")
    tmp.write_text(text, encoding="utf-8")
    os.replace(tmp, output)


def self_test() -> None:
    with tempfile.TemporaryDirectory() as td:
        root = Path(td)
        a = root / "a.apk"
        b = root / "b.apk"
        c = root / "c.apk"
        # Same logical payload, intentionally different ZIP metadata/compression.
        with zipfile.ZipFile(a, "w", compression=zipfile.ZIP_STORED) as zf:
            info = zipfile.ZipInfo("classes.dex", (2020, 1, 1, 0, 0, 0))
            zf.writestr(info, b"payload")
        with zipfile.ZipFile(b, "w", compression=zipfile.ZIP_DEFLATED) as zf:
            info = zipfile.ZipInfo("classes.dex", (2025, 2, 2, 2, 2, 2))
            info.compress_type = zipfile.ZIP_DEFLATED
            zf.writestr(info, b"payload")
        with zipfile.ZipFile(c, "w", compression=zipfile.ZIP_STORED) as zf:
            zf.writestr("classes.dex", b"mutated")
        ia, ib, ic = apk_identity(a), apk_identity(b), apk_identity(c)
        assert ia["transportSha256"] != ib["transportSha256"]
        assert ia["semanticPayloadSha256"] == ib["semanticPayloadSha256"]
        assert ia["semanticPayloadSha256"] != ic["semanticPayloadSha256"]

        t1 = root / "tree1"; t2 = root / "tree2"
        t1.mkdir(); t2.mkdir()
        (t1 / "index.html").write_text("same", encoding="utf-8")
        (t2 / "index.html").write_text("same", encoding="utf-8")
        assert tree_identity(t1)["semanticPayloadSha256"] == tree_identity(t2)["semanticPayloadSha256"]
        (t2 / "index.html").write_text("different", encoding="utf-8")
        assert tree_identity(t1)["semanticPayloadSha256"] != tree_identity(t2)["semanticPayloadSha256"]
    print("PASS artifact identity self-test")


def main() -> int:
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("self-test")
    apk = sub.add_parser("apk")
    apk.add_argument("input", type=Path)
    apk.add_argument("--output", type=Path, required=True)
    tree = sub.add_parser("tree")
    tree.add_argument("input", type=Path)
    tree.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.command == "self-test":
        self_test(); return 0
    if args.command == "apk":
        receipt = apk_identity(args.input)
    else:
        receipt = tree_identity(args.input)
    write_receipt(receipt, args.output)
    print(json.dumps(receipt, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
