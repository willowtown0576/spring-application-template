# Implementation Plan

設計判断の正本は [decisions.md](decisions.md)。この計画は既存Decisionを実装順序へ整理した派生資料であり、新しい技術選定を確定するものではない。

## 現在地

Phase 8（CI / Releaseと完成確認）完了。GitHubのCIとrelease build-only検証、Renovateのdependency抽出、clean build、JAR / OCI起動を確認した。全46項目の証跡は [Starter完成条件の検証](starter-verification.md) に集約した。

予定したPhase 0〜8の実装は完了。GitHub Releaseの実公開、Renovate Appの定期PR、案件の認証・secret・deployment接続、Windows検証は未実施。今後は案件の業務要件に応じて作業を指定する。

## Phase 0 — 資料整備

- [x] AGENTS.mdとDecision Ledgerの確認、既存ファイルの棚卸し。
- [x] README、architecture.md、database.md、implementation-plan.mdの作成。
- [x] 完成後の仕様と現在の未実装状態の区別。

完了条件: 派生資料が既存Decisionと整合し、phaseごとの成果物・検証・未確定事項を辿れること。

## Phase 1 — Buildと初期quality gate

対象Decision: D-020〜D-029、D-040〜D-042、D-450〜D-475。

- [x] Java 25とSpring Boot 4.1.xを軸に、公式情報で互換性と具体versionを確認し、選定をDecision Ledgerへ記録。
- [x] Gradle Kotlin DSL、Wrapper、Toolchain、Version Catalog、project version、Git除外・改行設定を整備。
- [x] 最小Spring Boot applicationと少数のcontext smoke testを用意。
- [x] test / integrationTest / architectureTestを分離。Spotless、Checkstyle、SpotBugs、JaCoCo、Modulith / ArchUnitを初期設定し `check` へ接続。

完了条件: `./gradlew spotlessApply`、`./gradlew check`、`./gradlew bootJar` が成功。まだ対象のないtestを架空のsampleで埋めず、未検証の制約を明記する。DBやWebの後続phaseは先行実装しない。

2026-09-13検証記録:

- JDK Temurin 25.0.4.1、Gradle 9.7.0、Spring Boot 4.1.1で `./gradlew spotlessApply check bootJar --warning-mode all` 成功。
- Integration Test 1件、Architecture Test 1件、失敗・skipともに0。Unit Testは対象がなくNO-SOURCE。
- main / integrationTest / architectureTestのCheckstyleとSpotBugsが成功。JaCoCo HTML / XML report生成を確認。
- executable JARを `java -jar` で起動し、正常終了（exit 0）を確認。
- Wrapper JARのSHA-256を公式checksumと照合。distribution SHA-256もWrapperへ設定済み。
- 実feature moduleの違反検出、technical ArchUnit rule、DB統合、Web/security、Windows上のWrapper実行は未検証。対象phase / 実行環境で検証する。

具体選定とscopeはD-570〜D-572に記録した。

## Phase 2 — PostgreSQL / Flyway / jOOQ

対象Decision: D-060〜D-064、D-190〜D-265。

- [x] migration配置、history table、generated package、codegen方式を公式仕様に基づいて先に確定。
- [x] 一つのCompose fileに開発PostgreSQLとpersistent named volumeを設定。
- [x] 最小feature schemaとmigration、COMMENTを追加。
- [x] 一時DB → Flyway → jOOQ生成 → compileのflowとinputs/outputsを実装。
- [x] PostgreSQL Testcontainersでmigration・DB制約を検証。開発DBから隔離し、一時DBの後始末を保証。

完了条件: `bootRun` のDB連携、`jooqCodegen` の再現性・変更なし時の再生成回避、`integrationTest` と `check` の成功。

2026-09-13検証記録:

- `./gradlew clean spotlessApply check bootJar --warning-mode all` 成功。DB Integration Test 8件、Architecture Test 1件、失敗・skip 0。
- 一時DBへV1を適用し、generated typeでUUID / 100文字のUnicode名のround-trip、NOT NULL、blank、最大長超過、PK重複、applicationによるID付与を検証。
- codegen source setを含むCheckstyle / SpotBugs成功。generated codeは静的解析とcoverage集計から除外。
- 検証専用Compose projectで `bootRun` を2回実行。初回のmigration適用と、named volumeを保持した2回目の再適用不要を確認。検証後はそのprojectのcontainer / volumeを削除。
- 一時的な不正migration追加でcodegenが失敗すること、および失敗後のPostgreSQL container削除を確認。不正migrationを除去して再生成するとsource全fileのSHA-256が元と一致。
- 続く `jooqCodegen` がUP-TO-DATEとなり、DBを起動しないことを確認。
- documentation DBはPhase 7、business transaction / module boundaryはPhase 3以降で検証する。Windows実行は未検証。

