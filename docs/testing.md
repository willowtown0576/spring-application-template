# テストと品質検証

[ADR-014](decisions.md#adr-014)と[ADR-025](decisions.md#adr-025)を、Gradleのquality gateとreviewで確認する。検証範囲は実装された契約と検査環境とし、配置先の受入確認は運用手順で行う。

## taskと配置

すべてのtestはsrc/test/javaに置く。別source setは作らず、JUnit Tagで実行を分ける。

| task | Tag | 主な対象 |
|---|---|---|
| unitTest | 下記3つを除外 | domain、UseCase、Result、UUID v7、相関ID／ログ |
| integrationTest | integration | PostgreSQL、公開API、REST、認可、HTTP、Batch、UI |
| architectureTest | architecture | Spring ModulithとArchUnit |
| apiDocumentation | documentation | OpenAPI契約と保護、YAML出力 |
| test | 集約 | unitTest・integrationTest・architectureTestとcoverage |

命名はFooTest、FooIntegrationTest、ArchitectureTest。単体testは対象のJavaオブジェクトを直接実行する。SpringBootTestは実際の組み合わせを確認する統合testへ限定する。DBはPostgreSQL Testcontainersとする。Dockerが利用できなければ失敗として扱う。

```bash
./gradlew test
./gradlew unitTest --tests '*FeatureNameTest'
./gradlew integrationTest --tests '*DatabaseIntegrationTest'
./gradlew integrationTest --tests '*FeatureUiIntegrationTest'
./gradlew architectureTest
./gradlew spotlessApply build
```

unitTestでもcompileに必要なjOOQ型がなければcodegenを実行するため、初回やclean後にはDockerが必要。

`test`は専用taskを束ねる集約taskとする。依存する3種類のtestはそれぞれ実行され、testReportで全体のHTML reportを生成する。個別選択の`--tests`はunitTest／integrationTest／architectureTestへ指定する。coverageはunitとintegrationの実行情報を集計し。生成資料の検証はapiDocumentationで実行する。

## quality gate

| 検査 | 対象・確認内容 |
|---|---|
| Java compiler | main／test／codegenの非推奨・削除予定API参照を警告からcompileエラーにする |
| Spotless | 手書きJava、Markdown、設定。Eclipse JDT format、import整理 |
| Checkstyle | 手書きmain／test／codegen。unused import、JavaDoc、final、非推奨警告の抑制・Deprecated宣言の禁止等 |
| SpotBugs | 手書きmain／test／codegen。generated型は解析classpathに保持 |
| unit／integration | 契約、境界値、実DB、認可、HTTP、UI |
| architecture | Modulith公開境界・cycle、domain依存、transaction位置、jOOQ参照範囲 |
| JaCoCo | unit＋integrationの集約coverage。generated型は除外 |
| verifyProductionJar | 実際のtest／codegen class出力とbootJarを照合し非収録を検査。実際の全main classの収録も確認 |

`check` はcompiler・Spotless・Checkstyle・SpotBugs・verifyProductionJarを担当する。解析やJAR照合に必要なcompile・jOOQ生成・frontend buildは実行する。`test` はunit・integration・architectureとJaCoCo reportを実行し、`build` はcheck・test・JAR生成を集約する。codegen subprojectも同じ役割でrootへ接続する。資料生成は別task。SpotBugsの抑制は[exclude.xml](../config/spotbugs/exclude.xml)の根拠を確認し、抑制対象は根拠に対応する最小範囲とする。coverageは検証の不足を探すために使い、testは業務契約や技術境界の保証に基づいて追加する。

## 契約ごとの検証箇所

| 契約 | 主なtest・確認方法 |
|---|---|
| module公開範囲、依存方向 | ArchitectureTest |
| 名前のUnicode境界・不変条件 | FeatureNameTest、CreateFeatureUseCaseTest |
| Clock、UUID v7 | UuidV7GeneratorTest |
| 空DB migration、制約、COMMENT、jOOQ | DatabaseIntegrationTest |
| commit／rollback／read-only | DatabaseIntegrationTest。書き込み後に例外を発生させ、行が残らないことも確認 |
| Swagger実操作、context path、CSRF token更新と拒否 | SwaggerUiIntegrationTest |
| REST成功、入力、認可、CSRF、500 | DatabaseIntegrationTest。直接Command／Query呼び出しの認可も確認 |
| 設定利用者・session認証・logout、Actuator保護、pool metrics | DatabaseIntegrationTest |
| BatchのCommand呼び出し、SchedulerのQuery／更新拒否、実行IDごとのmetadata | DatabaseIntegrationTest |
| system contextの復元・nested実行、利用者設定検証 | SystemExecutionTest、LocalAuthenticationConfigurationTest |
| HTTP timeout設定、相関ID、非retry | HttpTimeoutConfigurationIntegrationTest、HttpClientLoggingIntegrationTest |
| payload・例外messageのログ秘匿 | CorrelationFilterTest、HTTP／DB integration tests |
| 公開welcome／gallery、Aura／Lumo分離と操作、保護UI、DB操作、明暗・mobile | FeatureUiIntegrationTest |
| OpenAPI生成とops:readによる保護 | ApiDocumentationTest |
| Feature sampleのOpenAPI契約 | FeatureApiDocumentationTest |
| 増分codegen | 変更なしでjooqCodegenを再実行しUP-TO-DATEを確認 |
| DB・ER図 | documentation生成後、内容と描画結果を確認 |
| 配布JAR・OCI | verifyProductionJar、bootBuildImage、案件DB接続での実行 |

新しいJOIN、pagination、業務Failure、外部API、file I/O等には、その契約に合う意味のあるtestを追加する。検証範囲は実装済みの契約と実際の実行条件で明示する。

## report

| 種類 | 出力 |
|---|---|
| 全テスト | build/reports/tests/all/index.html |
| unit | build/reports/tests/unitTest/index.html |
| integration | build/reports/tests/integrationTest/index.html |
| architecture | build/reports/tests/architectureTest/index.html |
| coverage | build/reports/jacoco/test/html/index.html |
| XML test結果 | build/test-results配下 |
| UI capture | build/reports/ui配下 |
| codegen静的検査 | codegen/build/reports配下 |

reportとcaptureはGit管理外とする。UI captureはintegrationTestの出力として登録し、build cacheからも復元する。CIはreportをartifactへ保存する。

## 自動検査とreviewの分担

意味上の冗長importはCheckstyle／Spotlessに加えてreviewで確認する。業務規則の正しさは契約testとreviewで確認する。schema所有権、適用済migrationの変更禁止、必要以上の依存、ログ出力内容、ADRとの一致はdiffと実装も読む。

リリース時は[運用ガイド](operations.md)に従い、実際の認証provider、secret、DB接続、backup／restore、registry／deployment、GitHub権限を案件環境で検証する。実行した検査と未確認事項を分けて報告する。

## 案件の機能追加とsampleの変更

共通検査の対象はmodule境界・認可・transaction・配布境界とし、名称・件数・登録内容は各機能の契約testで検証する。Flyway履歴は解決済みmigrationと照合し、Batchの検証はそのtestの実行IDへ絞る。設定を無効にした場合の動作を検証するtestは、その条件をtest自身に指定する。

sampleの名前・入力制約・権限・表示文言はsampleの契約であり、転用・削除時に対応testも更新する。Modulithの依存宣言、Method Security、transaction、DB制約、非推奨API禁止等の品質規約は保持する。案件の設計を変更する場合はADRと検査を整合させる。
