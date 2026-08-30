# Material 3 Expressive redesign research

Research date: 2026-08-30

## Decision

Use Material 3 Expressive as a product design system, but keep its preview dependency behind one small theme and component layer.

The current stable Compose BOM is `2026.08.00`. It brings Compose UI `1.12.0`, requires `compileSdk 37` and AGP 9 or newer, and is already used by this project. The current stable Material 3 artifact is `1.4.0`; the full Expressive theme and motion APIs are in `1.5.0-alpha27`. The stable BOM is explicitly a set of stable, compatible versions, and Google says alpha BOMs are not intended for production. Therefore, a full API-level Expressive migration before MVP requires a deliberate, exact override to `androidx.compose.material3:material3:1.5.0-alpha27`, not an assumption that the stable BOM already provides it. Keep the stable BOM for the other Compose libraries and inspect the resolved dependency graph after the override. [Compose August 2026 release](https://developer.android.com/blog/posts/what-s-new-in-the-jetpack-compose-august-26-release), [Compose BOM guidance](https://developer.android.com/develop/ui/compose/bom), [Material 3 releases](https://developer.android.com/jetpack/androidx/releases/compose-material3)

This distinction matters:

- `MaterialExpressiveTheme`, `MotionScheme`, `expressiveLightColorScheme`, and the revised `MaterialTheme` motion subsystem were added to the `1.5.0-alpha` line. [MaterialExpressiveTheme API](https://developer.android.com/reference/kotlin/androidx/compose/material3/MaterialExpressiveTheme.composable), [MotionScheme API](https://developer.android.com/reference/kotlin/androidx/compose/material3/MotionScheme)
- Some Expressive APIs no longer carry an experimental annotation, but they are still distributed only in an alpha artifact. `MaterialShapes` and `LoadingIndicator` still use `ExperimentalMaterial3ExpressiveApi`. [MaterialShapes API](https://developer.android.com/reference/kotlin/androidx/compose/material3/MaterialShapes), [LoadingIndicator API](https://developer.android.com/reference/kotlin/androidx/compose/material3/LoadingIndicator.composable)
- Material 3 Adaptive `1.3.0` is stable as of 2026-08-26. Prefer that stable library for responsive behavior; do not couple adaptive layout work to the Material 3 alpha. [AndroidX versions](https://developer.android.com/jetpack/androidx/versions)

Recommended dependency policy for the MVP:

1. Pin Material 3 `1.5.0-alpha27` exactly and isolate all alpha-only calls in `ui/theme` and a small `ui/components` package.
2. Use stable `androidx.compose.material3.adaptive` `1.3.0`, including `adaptive-layout` and `adaptive-navigation` where list-detail behavior is required.
3. Do not use temporary `ComposeMaterial3Flags` as product configuration; the API says these flags are temporary and will be removed. [ComposeMaterial3Flags API](https://developer.android.com/reference/kotlin/androidx/compose/material3/ComposeMaterial3Flags)
4. Replace `material-icons-extended` progressively with app-owned Material Symbols vector assets. Google no longer recommends the icons library and removed it from the latest Material 3 release. [Material 3 `1.4.0` release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0)

## Current conventions to apply

Material describes Expressive as an expansion of M3 based on vibrant color, intuitive motion, adaptive components, flexible typography, and contrasting shapes. The update includes 35 shape primitives and 14 new or revised components, including toolbars, split buttons, button groups, and wavy progress indicators. It is not a license to decorate every surface; visual emphasis must identify purpose and hierarchy. [Building with M3 Expressive](https://m3.material.io/blog/building-with-m3-expressive), [Material 3 overview](https://m3.material.io/)

### Color

- Keep dynamic color on Android 12 and later, with a deliberate light and dark fallback for Android 10 and 11. Dynamic color is a user personalization layer, not a replacement for product status colors. [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
- Use the app color scheme for chrome, actions, selection, and containers. Treat issuer artwork and pass colors as content.
- Use category color only as a compact badge, icon container, or tonal metadata surface. Do not restore full-height side bars or use a category color as the only status signal.
- Use a text label or icon together with color for `Today`, `Upcoming`, `Past`, `Archived`, and notification states. Material recommends two visual indicators for interaction states. [Material state guidance](https://m3.material.io/foundations/interaction/states/overview)
- Keep QR, Aztec, PDF417, and Code 128 symbols on a plain, high-contrast neutral surface. Dynamic color must never recolor barcode modules.

### Typography and shape

- Use scale contrast to create hierarchy: a large date or event title for the current pass, medium titles for list items, and quiet label styles for issuer, category, gate, seat, and status. Do not make every heading oversized.
- Use the larger Expressive corner tokens for hero and modal surfaces. Use medium shapes for repeated ticket rows and small/pill shapes for category and status labels.
- Reserve non-rectangular `MaterialShapes` and shape morphing for transient, decorative, or loading elements. A barcode quiet zone, dense settings row, or long ticket list is not an appropriate shape-morph surface.
- Keep ticket silhouettes recognizable through spacing, perforation hints, artwork, typography, and grouped metadata rather than old cards with colored side strips.

### Motion

Material 3 now divides motion tokens by speed (`fast`, `default`, `slow`) and by purpose. Spatial specs change bounds or shape; effects specs change properties such as color or alpha. The Expressive scheme is recommended for prominent elements and hero interactions, while the standard scheme is intended for recurring, utilitarian interactions. [Material motion guidance](https://m3.material.io/styles/motion/overview/how-it-works), [MotionScheme API](https://developer.android.com/reference/kotlin/androidx/compose/material3/MotionScheme)

Apply that distinction as follows:

- Use expressive spatial motion for opening the highlighted current-day ticket, expanding its code, changing the selected pass in a two-pane layout, and revealing a primary FAB menu.
- Use fast spatial motion for swipe reveal, undo, sorting, archive transitions, and category reordering.
- Use effects motion for color, opacity, selected state, and notification state. Do not use a bouncy spatial spring for color or alpha.
- Use standard motion for repeated list interactions and settings. Too much spring motion reduces information density and makes the wallet feel slower.
- Preserve state and destination through resize, fold/unfold, and process recreation. Animation must never be the only explanation of a state change.
- Test with system animations disabled. The final state, selection, and destructive result must remain obvious without motion.

Navigation 3 `1.1` supports scene decorators and shared elements. A shared ticket-to-detail transition is appropriate; globally animated decoration is not. [Android adaptive I/O 2026 update](https://developer.android.com/blog/posts/adaptive-development-for-the-expanding-android-ecosystem)

### Progress and loading

Use progress indicators only for real work. Determinate indicators represent known progress; indeterminate indicators represent work whose completion cannot be estimated. [Material progress guidance](https://m3.material.io/components/progress-indicators/overview), [Compose progress guidance](https://developer.android.com/develop/ui/compose/components/progress)

- Use a determinate linear or wavy indicator while importing or exporting multiple passes when item count is known.
- Use a small indeterminate `LoadingIndicator` only for short parsing or storage transitions.
- Do not show a loading animation for local operations that complete in one frame.
- Wavy progress supports configurable amplitude, wavelength, thickness, and wave speed, but it is an information component, not decoration. [CircularWavyProgressIndicator API](https://developer.android.com/reference/kotlin/androidx/compose/material3/CircularWavyProgressIndicator.composable)

## Adaptive and foldable layout

The wallet naturally fits the list-detail canonical layout. `NavigableListDetailPaneScaffold` shows list and detail together in a large window and one pane at a time in a small window, with predictive-back support. The adaptive libraries also expose posture and hinge information. [List-detail implementation](https://developer.android.com/develop/adaptive-apps/guides/list-detail)

Use these layouts:

| Window | Home and pass detail | Settings and categories |
| --- | --- | --- |
| Compact | One ticket feed; detail replaces feed | One settings list; editor opens as a full screen or sheet |
| Medium | One or two ticket columns when content remains readable; detail remains a separate destination | Settings list with modal category editor |
| Expanded | Persistent ticket list plus pass detail; optional timeline as an extra/supporting pane | Settings section list plus detail editor |

Implementation rules:

- Derive layout from current window bounds and posture, not device model or physical orientation. Foldables can change compact, medium, and expanded class at runtime. [Foldable guidance](https://developer.android.com/develop/ui/compose/layouts/adaptive/foldables/trifolds-and-landscape-foldables)
- Avoid controls or essential text across an occluding hinge. Retain selection and scroll position through fold/unfold. [Learn about foldables](https://developer.android.com/develop/adaptive-apps/guides/foldables/learn-about-foldables)
- Do not stretch text, buttons, or a QR across all available width. Constrain content width and use additional panes or adaptive columns. [Adaptive do's and don'ts](https://developer.android.com/develop/adaptive-apps/guides/adaptive-dos-and-donts)
- Use `NavigationSuiteScaffold` only if the final information architecture has multiple primary destinations. A single wallet feed does not need a permanent bottom navigation bar.
- Prefer stable canonical scaffolds for the MVP. Grid, FlexBox, MediaQuery, and Styles were still presented as experimental at I/O 2026; use them only where they solve a measured layout problem. [I/O 2026 adaptive announcement](https://developer.android.com/blog/posts/adaptive-development-for-the-expanding-android-ecosystem)

## Screen redesign

### Home

Remove the app name, `Find pass files`, and Help from the home app bar as requested. Keep a compact, scroll-aware toolbar with:

- a sort control visible on the home screen;
- search/filter if the collection justifies it;
- Settings in the overflow or trailing action;
- one primary import action, preferably an expressive FAB or FAB menu for `Import pass` and `Create pass`.

Do not represent every pass as an isolated elevated card. Use a ticket feed made of visually grouped, edge-to-edge ticket rows on a shared surface:

- artwork or issuer mark at the leading edge;
- event or pass title as the primary line;
- date/time and key travel metadata as the second level;
- category and temporal status as small labeled badges;
- subtle shape and spacing differences between consecutive items;
- no persistent colored side strip.

Place today's passes in a distinct `Today` section above the rest. When the default setting is enabled, render them as larger hero tickets with the most useful next action and key metadata. Size and typography, not saturated color alone, provide emphasis. If more than one pass is current, use a horizontally paged or vertically stacked group that still exposes the count and does not hide passes off-screen without a cue.

Put the sort action in a compact button group or anchored menu. The selected order must be stated in text and exposed as selection semantics. Sort should act immediately with a short placement animation and preserve the visible anchor.

Use `SwipeToDismissBox` for directional actions:

- start-to-end: archive or restore;
- end-to-start: delete, with a confirmation threshold and immediate Snackbar undo;
- reveal icon plus label and distinct tonal backgrounds before commitment;
- do not make swipe the only path: expose Archive/Delete in an overflow menu and `CustomAccessibilityAction`s. Android explicitly recommends custom actions for swipe-to-dismiss because complex gestures can be inaccessible. [Swipe-to-dismiss guidance](https://developer.android.com/develop/ui/compose/touch-input/user-interactions/swipe-to-dismiss), [Compose semantics guidance](https://developer.android.com/develop/ui/compose/accessibility/semantics)

Automatic past-state behavior must be visible and reversible. A past pass belongs under `Past`, with a status label and a restore/manual override action. Archive is a user organization state; past is a derived temporal state. Do not collapse them into one boolean.

### Pass detail and code

Make the code the strongest action surface when present, but keep the normal detail page balanced:

- show essential event/travel metadata first;
- show a crisp code preview on an uncolored surface;
- tap the code to enter a distraction-free full-screen code mode;
- provide explicit size controls or pinch-to-zoom, persist the user's preferred size, and include a one-tap reset;
- keep alternative text visible and selectable.

The blur problem is primarily a rasterization problem, not a Material component problem. ZXing renders QR content as a `BitMatrix`, chooses an integer module multiplier, and defaults to a four-module quiet zone. Generate at the actual pixel target, draw exact square modules, preserve the quiet zone, and avoid fractional bitmap scaling or interpolation. [ZXing QRCodeWriter source](https://github.com/zxing/zxing/blob/master/core/src/main/java/com/google/zxing/qrcode/QRCodeWriter.java), [ZXing QRCodeWriter API](https://zxing.github.io/zxing/apidocs/com/google/zxing/qrcode/QRCodeWriter.html)

Full-screen code mode should temporarily set window brightness to full when the existing automatic-brightness setting is enabled, then restore the previous value on exit. Android supports a per-window brightness override. [Window brightness API](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#screenBrightness)

A hardware torch toggle belongs on a camera scanner surface. It does not improve the scanning of a code displayed on the same phone. If a torch shortcut is still desired in pass detail for environmental light, label it `Flashlight`, show its on/off state, handle unavailable camera hardware, and always restore it on lifecycle exit. Keep it separate from `Screen brightness` so the controls do not imply the same behavior.

Use a compact floating toolbar or button group for `Calendar`, `Share`, `Print`, and `More`, with one primary action based on pass state. A split button is suitable when the main action is `Open code` and related choices are `Share code` or `Print`; Material defines split buttons as a primary action paired with a menu of related actions. [Split button guidance](https://m3.material.io/components/split-button)

### Calendar and timeline

Create one chronological event model that feeds three surfaces: pass detail, the in-app timeline, and notification scheduling. Keep event occurrence separate from calendar-export state and notification state.

- Detail: show the next relevant event and an `Add to calendar` action.
- Timeline: group events by day, show past/current/upcoming states with label plus icon, and scroll to the nearest current event.
- Expanded window: use a supporting pane for the timeline while the pass remains visible.
- Calendar insertion: continue using `ACTION_INSERT` with `CalendarContract.Events.CONTENT_URI`; include title, description, location, start/end, and a stable app deep link in the description when the calendar app accepts it. This delegates final calendar/account choice to the user and avoids direct calendar-write permission. [Android common calendar intent](https://developer.android.com/guide/components/intents-common#Calendar)
- Automatic calendar insertion is materially different: it requires a clearly explained opt-in and either user confirmation per event or Calendar Provider permissions. Do not silently create events merely because a pass was imported.

### Notifications

Notification settings should contain an app-level enable switch, default lead time, optional per-category defaults, and a per-pass override. Ask for `POST_NOTIFICATIONS` only after the user enables reminders; Android 13 and later starts new installs with notifications off. [Notification permission guidance](https://developer.android.com/develop/ui/compose/notifications/notification-permission)

Use a stable reminder channel and link to the system channel settings. Android requires channels from API 26 and gives the user final control over sound, vibration, visibility, and importance after channel creation. [Notification channel guidance](https://developer.android.com/develop/ui/compose/notifications/channels)

Each notification should open its exact pass through the single Activity and typed destination, using an immutable `PendingIntent` and a valid app back stack. [Notification navigation guidance](https://developer.android.com/develop/ui/views/notifications/navigation)

Prefer inexact alarms for ordinary reminders. Exact alarms consume more resources and require special `Alarms & reminders` access on Android 12 or later; reserve them for a user-selected, truly precise reminder mode. WorkManager is durable but periodic work has a 15-minute minimum and execution time is not exact. [Alarm scheduling guidance](https://developer.android.com/develop/background-work/services/alarms), [WorkManager timing guidance](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)

### Settings and categories

Replace the flat settings list with sections: `Appearance`, `Pass list`, `Categories`, `Calendar`, `Notifications`, `Code display`, and `Storage`. On expanded windows, use list-detail so the section list remains visible beside its controls.

The category manager should support:

- create, rename, reorder, recolor, and delete;
- icon or symbol selection;
- a fallback category for passes whose category was deleted;
- optional matching rules shown separately from the visible category name;
- preview in light, dark, and dynamic color;
- an accessible label and icon so color is never the sole identifier.

Use segmented or grouped list items rather than a stack of elevated cards. Use a modal sheet for quick color/icon edits on compact screens and a supporting detail pane on expanded screens. Destructive category deletion must explain reassignment before confirmation.

### Widgets

Build widgets with Glance, but treat it as a separate UI toolkit: Glance uses Compose runtime concepts and its own composables; ordinary Compose UI components cannot be reused directly. `glance-material3` provides Material 3 colors and dynamic color. [Glance overview](https://developer.android.com/develop/ui/compose/glance), [Glance theming](https://developer.android.com/develop/ui/compose/glance/theme)

Useful MVP widgets are:

1. `Next pass`: next or current pass, time, location/gate, and tap to open that pass.
2. `Today`: up to the next few current-day passes with direct links.
3. `Quick code`: a configured pass with a tap target that opens full-screen code mode. Do not place a dense, operational barcode directly in a tiny widget.

Use `SizeMode.Responsive` with a small set of meaningful size layouts and dynamic color. Update only when pass data or temporal state changes; avoid minute-level periodic refresh. Glance recommends infrequent widget updates, no more than hourly through the provider period. [Responsive Glance layouts](https://developer.android.com/develop/ui/compose/glance/build-ui), [Create a Glance widget](https://developer.android.com/develop/ui/compose/glance/create-app-widget)

Provide generated widget previews on Android 15 and later plus a static fallback for older versions. [Widget preview guidance](https://developer.android.com/develop/ui/compose/glance/generated-previews)

## Accessibility acceptance rules

- All interactive targets are at least 48 dp, including code size controls, swipe alternatives, timeline events, and category handles. [Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)
- Every actionable icon has a localized description; decorative icons have no description.
- Swipe archive/delete also exists as labeled accessibility custom actions and a visible overflow action.
- Status, category, selected sort, and reminder state have `stateDescription` or equivalent semantics and are not communicated by color alone.
- The full-screen code has a useful semantic description but does not expose hundreds of meaningless image nodes.
- Large font and display scaling reflow content without horizontal reading. Do not place fixed-height text containers in ticket rows. [Scalable content guidance](https://developer.android.com/develop/ui/compose/accessibility/scalable-content)
- Keyboard, mouse, trackpad, and D-pad focus remains visible on large screens and foldables.
- Test TalkBack traversal, Switch Access actions, animations disabled, light/dark/dynamic color, and high-contrast barcode scanning. Android recommends manual assistive-technology testing in addition to automated checks. [Accessibility testing guidance](https://developer.android.com/guide/topics/ui/accessibility/testing)

## Actionable implementation checklist

### Foundation

- [ ] Pin Material 3 `1.5.0-alpha27` separately from the stable `2026.08.00` BOM and record the preview-risk decision.
- [ ] Add stable Material 3 Adaptive `adaptive-layout` and `adaptive-navigation` `1.3.0`.
- [ ] Create a small `PassDesignSystem` wrapper for color, typography, shapes, motion, and alpha-only components.
- [ ] Replace extended icon-library imports with app-owned Material Symbols vectors.
- [ ] Define stable semantic colors and labels for current, upcoming, past, and archived states.

### Home

- [ ] Remove the app name, `Find pass files`, and Help from home.
- [ ] Replace isolated cards and colored side strips with a grouped ticket feed.
- [ ] Add the default-enabled enlarged `Today` section.
- [ ] Put sort directly on home and preserve scroll position when it changes.
- [ ] Add bidirectional swipe: archive/restore and delete, with undo, overflow alternatives, and accessibility actions.
- [ ] Keep past and archived as separate concepts; add automatic past classification with manual override.

### Detail and code

- [ ] Render barcode matrices at integer pixel scale with a preserved quiet zone and neutral high contrast.
- [ ] Add full-screen code mode, explicit zoom, optional persisted scale, and brightness restoration.
- [ ] Put hardware flashlight only where its purpose is explicit and hardware/lifecycle behavior is safe.
- [ ] Group Calendar, Share, Print, and secondary actions in an adaptive toolbar.

### Time-based features

- [ ] Introduce one event/timeline model shared by UI, calendar, and reminders.
- [ ] Add a chronological timeline with current-state focus.
- [ ] Add per-pass calendar insertion and a separately explained automatic setting.
- [ ] Add app, category, and pass reminder settings; request notification permission in context.
- [ ] Deep-link every reminder to its pass and reschedule safely after pass changes or reboot.

### Categories, adaptive UI, and widgets

- [ ] Add category create, rename, reorder, color, icon, delete, and reassignment flows.
- [ ] Use `NavigableListDetailPaneScaffold` for feed/detail and settings/detail on expanded windows.
- [ ] Handle hinge occlusion and retain state through resize and fold transitions.
- [ ] Add responsive `Next pass`, `Today`, and configured quick-code Glance widgets.
- [ ] Add generated widget previews and direct pass-opening actions.

### Verification

- [ ] Add semantic Compose tests for sort, Today emphasis, swipe alternatives, category state, timeline, and code enlargement.
- [ ] Add screenshot checks for compact/expanded, fold posture, light/dark/dynamic color, and large fonts.
- [ ] Scan generated QR, Aztec, PDF417, and Code 128 output at each supported display size.
- [ ] Test notification permission denied/granted, channel disabled, alarm fallback, reboot rescheduling, and deep-link recovery.
- [ ] Test widget resizing and stale/deleted pass recovery.
- [ ] Keep the development loop narrow; run the full device matrix only at the release gate.

