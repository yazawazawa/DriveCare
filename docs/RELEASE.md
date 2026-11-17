# リリース（APK の配布）

GitHub Actions が APK をビルドして、リリースに添付します
（[`.github/workflows/release-apk.yml`](../.github/workflows/release-apk.yml)）。次のどちらかで始めます。

**方法1: GitHub の画面から（かんたん）**
1. リポジトリの「Actions」タブ > 左の「Release APK」を開く。
2. 「Run workflow」を押し、ブランチは `main` のまま、タグ名に `v0.0.1a` と入力して実行する。
   （タグが無ければ、`main` の最新コミットに自動で作られます）

**方法2: タグを push する**

```bash
git tag v0.0.1a
git push origin v0.0.1a
```

完了すると、リポジトリの「Releases」に `DriveCare-0.0.1a.apk` が出ます。
進行状況は「Actions」タブで確認できます。失敗した場合は、同じ画面から「Re-run all jobs」で再実行できます。

## バージョンを上げるとき
1. `app/build.gradle.kts` の `versionName`（表示用）と `versionCode`（更新判定用・必ず増やす）を変更する。
2. `main` にマージしてから、上のとおりタグを push する。

## 署名キーと Firebase 設定（推奨）
何も設定しなくてもビルドはできますが、その場合は次のようになります。

| 項目 | 未設定のとき |
|---|---|
| 署名 | ビルドのたびに署名が変わる。端末に入っているアプリへ上書きインストールできない |
| Google ログイン | 登録済みの SHA-1 と一致しないため、ログインに失敗する |
| クラウド共有 | `google-services.json` が無いため「未設定」になる |

手元のアプリと同じ署名・設定で配布するには、次の2つの秘密情報を GitHub の Secrets に登録します。

| Secret 名 | 中身 |
|---|---|
| `ANDROID_DEBUG_KEYSTORE_BASE64` | `~/.android/debug.keystore`（デバッグ用キーストア） |
| `GOOGLE_SERVICES_JSON_BASE64` | `app/google-services.json`（Firebase の設定） |

### 方法A: スクリプトで登録（かんたん）
[GitHub CLI](https://cli.github.com/) を入れて `gh auth login` を済ませたら、**自分の PC で**リポジトリのフォルダから実行します。

Windows（PowerShell）:

```powershell
.\scripts\set-github-secrets.ps1
```

実行できないと言われたら、先に `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass` を実行してください。
PowerShell で `bash scripts/...` を実行すると、Windows 標準の WSL が呼び出されて `E_UNEXPECTED` になることがあります。PowerShell 版を使ってください。

macOS / Linux（または Windows の Git Bash）:

```bash
bash scripts/set-github-secrets.sh
```

値は PC から GitHub へ直接送られ、画面には出ません。実行中にキーストアの SHA-1 が表示されるので、Firebase に登録した値と同じか確認してください。

### 方法B: 画面から手で登録
1. リポジトリの Settings > Secrets and variables > Actions > 「New repository secret」。
2. 上の表の名前で、次のコマンドで作った文字列を貼り付けます。

```bash
base64 -w0 ~/.android/debug.keystore
base64 -w0 app/google-services.json
```

macOS は `-w0` の代わりに `base64 -i <ファイル> | tr -d '\n'`、Windows の PowerShell は
`[Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\.android\debug.keystore"))` を使います。

キーストアは秘密情報です。Secrets 以外（リポジトリやチャット）には貼らないでください。

## 補足
- このAPKは debug ビルドです。Google Play で配布する場合は、別途リリース用のキーストアで署名した release ビルドが必要です。
