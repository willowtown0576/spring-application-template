# spring-application-starter

Java / Springで小規模〜中規模の業務システムを構築するための個人用starter。業務固有機能だけが存在しない、技術基盤として完成したapplicationを目指す。

build・DB・REST・Vaadin・横断基盤・資料生成と、GitHub Actions / Renovate / release基盤を実装している。検証の現在地と残事項は [実装計画](docs/implementation-plan.md) を参照する。

## 前提環境

- JDK 25
- Git
- Docker
- editor / IDE

applicationはhost JVMで実行する。global Gradleは不要。Vaadin用Node/npmはbuild toolingが管理する。標準componentの画面では公式precompiled frontend bundleを利用する。

codegenと統合テストには稼働中のDockerが必要。初回実行時はWrapper・依存・container image取得のためnetwork接続が必要。Windowsでは `./gradlew` の代わりに `gradlew.bat` を使う。

## 案件開始時の初期化

project name、Gradle group、base package（仮名 `dev.template.application`）、application nameを案件に合わせて一括置換する。独自initializerは導入しない。

## 主なコマンド

| コマンド | 用途 |
| --- | --- |
| `./gradlew bootRun` | 開発PostgreSQL起動 → Flyway → Web application起動（default port 8080） |
| `./gradlew test` | domain・usecase・UUID v7・ログの機密保護を検証 |
| `./gradlew integrationTest` | isolated PostgreSQLでDB・REST・認可・HTTP設定・Batch・Chromiumでの画面操作を検証 |
| `./gradlew jooqCodegen` | 一時PostgreSQLにmigrationを適用し、jOOQ sourceを生成 |
| `./gradlew architectureTest` | Modulithの境界・公開APIとArchUnitの技術的依存制約を検証 |
| `./gradlew spotlessApply` | Java formatと資料・設定の末尾空白などを修正 |
| `./gradlew check` | テスト・format・静的解析のquality gate |
| `./gradlew documentation` | 一時DBからDB資料、OpenAPI、project PDFを生成 |
| `./gradlew build` | 検証とbuild。重いdocumentation生成は含めない |
| `./gradlew bootJar` | 配布用executable JARの生成 |
| `./gradlew bootBuildImage` | 固定Paketo BuildpacksでOCI imageをローカル生成 |

`bootRun` の前に開発DB用passwordを `DEV_DB_PASSWORD` 環境変数へ設定する。Composeはloopbackの動的portを使用し、接続先はBootが検出する。終了時にDBを停止するが、named volumeのdataは保持する。既存volumeのDB passwordは環境変数の変更だけでは変更されないため、次回も同じpasswordを使用する。

`check` / `jooqCodegen` は開発password不要で、一時DBを自動で作成・破棄する。RESTの正常系はtest用認証を使う `integrationTest` で確認できる。

`check` はSpotless、手書きコード（codegenを含む）のCheckstyle / SpotBugs、Unit / Integration / Architecture Test、JaCoCoを実行する。reportは `build/reports/`、Unit / Integration Testの集約coverageは `build/reports/jacoco/test/html/index.html` に出力する。

配布用JARは `build/libs/` に生成する。JARには開発用Compose連携を含めない。実行時は既存PostgreSQLへの `SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD` を環境変数で設定する。初期versionでの実行例:

```bash
./gradlew bootJar
java -jar build/libs/spring-application-starter-0.1.0-SNAPSHOT.jar
```

## CI / Release

CIはGitHubのbranch push・pull requestで `./gradlew check build` を実行する。Ubuntu 24.04 / Temurin 25を使い、Playwrightのsystem libraryは `./gradlew playwrightInstallDeps` でinstallする（Linuxではsudo権限が必要）。検証reportはActions artifactで14日間保持する。

Renovate Appをrepositoryへ接続すると、Gradle / Wrapper / Docker / Actions / Mermaidの更新をPRで提案する。自動mergeは無効。PostgreSQLのCompose / Version Catalogは同じPRで更新し、固定Debian snapshotの日付は手動で検証して更新する。

release時は `build.gradle.kts` のproject versionを `1.0.0` 等のstable SemVerへ変更してcommitし、それと一致する `v1.0.0` tagをpushする。snapshotやversion不一致は拒否する。事前確認例:

```bash
./gradlew verifyReleaseVersion -PreleaseTag=v1.0.0
./gradlew build releaseArtifacts bootBuildImage
```

`releaseArtifacts` は検証後に `build/release/` へexecutable JAR、OpenAPI YAML、DB資料ZIP、project PDFを収集する。tag起点のworkflowはOCI image生成まで確認し、別jobで4成果物をGitHub Releaseへ添付する。workflow_dispatchでは同じbuildを実行し、公開を行わない。

`bootBuildImage` は `${project.name}:${project.version}` のimageをローカルDockerへ生成する。builderとrun imageの固定versionはVersion Catalogで管理する。実行時はJARと同じ3つのDB環境変数を渡す。image registryへのpush・deployment先は案件側で決める。Buildpacksの初回実行にもimage / JRE取得のnetwork接続とdisk容量が必要。

案件開始時はGitHub repository、Actionsの有効化、Renovate App、main branchの保護rule（CIの成功を必須化）を接続する。starterは認証providerやproduction secretを登録しない。

## Feature public API

