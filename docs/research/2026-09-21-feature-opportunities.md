# PassTick feature opportunities

Research date: 2026-09-21

Type: product analysis. This document does not make an implementation decision.

## Decision summary

PassTick is already a strong offline pass wallet. The largest verified gaps against competitors and user demand are: fast access to the code, the ability to create and migrate cards, data safety (backup, restore, original-file export), pass life cycle (expiration and archive), and scanner compatibility.

Owner decision on 2026-09-21: the app stays fully offline. Do not add the INTERNET permission. Vendor pass updates, logo download, and cloud backup are out of scope. Local backup and restore are the replacement.

## Method

Sources:

- Full code inventory of this repository, with `file:line` references.
- Competitor material: WalletPasses, Pass2U, PassWallet, Catima, FossWallet, VirtualCards, Google Wallet 2026.
- User demand: Play Store reviews, Reddit threads, and the public issue trackers of Catima and FossWallet (open issues sorted by reactions).
- Verification commands: `rg "CODABAR|UPC_A|UPC_E|CODE_93"`, `rg "snooze|automaticallyMarkPast|pkpasses"`, `rg "notificationLockScreenDetail|\.create\("`, and direct reads of the cited files. Run these again before you change the related code.

## Constraints that shape the backlog

- Offline-first. No analytics. No network permission.
- One application artifact. A Wear OS companion is a second artifact and needs an explicit owner decision.
- Internal app-private storage. `content://` URIs and the Storage Access Framework at process boundaries.
- One Activity, Compose Material 3, Navigation 3, ViewModels, immutable UI state, StateFlow.
- Homepage delete is a move to Trash with undo. Permanent deletion needs an explicit confirmation.
- Locations open with a `geo:` intent. Do not add a map SDK.

## Coverage today (verified)

| Area | Current capability | Reference |
| --- | --- | --- |
| Pass formats | Apple pkpass, esPass, internal JSON, image-only, PDF-only, ZIP | `reader/AppleStylePassReader.kt:29`, `ui/UnzipPassController.kt:120-199` |
| Import | VIEW and SEND intents, multi-file SEND, SAF picker, single PDF, photo picker, camera | `AndroidManifest.xml:77-109`, `PassTickApp.kt:212-264` |
| Code detection in import | 9 formats, 4 rotations, inverted colors, up to 40 PDF pages, review screen with selection | `imports/DocumentImportProcessor.kt:33-47`, `ui/compose/ImportReviewScreen.kt:543-590` |
| Barcode display | 9 formats, integer-scale render, quiet zones, multiple codes per pass, full screen, brightness boost, HDR headroom, flashlight, alt text | `model/pass/PassBarCodeFormat.kt`, `ui/barcode/PassCodePresentation.kt:204-233` |
| Search and order | Diacritic-insensitive multi-term search, 4 sort orders, manual drag order | `ui/state/PassSearch.kt:6-33`, `model/comparator/PassSortOrder.kt` |
| Organization | Tags with color and icon, pinned, archived, Trash with undo, 7-day purge | `ui/compose/CategorySettingsScreen.kt`, `repository/PassRepository.kt:500-509` |
| Temporal model | One time-zone-aware event model, Today section, timeline, agenda calendar | `domain/timeline/PassTimeline.kt:63-86`, `ui/compose/TimelineScreen.kt` |
| Reminders | AlarmManager, boot and timezone rescheduling, per-pass lead time, access window, notification actions, protected public version | `reminder/AndroidReminderScheduler.kt`, `reminder/NotificationPolicy.kt:40-130` |
| Calendar | Add to calendar, auto-add after import, already-in-calendar check | `functions/AddToCalendar.kt`, `platform/PlatformActions.kt:67-76` |
| Location | `geo:` intent from detail and from the notification action | `platform/PlatformActions.kt:130-145` |
| Export | Image export with layout options, print, file export, share | `repository/ImageExportOptions.kt`, `printing/PrintHelper.kt`, `repository/PassRepository.kt:513-538` |
| Widget | Glance list widget for active and upcoming passes | `widget/PassOverviewWidget.kt:41-160` |
| Privacy | Biometric app lock, per-pass protection, blur, separate locked section, FLAG_SECURE, no backup, no analytics, no network | `platform/PassAuthenticator.kt`, `PassTickApp.kt:164-182`, `AndroidManifest.xml:16-18` |
| UI platform | Material 3 Expressive, dynamic color, AMOLED, adaptive list-detail, deep links | `ui/theme/PassTheme.kt:19-56`, `navigation/PassDeepLink.kt:10-20` |

