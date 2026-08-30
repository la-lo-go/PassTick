# PassAndroid modernization

This repository is an independent modernization of the original
[PassAndroid project](https://github.com/ligi/PassAndroid) by ligi and its
contributors. It preserves the complete project history, original copyright,
and GNU GPL version 3 license.

The current application name and `org.ligi.passandroid` identifiers are
temporary. They will change after the replacement name is selected. This
repository is not yet published and does not represent the Play Store,
F-Droid, or Amazon releases of the original application.

## Current application

The application imports Apple Wallet (`.pkpass`), esPass (`.espass`), image,
and PDF files through Android's document providers. It stores passes privately
and supports local pass creation, configurable categories, offline viewing, barcodes, editing,
export, sharing, printing, calendar events, and location intents. It contains
no analytics.

The user interface uses Jetpack Compose and Material 3. Production and test
sources are Kotlin. The app targets Android 16 and supports Android 10 or
newer.

## Build

Use JDK 17 and the included wrapper:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleRelease
```

The automated suite is validated with emulators at API 29 and API 37. A release
must also be tested on a physical Android 10 or newer device before publication.

## License

This project is licensed under the [GNU General Public License version 3](COPYING).
It is not affiliated with Apple.