`FeatureCommands.create(name)` は `Result<UUID, CreateFailure>` を返す。不正な名前は `Failure(INVALID_NAME)`、作成成功はUUID v7を持つ `Success`。`FeatureQueries.find(id)` は `Optional<FeatureView>` を返し、未検出はemptyとなる。

名前は100 Unicode code point以内とし、入力をそのまま保存する。null・空文字・ASCII spaceのみ・NUL・不正なsurrogateを拒否する。同名の作成は許容する。Commandはtransaction、Queryはread-only transactionで実行し、技術障害はExceptionのまま伝播してrollbackする。Commandは `feature:write`、Queryは `feature:read` Authorityを要求する。

## RESTと認証

| 操作 | 成功時の応答 |
| --- | --- |
| `POST /api/v1/features`（JSON: `{"name":"sample"}`） | 201、Location、`{"id":"UUID"}` |
| `GET /api/v1/features/{id}` | 200、`{"id":"UUID","name":"sample"}` |

認証方式は案件側で接続する。固定ユーザー・自動生成ユーザー・login方式は用意していないため、接続前の外部requestは401となる。CSRF保護を有効にしており、認証済みでも権限不足やCSRF拒否は403となる。errorはProblemDetailを返す。入力形式不正は400、未検出は404、名前の業務validation違反は422、予期しない障害は詳細を伏せた500とする。

## Vaadin画面

`bootRun` で起動後の画面URLは `http://localhost:8080/features`。名前による作成とUUIDによる取得、入力エラー・未検出・権限拒否を表示する。認証済みユーザーにだけrouteを許可し、作成には `feature:write`、取得には `feature:read` を要求する。

認証方式を接続するまでは外部ブラウザーからも401となる。動作確認は `./gradlew integrationTest` を使う。テスト専用sessionを使って実HTTP serverとPostgreSQLに接続し、Chromiumで主要flowを検証する。テスト認証は配布JARには含めない。

初回の統合テストではGradleからPlaywright付属CLIを実行してChromiumを取得するため、network接続とbrowser用disk領域が必要。global Node/npmの操作は不要。画面の確認画像は `build/reports/ui/features.png` に出力する。`bootJar` はVaadinのproduction frontendを含む。

## 運用・横断基盤

`GET /actuator/health` は匿名で利用でき、詳細は返さない。`/actuator/info` と `/actuator/metrics` は `ops:read` Authorityを要求する。HikariCPの接続数などもmetricsから確認できる。

HTTP接続timeoutは `HTTP_CONNECT_TIMEOUT`（default `2s`）、読み取りtimeoutは `HTTP_READ_TIMEOUT`（default `10s`）。正のDurationが必要で、不正設定は起動時に失敗する。案件の外部APIはinfrastructureにHTTP Service interfaceを定義し、Springの `@ImportHttpServices` でgroupへ登録する。Bootから注入したRestClient.Builderとgroup clientに共通設定・loggingが適用される。

HTTP requestごとに生成する `X-Correlation-ID` をresponseとoutgoing requestへ伝播する。MDCの `correlationId` で関連ログを追跡できる。追加した共通ログは引数・body・query・例外messageを出さず、処理結果、所要時間、例外型とstack frameを記録する。

BatchのmetadataはFlywayが `system` schemaへ作成する。Jobは起動時に自動実行しない。案件Jobを作る際は共通 `JobExecutionListener` Beanをbuilderの `listener(...)` に登録する。JobParametersはDBへ保存されるためcredentialを渡さない。Schedulingは有効で、案件の単純な定期処理には `@Scheduled` を使える。業務Job・定期処理はまだ登録していない。

終了はBoot標準graceful shutdownを使い、phaseごとのtimeoutは30秒。file storage・event・async・retry・cacheは必要性が明確になった案件で追加する。

## ドキュメント生成

`./gradlew documentation` はDocker内の固定toolchainを使用する。初回は日本語PDF環境を含むimageの取得・buildに時間とdisk容量を要する。hostへのtbls・Pandoc・LaTeX・npmのinstallと開発DB用passwordは不要。

| 個別task | 出力 |
| --- | --- |
| `databaseDocumentation` | `build/documentation/database/`（Markdown / ER SVG） |
| `apiDocumentation` | `build/documentation/api/v1/openapi.yaml` |
| `projectDocumentation` | `build/documentation/project/project.pdf` と `diagrams/*.svg` |

DBは開発環境から隔離し、生成終了時・失敗時とも破棄する。生成物はGit管理せず、変更はmigration、Controller metadata、`docs/project/` のMarkdown / Mermaidへ行う。DBの手書き補足は `docs/database/notes/` に置く。

OpenAPI endpointとSwagger UIはdefault無効。案件の認証を接続した環境で `API_DOCUMENTATION_ENABLED=true` とすると、`ops:read` を持つユーザーが `/v3/api-docs.yaml` と `/swagger-ui.html` を利用できる。通常の `check` / `build` は資料生成を含めない。

## 構成と資料

実装はfeature単位で分割し、REST / Vaadin / Batch / Schedulingをadapterとして配置する。Docker設定は `docker/`、生成物はGit管理しない `build/` に集約する。

- [設計判断の正本](docs/decisions.md)
- [Codex向け指示](AGENTS.md)
- [architecture](docs/architecture.md)
- [database](docs/database.md)
- [実装計画・進捗](docs/implementation-plan.md)

資料と実装に矛盾がある場合は `docs/decisions.md` を優先する。