These items are already better than the closest open-source competitor (FossWallet): image and PDF import, code detection from import, Trash with undo, print, and image export.

## Promise debt (verified)

| Claim or expectation | Reality | Reference |
| --- | --- | --- |
| README line 60: notification snooze | The notification has two actions only: open code and directions | `README.md:60`, `reminder/NotificationPolicy.kt:7` |
| Setting `automaticallyMarkPast` | Stored and tested, but no Settings UI and no consumer | `repository/SettingsRepository.kt:155,253` |
| README line 77: share the original pass file | Share and export always re-zip to `.espass` | `README.md:77`, `repository/PassRepository.kt:513-538` |
| README line 73: lock-screen detail | The value is wired end to end, but no Settings UI calls the setter | `repository/SettingsRepository.kt:212`, `reminder/AndroidReminderScheduler.kt:108-169` |
| Barcode format support | CODABAR, CODE_93, UPC_A, and UPC_E are absent. Catima supports 13 formats | `model/pass/PassBarCodeFormat.kt:5-15` |
| Apple `.pkpasses` bundle | No MIME type, no reader, and no manifest entry | `repository/PassImportTypes.kt:7-19` |
| App-level backup | `allowBackup=false` and all domains excluded. No export-all and no restore | `AndroidManifest.xml:16-18` |

Promise debt is cheap to close and prevents user trust loss. Item 1 to item 4 are small. Item 3 is also an interop bug: Google Wallet can import `.pkpass`, not `.espass`.

## Opportunity backlog

Value and effort are relative to this codebase.

### P0 — high value, low risk

#### P0-1. Instant access to the code

- User problem: at a checkout or a gate, the user needs the barcode in one step, not three.
- Evidence: Catima issue #737 "Barcode-showing widget" (9 reactions). FossWallet ships home screen shortcuts and pass display over the lock screen. Reddit asks for a widget that lets the user pick the card. Google Wallet uses a Quick Settings tile and Wear OS complications.
- Work: a single-pass Glance widget that shows the code, a Quick Settings tile, and per-pass app shortcuts.
- Acceptance criteria: the widget shows the barcode of one chosen pinned pass. A tap opens the code view. The widget updates after an edit or at the next day boundary. Protected passes stay hidden unless the user unlocks them. No new permission.
- Likely files: `widget/PassOverviewWidget.kt`, `widget/PassWidgetSnapshot.kt`, new `widget/PassCodeWidget.kt`, `res/xml/pass_overview_widget_info.xml`, new tile service, `res/xml/shortcuts.xml`, `AndroidManifest.xml`, `MainViewModel.kt`.
- Tests: widget snapshot selection without protection, shortcut target resolution, tile state after settings change.

#### P0-2. Create and duplicate passes

- User problem: many users hold loyalty cards that have no file. They cannot add these cards today.
- Evidence: Catima and Pass2U make card creation a core flow. FossWallet can create and download passes. This repository already supports creation and artwork replacement with no UI entry point.
- Work: a create flow, a duplicate action, and artwork replacement from the device.
- Acceptance criteria: the user makes a pass with a title, a type, an optional barcode, and optional fields. Duplicate copies all content and metadata. Artwork replacement uses SAF and keeps the original file. Unit tests cover create, duplicate, and artwork update.
- Likely files: `repository/PassRepository.kt:116,134,374-381`, `ui/compose/EditPassScreen.kt`, `ui/compose/PassCustomizationScreen.kt`, `navigation/AppDestination.kt`, `MainViewModel.kt`.
- Tests: repository tests for create and duplicate, UI test for the create flow.

#### P0-3. Data safety: backup, restore, original export

