# Feature brief: pass life cycle and barcode parity

Slice: 4 of the opportunity backlog. See `docs/research/2026-09-21-feature-opportunities.md`, items P0-4 and P0-5.

## Goal

Expired passes stop mixing with active ones, the user hears about an expiry in time, a reminder can be postponed, and four more barcode formats render and decode.

## User problem

An expired pass keeps its place in the list and the user misses the expiry date. A reminder cannot be postponed, although the README promises snooze. Some loyalty and membership cards use formats the app cannot render.

## Scope

1. Expiration: a distinct expiry field, an "Expired" badge, an "Expired" filter, and an expiration notification with the existing lead time.
2. Snooze: a notification action that shows the reminder again after a fixed interval.
3. Automatic archive: wire the stored `automaticallyMarkPast` setting to a real effect and expose it in Settings.
4. Barcode parity: CODABAR, CODE_93, UPC_A, and UPC_E render, decode, and appear in the import review.

## Decisions

- Expiry is a **new nullable field**, never a change to the existing `calendarTimeSpan` fallback. `PassSnapshot.expiresAt` and `PassUiModel.expiresAt` come from `validTimespans.firstOrNull()?.to`. Sorting, Today, the widget, the calendar, and image export keep reading `calendarTimeSpan` exactly as today.
- Expired means `expiresAt` is in the past. The Expired filter is a virtual category (`EXPIRED_PASSES_CATEGORY_ID`), like Pinned or Archived, added to the validation set in `MainViewModel` and to the drawer.
- The badge follows the existing `CategoryBadge`/lock-badge pattern in `TicketFeed`.
- The expiration notification uses the same lead minutes as event reminders, gets its own reminder id suffix (`:expiration`) and its own notification id namespace so it never overwrites an event reminder, and does not own the lifecycle.
- Snooze postpones by a fixed 10 minutes, uses a new `NotificationAction.SNOOZE`, and the receiver bypasses the policy CANCEL branch, which would otherwise drop a reminder whose event already started.
- Automatic archive runs when the setting is on, the pass event has **ended** (`normalizedTimeSpan().endsAt` before now), and the pass is neither archived nor trashed. The effect compares before it writes, so the `setArchived` notification cannot re-trigger it in a loop.
- Barcode parity is one atomic change: the enum, the ZXing mapping, `isLinear`, the quiet zones, the import review labels, the reader's format strings, and the test catalog land together, because two exhaustive `when` blocks and one test fail to compile until they do.
- Extend `BarCode.getFormatFromString` for the four new formats so a `.pkpass` or `.espass` that carries one does not fall back to QR.

## Out of scope

- A lock-screen-detail Settings row (`notificationLockScreenDetail`).
- A per-pass expiration lead time.
- Changing the Today or sorting behaviour of expiration-only passes.

## Likely files

| File | Change |
| --- | --- |
| `model/pass/PassBarCodeFormat.kt` | Four enum entries and the ZXing mapping |
| `ui/barcode/CrispBarcodeRenderer.kt` | `isLinear` and `quietZonePixels` |
| `ui/compose/ImportReviewScreen.kt` | Format labels |
| `model/pass/BarCode.kt` | Format strings |
| `repository/PassRepository.kt` | `expiresAt` in the snapshot |
| `ui/state/MainUiState.kt`, `MainViewModel.kt` | `expiresAt`, the Expired category, the automatic-archive effect |
| `ui/compose/PassHomeScreen.kt`, `TicketFeed.kt` | Filter, drawer item, badge |
| `reminder/PassReminder.kt`, `NotificationPolicy.kt`, `AndroidReminderScheduler.kt` | Expiration reminder, snooze action, notification ids |
| `ui/compose/PassDetailScreen.kt` | Snooze label in the reminder actions |
| `ui/AppNavigation.kt`, `ui/state/ScreenActions.kt`, `ui/compose/SettingsScreen.kt` | Automatic-archive row and wiring |
| `res/values/strings.xml` | Labels and messages |
| `android/src/test/resources/test-passes/catalog.json` | Four new catalog entries |

## Tests

- Characterization first: how `validTimespans` maps into the snapshot; the timeline boundary; the reminder and policy behaviour.
- Timeline and reminder: an expiration reminder has its own id, keeps the lead, and never owns the lifecycle; snooze bypasses the CANCEL branch.
- Home: an expired pass appears in the Expired filter and carries the badge; an active pass does not.
- Automatic archive: a past pass archives once, an already archived pass is untouched, and a trashed pass is untouched.
- Formats: the renderer test covers 13 formats with valid payloads and asserts `isLinear` for the new linear ones; the detector test decodes each new format; the catalog test passes.

## Working limits

- English for code, comments, UI text, and commits.
- No commit and no push from a subagent. The coordinator commits.
- Characterization tests before the temporal or persistence change.
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
