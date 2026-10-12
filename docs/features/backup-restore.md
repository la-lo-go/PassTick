# Feature brief: data safety (backup, restore, original file)

Slice: 3 of the opportunity backlog. See `docs/research/2026-09-21-feature-opportunities.md`, item P0-3.

## Goal

The user moves all passes and their data to another device with one file, and shares the original imported pass file.

## User problem

`allowBackup=false`. A lost phone loses every pass. The app stays offline, so a local archive replaces a cloud backup.

## Scope

1. Backup archive: export all passes and metadata to one ZIP. Restore merges into a non-empty install without duplicates.
2. Original file: keep the imported original bytes and share them when they exist.
3. Apple `.pkpasses` bundle import: a ZIP of `.pkpass` files imports every pass in it.

## Decisions

- Archive format: one ZIP with
  - `backup.json` (format version, export timestamp, pass count),
  - `metadata/pass-metadata.json`, `metadata/pass-pinned.json`, `metadata/pass-protection.json`, `metadata/classifier_state.json`,
  - `passes/<id>/...` copied verbatim (main.json, artwork, source.pdf, source.pkpass).
  Trashed passes are included.
- Merge rule: dedupe by pass id. An id that already exists is skipped and counted. Nothing is overwritten.
- Restore is atomic per pass: extract to a temp directory first, then move into `files/passes/<id>`.
- Path safety: every id must pass `safePassIdOrNull`. Reject the whole archive when one id is unsafe.
- Original retention: the import writes the received bytes to `source.pkpass` in the pass directory. `prepareShare` returns the original file when present, else re-zips as today.
- `.pkpasses` import: detect by MIME `application/vnd.apple.pkpasses` or the `.pkpasses` extension, expand the bundle, import each entry through the existing single-file path, and report the count.
- Out of scope, recorded as follow-up: `.pkpasses` export (needs valid `pass.json` generation) and automatic export to a chosen SAF folder (needs a persisted URI permission and a trigger).

## Repository contract

```kotlin
data class ArchiveRestoreSummary(val restored: Int, val skipped: Int, val failed: Int)

suspend fun exportArchive(destination: Uri): Result<Int>
suspend fun importArchive(source: Uri): Result<ArchiveRestoreSummary>
suspend fun prepareShare(id: String): Result<Uri>  // unchanged signature, original bytes when present
```

## UI

Settings gains a "Data" section:

- "Export all passes" -> `CreateDocument("application/zip")`, suggested name `passtick-backup-<date>.zip`.
- "Restore from backup" -> `OpenDocument`, MIME `application/zip`.
- Results go through the existing snackbar message flow.

## Tests

- Characterisation (instrumented, before the storage change): pass directory layout after create; export re-zips; metadata stores round-trip.
- Archive: round trip on a clean store; merge skips an existing id; an unsafe id rejects the archive; a trashed pass keeps its timestamp.
- Bundle: a `.pkpasses` with two `.pkpass` entries imports two passes.
- Original: an import retains `source.pkpass`; `prepareShare` returns it.
- JVM: archive entry-name validation and the summary arithmetic.

## Working limits

- English for code, comments, UI text, and commits.
- No commit and no push from a subagent. The coordinator commits.
- Do not touch `SettingsScreen.kt` in the owner's uncommitted changes until the UI task; never revert the owner's edits.
- Characterisation tests come before any persistence change.
- Scratch files only in `%TEMP%\opencode`. Stay inside `C:\dev\PassAndroid`.

## Validation

```
.\gradlew.bat :android:testDebugUnitTest
.\gradlew.bat :android:lintDebug
.\gradlew.bat :android:detektComplexity
.\gradlew.bat :android:assembleRelease
.\gradlew.bat :android:assembleDebugAndroidTest
```
