"""Locate or download the vanilla Minecraft client jar used for vanilla icons, names, and tags."""

from __future__ import annotations

import argparse
import glob
import hashlib
import json
import os
import sys
import urllib.request
import zipfile
from pathlib import Path

MC_VERSION = "1.21.1"
MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
REPO_ROOT = Path(__file__).resolve().parents[2]
CACHE_JAR = REPO_ROOT / "build" / "wiki" / f"client-{MC_VERSION}.jar"


def _jar_version(path: str) -> str | None:
    try:
        with zipfile.ZipFile(path) as jar:
            return json.loads(jar.read("version.json")).get("id")
    except (OSError, KeyError, ValueError, zipfile.BadZipFile):
        return None


def find_client_jar() -> Path | None:
    env = os.environ.get("RNGTECH_MC_CLIENT_JAR")
    if env and Path(env).is_file():
        return Path(env)
    if CACHE_JAR.is_file():
        return CACHE_JAR
    gradle_home = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))
    for candidate in glob.glob(str(gradle_home / "caches" / "ng_execute" / "*" / "client.jar")):
        if _jar_version(candidate) == MC_VERSION:
            return Path(candidate)
    return None


def _fetch_json(url: str) -> dict:
    with urllib.request.urlopen(url, timeout=60) as response:
        return json.load(response)


def download_client_jar() -> Path:
    manifest = _fetch_json(MANIFEST_URL)
    entry = next(v for v in manifest["versions"] if v["id"] == MC_VERSION)
    download = _fetch_json(entry["url"])["downloads"]["client"]
    CACHE_JAR.parent.mkdir(parents=True, exist_ok=True)
    partial = CACHE_JAR.with_suffix(".part")
    digest = hashlib.sha1()
    with urllib.request.urlopen(download["url"], timeout=120) as response, open(partial, "wb") as out:
        while chunk := response.read(1 << 16):
            digest.update(chunk)
            out.write(chunk)
    if digest.hexdigest() != download["sha1"]:
        partial.unlink()
        raise RuntimeError(f"SHA-1 mismatch for Minecraft {MC_VERSION} client jar")
    partial.replace(CACHE_JAR)
    return CACHE_JAR


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--fetch", action="store_true", help="download the client jar from Mojang if no local copy exists")
    args = parser.parse_args()
    jar = find_client_jar()
    if jar is None and args.fetch:
        jar = download_client_jar()
    if jar is None:
        print(f"No Minecraft {MC_VERSION} client jar found; run with --fetch to download it.", file=sys.stderr)
        return 1
    print(jar)
    return 0


if __name__ == "__main__":
    sys.exit(main())
