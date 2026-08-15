#!/usr/bin/env python3
"""Create a content-addressed Taiwan source snapshot receipt from a local file.

This command deliberately has no network client. An operator must download a source
through an approved process, inspect the response, and pass the exact local bytes.
The generated receipt is always HASH_VERIFIED + DENY; legal review and production
promotion are separate signed state transitions.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import stat
import sys
import tempfile
from datetime import date
from pathlib import Path
from typing import BinaryIO, NoReturn

BUFFER_SIZE = 1024 * 1024
DEFAULT_MAX_BYTES = 512 * 1024 * 1024
READ_ONLY_MODE = stat.S_IRUSR | stat.S_IRGRP | stat.S_IROTH
SLUG = re.compile(r"^[a-z0-9][a-z0-9._-]{1,127}$")
HTTPS = re.compile(r"^https://[^\s]+$")
SYNTHETIC_URL = re.compile(r"^(?:https|repo)://[^\s]+$")
ARCHIVE_PREFIX = re.compile(r"^(?:repo|evidence|s3|gs|az|oci|ipfs|file)://[^\s]+$")
ARTIFACT_SUFFIX = {
    "PDF": ".pdf",
    "CSV": ".csv",
    "JSON": ".json",
    "XML": ".xml",
    "HTML": ".html",
    "ZIP": ".zip",
    "TEXT": ".txt",
}


class CaptureError(RuntimeError):
    """Raised when local source capture cannot produce a trustworthy receipt."""


def fail(message: str) -> NoReturn:
    raise CaptureError(message)


def parse_iso_date(value: str) -> str:
    try:
        return date.fromisoformat(value).isoformat()
    except ValueError as error:
        raise argparse.ArgumentTypeError(f"invalid ISO date {value!r}") from error


def safe_slug(value: str) -> str:
    if not SLUG.fullmatch(value):
        raise argparse.ArgumentTypeError(
            "expected 2-128 lowercase slug characters: a-z, 0-9, dot, underscore, hyphen"
        )
    return value


def ensure_directory_without_symlink(path: Path, label: str) -> None:
    if path.is_symlink():
        fail(f"{label} cannot be a symlink: {path}")
    path.mkdir(parents=True, exist_ok=True)
    if path.is_symlink() or not path.is_dir():
        fail(f"{label} is not a trusted directory: {path}")


def fsync_directory(path: Path) -> None:
    if not hasattr(os, "O_DIRECTORY"):
        return
    descriptor = os.open(path, os.O_RDONLY | os.O_DIRECTORY)
    try:
        os.fsync(descriptor)
    finally:
        os.close(descriptor)


def open_regular_file_without_following_symlinks(path: Path) -> BinaryIO:
    flags = os.O_RDONLY
    if hasattr(os, "O_NOFOLLOW"):
        flags |= os.O_NOFOLLOW
    try:
        descriptor = os.open(path, flags)
    except OSError as error:
        fail(f"cannot open input without following symlinks: {path}: {error}")
    file = os.fdopen(descriptor, "rb")
    metadata = os.fstat(file.fileno())
    if not stat.S_ISREG(metadata.st_mode):
        file.close()
        fail(f"input is not a regular file: {path}")
    return file


def hash_file(path: Path) -> tuple[str, int]:
    digest = hashlib.sha256()
    size = 0
    with open_regular_file_without_following_symlinks(path) as source:
        while chunk := source.read(BUFFER_SIZE):
            digest.update(chunk)
            size += len(chunk)
    return digest.hexdigest(), size


def capture_bytes(
    source_path: Path,
    archive_root: Path,
    source_id: str,
    suffix: str,
    max_bytes: int,
) -> tuple[Path, str, int]:
    if max_bytes <= 0:
        fail("max bytes must be positive")
    if source_path.is_symlink():
        fail(f"symlink input is not admitted: {source_path}")

    ensure_directory_without_symlink(archive_root, "archive root")
    source_directory = archive_root / source_id
    ensure_directory_without_symlink(source_directory, "source archive directory")

    digest = hashlib.sha256()
    size = 0
    temporary_path: Path | None = None
    try:
        descriptor, temporary_name = tempfile.mkstemp(
            prefix=".capture-",
            suffix=".tmp",
            dir=source_directory,
        )
        temporary_path = Path(temporary_name)
        with open_regular_file_without_following_symlinks(source_path) as source:
            with os.fdopen(descriptor, "wb") as destination:
                while chunk := source.read(BUFFER_SIZE):
                    size += len(chunk)
                    if size > max_bytes:
                        fail(f"input exceeds maximum admitted size of {max_bytes} bytes")
                    digest.update(chunk)
                    destination.write(chunk)
                destination.flush()
                os.fsync(destination.fileno())

        if size == 0:
            fail("empty source artifacts are not admitted")

        sha256 = digest.hexdigest()
        target = source_directory / f"{sha256}{suffix}"
        if target.exists() or target.is_symlink():
            existing_sha, existing_size = hash_file(target)
            if existing_sha != sha256 or existing_size != size:
                fail(f"content-address collision or corrupted archive target: {target}")
            temporary_path.unlink()
        else:
            os.replace(temporary_path, target)
        temporary_path = None

        archived_sha, archived_size = hash_file(target)
        if archived_sha != sha256 or archived_size != size:
            fail(f"archived bytes do not match the capture receipt: {target}")
        os.chmod(target, READ_ONLY_MODE)
        fsync_directory(source_directory)
        return target, sha256, size
    finally:
        if temporary_path is not None:
            temporary_path.unlink(missing_ok=True)


def write_json_atomically(
    path: Path,
    payload: dict[str, object],
    *,
    replace_manifest: bool,
) -> None:
    if path.parent.is_symlink():
        fail(f"manifest parent cannot be a symlink: {path.parent}")
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.is_symlink():
        fail(f"manifest output cannot be a symlink: {path}")
    if path.exists() and not replace_manifest:
        fail(
            f"manifest already exists; refuse silent evidence-receipt replacement: {path}. "
            "Use --replace-manifest only after explicit operator review."
        )

    descriptor, temporary_name = tempfile.mkstemp(
        prefix=f".{path.name}.",
        suffix=".tmp",
        dir=path.parent,
    )
    temporary_path = Path(temporary_name)
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8", newline="\n") as output:
            json.dump(payload, output, ensure_ascii=False, indent=2, sort_keys=False)
            output.write("\n")
            output.flush()
            os.fsync(output.fileno())
        os.replace(temporary_path, path)
        fsync_directory(path.parent)
    finally:
        temporary_path.unlink(missing_ok=True)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-id", required=True, type=safe_slug)
    parser.add_argument("--snapshot-id", required=True, type=safe_slug)
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--artifact-kind", required=True, choices=tuple(ARTIFACT_SUFFIX))
    parser.add_argument("--canonical-url", required=True)
    parser.add_argument("--retrieval-url")
    parser.add_argument("--captured-at", type=parse_iso_date, default=date.today().isoformat())
    parser.add_argument("--source-modified-at", type=parse_iso_date)
    parser.add_argument("--effective-from", type=parse_iso_date)
    parser.add_argument("--effective-until", type=parse_iso_date)
    parser.add_argument("--media-type", required=True)
    parser.add_argument("--license-id", required=True)
    parser.add_argument("--attribution-text", required=True)
    parser.add_argument("--note", required=True)
    parser.add_argument("--archive-root", required=True, type=Path)
    parser.add_argument(
        "--archive-uri-prefix",
        default="evidence://local/taiwan-source-snapshots",
        help="Content-addressed evidence URI prefix; never a mutable live URL.",
    )
    parser.add_argument("--manifest-out", required=True, type=Path)
    parser.add_argument("--max-bytes", type=int, default=DEFAULT_MAX_BYTES)
    parser.add_argument("--synthetic", action="store_true")
    parser.add_argument("--redistributable", action="store_true")
    parser.add_argument(
        "--replace-manifest",
        action="store_true",
        help="Explicitly replace an existing manifest receipt after operator review.",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    arguments = build_parser().parse_args(argv)
    try:
        canonical_pattern = SYNTHETIC_URL if arguments.synthetic else HTTPS
        if not canonical_pattern.fullmatch(arguments.canonical_url):
            fail("canonical URL must be HTTPS; repository synthetic fixtures may use repo://")
        if arguments.retrieval_url and not HTTPS.fullmatch(arguments.retrieval_url):
            fail("retrieval URL must use HTTPS")
        if not ARCHIVE_PREFIX.fullmatch(arguments.archive_uri_prefix):
            fail("archive URI prefix must use an admitted immutable-evidence scheme")
        if not arguments.media_type.strip():
            fail("media type cannot be blank")
        if not arguments.license_id.strip():
            fail("license id cannot be blank")
        if not arguments.attribution_text.strip():
            fail("attribution text cannot be blank")
        if not arguments.note.strip():
            fail("note cannot be blank")
        if (
            arguments.effective_from
            and arguments.effective_until
            and arguments.effective_from > arguments.effective_until
        ):
            fail("effective-from cannot be later than effective-until")

        target, sha256, byte_length = capture_bytes(
            source_path=arguments.input,
            archive_root=arguments.archive_root,
            source_id=arguments.source_id,
            suffix=ARTIFACT_SUFFIX[arguments.artifact_kind],
            max_bytes=arguments.max_bytes,
        )
        archive_uri = (
            f"{arguments.archive_uri_prefix.rstrip('/')}/"
            f"{arguments.source_id}/{target.name}#sha256={sha256}"
        )
        manifest: dict[str, object] = {
            "schemaVersion": 1,
            "snapshotId": arguments.snapshot_id,
            "sourceId": arguments.source_id,
            "state": "HASH_VERIFIED",
            "artifactKind": arguments.artifact_kind,
            "jurisdiction": "TW",
            "canonicalUrl": arguments.canonical_url,
            "retrievalUrl": arguments.retrieval_url,
            "capturedAtIsoDate": arguments.captured_at,
            "sourceModifiedAtIsoDate": arguments.source_modified_at,
            "effectiveFromIsoDate": arguments.effective_from,
            "effectiveUntilIsoDate": arguments.effective_until,
            "mediaType": arguments.media_type.strip(),
            "byteLength": byte_length,
            "sha256": sha256,
            "archiveUri": archive_uri,
            "licenseId": arguments.license_id.strip(),
            "attributionText": arguments.attribution_text.strip(),
            "redistributable": bool(arguments.redistributable),
            "synthetic": bool(arguments.synthetic),
            "legalReviewRef": None,
            "productionUse": "DENY",
            "modelGenerated": False,
            "note": arguments.note.strip(),
        }
        write_json_atomically(
            arguments.manifest_out,
            manifest,
            replace_manifest=bool(arguments.replace_manifest),
        )
        print(
            json.dumps(
                {
                    "manifest": str(arguments.manifest_out),
                    "localArchive": str(target),
                    "sha256": sha256,
                    "byteLength": byte_length,
                    "productionUse": "DENY",
                },
                ensure_ascii=False,
                sort_keys=True,
            )
        )
        return 0
    except CaptureError as error:
        print(f"FAIL {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