具体方式はD-580〜D-583、確定済みmigration方針はD-234 / D-235へ反映した。

## Phase 3 — Featureの最小縦断実装

対象Decision: D-100〜D-186、D-263、D-450〜D-454。

- [x] 最小sampleの操作・不変条件・typed FailureとUUID v7生成手段を先に確定。
- [x] public Command/Query APIとNamed Interface、Command/usecase/domain/Repository、Query/DataSourceを実装。
- [x] transaction boundary、Result、Clock、jOOQ adapterを接続。
- [x] domain/usecaseのUnit Test、永続化とrollbackのIntegration Test、Modulith boundaryと技術的依存のArchitecture Testを追加。

完了条件: 正常系・expected Failure・技術障害を検証し、partial commitを防止。Queryがdomainを経由せず、`check` が成功。

2026-09-13検証記録:

- `./gradlew spotlessApply check bootJar --warning-mode all` 成功。Unit 10件、Integration 16件、Architecture 5件、失敗・skip 0。
- 名前のnull / blank / NUL / unpaired surrogate / Unicode最大長を検証。usecaseはSpring contextなしで永続化と例外伝播を検証。
- Clock.fixedから生成したUUIDのversion / variant / timestampと異なるID生成を確認。
- public Command APIのcommit、typed Failure時のwriteなし、insert後の技術例外によるrollback、QueryのPostgreSQL read-only transactionを確認。
- ModulithのNamed Interface公開範囲・内部型非公開・依存制約と、ArchUnitのdomain/usecase・Query・generated type・transaction配置ruleを検証。
- SpotBugsのRepository共有に関するfalse positiveだけを限定filterで除外。generated classはauxiliary classpathへ含めて型解決する。
- schema変更なし。security / RESTはPhase 4で実装する。Windows実行は未検証。

設計判断はD-590〜D-594、UUIDの具体実装はD-174へ反映した。

## Phase 4 — RESTとsecurity

対象Decision: D-280〜D-305、D-320〜D-332、D-455。

- [x] Spring MVC標準versioningのAPIを確認し、REST adapterとProblemDetailを実装。
- [x] Method Security、feature単位Authority、入口の制限を追加。
- [x] 案件の認証方式を固定せず、安全なbaselineと動作確認方法をDecision Ledgerに記録。
- [x] validation、JSON、HTTP status、401/403、認可拒否時の更新防止を検証。

完了条件: MockMvcとsecurity/transactionの統合検証、`check` が成功。RESTからfeature internalへの直接依存がない。

2026-09-13検証記録:

- `./gradlew spotlessApply check bootJar --warning-mode all` 成功。Unit 10件、Integration 30件、Architecture 7件、失敗・skip 0。
- 作成201 / Locationと取得200、入力不正400、未認証401、権限不足・CSRF拒否403、未検出404、業務validation違反422、技術障害500を検証。
- RESTとdirect APIの認可拒否でwriteが発生しないこと、insert後の技術例外でrollbackすること、500応答が内部詳細を含まないことを確認。
- 自動生成ユーザーなし、Spring MVC標準versioning、webのpublic API境界とSQL直接依存禁止、transaction境界の認可宣言を検証。
- Checkstyle / SpotBugs / Spotless成功、JaCoCo reportとexecutable JAR生成を確認。
- 案件固有の認証providerとの接続、Vaadin、Windows実行は未検証。対象phase / 案件 / 実行環境で検証する。

設計判断と認証接続前の動作確認方法はD-600〜D-602へ記録した。

## Phase 5 — Vaadin

対象Decision: D-023、D-072、D-310〜D-312、D-456。

- [x] Vaadin 25 BOM・build tooling・Node管理とsecurity連携を公式仕様で確認。
- [x] public APIを使う最小View、入力validation、Failure表示とroute制限を実装。
- [x] 重要flowに絞ってE2E検証を用意。

完了条件: global Node/npm管理を要求せずbuildでき、UIの重要flowと認可を検証。`check` が成功。

2026-09-13検証記録:

- `./gradlew clean spotlessApply check bootJar --warning-mode all` 成功。Unit 10件、Integration 31件（ブラウザーE2E 1件を含む）、Architecture 7件、失敗・skip 0。
- Chromiumで未認証401、名前validation、作成、生成IDによる取得、UUID形式不正、未検出、read-onlyユーザーの取得と作成拒否を検証。
- 実HTTP serverとPostgreSQLを使用し、既存RESTの401 / 403 / CSRF / rollback検証も成功。UIも既存Modulith / ArchUnit ruleで検証。
- Vaadin公式precompiled frontendを使ってclean buildが成功。global Node/npmの操作なし。custom frontendを追加した場合のNodeを使う再bundleは未検証。
- JARのproductionModeとfrontend asset同梱、テスト用認証class / Playwright非同梱を確認。
- `build/reports/ui/features.png` を目視し、日本語表示とUUID全体が見える入力幅を確認。Checkstyle / SpotBugs / SpotlessとJaCoCo report生成も成功。
- 案件固有の認証provider、Windows / Linuxでのブラウザー実行は未検証。Linux CIのbrowser system dependency整備はPhase 8で行う。