- User problem: a lost phone means lost passes. `allowBackup=false` makes this risk real.
- Evidence: Catima issue #1464 "Automatic export of vault for backing up to cloud" (12 reactions) and issue #791 "Multi-device / sync support" (14 reactions). Pass2U backs up to Google Drive. FossWallet imports and exports `.pkpasses`. Play Store reviews of WalletPasses ask how to move passes to a new phone.
- Work: export all passes and metadata to one archive, restore from that archive, optional automatic export to a chosen SAF folder, keep and share the original imported file, and support `.pkpasses` import and export.
- Acceptance criteria: export writes one archive with all passes, tags, pins, notes, and artwork. Restore on a clean install gives the same list. Restore on a non-empty install merges and does not duplicate. Share uses the original `.pkpass` bytes when they exist. Characterisation tests come before the storage change.
- Likely files: new `repository/PassArchive.kt`, `repository/PassRepository.kt`, `model/AndroidFileSystemPassStore.kt`, `ui/compose/SettingsScreen.kt`, `PassTickApp.kt`, `repository/PassImportTypes.kt`.
- Tests: round-trip export and restore, merge without duplicates, original-file retention.

#### P0-4. Pass life cycle: expiration, archive, snooze

- User problem: expired passes mix with active passes. The user misses the expiry date. A reminder cannot be postponed.
- Evidence: Pass2U sends expiration notifications. FossWallet issue #241 asks for an "Expired passes" group and separation of passes without a date. README line 60 promises snooze. The setting `automaticallyMarkPast` is already stored.
- Work: an expiration notification with a lead time, an expired badge, an "Expired" filter, automatic archive for past events, and a snooze notification action.
- Acceptance criteria: a pass with an `expirationDate` sends one notification before the date. Past passes show a badge and appear in the Expired filter. Snooze postpones the reminder by a fixed interval. The dead setting gets wired or removed.
- Likely files: `reminder/AndroidReminderScheduler.kt`, `reminder/NotificationPolicy.kt`, `domain/timeline/PassTimeline.kt`, `ui/compose/PassHomeScreen.kt`, `repository/SettingsRepository.kt`.
- Tests: reminder scheduling at a fixed clock, expiration boundary in the timeline, snooze action.

#### P0-5. Barcode format parity

- User problem: some loyalty and membership cards use formats that the app cannot render.
- Evidence: Catima supports 13 formats, including CODABAR, CODE_93, UPC_A, and UPC_E. Users of these formats cannot migrate.
- Work: add the four formats to the enum, the ZXing mapping, the renderer quiet zones, and the detector list.
- Acceptance criteria: each new format renders on screen and is detected from an image. Unit tests cover encode and decode round trips. The import review screen shows the new formats.
- Likely files: `model/pass/PassBarCodeFormat.kt`, `ui/barcode/CrispBarcodeRenderer.kt`, `imports/DocumentImportProcessor.kt`.
- Tests: renderer golden tests and detector tests for the four formats.

### P1 — medium effort

#### P1-1. Scanner compatibility mode

- User problem: some point-of-sale scanners cannot read a code from a phone screen.
- Evidence: FossWallet issue #746 asks for a configurable barcode size and issue #724 asks for brightness in the full-screen view only. Catima added a barcode width choice in the full-screen view. Scanner vendors publish the standard causes: glare, low brightness, small size, and an unclean quiet zone.
- Work: a manual size control, keep-screen-on, a white background option, an extra quiet zone, and a rotation control in the code view.
- Acceptance criteria: the user sets the code size once and the app keeps it. The screen stays awake while the code shows. The options apply to the single code, the pager, and the full-screen view.
- Likely files: `ui/barcode/PassCodePresentation.kt`, `ui/barcode/CrispBarcodeRenderer.kt`, `repository/SettingsRepository.kt`.
- Tests: settings persistence and renderer output at each size step.

#### P1-2. Live camera scan to create a pass

- User problem: the user holds a plastic card and cannot add it.
- Evidence: Catima scans cards with the camera. Pass2U and Google Wallet scan barcodes. The repository already detects codes from photos, so the missing part is only the live surface.
- Work: a camera scanner that creates a pass from the first code, with the format and the value prefilled.
- Acceptance criteria: the scanner reads the 13 formats, stops at the first result, and opens the editor with the data. The permission is requested only for this flow. The photo-import path keeps working.
- Likely files: new `ui/compose/ScanPassScreen.kt`, `imports/DocumentImportProcessor.kt`, `navigation/AppDestination.kt`, `AndroidManifest.xml`.
- Tests: decode unit tests with fixed frames, UI test for the prefill.

