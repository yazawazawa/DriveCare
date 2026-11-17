# Registers the two secrets needed by the "Release APK" GitHub Actions workflow (Windows / PowerShell).
#
#   ANDROID_DEBUG_KEYSTORE_BASE64 : debug keystore  (%USERPROFILE%\.android\debug.keystore)
#   GOOGLE_SERVICES_JSON_BASE64   : Firebase config (app\google-services.json)
#
# Usage (run in the repository folder):
#   1. Install GitHub CLI and log in:   winget install GitHub.cli   then   gh auth login
#   2. .\scripts\set-github-secrets.ps1
#
# If scripts are blocked, run this first (valid for this window only):
#   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
#
# NOTE: This file is intentionally ASCII-only. Windows PowerShell 5.1 reads BOM-less UTF-8 as the
# system code page and garbles non-ASCII text, which breaks parsing.
param(
    [string]$Repo = "yazawazawa/DriveCare",
    [string]$Keystore = "$env:USERPROFILE\.android\debug.keystore",
    [string]$Services = "app\google-services.json"
)
$ErrorActionPreference = "Stop"

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "GitHub CLI (gh) not found. Run 'winget install GitHub.cli', then reopen PowerShell."
}
gh auth status *> $null
if ($LASTEXITCODE -ne 0) { throw "Not logged in to gh. Run 'gh auth login' first." }
if (-not (Test-Path $Keystore)) { throw "Keystore not found: $Keystore" }
if (-not (Test-Path $Services)) { throw "google-services.json not found: $Services (are you in the repository folder?)" }

# Show the keystore SHA-1 so you can compare it with the one registered in Firebase (needs keytool).
if (Get-Command keytool -ErrorAction SilentlyContinue) {
    Write-Host "Keystore SHA-1 (compare with the fingerprint registered in Firebase):"
    keytool -list -v -alias androiddebugkey -keystore $Keystore -storepass android -keypass android 2>$null |
        Select-String "SHA1:" | ForEach-Object { $_.Line.Trim() }
} else {
    Write-Host "(keytool not found on PATH: skipping the SHA-1 display)"
}

function ConvertTo-Base64([string]$Path) {
    [Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path $Path)))
}

Write-Host "Target repository: $Repo"
# Pass the value with --body instead of stdin so that no stray CRLF is appended.
gh secret set ANDROID_DEBUG_KEYSTORE_BASE64 --repo $Repo --body (ConvertTo-Base64 $Keystore)
if ($LASTEXITCODE -ne 0) { throw "Failed to set ANDROID_DEBUG_KEYSTORE_BASE64" }
gh secret set GOOGLE_SERVICES_JSON_BASE64 --repo $Repo --body (ConvertTo-Base64 $Services)
if ($LASTEXITCODE -ne 0) { throw "Failed to set GOOGLE_SERVICES_JSON_BASE64" }
Write-Host "Done. Now run the 'Release APK' workflow again in GitHub Actions."
