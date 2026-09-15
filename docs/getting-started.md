# 初期セットアップ

## 環境

JDK 25、Git、稼働中のDocker、editor／IDEを用意する。applicationはhost JVM、PostgreSQLと資料toolchainはDockerで動く。global Gradle、Node、npm、tblsのinstallは不要。初回は依存とimageを取得できるnetworkとdisk領域が必要。

```bash
java -version
docker info
./gradlew --version
```

Windowsでは `gradlew.bat` を使う。IDEではGradle projectとしてimportし、Gradle JVMにJDK 25を指定する。

## 起動

1. `config/local-env.properties.example` を `config/local-env.properties` へコピーする。
2. DEV_DB_PASSWORDに開発DBのpassword、APP_USER_NAME／APP_USER_PASSWORDにログインIDとpasswordを記入する。ログインpasswordは12文字以上・72 UTF-8 bytes以下。
3. `./gradlew bootRun` を実行する。
4. `http://localhost:8080/` を開く。

local fileはUTF-8 Java Properties形式。値はJava Propertiesのliteralとして記述する。literal backslashは `\\` と記載する。HTTP timeout等も環境変数名で追記できる。

既存環境変数がlocal fileより優先される。fileは任意で、環境変数だけでも起動できる。読み込みはbootRunの子プロセスへの環境変数注入に限定する。secretの管理先はGit管理外のローカル設定または実行環境のsecret管理基盤とする。

起動時はComposeのdev PostgreSQLを起動し、Bootがloopbackの動的portを検出する。Flyway migration後にapplicationが8080で待ち受ける。停止はbootRunの端末でCtrl+C。DB containerも停止し、named volumeのdataは残る。

| URL | 用途・必要な設定 |
|---|---|
| `/` | 匿名welcome |
| `/components` | 匿名Vaadin部品集。変更はView内だけ |
| `/theme-comparison` | 匿名Aura／Lumo比較。各テーマで同じ部品集を操作 |
| `/features` | DB作成・UUID検索。未ログインなら/loginへ誘導 |
| `/api/v1/features` | 認証必須REST。正常系はintegrationTestでも確認可能 |
| `/actuator/health` | 匿名health。詳細は非公開 |
| `/swagger-ui/index.html` | API資料を有効化し、ops:readを持つ利用者でログイン |

Swagger UIを使用する場合、local fileの `API_DOCUMENTATION_ENABLED=true` と `APP_USER_OPERATIONS_READ=true` の両方をコメント解除して再起動する。既にログインしていた場合はログアウトして再ログインする。port・context path・外部HTTP timeoutの任意設定もexampleに記載している。配布先用のDB接続設定と全体の設定一覧は[運用ガイド](operations.md#設定)を参照する。

部品集は再読み込みで初期データへ戻る。永続化の実装例には `/features` とFeatureCommands／FeatureQueriesを使用する。ログインとシステム実行は[運用ガイド](operations.md#authentication)を参照する。

## 動作確認

```bash
./gradlew build
./gradlew documentation
```

`build` は一時DBとChromiumを使うquality gateを含む。test／codegen／資料用DBは開発DBから隔離され、DEV_DB_PASSWORDは不要。Linuxでbrowserの共有libraryが不足する場合は `./gradlew playwrightInstallDeps` を使う。このtaskにはsystem packageをinstallできる権限が必要。

## 案件への置換

[案件開始チェックリスト](project-adoption.md)の順に進める。必須変更、採用判断、条件付き変更を区別し、名称・packageの全置換先、sample変更時の連動先、認証・DB・CI・運用の完了条件を確認する。煩雑な変更については同書の自動化案から方式を選ぶ。

生成Javaの更新はsource変更後の再生成で行う。通常の業務実装は[開発ガイド](developer-guide.md)に従う。

## 起動時の問題

| 症状 | 確認すること |
|---|---|
| DEV_DB_PASSWORD不足 | local fileの名前と値、既存環境変数による上書き |
| DBのpassword不一致 | 初回volume作成時のpasswordを使う。file変更だけでは既存DBユーザーのpasswordは変わらない |
| Docker接続失敗 | Dockerの起動、利用context、権限、image取得のnetwork |
| 8080が使用中 | 同じprojectの起動が残っていないか確認。別portが必要ならSERVER_PORTを設定 |
| RESTが401 | /loginで設定した利用者としてログインし、同じsessionで呼ぶ |
| 利用者設定のvalidation失敗 | APP_USER_NAME、APP_USER_PASSWORDの設定と長さを確認 |
| 認証済みPOSTが403 | feature:writeとCSRF tokenの両方を確認 |
| 統合テストのbrowser起動失敗 | Playwrightのdownload、Linux共有library、network／disk容量 |

永続volumeの削除はdataを失う。volumeを削除する場合は、必要なdataの確認とbackupを先に行う。
