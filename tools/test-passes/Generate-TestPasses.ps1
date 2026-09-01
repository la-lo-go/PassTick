param(
    [string] $OutputDirectory = (Join-Path $PSScriptRoot "../../build/test-passes")
)

$ErrorActionPreference = "Stop"
$catalogPath = Join-Path $PSScriptRoot "../../android/src/test/resources/test-passes/catalog.json"
$catalog = Get-Content -LiteralPath $catalogPath -Raw -Encoding UTF8 | ConvertFrom-Json
$resolvedOutput = [System.IO.Path]::GetFullPath($OutputDirectory)

New-Item -ItemType Directory -Force -Path $resolvedOutput | Out-Null

foreach ($fixture in $catalog) {
    $stagingDirectory = Join-Path ([System.IO.Path]::GetTempPath()) ("passandroid-" + [guid]::NewGuid())
    New-Item -ItemType Directory -Path $stagingDirectory | Out-Null

    try {
        $mainJson = $fixture.pass | ConvertTo-Json -Depth 12
        $mainJsonPath = Join-Path $stagingDirectory "main.json"
        [System.IO.File]::WriteAllText($mainJsonPath, $mainJson, [System.Text.UTF8Encoding]::new($false))

        $destination = Join-Path $resolvedOutput $fixture.fileName
        $zipDestination = [System.IO.Path]::ChangeExtension($destination, ".zip")
        if (Test-Path -LiteralPath $destination) {
            Remove-Item -LiteralPath $destination
        }
        if (Test-Path -LiteralPath $zipDestination) {
            Remove-Item -LiteralPath $zipDestination
        }
        Compress-Archive -LiteralPath $mainJsonPath -DestinationPath $zipDestination
        Move-Item -LiteralPath $zipDestination -Destination $destination
    }
    finally {
        Remove-Item -LiteralPath $stagingDirectory -Recurse -Force
    }
}

Write-Output "Generated $($catalog.Count) passes in $resolvedOutput"
