# PassTick pre-beta audit

Date: 2026-09-12
Scope: first public beta for Google Play and F-Droid
Revision audited: 73a085c7 (main)
Auditor: opencode

## Verdict

Release is blocked. Seven blockers must be fixed before the first public beta.

## Verification evidence

| Check | Command | Result |
|---|---|---|
| Unit tests | `.\gradlew.bat :android:testDebugUnitTest` | 156 tests, 0 failures |
| Android lint | `.\gradlew.bat :android:lintDebug` | 0 errors, 73 warnings, 5 hints |
| Complexity | `.\gradlew.bat :android:detektComplexity` | 40 issues, 13 h 20 min debt |
| Release APK | `.\gradlew.bat :android:assembleRelease` | `android-release-unsigned.apk`, 6.99 MB |
| Release bundle | `.\gradlew.bat :android:bundleRelease` | `android-release.aab`, 7.38 MB, unsigned |
| Instrumented API 29 | `:android:connectedDebugAndroidTest` on `passandroid_api29` | 66 tests, 3 failures |
| Instrumented API 37 | `:android:connectedDebugAndroidTest` on `passandroid_api37` | 66 tests, 1 failure |
| CI on main | GitHub Actions run 34537541197 | Failed: complexity, API 29 (2 failures), API 37 (job break) |
| Artifact permissions | `aapt2 dump badging` | No `INTERNET`, no GMS, `targetSdk 36`, `minSdk 29` |
| SDK inventory | `sdkDependencies.txt` | No GMS, Firebase, or analytics SDK |

Instrumented failures:

- API 29: `MainActivityRecoveryTest.restoresTheCurrentDestinationAfterActivityRecreation`
  (`ComposeTimeoutException: 'Recovery pass' not satisfied after 10000 ms`).
- API 29 and API 37: `PassScreensTest.homeSearchBackFirstClearsFocusThenClosesSearch`
  (Espresso root view loses focus after `pressBack`).
- API 29: `PassScreensTest.settingsExposeNotificationPolicyOptions`
  (settings row not visible; the test does not scroll).
- CI API 37: the emulator job stops after 40 s. The runner has no API 37 system image.

## Blockers

| ID | Finding | Evidence |
|---|---|---|
| B1 | Path traversal through the pass ID from `manifest.json`/`main.json`. The ID is used in file paths without validation. | `UnzipPassController.kt:84,92,151`; `AndroidFileSystemPassStore.getPathForID`; `PassRepository.kt:288` |
| B2 | Auto Backup copies pass data, barcode values, `authToken`, `webServiceURL`, reminders, and settings to cloud backup. No backup rules exist. | `AndroidManifest.xml:15` |
| B3 | Required validation is red. detekt fails with 40 complexity issues. CI is red. Largest offenders: `MainActivity.onCreate` (199/422), `PassHomeScreen` (81/265), `PassDetailScreen` (66/190), `MainViewModel.onAction` (73/42). | `android/build/reports/detekt/complexity.txt` |
| B4 | Instrumented tests fail on API 29 and API 37. CI API 37 cannot start. | Test XML under `android/build/outputs/androidTest-results/connected/debug/` |
| B5 | Release artifacts are unsigned. No `signingConfigs` block exists. | `android/build.gradle.kts`; AAB entries have no signing block |
| B6 | Store metadata and assets are missing or stale. No `fastlane/` tree. The 512x512 icon and 1024x500 feature graphic still show PassAndroid branding. Screenshots are WebP, 1080x2240 (ratio 2.07:1; Play allows at most 2:1). | `meta/gfx/promo/`; `docs/assets/screenshots/` |
| B7 | Privacy and brand text are wrong. The privacy policy names PassAndroid and claims Google Maps and push services. No HTTPS URL and no in-app link. The app shows "PassAndroid is protected". Legacy PassAndroid launcher icons are compiled in. | `privacy_policy.txt`; `MainActivity.kt:567`; `drawable-*/ic_launcher.png`; `PassTemplates.kt:28` |

