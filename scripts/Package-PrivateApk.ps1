param(
    [string]$SdkDirectory = "$env:LOCALAPPDATA\Android\Sdk",
    [string]$SigningDirectory = "$env:USERPROFILE\.android\shopping-list-app-signing",
    [string]$OutputApk = ""
)
$ErrorActionPreference = 'Stop'
$unsignedApk = Join-Path $PSScriptRoot '..\app\build\outputs\apk\release\app-release-unsigned.apk'
$keyFile = Join-Path $SigningDirectory 'shopping-list-app-release.p12'
$passwordFile = Join-Path $SigningDirectory 'release-keystore-password.txt'
foreach ($requiredFile in @($unsignedApk, $keyFile, $passwordFile)) {
    if (-not (Test-Path -LiteralPath $requiredFile)) { throw "Required file missing: $requiredFile" }
}
$buildTools = Get-ChildItem -LiteralPath (Join-Path $SdkDirectory 'build-tools') -Directory |
    Where-Object { $_.Name -match '^\d+\.\d+\.\d+$' } | Sort-Object { [version]$_.Name } -Descending | Select-Object -First 1
if (-not $buildTools) { throw 'Android SDK build-tools are required' }
$apksigner = Join-Path $buildTools.FullName 'apksigner.bat'
if ([string]::IsNullOrWhiteSpace($OutputApk)) {
    $metadata = Get-Content -LiteralPath (Join-Path (Split-Path $unsignedApk) 'output-metadata.json') -Raw | ConvertFrom-Json
    $versionLabel = $metadata.elements[0].versionName -replace '[^a-zA-Z0-9._-]', '_'
    $OutputApk = Join-Path $PSScriptRoot "..\build\distribution\Shopping-List-App-$versionLabel.apk"
}
$outputFullPath = [IO.Path]::GetFullPath($OutputApk)
New-Item -ItemType Directory -Path (Split-Path -Parent $outputFullPath) -Force | Out-Null
if (Test-Path -LiteralPath $outputFullPath) { throw 'Output already exists. Choose a new filename rather than replacing a distributed build.' }
$env:SHOPPING_LIST_SIGN_PASSWORD = [IO.File]::ReadAllText($passwordFile).Trim()
try {
    & $apksigner sign --ks $keyFile --ks-key-alias shopping-list-app --ks-pass env:SHOPPING_LIST_SIGN_PASSWORD --key-pass env:SHOPPING_LIST_SIGN_PASSWORD --out $outputFullPath $unsignedApk
    if ($LASTEXITCODE -ne 0) { throw 'APK signing failed' }
    & $apksigner verify --verbose $outputFullPath
    if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed' }
} finally {
    Remove-Item Env:SHOPPING_LIST_SIGN_PASSWORD -ErrorAction SilentlyContinue
}
Get-FileHash -Algorithm SHA256 -LiteralPath $outputFullPath
