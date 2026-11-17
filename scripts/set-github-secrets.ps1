# リリース用 APK のビルド（GitHub Actions）に必要な 2 つの秘密情報を、GitHub の Secrets に登録する（Windows / PowerShell 用）。
#
#   ANDROID_DEBUG_KEYSTORE_BASE64 : デバッグ用キーストア（%USERPROFILE%\.android\debug.keystore）
#   GOOGLE_SERVICES_JSON_BASE64   : Firebase の設定（app\google-services.json）
#
# 使い方（リポジトリのフォルダで実行）:
#   1. GitHub CLI を入れてログインしておく:  winget install GitHub.cli   →   gh auth login
#   2. .\scripts\set-github-secrets.ps1
#
# 実行できない場合は、先に次を実行してください（このウィンドウだけ有効）:
#   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
param(
    [string]$Repo = "yazawazawa/DriveCare",
    [string]$Keystore = "$env:USERPROFILE\.android\debug.keystore",
    [string]$Services = "app\google-services.json"
)
$ErrorActionPreference = "Stop"

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "GitHub CLI (gh) が見つかりません。'winget install GitHub.cli' で入れて、PowerShell を開き直してください。"
}
gh auth status *> $null
if ($LASTEXITCODE -ne 0) { throw "gh にログインしていません。'gh auth login' を実行してください。" }
if (-not (Test-Path $Keystore)) { throw "キーストアが見つかりません: $Keystore" }
if (-not (Test-Path $Services)) { throw "google-services.json が見つかりません: $Services（リポジトリのフォルダで実行していますか？）" }

# Firebase に登録した SHA-1 と一致するか、目で確認できるように表示する（keytool がある場合のみ）
if (Get-Command keytool -ErrorAction SilentlyContinue) {
    Write-Host "キーストアの SHA-1（Firebase の「SHA 証明書フィンガープリント」と同じか確認してください）:"
    keytool -list -v -alias androiddebugkey -keystore $Keystore -storepass android -keypass android 2>$null |
        Select-String "SHA1:" | ForEach-Object { $_.Line.Trim() }
}

function ConvertTo-Base64([string]$Path) {
    [Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path $Path)))
}

Write-Host "登録先: $Repo"
# 標準入力ではなく --body で渡す（改行コードが混ざらないようにするため）
gh secret set ANDROID_DEBUG_KEYSTORE_BASE64 --repo $Repo --body (ConvertTo-Base64 $Keystore)
if ($LASTEXITCODE -ne 0) { throw "ANDROID_DEBUG_KEYSTORE_BASE64 の登録に失敗しました" }
gh secret set GOOGLE_SERVICES_JSON_BASE64 --repo $Repo --body (ConvertTo-Base64 $Services)
if ($LASTEXITCODE -ne 0) { throw "GOOGLE_SERVICES_JSON_BASE64 の登録に失敗しました" }
Write-Host "完了しました。GitHub の Actions で「Release APK」をもう一度実行してください。"
