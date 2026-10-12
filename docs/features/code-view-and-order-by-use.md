# Feature brief: code view options and order by use

Slice: 5 of the opportunity backlog. See `docs/research/2026-09-21-feature-opportunities.md`, items P1-1 and P1-4.

## Goal

The code view survives difficult scanners, and the card the user needs most is easy to reach.

## User problem

Some point-of-sale scanners cannot read a code from a phone screen (glare, size, quiet zone, orientation). The card the user needs now sits at the bottom of a long list, and the selected filter and the scroll position reset.

## Scope

1. Code view options: a size control, an extra quiet zone, a white full-screen surround, a rotation control, and keep-screen-on.
2. Order by use: a use counter per pass, a "Most used" sort order.
3. Remembered list state: the selected filter survives process death, and the scroll position survives navigation.

## Decisions

- One immutable `CodeViewOptions(sizeStep: Int, whiteSurround: Boolean, extraQuietZone: Boolean, rotateQuarterTurn: Boolean, keepScreenOn: Boolean)` travels from `AppNavigation` into `PassDetailScreen` and `ExpandedPassCodeDialog`. All code surfaces use it: the single code, the pager, and the full-screen dialog.
- Size: `codeHeight()` multiplies by a step factor (`0.85f`, `1f`, `1.25f`). The dialog keeps its aspect ratio and applies the same factor. The renderer keeps integer module scaling; a smaller height reduces the module scale.
- Extra quiet zone: a `renderMatrix`/`renderBitmap` parameter adds modules around the code. Keep integer scaling and the existing per-format base quiet zones. Linear formats gain a vertical zone.
- White surround: the full-screen dialog replaces its dark scrim with white when enabled. The code surfaces are already white.
- Rotation: rotate the rendered bitmap in the presentation layer by one quarter turn and swap the target width and height. Do **not** rotate the activity: `expandedCodePassId` is `remember`, and an activity recreation would close the dialog.
- Keep-screen-on: one owner in `PassTickApp` following the `FLAG_SECURE` pattern, active on the pass detail destination when the pass shows a code and the setting is on.
- Settings fields: `codeSizeStep: Int = 1`, `codeWhiteSurround: Boolean = false`, `codeExtraQuietZone: Boolean = false`, `codeRotateQuarterTurn: Boolean = false`, `codeKeepScreenOn: Boolean = true`.
- Use counter: a sixth map in `FilePassMetadataStore` (`useCount`), `PassRepository.recordUse(id)`, and `PassSnapshot.useCount`. One display hook per surface: the detail body, the full-screen dialog, and the home long-press preview. The effect key is the pass id, so a recomposition or a pager swipe never counts twice.
- `PassSortOrder.MOST_USED`: descending use count, then id. `toComparator()` (used only by an unused class and one test) gets a documented stable fallback.
- Remembered filter: `selectedCategoryId` moves into `AppSettings`. The scroll position survives navigation through a hoisted `rememberSaveable` `LazyListState`; it is not written to DataStore (write churn and jank).

## Out of scope

- Screen rotation through `activity.requestedOrientation`.
- A DataStore-backed scroll offset.
- Counting a display from the widget.

## Likely files

| File | Change |
| --- | --- |
| `ui/barcode/CrispBarcodeRenderer.kt` | Extra quiet zone parameter |
| `ui/barcode/PassCodePresentation.kt` | Options in the preview, the image, and the dialog; bitmap rotation |
| `ui/compose/PassDetailScreen.kt` | Options into the card, the pager, and the dialog; the use-counter hook |
| `ui/PassTickApp.kt` | Keep-screen-on owner; hoisted `LazyListState` |
| `ui/AppNavigation.kt` | Pass the options and the list state |
| `ui/compose/SettingsScreen.kt` | Code section rows |
| `ui/state/ScreenActions.kt`, `MainUiState.kt`, `MainViewModel.kt` | Settings actions, `RecordPassUse`, `MOST_USED` |
| `repository/SettingsRepository.kt` | New fields and setters |
| `repository/FilePassMetadataStore.kt` | `useCount` map |
| `repository/PassRepository.kt` | `recordUse`, `PassSnapshot.useCount` |
| `model/comparator/PassSortOrder.kt` | `MOST_USED` |
| `res/values/strings.xml` | New labels |

## Tests

- Renderer: extra quiet zone keeps module alignment; a size step changes the module scale.
- Metadata: counter round trip, increment, remove, exact key set.
- Repository: `recordUse` increments and notifies.
- ViewModel: `MOST_USED` order with equal counts; filter persistence.
- Settings: extend `DataStoreSettingsRepositoryTest` for the new fields.
- Compose: the new Settings rows; the sort selector offers "Most used".

## Working limits

- English for code, comments, UI text, and commits.
- No commit and no push from a subagent. The coordinator commits.
- Characterisation tests before the persistence change (`FilePassMetadataStore`).
- Do not touch the owner's uncommitted files.
- Scratch files only in `%TEMP%\opencode`. Stay inside `C:\dev\PassAndroid`.

## Validation

```
.\gradlew.bat :android:testDebugUnitTest
.\gradlew.bat :android:lintDebug
.\gradlew.bat :android:detektComplexity
.\gradlew.bat :android:assembleRelease
.\gradlew.bat :android:assembleDebugAndroidTest
```
