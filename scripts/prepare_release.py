"""Validate a version tag and stage assets built by GitHub Actions."""

import argparse
import hashlib
import re
import shutil
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("tag")
    parser.add_argument("--check", action="store_true", help="Validate metadata without staging assets")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    build = (root / "app/build.gradle.kts").read_text(encoding="utf-8")
    match = re.search(r'^\s*versionName\s*=\s*"([^"]+)"', build, re.MULTILINE)
    if not match:
        parser.error("versionName not found in app/build.gradle.kts")
    version = match.group(1)
    if not re.fullmatch(r"\d+\.\d+\.\d+", version) or args.tag != f"v{version}":
        parser.error(f"Expected tag v{version}, received {args.tag!r}")
    changelog = (root / "CHANGELOG.md").read_text(encoding="utf-8")
    section = re.search(
        rf"^## {re.escape(version)} - \d{{4}}-\d{{2}}-\d{{2}}\s*\n(.*?)(?=^## |\Z)",
        changelog,
        re.MULTILINE | re.DOTALL,
    )
    if not section or not section.group(1).strip():
        parser.error(f"No release notes found for {version}")
    print(f"Validated {args.tag} and its changelog")
    if args.check:
        return
    apk = root / "app/build/outputs/apk/release/app-release.apk"
    if not apk.is_file():
        parser.error("Release APK is missing; run :app:assembleRelease first")
    output = root / "dist"
    output.mkdir(exist_ok=True)
    asset = output / f"WiFiCloak-{version}.apk"
    shutil.copyfile(apk, asset)
    digest = hashlib.sha256(asset.read_bytes()).hexdigest()
    (output / "checksums.sha256").write_text(f"{digest}  {asset.name}\n", encoding="utf-8")
    notes = section.group(1).strip() + "\n\n直接安装下方 APK；升级时直接覆盖安装。下载后可使用 checksums.sha256 校验文件。\n"
    (output / "RELEASE_NOTES.md").write_text(notes, encoding="utf-8")
    print(f"Staged {asset.name} and checksums.sha256")


if __name__ == "__main__":
    main()
