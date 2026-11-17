# クラウド共有のセットアップ（開発者向け）

DriveCare のクラウド共有は **Firebase**（Authentication + Firestore）を使います。
`app/google-services.json` が無い場合もアプリはビルドでき、設定画面に「クラウド共有: 未設定」と表示されるだけです。
共有を使うには、次の手順で Firebase プロジェクトを用意してください（無料の Spark プランで足ります）。

## 1. Firebase プロジェクトを作る
1. [Firebase コンソール](https://console.firebase.google.com/) で「プロジェクトを追加」。
2. プロジェクトの設定 > 「アプリを追加」> **Android**。
   - パッケージ名: `com.takumi.drivecare`
   - **デバッグ用 SHA-1** を登録（下記コマンドで取得）。リリース版を配布する場合は、そのキーストアの SHA-1 も追加します。

```bash
keytool -list -v -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android -keypass android
```

3. `google-services.json` をダウンロードし、`app/` フォルダ直下（`app/google-services.json`）に置く。

## 2. ログイン（Authentication）
1. Authentication > 「始める」> ログイン方法 > **Google** を有効化（サポートメールを設定）。
2. SHA-1 を追加・変更した場合は、`google-services.json` をダウンロードし直して置き換える。

## 3. データベース（Firestore）
1. Firestore Database > 「データベースを作成」（本番環境モード、ロケーションは `asia-northeast1`（東京）など）。
2. 「ルール」タブに、リポジトリの [`firestore.rules`](../firestore.rules) の内容を貼り付けて **公開**。

## 4. ビルドして確認
Android Studio で Sync → Run。設定 > クラウド共有 から「Google でログイン」できれば成功です。

## データの仕組み
- 共有単位は「グループ」。作成した人が招待コード（10文字・7日間有効）を発行し、相手が入力して参加します。
- グループのメンバー全員が、車両・給油・整備の記録と整備項目の設定を **閲覧・編集** できます。
- 共有中の読み書きは Firestore のオフラインキャッシュ経由です。オフラインでも記録でき、接続が戻ると自動で同期します。
- 同じ記録を同時に編集した場合は、最後に保存した内容が残ります（記録単位）。
- 共有中は、端末内のバックアップ復元（全置換）は使えません。共有を終了してから行ってください。
- 共有を終了すると、選択に応じてグループのデータが端末内へコピーされます。

## 費用の目安
Spark プラン（無料）の無料枠は、1日あたり読み取り5万件・書き込み2万件です。個人〜家族での利用では通常十分です。

## トラブルシューティング

### 「Google でログイン」を押すと、すぐ元の画面に戻る
たいてい、**Firebase に登録した SHA-1 と、インストールしたアプリの署名が一致していません**
（デバッグビルドは、ビルドした PC や CI の署名キーによって SHA-1 が変わります）。

1. **いま入っているアプリの SHA-1 を調べる**（APK ファイルを使います。`keytool` は Android Studio 同梱のものでも構いません）。

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -printcert -jarfile DriveCare-0.0.1c.apk
```

   出力の `SHA1:` の行が、そのアプリの署名です。

2. Firebase コンソール > プロジェクトの設定 > 全般 > Android アプリ > 「SHA 証明書フィンガープリント」に、**同じ値が登録されているか**確認する。無ければ追加する。
3. 追加したら `google-services.json` をダウンロードし直し、`app/` の中身と GitHub の Secrets（`GOOGLE_SERVICES_JSON_BASE64`）を更新する。
4. 反映まで数分かかることがあります。アプリを一度終了してから、もう一度試してください。

### その他
- 「この端末に Google アカウントが見つかりません」: 端末の設定で Google アカウントを追加する。
- 「クラウド共有は未設定です」: `google-services.json` が入っていない、または Google ログインを有効にする前にダウンロードした古いファイル（`oauth_client` が空）です。Authentication で Google を有効にしてから、ダウンロードし直す。
