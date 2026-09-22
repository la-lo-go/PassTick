# Feature brief: quick code access

Slice: 1 of the opportunity backlog. See `docs/research/2026-09-21-feature-opportunities.md`, item P0-1.

## Goal

The user reaches the barcode of one chosen pass in one step from the home screen, the Quick Settings panel, or the launcher.

## User problem

At a checkout or a gate, the current flow needs several steps: open the app, find the pass, open it, expand the code. Competitors offer faster paths. Catima issue #737 asks for a barcode widget. FossWallet ships home screen shortcuts. Google Wallet uses a Quick Settings tile.

## Scope

Three deliverables. One shared selection rule and one shared privacy rule.

1. A single-pass code widget (Glance).
2. A Quick Settings tile.
3. Dynamic launcher shortcuts for pinned passes.

## Shared rules

### Identification line

The line that identifies a pass in the widget and in a shortcut is the first
visible line from the home card layout configuration. Use the existing pure
function `resolvePassCardTitle` in `ui/state/PassCardLines.kt:125` with
`homeCardSectionOrder`, `hiddenHomeCardSections`, and `categories`. Do not use a
fixed `description` field.

- `WidgetPass` carries the resolved value in a new `primaryLine` field.
- The widget shows `primaryLine` as the main line and the creator as the second line.
- A shortcut uses `primaryLine` as the short and long label.
- The snapshot store reads and writes the new field as optional.

### Selection

One pure function resolves the pass for the widget and the tile:

1. The pass from the new setting `codePassId`, when the pass exists and is not trashed.
2. Else the first pinned pass in the current sort order.
3. Else the next upcoming pass from the existing snapshot.
4. Else `null`. Show an empty state that opens the app.

Store `codePassId` in `AppSettings` through `SettingsRepository`. Add the setter to the interface and to the DataStore implementation. Keep the default `null`.

All widget instances show the same pass. Per-widget state is out of scope.

### Privacy

- Never show a protected pass code, title, or artwork in a widget, a tile label, or a shortcut.
- When `lockAllPasses` is true, show no code and no pass title in these surfaces.
- Reuse the exclusion logic in `widget/PassWidgetSnapshotPublisher.kt:34-53`.
- Do not add a new permission.

## Deliverable 1: single-pass code widget

- Add a Glance widget that shows the barcode image, the pass title, and the issuer.
- Support a small size (2x2) and a wide size (4x2). The barcode keeps its aspect ratio and a quiet zone.
- A tap opens the pass detail through the existing deep link.
- Add a configuration activity. The user picks one pass from the active passes. The activity writes `codePassId`.
- Reuse the existing renderer in `ui/barcode/CrispBarcodeRenderer.kt`. Do not write a second renderer.
- Reuse the refresh scheduling in `widget/PassWidgetRefreshScheduler.kt`. Refresh at the next day boundary and on data change.

## Deliverable 2: Quick Settings tile

- Add a `TileService` with `android:permission="android.permission.BIND_QUICK_SETTINGS_TILE"` and `android:exported="true"`.
- The tile is active when the selection function returns a pass.
- On click, start the activity with `passtick://pass/<id>?view=code`. When the selection is `null`, open the app.
- Update the tile state in `onStartListening`.
- The app lock stays in control. A shortcut or a tile must not bypass the lock screen.

## Deliverable 3: dynamic shortcuts

- Publish dynamic shortcuts for up to four pinned passes with `ShortcutManagerCompat`.
- Each shortcut opens `passtick://pass/<id>?view=code` and uses the pass title as the label.
- Update the shortcuts when the pass list or the pin state changes. Reuse the existing data-change hook in `MainViewModel.kt:164-173`.
- Clear the shortcuts when `lockAllPasses` is true.
- Do not add static shortcuts.

## Likely files

| File | Change |
| --- | --- |
| `widget/PassCodeWidget.kt` | New Glance widget and receiver |
| `widget/CodePassResolver.kt` | New pure selection function |
| `res/xml/pass_code_widget_info.xml` | New widget metadata |
| `ui/compose/PassCodeWidgetConfigActivity.kt` | New configuration activity |
| `quicksettings/PassCodeTileService.kt` | New tile service |
| `shortcuts/PassShortcutsPublisher.kt` | New dynamic shortcut publisher |
| `repository/SettingsRepository.kt` | Add `codePassId` |
| `MainViewModel.kt` | Publish shortcuts on data change |
| `AndroidManifest.xml` | Receiver, tile service, config activity |

## Tests

Unit tests:

1. `CodePassResolver`: chosen pass, first pinned pass, next upcoming pass, protected pass, `lockAllPasses`, missing pass ID, empty list.
2. Settings: extend the existing instrumented test in
   `android/src/androidTest/java/org/ligi/passandroid/repository/DataStoreSettingsRepositoryTest.kt`.
   Set, assert, and reset `codePassId` there. Do not add JVM DataStore tests: DataStore needs an
   Android context and its file replace is not reliable on Windows.
3. Shortcut specs: four-pass limit, pinned order, protection filter, lock-all clear.

Pending device tests (report, do not run):

1. Widget render in both sizes, light and dark theme.
2. Tile active and inactive states.
3. Launcher shortcuts after pin and unpin.

## Out of scope

- Wear OS.
- Lock-screen display.
- Per-widget pass state.
- A live camera scanner.
- Any new dependency. Glance `1.2.0` is already available.

## Working limits

- Stay inside the worktree. Do not read or write outside it.
- Use `%TEMP%\opencode` for scratch files. No other temp directory is approved.
- Do not open Gradle cache sources unless a build error needs them.

## Validation

Run these commands and paste the result in the report:

```
.\gradlew.bat :android:testDebugUnitTest
.\gradlew.bat :android:lintDebug
.\gradlew.bat :android:detektComplexity
.\gradlew.bat :android:assembleRelease
```

## Report

Use the format in the agent prompt. Do not commit and do not push.