#### P1-3. Migration importers

- User problem: users who leave another wallet must re-enter every card by hand.
- Evidence: Catima imports from FidMe, Loyalty Card Keychain, Voucher Vault, and its own export. VirtualCards markets a Stocard import. The Stocard service closed and its users look for a replacement.
- Work: importers for the Catima export file, the FidMe export, the Loyalty Card Keychain export, and the Voucher Vault export.
- Acceptance criteria: each importer maps cards to passes, reports skipped rows, and never overwrites an existing pass.
- Likely files: new `imports/MigrationImporters.kt`, `ui/compose/ImportSourceSheet.kt`, `repository/PassRepository.kt`.
- Tests: fixture files per source with expected pass counts.

#### P1-4. Order by use and persistent list state

- User problem: the card that the user needs now is at the bottom of a long list.
- Evidence: Catima issue #1184 asks for "Order by most commonly used" (5 reactions). FossWallet issue #665 reports that the selected tag and the filter state reset after a visit to a pass.
- Work: a use counter per pass, a "Most used" sort order, and a remembered filter and scroll state.
- Acceptance criteria: the counter increases when the code shows. The sort order applies immediately. The selected filter and the collapsed sections survive navigation.
- Likely files: `repository/FilePassMetadataStore.kt`, `model/comparator/PassSortOrder.kt`, `MainViewModel.kt`, `ui/compose/PassHomeScreen.kt`.
- Tests: counter persistence, sort order with equal counts.

### P2 — owner decision

#### P2-1. Wear OS or Gadgetbridge sync

Catima shipped a Wear OS companion in 2026 after years of requests. FossWallet issue #742 asks for Gadgetbridge sync. This work adds a second application artifact and conflicts with a persistent decision. Decide before any design work.

#### P2-2. Location-based reminders

Pass2U and WalletPasses show passes by location. A geofence needs the location permission, runs in the background, and costs battery. Decide if the value is worth the permission. The current temporal model stays the source of truth for reminders.

#### P2-3. Localisation

The app is English only, and many Compose strings are hardcoded. Competitors ship many languages. A translation platform and a string extraction pass are the work items. Per-app language support is a separate small item.

#### P2-4. Loyalty balance and points

Stocard and Google Wallet show a balance on the card. A balance field needs a quick update control and a history, or it becomes stale and useless. Decide if this belongs to the pass editor or to a separate loyalty flow.

#### P2-5. Company and logo database

Catima issue #89 (12 reactions) asks for a list of companies with logos and card numbers. A bundled list keeps the app offline. The list needs a licence and a maintenance plan.

## Decision record: no network access

The owner denied the INTERNET permission on 2026-09-21. Consequences:

- Do not add vendor pass updates through `webServiceURL` and `authenticationToken`.
- Do not download logos or templates.
- Do not add cloud backup or account sync.
- Keep the privacy policy and the F-Droid listing accurate.

The substitute is P0-3: local backup, restore, and original-file export.

## Non-goals

- Account or cloud sync as a first-class feature.
- NFC payment or NFC pass exchange.
- An embedded map SDK.
- Analytics, crash reporting with a network upload, or advertising.
- A Stocard-style offer feed or a shopping list.

## Slicing proposal

| Slice | Content | Reason |
| --- | --- | --- |
| 1 | P0-1 | Daily value, no new permission, few file conflicts |
| 2 | P0-2 and P1-2 | One user story: add any card |
| 3 | P0-3 | Trust and interop, but storage risk. Characterisation tests first |
| 4 | P0-4 and P0-5 | Close promise debt and format parity |
| 5 | P1-1 and P1-4 | Field reliability and list ergonomics |

Each slice ends with unit tests, lint, the complexity check, and a release build. Run instrumented and Compose tests on API 29 and API 37 when an emulator is available. A release stays blocked until a physical Android 10 or later device passes the test.

## Open questions

1. Does the backup archive include settings, or passes and metadata only?
2. Does the backup need encryption, and which key flow?
3. Do we wire `automaticallyMarkPast` to the archive action, or delete the setting?
4. Which slice starts first, and how many parallel worker sessions?
