# Store assets

The store listing uses two screenshot sets. The framed set has a navy background, a device frame, and an English headline. The clean set contains the plain captures.

## Capture the screens

The instrumented test `StoreScreenshotTest` renders the showcase passes on a device and writes eight PNG files to the app external files directory. Set up and run it with:

```powershell
./tools/store-assets/Capture-Screenshots.ps1
```

The script starts the `passandroid_api37` emulator when no emulator is connected, sets the locale and the time zone, installs the debug APKs, runs the test class, and pulls the captures to `build/screenshots-raw`.

## Generate the assets

```powershell
python tools/store-assets/generate.py            # framed set into fastlane
python tools/store-assets/generate.py --set clean  # clean set into fastlane
```

The generator imports new captures from `build/screenshots-raw` into `docs/assets/screenshots` as WebP sources, renders the icon and the feature graphic, and writes both sets to `build/store-assets`. The selected set goes to `fastlane/metadata/android/en-US/images/phoneScreenshots` for Play and F-Droid.

Edit the `CAPTIONS` table in `generate.py` to change the listing order or the headlines.