具体選定と検証方法はD-610〜D-612へ記録した。

## Phase 6 — 横断基盤

対象Decision: D-340〜D-447、D-500〜D-510。

- [x] Logging、MDC correlation ID、機密情報を出さない処理を実装・検証。
- [x] 型付きConfigurationPropertiesと起動時validation、HTTP Service Clientのtimeout設定を整備。不要な外部serviceやclient frameworkは作らない。
- [x] Actuator公開制限、Hikari metrics、graceful shutdownを検証。
- [x] Batch / Schedulingを利用可能にし、metadataが必要な場合は配置を先に確定。意味のないJobやfake Authenticationは作らない。
- [x] file I/O、event、async、retry、cacheの採用条件を資料と照合。案件依存の実装は追加しない。

完了条件: configuration不足、ログの機密保護、timeout、Actuatorのアクセス制限を検証し、`check` が成功。

2026-09-13検証記録:

- `./gradlew spotlessApply check bootJar --warning-mode all` 成功。Unit 12件、Integration 41件、Architecture 7件、失敗・skip 0。
- HTTP Service Groupの実通信でcorrelation伝播、read timeout、503応答のretryなしを確認。connect timeoutの設定値、必須Durationの欠落・非正値・形式不正によるcontext起動失敗を検証。
- HTTP filterの例外時MDC復元と500記録、Commandログの結果・数値duration・引数／Result値の非出力、共通例外ログのmessage非出力を検証。
- ChromiumでVaadin共通error handlerの一般的な通知と例外message非漏洩を確認。既存REST / UI / transaction / module boundary検証も成功。
- healthの匿名公開とdetail非公開、info / metricsのops:read認可、HikariCP metrics取得、envの非公開を検証。
- V2 migrationでBatch metadataを作成。正常終了、完了instanceの重複実行拒否、失敗状態の永続化と同一instanceからの再実行完了を確認。production Jobなし・自動起動なしを確認。
- Boot schedulerでtask実行、登録された定期処理なしを確認。graceful shutdown設定とtest server終了時の正常shutdownを確認。負荷中のdrain時間や実運用環境の終了signalは未検証。
- Checkstyle / SpotBugs / Spotless、JaCoCo、executable JAR生成が成功。外部service固有の接続・networkのconnect timeout発火・案件Job / 定期処理・Windows / Linuxは対象案件・環境で検証する。

設計判断はD-620〜D-623。file I/O、event、async、retry、cacheは既存の採用条件を維持し、不要な実装・dependencyは追加していない。

## Phase 7 — 再現可能なdocumentation

対象Decision: D-065、D-270〜D-276、D-480〜D-494。

- [x] Composeのdocs profileと一時DB、固定versionのdocumentation tool imageを追加。
- [x] `databaseDocumentation`、`apiDocumentation`、`projectDocumentation` とaggregate `documentation` を実装。
- [x] DB documentationのGit管理方針を先に確定。OpenAPIのversion別出力とproductionでの公開制限を整備。
- [x] `docs/project/` にsourceを作成。Mermaidを事前にSVG化し、Pandoc / LuaLaTeXで日本語PDFを生成。

完了条件: `documentation` の生成物を確認し、SVG・日本語PDFの表示、DBの隔離と後始末を検証。通常 `build` は重いdocumentation生成を要求しない。

2026-09-14検証記録:

- `./gradlew spotlessApply documentation check bootJar --warning-mode all` 成功。Unit 12件、Integration 42件、Architecture 7件、OpenAPI生成test 1件、失敗・skip 0。最終ER描画調整後の `documentation` も成功。
- docs profileの一時PostgreSQLへV1 / V2を適用し、featureとsystemの7 tableについてCOMMENT・制約を含むMarkdown、schema JSON、8点のER SVGを生成。開発DBは使用していない。
- tbls失敗時にもfinalizerで一時container / networkが削除されること、正常終了後も残らないことを確認。生成失敗の原因が分かるようにCompose出力を報告し、一時passwordを伏せる。
- OpenAPIのv1 path、JSON / ProblemDetail schema、名前の最大長、未認証401・権限不足403・ops:readでの取得、Swagger UIの認可を検証。default設定ではOpenAPI / Swaggerの公開Beanが存在しないことを確認。
- Pandoc / LuaLaTeXで日本語PDF 2ページを生成。全ページをPNGへrenderし、日本語・Mermaidのラベル・表の表示を目視確認。PDFのtext抽出も確認。
- tbls内蔵SVG rendererの文字重なりを検出し、tbls DOT → native Graphvizで解消。全体ERとfeature図の表示を確認。検証用画像は `build/reports/documentation/`。
- `./gradlew build --dry-run` にdocumentation関連taskが含まれないことを確認。Checkstyle / SpotBugs / SpotlessとJaCoCo report生成も成功。
- macOS hostとDocker Linux containerで検証。Windows host、Linux CI、releaseへの添付は未検証で、Phase 8または対象環境で確認する。PDFのbyte単位の同一性は保証対象にしていない。

