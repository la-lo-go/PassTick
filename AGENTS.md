# PassAndroid modernization guide

## Scope

This repository preserves the complete history of the original PassAndroid project. Keep `upstream` pointed at `https://github.com/ligi/PassAndroid.git` and `origin` pointed at `https://github.com/la-lo-go/PassTick.git`. The owner has selected the final name: the app is "PassTick" with applicationId `dev.lalogo.passtick`. Do not push changes until the owner authorizes publication.

The maintained application is a private, offline-first Android app for Android 10 and later. It reads, stores, displays, edits, imports, and exports `.pkpass` and `.esPass` files. It has one application artifact, no analytics, and no vendor-specific distribution code.

## Persistent decisions

- Use English for code, comments, user interface text, documentation, and commits.
- Preserve GPL-3.0, original copyright notices, file formats, and visible upstream attribution.
- Use JDK 17, Gradle 9.5, AGP 9.3, built-in Kotlin, `compileSdk` 37, `targetSdk` 36, and `minSdk` 29.
- Use Kotlin DSL and the Gradle version catalog. Do not execute remote build scripts.
- Use one Activity, Compose Material 3, Navigation 3, ViewModels, immutable UI state, explicit UI actions, `StateFlow`, and unidirectional data flow.
- Keep Material 3 Expressive preview APIs behind the theme and design-system layer. Pin their exact version until the stable BOM includes them.
- Treat homepage delete gestures as moves to Trash with undo. Reserve permanent deletion for an explicit confirmed action.
- Derive the homepage Today section, timeline, calendar, and reminders from one temporal pass model with explicit time zones.
- Keep Koin as the dependency injection framework.
- Use app-private storage internally. Use `content://` URIs and the Storage Access Framework at process boundaries.
- Open locations with a `geo:` intent. Do not embed a proprietary map SDK.
- Do not add analytics, App Engine, GCM, Amazon code, product flavors, JitPack-only UI libraries, Fragments, RecyclerView, ViewBinding, or XML screen layouts.
- Do not use `GlobalScope` or obsolete coroutine channels. Tie work to a lifecycle or an injected application scope and expose Flow.

## Architecture contracts

- `PassRepository` observes, imports, creates, updates, classifies, deletes, and exports passes. The repository owns the internal pass representation and storage layout.
- `SettingsRepository` exposes `Flow<AppSettings>` and persists settings with DataStore.
- `AppDestination` contains typed Navigation 3 keys.
- `PlatformActions` owns calendar, sharing, printing, and location intents.
- Each screen exposes an immutable `UiState` and accepts explicit actions.
- Domain readers, models, classification, sorting, barcode handling, and import behavior remain independent from Compose.

## Change rules

- Add characterization tests before changing pass readers, models, classification, sorting, or persistence.
- Convert stable Java logic semantically before refactoring it. Do not mix functional changes into conversion commits.
- Refactor converted code only when it improves type safety, null safety, structured concurrency, testability, reuse, or removes duplication or Android coupling.
- Comments explain invariants, constraints, compatibility behavior, failures, or non-obvious decisions. Remove narration and commented-out code.
- Use short, literal technical English.
- Use ASD-STE100 Simplified Technical English for documentation and plans.
- Follow YAGNI (You Aren't Gonna Need It). Prefer clear one-line solutions when they do not reduce readability.
- Make atomic English commits as `Lalo`. Do not add co-author trailers or tool references.

## Required validation

Run the narrowest relevant test during development. Before each milestone, run unit tests, lint, and the release build. Run instrumented and Compose tests on API 29 and API 37 when emulators are available. A release remains blocked until it is also tested on a physical Android 10 or later device.

At the end of each major change or implementation of a complex feature, run cyclomatic-complexity lint or an equivalent complexity check.

Modernization is complete only when maintained code contains no Java, XML screens, Fragments, RecyclerView, ViewBinding, backend, analytics, Amazon code, broken dependencies, or product flavors, and all required checks pass.
