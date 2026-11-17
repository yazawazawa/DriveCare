#!/usr/bin/env bash
# リリース用 APK のビルド（GitHub Actions）に必要な 2 つの秘密情報を、GitHub の Secrets に登録する。
#
#   ANDROID_DEBUG_KEYSTORE_BASE64 : デバッグ用キーストア（~/.android/debug.keystore）
#   GOOGLE_SERVICES_JSON_BASE64   : Firebase の設定（app/google-services.json）
#
# 使い方（あなたの PC で、リポジトリのフォルダから実行）:
#   1. GitHub CLI を入れてログインしておく:  https://cli.github.com/  →  gh auth login
#   2. bash scripts/set-github-secrets.sh
#
# 値はこの PC から GitHub へ直接送られ、画面には表示されない。Windows では Git Bash で実行する。
set -euo pipefail

REPO="${1:-yazawazawa/DriveCare}"
KEYSTORE="${ANDROID_DEBUG_KEYSTORE:-$HOME/.android/debug.keystore}"
SERVICES="${GOOGLE_SERVICES_JSON:-app/google-services.json}"

command -v gh >/dev/null 2>&1 || { echo "GitHub CLI (gh) が見つかりません。https://cli.github.com/ から入れてください。" >&2; exit 1; }
gh auth status >/dev/null 2>&1 || { echo "gh にログインしていません。'gh auth login' を実行してください。" >&2; exit 1; }
[ -f "$KEYSTORE" ] || { echo "キーストアが見つかりません: $KEYSTORE" >&2; exit 1; }
[ -f "$SERVICES" ] || { echo "google-services.json が見つかりません: $SERVICES" >&2; exit 1; }

# Firebase に登録した SHA-1 と一致するか、目で確認できるように表示する
if command -v keytool >/dev/null 2>&1; then
  echo "キーストアの SHA-1（Firebase の「SHA 証明書フィンガープリント」と同じか確認してください）:"
  keytool -list -v -alias androiddebugkey -keystore "$KEYSTORE" -storepass android -keypass android 2>/dev/null \
    | grep -E "SHA1:" | sed 's/^ *//' || true
fi

encode() { base64 < "$1" | tr -d '\n'; }

echo "登録先: $REPO"
encode "$KEYSTORE" | gh secret set ANDROID_DEBUG_KEYSTORE_BASE64 --repo "$REPO"
encode "$SERVICES" | gh secret set GOOGLE_SERVICES_JSON_BASE64 --repo "$REPO"
echo "完了しました。GitHub の Actions で「Release APK」をもう一度実行してください。"