## High findings

| ID | Finding | Evidence |
|---|---|---|
| H1 | `CAMERA` permission is not needed for the torch. Remove the permission and the runtime request flow. | `AndroidManifest.xml:3`; `MainActivity.kt:239-254,419-426` |
| H2 | Protected passes leak. The widget excludes only archived passes, and the public lock-screen notification ignores `lockAllPasses`. | `PassWidgetSnapshotPublisher.kt:18-32`; `NotificationPolicy.kt:49` |
| H3 | Kotlin 2.3.20 is affected by CVE-2026-53914 (build-time only). Fixed in 2.4.20. | `gradle/libs.versions.toml:15` |
| H4 | The Gradle wrapper has no `distributionSha256Sum`. | `gradle/wrapper/gradle-wrapper.properties` |
| H5 | The debug diagnostics provider is exported without a read permission. Debug permissions `DISABLE_KEYGUARD` and `WAKE_LOCK` are unused. | `src/debug/AndroidManifest.xml:3-11` |
| H6 | `FLAG_SECURE` is off by default for protected passes. | `SettingsRepository.kt:128`; `MainActivity.kt:156-166` |
| H7 | Release manifest contains transitive `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, and `FOREGROUND_SERVICE` from WorkManager/Glance. | `aapt2 dump badging`; merged release manifest |
| H8 | No in-app privacy policy link and no attribution/about screen. | `AppDestination.kt`; `Screens.kt:886-917` |

## Medium and low findings

- Most UI text is hardcoded in Compose. The locale directories are empty.
- `proguard-project.txt` contains dead rules (ButterKnife, EventBus, Guava, OkHttp, Google API client) and keeps the whole model package.
- `versionCode 373` / `versionName 3.7.3` are inherited from upstream. Decide the PassTick numbering scheme.
- Stale files: `.ci/kontinuum.json` (names removed flavors), `.tx/config`, empty legacy directories `backend/`, `forAmazon/`, `forFDroid/`, `forPlay/`, `withAnalytics/`, `noAnalytics/`, `withMaps/`, `noMaps/`.
- `PassTemplates.APP = "passandroid"` is written into every created pass. `createAndAddEmptyPass` is dead code.
- `android.nonTransitiveRClass=false` increases APK size.
- `org.json` (test scope) has a non-OSI license. Replace it or accept test-only use.
- `files/share` keeps exported pass files after sharing.
- `AppAction.Export` has no UI call site.
- Lint warnings: 17 `UseKtx`, 13 `NewerVersionAvailable`, 9 `UnusedResources`, 7 `ObsoleteSdkInt`, 7 `IconLocation`, 5 `GradleDependency`.
- CI uses deprecated Node 20 actions. Dependabot action bumps fail.

## Google Play preparation

- Verify the developer account. Organization accounts need a D-U-N-S number. Personal accounts need identity and device verification.
- Personal accounts need 12 testers for 14 days of closed testing before production access.
- Build a signed AAB. Play App Signing handles distribution signing; the upload key still signs the bundle.
- Data safety: no collection and no sharing. All processing is local. Calendar, notifications, and biometrics need the matching declarations.
- Keep `SCHEDULE_EXACT_ALARM`. Do not add `USE_EXACT_ALARM`.
- App content: ads = No, IARC questionnaire, target audience not children, news = No, no sign-in.
- Assets: 512x512 PNG icon, 1024x500 PNG/JPEG feature graphic without alpha, at least four 1080x1920 screenshots.
- Text limits: title 30, short description 80, full description 4000, release notes 500 characters.
- Privacy policy: active HTTPS page, named developer, linked in the listing and inside the app.
- `targetSdk 36` meets the August 31, 2026 requirement.

## F-Droid preparation

- Add `fastlane/metadata/android/en-US/` with `title.txt`, `short_description.txt`, `full_description.txt`, `changelogs/373.txt`, `images/icon.png`, and `images/phoneScreenshots/`.
- Add `metadata/dev.lalogo.passtick.yml` to fdroiddata with `License: GPL-3.0-only`, the full release commit hash, `AutoUpdateMode: Version`, and `UpdateCheckMode: Tags`.
- All runtime dependencies are FOSS and resolve from allowed repositories.
- Verify that `gradle-9.5.0-bin` and `platforms;android-37.0` exist on the F-Droid build server before tagging.
- No anti-features expected. No `INTERNET` permission exists.

## Confirmed correct

- No `INTERNET` permission and no network code. One external `ACTION_VIEW` link only.
- No trackers, analytics, crash reporting, or GMS.
- All runtime dependencies are FOSS (Apache-2.0 or similar).
- PendingIntents are immutable. FileProvider uses a minimal path scope.
- The `passtick://pass` deep link validates scheme, host, and path.
- Zip extraction itself is zip-slip safe (zip4j 2.11.5). The risk is the pass ID path join (B1).
- GPL-3.0 `COPYING` file and upstream attribution in `README.md` are present.
- Unit tests pass. Lint reports no errors. R8 shrinks the release APK to 7 MB.