設計判断はD-630〜D-633。生成物はGit管理せず、手書きのDB補足とMarkdown / Mermaid sourceを管理する。

## Phase 8 — CI / Releaseと完成確認

対象Decision: D-042、D-520〜D-543、AGENTS.mdのStarter Definition of Done。

- [x] GitHub ActionsからGradleの `check` を実行し、build logicの重複を避ける。
- [x] Renovateでdependencies、plugins、Wrapper、Docker images、Actionsを追跡。
- [x] tag起点のrelease workflow、executable JAR、Buildpacks、必要なdocumentation assetsを整備。
- [x] READMEを実際に動作する初期化・起動・検証・release手順へ更新。
- [x] Starter Definition of Doneを全件照合し、成果物と検証証跡を確認。

完了条件: clean環境で `check`、`build`、`documentation`、`bootBuildImage` を検証し、CI/releaseの動作を確認。未実行項目を成功として扱わない。

2026-09-14検証記録:

- `./gradlew spotlessApply clean check build releaseArtifacts bootBuildImage --no-build-cache --warning-mode all` 成功。全42 taskを実行し、既存のbuild出力とGradle build cacheを使わず再生成。Unit 12件、Integration 42件、Architecture 7件、OpenAPI生成test 1件、失敗・skip 0。
- Paketo Noble Java tiny builder 0.0.187 / run image 0.0.130からOCIを生成。JRE 25、non-root（1002:1001）で起動し、一時DBへFlywayを適用。JARとOCIのhealth UP、REST / UI / info / OpenAPIの未認証401、終了と一時DB破棄を確認。
- releaseArtifactsがexecutable JAR、OpenAPI YAML、DB資料ZIP（8 ER SVGを含む）、project PDFの4成果物だけを収集すること、JARへtest認証 / Playwrightが入らないことを確認。
- release version検証はstable version一致で成功し、不一致・不正tag・snapshotで失敗する。正の検証では一時的にproject versionを1.2.3へ変更し、終了後0.1.0-SNAPSHOTへ復元した。実release tagは作成していない。
- actionlint 1.7.12で両workflowの構文検証成功。GitHubの [CI run 34794141586](https://github.com/willowtown0576/spring-application-template/actions/runs/34794141586) はUbuntu 24.04 / Java 25でcheck / build / report uploadが成功。
- GitHubの [Release run 34794156551](https://github.com/willowtown0576/spring-application-template/actions/runs/34794156551) はbuild / documentation / Buildpacks / artifact uploadが成功。取得した成果物のJARとOpenAPIはlocal版とSHA-256が一致。DB ZIPの8 ER SVGとGitHub生成PDF全2ページの日本語表示も確認。workflow_dispatchのためpublish jobは意図どおりskip。実際のGitHub Release作成とregistry pushは行っていない。
- Renovate 44.83.0公式imageでstrict config validationとlocal extract dry-runが成功。Gradle 39、Wrapper 1、Compose 2、Dockerfile 3、Actions 13、npm 1、Version Catalog内Docker image 3の計62依存参照を11設定ファイルから検出。remote lookup / Appの定期PR実行は未検証。
- Git除外の`build/`が手書きの`src/codegen/java/dev/template/build/`にも一致する不具合を修正。ルート限定`/build/` / `/bin/`とし、2つのcodegen実行classをGit管理。generated classは含めず、指定repositoryのmainへ初回pushした。
- AGENTS.md §80の46項目を実装・検証へ対応付けた。branch保護、Renovate App、認証provider、production secret、deploymentは案件側で接続する。Windowsと負荷中のgraceful drainは対象環境で検証する。

設計判断はD-640〜D-642。application versionは0.1.0-SNAPSHOTを維持し、架空の業務処理や独自release frameworkは追加していない。

## 各phase共通の検証と記録

formatter → relevant tests → static analysis → 必要なら `check` → failure修正 → changed files確認の順で進める（D-552）。新しい設計判断は実装より先にDecision Ledgerへ反映する。version依存APIはそのphaseで公式仕様を確認し、参照先と選定結果を残す（D-560）。

完了報告には変更内容、実行した検証、実行できなかった検証と理由、残る未確定事項を記載する。チェックボックスは実施後にのみ更新する。
