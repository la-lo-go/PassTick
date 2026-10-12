"""Publishes the PassTick store listing and the release bundle to Google Play.

Every run validates only and discards its edit, unless it gets --upload.

Usage:
    python tools/play/publish.py [--track internal] [--skip-bundle] [--skip-listing] [--upload]
Requires: google-api-python-client and google-auth (see requirements.txt).
The service account key path comes from --json-key or PASSTICK_PLAY_JSON_KEY.
"""

from __future__ import annotations

import argparse
import os
import sys
from pathlib import Path

from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError
from googleapiclient.http import MediaFileUpload

ROOT = Path(__file__).resolve().parents[2]
METADATA = ROOT / "fastlane" / "metadata" / "android" / "en-US"
BUNDLE = ROOT / "android" / "build" / "outputs" / "bundle" / "release" / "android-release.aab"
GRADLE_BUILD = ROOT / "android" / "build.gradle.kts"
LANGUAGE = "en-US"
PACKAGE = "dev.lalogo.passtick"
SCOPE = "https://www.googleapis.com/auth/androidpublisher"

# Play listing limits.
LIMITS = {"title.txt": 30, "short_description.txt": 80, "full_description.txt": 4000}
CHANGELOG_LIMIT = 500

# Play image types with their local files.
SCREENSHOT_TYPE = "phoneScreenshots"


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8").strip()


def version_code() -> int:
    for line in GRADLE_BUILD.read_text(encoding="utf-8").splitlines():
        if line.strip().startswith("versionCode"):
            return int(line.split("=")[1].strip())
    raise SystemExit("versionCode not found in android/build.gradle.kts")


def check_listing() -> dict[str, str]:
    texts = {name: read_text(METADATA / name) for name in LIMITS}
    for name, limit in LIMITS.items():
        if not texts[name]:
            raise SystemExit(f"{name} is empty.")
        if len(texts[name]) > limit:
            raise SystemExit(f"{name} has {len(texts[name])} characters. The limit is {limit}.")
    return texts


def changelog(code: int) -> str:
    path = METADATA / "changelogs" / f"{code}.txt"
    if not path.is_file():
        raise SystemExit(f"Missing changelog {path.relative_to(ROOT)}.")
    text = read_text(path)
    if len(text) > CHANGELOG_LIMIT:
        raise SystemExit(f"{path.name} has {len(text)} characters. The limit is {CHANGELOG_LIMIT}.")
    return text


def upload_images(edits, edit_id: str, package: str, image_type: str, files: list[Path]) -> None:
    edits.images().deleteall(packageName=package, editId=edit_id, language=LANGUAGE, imageType=image_type).execute()
    for file in files:
        media = MediaFileUpload(str(file), mimetype="image/png")
        edits.images().upload(
            packageName=package, editId=edit_id, language=LANGUAGE, imageType=image_type, media_body=media
        ).execute()
        print(f"  {image_type}: {file.name}")


def sync_listing(edits, edit_id: str, package: str) -> None:
    texts = check_listing()
    edits.listings().update(
        packageName=package,
        editId=edit_id,
        language=LANGUAGE,
        body={
            "title": texts["title.txt"],
            "shortDescription": texts["short_description.txt"],
            "fullDescription": texts["full_description.txt"],
        },
    ).execute()
    print("  listing texts")
    images = METADATA / "images"
    screenshots = sorted((images / SCREENSHOT_TYPE).glob("*.png"), key=lambda p: int(p.stem))
    if not screenshots:
        raise SystemExit("No phone screenshots found.")
    upload_images(edits, edit_id, package, SCREENSHOT_TYPE, screenshots)
    upload_images(edits, edit_id, package, "featureGraphic", [images / "featureGraphic.png"])
    upload_images(edits, edit_id, package, "icon", [images / "icon.png"])


def sync_bundle(edits, edit_id: str, package: str, track: str) -> None:
    if not BUNDLE.is_file():
        raise SystemExit(f"Missing {BUNDLE.relative_to(ROOT)}. Run :android:bundleRelease first.")
    code = version_code()
    notes = changelog(code)
    media = MediaFileUpload(str(BUNDLE), mimetype="application/octet-stream", resumable=True)
    uploaded = edits.bundles().upload(packageName=package, editId=edit_id, media_body=media).execute()
    if uploaded["versionCode"] != code:
        raise SystemExit(f"The bundle has versionCode {uploaded['versionCode']}. build.gradle.kts has {code}.")
    edits.tracks().update(
        packageName=package,
        editId=edit_id,
        track=track,
        body={
            "track": track,
            "releases": [
                {
                    "status": "draft",
                    "versionCodes": [str(code)],
                    "releaseNotes": [{"language": LANGUAGE, "text": notes}],
                }
            ],
        },
    ).execute()
    print(f"  bundle versionCode {code} on track {track}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--json-key", default=os.environ.get("PASSTICK_PLAY_JSON_KEY"))
    parser.add_argument("--package", default=PACKAGE)
    parser.add_argument("--track", default="internal", help="internal, alpha, beta, or production")
    parser.add_argument("--skip-bundle", action="store_true")
    parser.add_argument("--skip-listing", action="store_true")
    parser.add_argument("--upload", action="store_true", help="commit the edit. Without it the edit is discarded.")
    args = parser.parse_args()

    if not args.json_key or not Path(args.json_key).is_file():
        raise SystemExit("Set --json-key or PASSTICK_PLAY_JSON_KEY to the service account key file.")
    if args.skip_bundle and args.skip_listing:
        raise SystemExit("Nothing to do.")
    if not args.skip_listing:
        check_listing()
    if not args.skip_bundle:
        changelog(version_code())

    credentials = service_account.Credentials.from_service_account_file(args.json_key, scopes=[SCOPE])
    service = build("androidpublisher", "v3", credentials=credentials, cache_discovery=False)
    edits = service.edits()
    edit_id = edits.insert(packageName=args.package, body={}).execute()["id"]
    mode = "UPLOAD" if args.upload else "VALIDATE ONLY"
    print(f"{mode} {args.package} (edit {edit_id})")
    try:
        if not args.skip_listing:
            sync_listing(edits, edit_id, args.package)
        if not args.skip_bundle:
            sync_bundle(edits, edit_id, args.package, args.track)
        edits.validate(packageName=args.package, editId=edit_id).execute()
        if args.upload:
            edits.commit(packageName=args.package, editId=edit_id).execute()
            print("Committed.")
        else:
            edits.delete(packageName=args.package, editId=edit_id).execute()
            print("Valid. The edit was discarded. Run again with --upload to publish.")
    except HttpError as error:
        edits.delete(packageName=args.package, editId=edit_id).execute()
        print(f"Google Play rejected the edit: {error.reason}", file=sys.stderr)
        raise SystemExit(1) from error


if __name__ == "__main__":
    main()