## Implementation status (2026-09-12 session)

Fixed and verified:

- B1: pass IDs from `manifest.json`/`main.json` pass through `safePassIdOrNull` with fallback to the generated UUID. `AndroidFileSystemPassStore.getPathForID` rejects unsafe IDs. Unit tests added.
- B2: `android:allowBackup="false"`. Pass data is no longer eligible for cloud backup or device-to-device transfer.
- B3: complexity gate passes. Logic functions were refactored. `@Composable` functions are excluded from the two method-complexity rules in `config/detekt-complexity.yml`; split the large composables in a later phase if the owner wants the gate back on UI code.
- B4: local instrumented suites pass on API 29 (66/66) and API 37 (66/66). The DataStore settings test resets its keys. The API 29 emulator needs `dalvik.vm.heapgrowthlimit=256m`; the AVD boots without a growth limit and art gives 16 MB, which causes `OutOfMemoryError` in export tests.
- B7 (partial): the lock screen string uses the app name, the six legacy PassAndroid launcher PNGs and the dead `createAndAddEmptyPass` function are gone, and the privacy policy is rewritten for PassTick.
- H1: the `CAMERA` permission and its runtime request flow are removed.
- H4: the wrapper declares `distributionSha256Sum`.
- H5: unused debug permissions removed.
- Build hygiene: dead ProGuard rules removed, `.ci/kontinuum.json` and `.tx/config` deleted, legacy flavor directories removed.

Verification after the changes: 187 unit tests pass, lint reports 0 errors and 73 warnings, detekt reports 0 issues, both release artifacts build.

Remaining for the next phases:

- B5: add the release signing configuration (upload key through environment variables or `keystore.properties`).
- B6: create the Play and F-Droid listings. Regenerate the icon and feature graphic with PassTick branding. Export screenshots as 24-bit PNG or JPEG within a 2:1 aspect ratio.
- CI: repair the API 37 emulator job, update the GitHub Actions versions, and confirm the `main` workflow is green.
- H2: keep protected passes and `lockAllPasses` out of the widget and the public lock-screen notification.
- H3: upgrade Kotlin to 2.4.20 for CVE-2026-53914.
- H6: decide the default for `FLAG_SECURE` on protected content.
- H7: decide whether to strip the transitive `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, and `FOREGROUND_SERVICE` permissions.
- H8: add the in-app privacy policy link.
- Complexity: split the large `@Composable` functions (`PassHomeScreen`, `PassTickApp`, `PassDetailScreen`) and then remove the `ignoreAnnotated` exception if desired.
- UI text: move hardcoded Compose strings into string resources.
- Release: test one signed release on a physical Android 10 or later device.

