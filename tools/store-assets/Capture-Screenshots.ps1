param(
    [string] $Avd = "passandroid_api37",
    [string] $Serial
)

$ErrorActionPreference = "Stop"
$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot "../.."))
$outputDirectory = Join-Path $repositoryRoot "build/screenshots-raw"
$packageName = "dev.lalogo.passtick"
$testApplicationId = "$packageName.test"
$testRunner = "org.ligi.passandroid.AppReplacingRunner"
$testClass = "org.ligi.passandroid.screenshots.StoreScreenshotTest"
$appApk = Join-Path $repositoryRoot "android/build/outputs/apk/debug/android-debug.apk"
$testApk = Join-Path $repositoryRoot "android/build/outputs/apk/androidTest/debug/android-debug-androidTest.apk"

function Resolve-SdkRoot {
    foreach ($candidate in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, (Join-Path $env:LOCALAPPDATA "Android/Sdk"))) {
        if ($candidate -and (Test-Path -LiteralPath (Join-Path $candidate "platform-tools/adb.exe"))) {
            return $candidate
        }
    }
    throw "Android SDK not found. Set ANDROID_HOME."
}

$sdkRoot = Resolve-SdkRoot
$adb = Join-Path $sdkRoot "platform-tools/adb.exe"
$emulator = Join-Path $sdkRoot "emulator/emulator.exe"

function Get-ConnectedSerials {
    & $adb devices | Select-String -Pattern "^(\S+)\s+device$" | ForEach-Object { $_.Matches[0].Groups[1].Value }
}

function Get-EmulatorSerial {
    Get-ConnectedSerials | Where-Object { $_.StartsWith("emulator-") } | Select-Object -First 1
}

function Start-Emulator {
    Write-Host "Starting emulator $Avd"
    Start-Process -FilePath $emulator -ArgumentList "-avd", $Avd, "-no-snapshot", "-no-boot-anim", "-prop", "persist.sys.locale=en-US", "-timezone", "Europe/Madrid" | Out-Null
    $deadline = (Get-Date).AddMinutes(8)
    $booted = ""
    $serial = $null
    do {
        Start-Sleep -Seconds 5
        $serial = Get-EmulatorSerial
        if ($serial) { $booted = ((& $adb -s $serial shell getprop sys.boot_completed) -join "").Trim() }
    } until (($serial -and $booted -eq "1") -or (Get-Date) -gt $deadline)
    if (-not $serial -or $booted -ne "1") { throw "Emulator did not boot within eight minutes." }
    return $serial
}

if ($Serial) {
    $target = $Serial
} else {
    $target = Get-EmulatorSerial
    if (-not $target) { $target = Start-Emulator }
}
if (-not $target) { throw "No Android emulator is available for the capture. Start one or pass -Serial." }

Write-Host "Using device $target"

Push-Location $repositoryRoot
try {
    & (Join-Path $repositoryRoot "gradlew.bat") ":android:assembleDebug" ":android:assembleDebugAndroidTest"
    if ($LASTEXITCODE -ne 0) { throw "Build failed with exit code $LASTEXITCODE." }
}
finally {
    Pop-Location
}

& $adb -s $target install -r -t $appApk
if ($LASTEXITCODE -ne 0) { throw "Could not install the app on $target." }
& $adb -s $target install -r -t $testApk
if ($LASTEXITCODE -ne 0) { throw "Could not install the test app on $target." }

& $adb -s $target shell "settings put system system_locales en-US" | Out-Null
& $adb -s $target shell "setprop persist.sys.timezone Europe/Madrid" | Out-Null
& $adb -s $target shell "rm -rf /sdcard/Android/data/$packageName/files/screenshots" | Out-Null

# The capture test hides the navigation bar, so capture only needs the education prompt suppressed.
& $adb -s $target shell "settings put secure immersive_mode_confirmations confirmed" | Out-Null

$instrumentation = & $adb -s $target shell am instrument -w -r -e class $testClass "$testApplicationId/$testRunner" 2>&1
$instrumentation | Select-Object -Last 8 | ForEach-Object { Write-Host $_ }
if (($instrumentation -join "`n") -match "FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|Process crashed") {
    throw "Instrumented screenshot test failed."
}

New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
Get-ChildItem -LiteralPath $outputDirectory -File | Remove-Item -Force
& $adb -s $target pull "/sdcard/Android/data/$packageName/files/screenshots/." $outputDirectory
if ($LASTEXITCODE -ne 0) { throw "Could not pull screenshots from $target." }

Get-ChildItem -LiteralPath $outputDirectory -File | Select-Object Name, Length
