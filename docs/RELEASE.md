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

手元のアプリと同じ署名・設定で配布するには、リポジトリの Settings > Secrets and variables > Actions に、次の2つを登録します。

| Secret 名 | 中身 |
|---|---|
| `ANDROID_DEBUG_KEYSTORE_BASE64` | `~/.android/debug.keystore` を base64 にした文字列 |
| `GOOGLE_SERVICES_JSON_BASE64` | `app/google-services.json` を base64 にした文字列 |

```bash
base64 -w0 ~/.android/debug.keystore
base64 -w0 app/google-services.json
```

macOS では `-w0` の代わりに `base64 -i <ファイル>` を使ってください。
キーストアは秘密情報です。Secrets 以外（リポジトリやチャット）には貼らないでください。

## 補足
- このAPKは debug ビルドです。Google Play で配布する場合は、別途リリース用のキーストアで署名した release ビルドが必要です。
