# Architecture Decision Records

本書は、このstarterの設計判断の正本である。README、各ガイド、AGENTS.md、実装が矛盾する場合は本書を優先する。操作手順は[ドキュメント一覧](index.md)から参照する。

## ADRの運用

- ADRは背景、決定、影響を記載する。状態は `Accepted`、`Proposed`、`Superseded` を用いる。
- 設計を変更する場合は、実装前に新しいADRを追加する。既存判断を置き換える場合は旧ADRを `Superseded` とし、後継ADRへリンクする。
- 誤記や説明の明確化は既存ADRを修正できる。ADRの記載対象は設計課題・判断・理由・影響とする。
- 「必要時」「案件で決定」とする機能は採用条件を表す。採用時に具体的な契約と検証方法を決める。
- patch versionの正本はVersion Catalog、BOM、Wrapper、Docker設定である。ADRではその管理元を参照する。frameworkの変更時は採用versionの公式仕様を確認する。

## 一覧

| ADR | 判断 |
|---|---|
| [ADR-001](#adr-001) | 完成した技術基盤と依存方針 |
| [ADR-002](#adr-002) | 技術スタックとversion管理 |
| [ADR-003](#adr-003) | Gradle・開発環境・配布境界 |
| [ADR-004](#adr-004) | Feature単位のModular Monolith |
| [ADR-005](#adr-005) | Command／Queryとトランザクション |
| [ADR-006](#adr-006) | 公開契約・Result・最小sample |
| [ADR-007](#adr-007) | ID・時刻・データ整合性 |
| [ADR-008](#adr-008) | Flyway・DB環境・jOOQ生成 |
| [ADR-009](#adr-009) | RESTとAPI versioning |
| [ADR-010](#adr-010) | 認証・型付き認可 |
| [ADR-011](#adr-011) | Vaadin標準UIと公開sample |
| [ADR-012](#adr-012) | HTTP・設定・ログ・運用 |
| [ADR-013](#adr-013) | Batch・Scheduling・任意機能 |
| [ADR-014](#adr-014) | テストとコード品質 |
| [ADR-015](#adr-015) | 再現可能な資料生成 |
| [ADR-016](#adr-016) | CI・リリース・依存更新 |
| [ADR-017](#adr-017) | 完成品の資料とAIの作業規則 |
| [ADR-018](#adr-018) | デフォルト認証とシステム実行主体 |
| [ADR-019](#adr-019) | 非推奨APIの禁止 |
| [ADR-020](#adr-020) | AuraとLumoの比較 |
| [ADR-021](#adr-021) | 標準認証の差し替えと境界の検証 |
| [ADR-022](#adr-022) | 案件拡張を妨げない生成と検証 |
| [ADR-023](#adr-023) | 資料の正本統合と関係に集中したER図 |
| [ADR-024](#adr-024) | PDF資料生成を標準機能から除外する |
| [ADR-025](#adr-025) | Gradleコマンドの責務を分離する |

<a id="adr-001"></a>
## ADR-001: 完成した技術基盤と依存方針

**状態:** Accepted

**背景:** 一人または少人数で小規模〜中規模の業務システムを開発する。案件ごとに設計、build、DB、品質管理を組み直す負担を減らす。

**決定:** 業務固有機能を追加して利用する、技術基盤として完成したapplicationを提供する。品質の優先順位は、正しさ、型安全性、データ整合性、セキュリティ、責務、依存方向、保守性、単純さ、実装速度とする。公開可能なコード品質を基準にする。

依存を選ぶ順序はJDK標準、Spring標準、導入済みlibrary、成熟した外部library、独自実装。Javaの静的型、不変性、小さい公開API、明示的SQLを使う。abstraction、framework、extension point、wrapper、generic utilityは具体的な要件に基づいて採用する。transaction、security、整合性、境界、意味のあるvalidation・test・error handling、再現可能なbuild・資料生成は必須の品質条件とする。

**影響:** 業務sampleはliteral名 `feature` の最小限にする。業務domainとdirectoryは実装する機能に合わせて定義する。案件固有の選択は、その必要性が明確になった時点で行う。

<a id="adr-002"></a>
## ADR-002: 技術スタックとversion管理

**状態:** Accepted

**背景:** 少人数で保守できる成熟した統合基盤と、再現可能な依存解決が必要である。

**決定:** Java 25、Gradle Kotlin DSL／Wrapper／Toolchain／Version Catalog、Spring Boot 4.1系、Vaadin 25／Flow、PostgreSQL、Flyway、jOOQを採用する。Spring MVC、Security／Method Security、Jakarta Validation、Batch JDBC、Scheduling、HTTP Service Client／RestClient、Actuator／Micrometer、Modulithを標準とする。品質・資料ツールは[技術スタック](technology-stack.md)に示す。

JavaはToolchain、GradleはWrapperと配布SHA、明示dependency／pluginはCatalog、Spring系はBoot BOM、Vaadin系はVaadin BOM、Modulithは専用BOM、application versionはGradle project versionで管理する。codegenもBoot BOMを使う。Docker imageは固定tagを使い `latest` を禁止する。

案件要件に応じて採用するもの: JPA／Hibernate ORM、Redis、Kafka、RabbitMQ、JMS、Quartz、Spring Integration、Mail、OAuth2／OIDC／SAML／LDAPの認証方式、React／Angular／Vue、TypeScript中心のfrontend、汎用master import・cache・retry・workflow基盤、巨大なcustom exception階層。Jakarta Validationの実装であるHibernate ValidatorはORMとは別である。

**影響:** GradleはWrapper、frontendの実行環境はVaadinのbuild toolingで管理する。dependency追加やmajor upgradeは必要性・互換性を確認する。Renovateは更新提案を行い、mergeは互換性の確認後に行う。

<a id="adr-003"></a>
## ADR-003: Gradle・開発環境・配布境界

**状態:** Accepted

**背景:** 実行手順と環境差を減らし、配布物をapplicationの実行に必要な構成へ限定する。

**決定:** 開発者向け操作をGradleへ集約する。hostにはJDK 25、Git、Docker、IDEを用意し、applicationはhost JVMで起動する。開発環境はhost JVMを標準とする。VaadinのNode等はbuild toolingが管理する。

Docker設定は `docker/`、Composeは `docker/compose.yaml` 一つとし、`dev`／`docs` profileで用途を分ける。Composeの用途は一つのfile内のprofileで切り替える。開発PostgreSQLはloopbackの動的port、persistent named volumeを使用する。BootのCompose連携で起動・停止し、dataは保持する。passwordは必須設定とする。

`bootRun` は任意のGit管理外 `config/local-env.properties` をUTF-8 Java Propertiesとして読み、環境変数へ注入する。既存環境変数が優先される。読み取りにはJDKのPropertiesを使い、fileはbootRunのローカル設定として扱う。

applicationは `src/main/java`、全testは `src/test/java`、生成ツールは独立 `codegen` subprojectに置く。生成jOOQはmainでcompileし配布する。test、codegen実行class／専用依存、開発用Compose／Vaadin devは配布JARの収録対象外とする。`verifyProductionJar` をquality gateにする。

生成source／資料／reportは `build/`、subproject出力は `codegen/build/` へ置き、Git管理外とする。Vaadinが生成するfrontend資産と `target/vaadin-dev-server-settings.json` も管理対象外とする。

**影響:** 初回buildにはnetworkとDockerが必要である。案件開始時はproject name、group、base package `dev.template.application`、application nameを一括置換する。専用initializerは具体的な必要性に基づいて採用する。

<a id="adr-004"></a>
## ADR-004: Feature単位のModular Monolith

**状態:** Accepted

**背景:** layerごとに全機能を分割すると、変更範囲と業務上の所有権が見えにくくなる。

**決定:** Feature-oriented Modular Monolith、Command／Query分離、意味のある箇所でPorts and Adaptersを採用する。公開packageは `feature.api.command`／`query`、権限契約は `api.authorization` とし、`package-info.java` のNamed Interfaceで公開する。`api.event` は必要なfeatureだけに追加する。

module間の参照は公開APIに限定し、依存関係は非循環とする。Spring Modulith `verify()` をmodule境界の正本とし、module境界の検証をModulithへ集約する。ArchUnitはdomainのframework非依存、transaction配置、generated SQL型の参照範囲等を補う。

`public` は外部公開またはJavaのpackage間連携が必要な型に限定する。Beanの可視性は利用範囲に応じて決める。internal実装はpackage-privateを優先し、`protected` は意図した継承に限定する。Javaのpublicなinternal型も、Modulith上は内部実装として扱う。

**影響:** REST／Vaadin／Batch／Schedulingはadapterとして公開APIを使う。`common` はResult・Clock・ID生成を持ち、他application moduleから独立した共通契約を定義する。生成SQL型の親 `jooq` はopen technical moduleとし、参照をinfrastructureに制限する。[依存図](architecture.md)を参照する。

<a id="adr-005"></a>
## ADR-005: Command／Queryとトランザクション

**状態:** Accepted

**背景:** 更新の不変条件と参照SQLの最適化は異なる責務である。

**決定:** Commandは公開APIの実装からUseCaseを呼び、domainの業務規則を適用する。UseCaseが所有するRepository Portをinfrastructureが実装する。Command実装は入力変換・認可・transaction・UseCase呼び出し・Result返却に限定する。

Queryは公開APIの実装からDataSource Portを呼び、そのinfrastructure実装で参照処理を行う。DataSourceは `internal.query` が所有し、QueryはDataSource経由でread modelを取得する。内部取得recordから公開read modelへの変換はQuery実装が担う。Repositoryは更新、DataSourceは参照のPortとする。関係は[アーキテクチャの依存図](architecture.md#依存方向)を参照する。

Command実装methodは `@Transactional`、Query実装methodは `@Transactional(readOnly = true)` を持つ。transactionとSecurityの制御はCommand／Queryの境界に配置する。default propagationは `REQUIRED`、`REQUIRES_NEW` は理由がある場合だけ。transaction境界はSpring proxyを通る呼び出しとする。他featureへの更新は親Commandから対象の公開Command APIを同期呼び出しする。

application Beanはcomponent scanとconstructor injectionで登録する。UseCaseには登録用 `@Service` のみ許可する。domainはSpring非依存。JDKのClockやframework customizerは `@Bean` で構成する。

**影響:** CommandとQueryの構造は意図的に非対称となる。SQL／framework型は境界を越えない。DB integration testで実commit、insert後の例外によるrollback、read-onlyを検証する。

<a id="adr-006"></a>
## ADR-006: 公開契約・Result・最小sample

**状態:** Accepted

**背景:** 業務上の拒否と技術障害を区別し、adapterによらず同じ契約を使用する。

**決定:** 公開API入力は用途別Param record、参照出力は用途別read modelとする。共通sealed `Result<S,F>` のvariantは `Success`／`Failure`、成功値と型付き失敗理由は非null。Commandは原則Resultを返す。単純Queryは値やOptionalを直接返し、意味のある業務失敗がある場合のみResultを使う。

期待される業務結果はFailure、予期しない技術障害・到達不能な内部状態はException。業務失敗は型付き理由、技術障害は意味を持つ既存Exceptionで表現する。transaction内の技術例外は境界へ伝播し、rollbackを成立させる。例外変換が必要ならrollback semanticsを保つ境界で行う。

最小sample契約:

- `FeatureCommands.create(CreateFeatureParam)` の戻り値は `Result<UUID, CreateFailure>`。
- `FeatureQueries.find(FindFeatureParam)` の戻り値は `Optional<FeatureResult>`。未検出はempty。
- nameは1〜100 Unicode code point。null、空文字、ASCII spaceのみ、NUL、不正surrogateを拒否し `INVALID_NAME` を返す。入力文字列をそのまま保存する。同名作成を許可する。
- null Param自体とnull検索IDはprogramming errorとして拒否する。
- `FeatureCommandsImpl`／`FeatureQueriesImpl`、`JooqFeatureRepository`／`JooqFeatureDataSource` を用いる。DataSourceは内部 `FeatureData` を返す。

**影響:** RESTはFailureをHTTP status／ProblemDetailへ、Vaadinは利用者向け表示へ、BatchはJobの意味に応じた終了状態へ変換する。技術例外はrollback／Job failureと共通error handlingの対象となる。

<a id="adr-007"></a>
## ADR-007: ID・時刻・データ整合性

**状態:** Accepted

**背景:** applicationとPostgreSQLの型・制約を一致させる。

**決定:** IDはapplication側生成のUUID v7、JavaはUUID、DBはuuid。Java標準APIで不足するv7生成にはuuid-creatorを使う。`UuidV7Generator` はinjectable Clockを受け `Supplier<UUID>` を提供する。domainのfeature固有ID Value Objectは必要時に使い、Queryではraw UUIDを許可する。業務IDの生成責任はapplicationに置く。

実時刻はInstant／timestamptz、業務日付はLocalDate／date、時刻のみはLocalTime／time、地域計算はZonedDateTime＋ZoneId、外部offset付き時刻はOffsetDateTimeから必要に応じInstantへ変換する。内部UTC、productionはClock.systemUTC、testはClock.fixed。timezoneを明示して処理する。

schemaは原則featureが所有する。他featureへの直接INSERT／UPDATE／DELETEは禁止、参照JOINは許可する。cross-feature queryのView／projection等の構造とcross-feature FKは必要時に整合性・自律性を比較して決める。同feature内のFKは積極的に使う。技術schemaは必要時に作る。

命名・型・nullabilityは[DB規約](database.md)に従う。金額はBigDecimal／numeric、長さ制限はValidationとDBを一致させる。booleanは本当に二値の概念に限定する。audit列は必要なtableだけ、actor設定は案件で決める。削除はphysical delete、soft deleteの採用は案件要件で判断し、履歴要件は履歴modelで表す。

**影響:** 最小tableは `feature.feature(id, name)` とし、NOT NULL、PK、100文字制約、ASCII spaceだけの値を拒否するCHECKを持つ。seed・audit・名前のUNIQUEは不要。Spring Batch所有metadataはframework互換性のため、その型・数値ID・nullable規則を維持する。

<a id="adr-008"></a>
## ADR-008: Flyway・DB環境・jOOQ生成

**状態:** Accepted

**背景:** migrationからschemaと生成型を再現する必要がある。

**決定:** Flyway migrationをschema変更履歴の正本とする。適用済みmigrationは履歴として保持し、変更は新migrationで行う。`db/migration/<feature>/` を標準再帰scanし、versionはapplication全体で一意、履歴は `public.flyway_schema_history` とする。schema作成とCOMMENTはSQLに含める。migrationの読み込みはFlyway標準のscanを使用する。productionも原則startup時に適用し、運用要件により事前適用を許可する。

DBは4用途に分離する: persistentな開発Compose、Testcontainersのtest、一時codegen DB、一時資料DB。codegenと資料には各用途の一時DBを使用する。

jOOQは一時PostgreSQLへFlywayを適用し、標準GenerationToolでSQL型を生成してDBを破棄する。[生成フロー](database.md#4用途のdb)を参照する。独立codegen subprojectのJavaExecを使い、生成先は `build/generated-src/jooq/`、packageは `dev.template.application.jooq.<schema>`。現行対象はfeature schema。日時等の非決定的生成情報を抑え、inputs／outputsを宣言して不要な再生成を避ける。失敗時も一時DBを後始末する。

generated型は原則 `internal.infrastructure` からだけ参照し、変更は再生成で行いGit管理外とする。生成sourceをCheckstyle／SpotBugs／coverage対象から除外するが、解析用classpathには保持する。手書きcodegenは品質検査する。

**影響:** DockerをDB testの必須実行条件とする。starter動作に必要な最小system／sample dataはFlywayで登録可能だが、業務masterの大量・高頻度更新や汎用import基盤には用いない。

<a id="adr-009"></a>
## ADR-009: RESTとAPI versioning

**状態:** Accepted

**背景:** HTTPの都合を業務契約へ持ち込まず、標準の応答を使う。

**決定:** Controllerは `web.rest` に置き、feature公開APIだけを呼ぶ。HTTP固有DTOはadapterに閉じる。URLは複数形の名詞、非CRUD操作は必要に応じaction subresourceを使う。JSONはJackson標準のcamelCase／ISO-8601、JSON表現はJackson標準を基本とする。

major versionは `/api/v1`。Spring MVC標準path segment解決を使い、breaking changeのみmajorを増やす。version追加は非互換変更の導入時に行い、解決はSpring MVC標準に委ねる。deprecation／sunsetは必要時に標準mechanismを使う。

成功は200／201／204、入力形式は400、未認証401、権限不足403、未検出404、競合409、意味上の業務拒否422、予期しない障害500。errorはSpring ProblemDetailを使い、必要なときだけstable machine codeを付ける。Security例外は認証・認可のHTTP statusで扱う。

sampleはPOST `/api/v1/features` で201＋Location＋ID、GET `/api/v1/features/{id}` でFeatureResultを返す。nameの欠落／nullと不正JSON／UUIDは400、名前の業務制約は422、未登録404、未対応version400。文字数の検証単位はUnicode code pointとする。

pagination／sortはQueryのtyped conceptとする。offset、Slice、Page、keysetは用途で選び、COUNTは総件数を必要とするQueryに限定する。大量・深いpageはseekを検討する。sortはenum等へ変換し、外部のsort指定は許可したSQL columnへ対応付ける。paginationの型はQueryの契約で定義する。

**影響:** MockMvcで正常系、validation、認可、500の情報秘匿を検証する。API資料は実Controller metadataから生成する。

<a id="adr-010"></a>
## ADR-010: 認証・型付き認可

**状態:** Superseded — [ADR-018](#adr-018)。以下は従来の判断。

**背景:** 認証方式は案件で選択し、認証構成の有無にかかわらず業務操作に認可を適用する。

**決定:** Spring Securityと `@EnableMethodSecurity` を有効にする。認証ユーザーとlogin方式はlocal認証・OIDC等の案件側の構成で定義する。RESTとDB連携UIは認証必須、公開welcome／部品集は認証不要とする。

本認可は `internal.command`／`internal.query` のMethod Security、URL／routeは粗い入口制限。業務操作の権限は公開APIの境界で検証する。RoleはAuthority集合として扱う。業務Authorityはfeatureが所有し、Authority定義をfeature内へ集約する。

`FeatureAuthority` enumはGrantedAuthorityを実装し `feature:read`／`feature:write` を所有する。Spring標準meta-annotation templateにより `@RequiresFeatureAuthority(...)` をtransaction境界へ指定する。運用権限 `ops:read` はOperationsAuthorityが所有する。認可判定にはSpring標準機構を使う。

Spring Security型の利用範囲はsecurityと認可adapterとする。操作者が必要ならsecurity adapterでActor／UserId等へ変換する。Batch／Schedulingはシステム実行主体として認証・認可する。

CSRF保護を保持し、Vaadin内部通信は標準VaadinSecurityConfigurerに委ねる。未認証401、認証済み権限不足／CSRF拒否403。Actuatorはhealthを詳細なし匿名公開、info／metricsをops:readで保護する。その他endpointの公開は運用要件で選択する。

**影響:** 認証接続前の `/features` とRESTの401は正しい動作となる。正常系は配布されないtest専用認証で検証する。認証追加時にも直接Command／Query呼び出しの認可を維持する。

<a id="adr-011"></a>
## ADR-011: Vaadin標準UIと公開sample

**状態:** Accepted

**背景:** 初回起動時から完成した画面と部品の使い方を確認できる必要がある。

**決定:** Vaadin core／FlowとSpring連携を使用する。Viewは `web.ui` に置き、業務操作は公開APIへ委譲する。Vaadin型の利用範囲はweb.uiとし、標準依存はVaadinの無償core componentに限定する。

`/` はwelcome、`/components` は部品集として匿名公開し、`/features` は認証必須のDB作成・ID検索例とする。部品集のサンプルはView内Listに保持する。画面内連番の行recordと入力recordを分け、編集時のIDを維持する。パスワード入力例は架空値用とし、入力値の利用範囲は当該入力欄に限定する。

Aura既定theme、AppLayout／SideNav、Card、標準Button／Grid variantを使う。装飾と配色はVaadin提供のstyleに統一する。FormLayout／AppLayout／MasterDetailLayoutの標準responsive機能を使い、Page.setColorSchemeで現在のブラウザーUIの明暗表示を切り替える。

部品集は一覧検索・sort・filter、Binder／入力validation、Dialog／ConfirmDialog、Tabs、通知、ProgressBar、Details、日付・数値・時刻・メール・パスワード、単一／複数選択、Checkbox、Accordion、MenuBar、Avatar、Tooltip、Icon、一覧詳細、カードとボタンのvariantを操作できる例とする。

**影響:** 標準precompiled frontendを優先し、production frontendをJARへ含める。Vaadin devはdevelopmentOnly。Playwrightをtest専用とし、Chromiumで公開導線、DB画面、拒否、明暗・mobile表示を検証する。

<a id="adr-012"></a>
## ADR-012: HTTP・設定・ログ・運用

**状態:** Accepted

**背景:** 通信障害や運用情報を業務コードから分離し、機密を出さずに観測可能にする。

**決定:** 外部HTTPはHTTP Service Client、RestClient、必要なreactive／streamingに限りWebClientの順で選ぶ。新規の同期HTTP実装はHTTP Service ClientまたはRestClientとする。Boot標準HTTP Service Groupと注入されたBuilderを使い、外部DTOとcredentialはinfrastructureに閉じる。業務側は自身が必要なPortを定義する。

connect／read timeoutは明示し、環境変数からDurationとして設定する。標準値は2秒／10秒、正値を起動時検証する。default retryは行わず、特にPOST／PUT／PATCHは冪等性と失敗時の意味を確認した場合だけ個別採用する。意味のある外部応答はFailureに変換可能、network／timeout／5xx／protocol異常はExceptionとする。

環境依存値は環境変数、application.ymlは安定設定・mapping・安全なdefaultに限定する。secretはGit管理外とする。custom設定は `@ConfigurationProperties @Validated record` と型付き値を使い、必須不足・不正値はstartup failureとしてtestする。散在する@Valueや環境別YAMLの大量複製を避ける。JAR／OCIも同じ設定modelを使う。

loggingはtop-level `logging` のAOP／filter／interceptorで行う。Command開始・終了INFO、Query DEBUG、期待Failure INFO、予期しない障害ERROR。WARNはwarningの場合だけ。business codeの通常logger callとmarker annotation基盤を避け、AOPで表現できないtechnical eventのみ直接記録を許可する。

共通ログの記録項目は処理の識別と診断に必要な非機密情報に限定する。applicationの共通loggerでは技術例外のmessageを伏せて型・stack frameを記録する。Spring Batch自身のログとmetadataへの例外記録はこの秘匿処理の対象外であり、詳細は運用ガイドを参照する。受信HTTPにはserver生成の相関IDを付与し、MDCを終了時に復元する。X-Correlation-IDをresponseとoutgoingへ伝播する。標準のHTTPログはmethod、host、status、時間等のmetadataに限定する。

HikariCPはBoot標準を使い、poolのtuningは計測結果に基づいて行う。Actuator／Micrometerでpoolを観測可能にする。Prometheus、OpenTelemetry、JSON loggingは必要時に追加する。終了はBoot標準graceful shutdown、phase timeoutは30秒。

**影響:** timeout・相関ID・非retryを実HTTPで、設定検証とログ秘匿をtestする。HTTPとshutdownの制御はSpring標準機構に委ねる。

<a id="adr-013"></a>
## ADR-013: Batch・Scheduling・任意機能

**状態:** Accepted

**背景:** 技術基盤を用意しつつ、常駐処理の追加を業務要件に基づいて判断する。

**決定:** Batchはrestart／history／chunkが必要な処理に使う。top-level `batch` adapterから公開APIを呼ぶ。Boot Batch JDBCと公式PostgreSQL metadata DDLを使用し、Flywayの `system` schemaで管理する。table prefixは `system.BATCH_`、Bootのschema初期化はnever。framework metadataの型・ID生成は公式仕様を維持し、constraint名はrepository規約に合わせる。

Job自動起動は無効。稼働Jobは案件の要件に基づいて実装し、基盤のmetadataと終了状態はtest用Jobで検証する。共通JobExecutionListenerを案件のJob builderへ明示登録する。JobParameters／ExecutionContextは永続化されるため保存内容は処理に必要な非機密情報に限定する。launcherのparameter全文INFO logはWARN thresholdで抑える。FailureはJobの意味で処理し、予期しないExceptionはJob failureとする。

Schedulingは@EnableSchedulingと標準scheduler、単純定期処理は@Scheduledを使う。業務処理は公開APIに委譲する。定期処理の実装とschedulerの選択は業務要件に基づいて行う。

以下は必要性が明確な案件だけに導入する。

| 対象 | 採用条件・守る境界 |
|---|---|
| Event | 通常は同期公開API。即時結果が不要な副作用にModulith Event。api.eventとpublication metadataは必要時に設計 |
| Async | 同期がdefault。transaction、順序、重複、失敗復旧、retryを明確にする |
| Cache | 計測で必要箇所を特定し、無効化・整合性を設計。Spring Cache／Caffeine／Redisの採用は計測結果で判断する |
| Mail／通知 | 標準にMailを含めず、業務が必要とするPortとadapterで接続 |
| File I/O | upload／downloadはweb、永続storageはPort。MultipartFile／response／StreamResource／Path／raw streamを業務APIへ漏らさない |
| File安全性 | 保存pathはUUID等の内部名から構築する。size／MIME／拡張子／内容を必要に応じ検証し、path traversalを防ぐ。適切な一時領域を使い後始末し、大容量はstream処理とする |
| CSV | UTF-8、comma、適切なquoting、LF／CRLF。独自parserを作らず成熟libraryを使う |
| Excel／業務PDF | 案件の出力要件がある場合だけ追加 |

**影響:** Port／event／storage／workflow等は実要件に合わせて追加する。任意機能を追加する際も認可・transaction・失敗時の安全性を維持する。

<a id="adr-014"></a>
## ADR-014: テストとコード品質

**状態:** Accepted。taskの責務は[ADR-025](#adr-025)で更新。

**背景:** 少人数で継続的に品質を保つには、意味のある自動検査と読みやすいコードが必要である。

**決定:** JUnit／AssertJでdomain・UseCaseの単体testをSpringなしで行う。DB統合は実PostgreSQL／Testcontainersを使う。適用migration、制約、公開API、rollback、認可、必要なJOIN／paginationを検証する。RESTはMockMvc、主要UI flowはPlaywright、module境界はModulithと追加ArchUnit規則で検証する。SpringBootTestは統合が必要な範囲に限定する。

testは全てsrc/test/javaに置き、JUnit Tag `integration`／`architecture`／`documentation` でtaskを分ける。命名はFooTest／FooIntegrationTest／ArchitectureTest。test専用認証はtest sourceに配置する。

SpotlessはEclipse JDTを使用し、通常indent4スペース、継続行は追加4スペース、行幅120とする。arrowと本体は収まれば同じ行にし、過剰な既存折り返しを再結合する。設定は `config/formatter/eclipse-java.xml`。importOrder／removeUnusedImportsを併用し、member順はsourceの定義順を保持する。

再代入しないlocal variable・parameter・fieldはfinal。record component等の暗黙finalと再代入が必要な変数は適切に扱う。testもconstructor injection、MockitoSpyBeanは型宣言＋constructor injection。変更可能fieldは理由を示した狭い例外に限定する。全ての手書き型・methodに責務と契約を説明するJavaDocを付け、Overrideは{@inheritDoc}、test／lifecycleは条件と期待結果を書く。

Checkstyleはunused／redundant import、JavaDoc、final等を検査する。SpotBugsは欠陥候補を検出し、抑制は根拠のある狭い対象だけ。JaCoCoでunit＋integration coverageを出力し、数値thresholdは案件の品質目標に応じて設定する。quality gateはformat、静的解析、unit／integration／architecture、coverage、codegen検査、配布JAR境界を含む。

**影響:** 品質検査のために意味のないtestを増やさない。自動import整理で検出できない意味上冗長なimport等はreviewでも確認する。詳細な実行方法と検査範囲は[検証ガイド](testing.md)に置く。

<a id="adr-015"></a>
## ADR-015: 再現可能な資料生成

**状態:** Accepted。資料配置・ER表示は[ADR-023](#adr-023)、PDF生成・配布は[ADR-024](#adr-024)で更新。

**背景:** 実装から離れた資料やhost依存の日本語PDF生成を避ける。

**決定:** 手書き資料の正本はMarkdown、図はMermaid。DB資料は一時PostgreSQLへ実migrationを適用してtblsで生成する。説明はPostgreSQL COMMENT、設定は.tbls.yml、手書き補足はdocs/database/notes。ERはSVGを優先し、tblsのschema出力からDOTを生成してGraphvizで描画する。

OpenAPIはspringdocとController metadataから生成し、annotationを過剰に付けない。出力はversion別 `build/documentation/api/v1/openapi.yaml`。OpenAPI／Swagger UIはdefault無効、有効化時もops:readで保護する。API資料の生成元はspringdocへ統一する。

project PDFは `docs/project/` のMarkdownとMermaidから生成する。MermaidはSVGへ事前renderし、Pandoc＋LuaLaTeXと日本語fontを使用する。font fileはcontainerへのinstallで供給する。tbls、Mermaid CLI、Pandoc、LuaLaTeX、font、Graphvizを `docker/docs/Dockerfile` の固定toolchainへ集約する。Node／Mermaidは固定versionとpackage-lock／npm ci、OS packageは固定Debian snapshotを使用する。

`documentation` はdatabaseDocumentation／apiDocumentation／projectDocumentationを集約し、実行入口を資料生成commandに分ける。出力は `build/documentation/{database,api,project}`。一時DBは一意のCompose projectで分離し、成功・失敗時とも破棄する。生成資料はGit管理外とし、修正はsourceの更新と再生成によって行う。

**影響:** 初回資料生成はimage取得・buildに時間を要する。PDFは文字欠け・図の可読性・改頁を描画結果で確認する。操作方法は[資料生成](documentation.md)に置く。

<a id="adr-016"></a>
## ADR-016: CI・リリース・依存更新

**状態:** Accepted。PDF配布は[ADR-024](#adr-024)で廃止。

**背景:** ローカルとCIで同じ検証を行い、配布物とversionを対応させる。

**決定:** GitHub ActionsはGradleを呼び、build logicの正本をGradleへ集約する。CIはbranch push／PRでcheck／buildを行う。Ubuntu 24.04、JDK 25、SHA固定Actions、最小権限contents:read、checkout credential非保持を使う。

正式配布物はexecutable JAR。OCIはSpring Boot Buildpacksを使い、image構築をBuildpacksへ委ねる。Paketo builder／run imageはCatalogで固定する。配置先、registry push、production secretは案件で決める。

application versionはGradleのSemVer、API versionとは独立する。releaseはsuffixなしの `major.minor.patch` と一致する `v<version>` tagを要求する。tag releaseではcheck、build、資料、OCI生成を検証し、JAR／OpenAPI YAML／DB資料ZIP／project PDFを添付する。公開jobだけcontents:writeを持ち、workflow_dispatchはbuildとartifact保存を担当する。

RenovateはGradle dependency／plugin／Wrapper、Docker、Actions、資料用npmを追跡する。PostgreSQLのCompose／Catalog、Paketo imageはgroup化する。auto-mergeは無効、Debian snapshot日付は手動検証で更新する。

**影響:** repositoryのActions、Renovate App、branch protectionとdeployment環境は案件側で接続する。外部サービスの設定と公開は配置先で受入確認する。[運用・リリース](operations.md)に手順を置く。

<a id="adr-017"></a>
## ADR-017: 完成品の資料とAIの作業規則

**状態:** Accepted

**背景:** 開発者とAIがrepository内の資料から保守に必要な仕様と手順を理解できる必要がある。

**決定:** READMEを起動・開発・全体思想の入口、docs/index.mdを資料一覧とする。docsにはarchitecture、stack、setup、開発規約、DB、運用、品質検証、資料生成とADRを置く。作業経緯・進捗・実行結果の記録先はIssue・会話・CI reportとする。検証方法と保証範囲は記載し、実行結果はCI reportや変更の報告で示す。

rootのAGENTS.mdだけをAI向け指示書に使う。読むべきdocs、変更時の前提、品質確認を示す。AI向け指示の配置先はrootの一つに集約する。生成物は会話の成果物ではなく、最初から存在していたコードとして書く。ソースコメント・JavaDoc・資料は現在の契約、責務、理由、操作方法を説明する。

AIはREADME、AGENTS、ADRと対象領域の資料・実装を読み、既存変更を保持する。設計変更はADRが先、実装と手順の変更は同時に整合させる。依頼範囲の必要な作業を完了し、実装範囲は依頼の達成に必要な機能とする。完了前にbuildを実行し、資料変更時は該当生成も確認する。未実行・失敗・外部設定の未確認は、それぞれの状態を明記する。

**影響:** 作業計画と会話は開発の補助情報とし、製品仕様の正本はdocsに置く。補助skillの適用時も型安全性、整合性、security、意味のあるtestと再現性を品質条件とする。

<a id="adr-018"></a>
## ADR-018: デフォルト認証と利用者・システム実行主体

**状態:** Accepted

**背景:** Web、Batch、Schedulerから同じ公開Command／Queryを安全に呼ぶため、認証と実行主体を標準で用意する必要がある。

**決定:** ADR-010の認証方式未提供方針を置き換える。Spring Securityのフォーム認証とVaadin LoginForm、標準logoutを提供する。利用者は明示設定したAPP_USER_NAME／APP_USER_PASSWORDで作成し、passwordは実行環境から明示設定する。passwordは12文字以上、BCryptの上限72 UTF-8 bytes以下を要求し、起動時にencodeして標準InMemoryUserDetailsManagerへ登録する。利用者名のsystem: prefixはシステム主体用に予約する。設定のtoStringはsecretを伏せる。設定不足はstartup failureとする。標準利用者にはfeature:read／writeを付与し、運用権限はAPP_USER_OPERATIONS_READ=trueの場合だけ付与する。利用者管理・永続ユーザーDBは標準に含めず、複数ユーザーや外部認証が必要な案件ではUserDetailsService／認証providerを置き換える。

Webはsession認証、保護UIはloginへ誘導する。REST／Actuator／API資料は未認証401を維持する。CSRFとsession fixation対策を標準機構で維持し、logout後はsessionを無効化する。認可はCommand／Queryで引き続きAuthorityを要求する。feature所有の権限enum、標準meta-annotation template、運用権限の分離、最小限のActuator公開という認可規約を維持する。

システム主体はsecurityのSystemActorで定義する。BATCHはfeature:read／write、SCHEDULERはfeature:readのみとし、権限は列挙した業務Authorityに限定し、credentialは空とする。信頼されたprocess内のbatch／scheduling adapterだけがSystemExecutionから明示的に選択できる。主体と権限は信頼されたadapterのコードから選択する。SystemExecutionはSpring標準DelegatingSecurityContextCallableを使い、新しいcontextで呼び出し、正常・例外・nested呼び出し後に以前のcontextを復元する。非同期処理では実際に業務APIを呼ぶworker内で境界を開始する。業務操作には利用者と同じ認可規則を適用する。

システム実行には、利用者と区別された主体と明示的なAuthorityを使用する。domain／UseCaseへSecurityContextを持ち込まず、system境界の呼び出しをArchUnitでbatch／scheduling／securityに制限する。

**影響:** 実フォームlogin・失敗・logout、REST session認証とCSRF、BatchのCommand呼び出しとrollback、scheduler workerのQueryと書き込み拒否、context復元を検証する。認証設定は各環境で供給し、test credentialはtest用classpathに限定する。認証基盤を変更してもAuthorityとCommand／Query境界を維持する。

参照: [Vaadin Security Configurer](https://vaadin.com/docs/latest/flow/security/vaadin-security-configurer)、[Spring Security concurrency](https://docs.spring.io/spring-security/reference/features/integrations/concurrency.html)。

<a id="adr-019"></a>
## ADR-019: 非推奨APIの禁止

**状態:** Accepted

**背景:** library更新で非推奨になったAPIを警告だけで放置すると、削除時の移行負担が増える。

**決定:** rootとcodegenの全JavaCompileで `-Xlint:deprecation`、`-Xlint:removal`、`-Werror` を指定する。依存先の型・method・fieldを解決するJavaコンパイラーを検出の正本とし、main／test／codegenの非推奨API参照をcompile失敗にする。検出した参照は推奨APIへ置き換える。CheckstyleのSuppressWarnings検査でdeprecation／removal／allの抑制指定も拒否する。

**影響:** 通常のtest／build／CIで検出する。生成sourceも同じcompile設定を受けるため、生成器との互換性を維持する。検出対象は本projectのJava sourceからのAPI参照とする。

<a id="adr-020"></a>
## ADR-020: AuraとLumoの比較

**状態:** Accepted

**背景:** 同じ画面部品を操作しながら、標準テーマの外観を比較できる必要がある。

**決定:** 通常画面はAuraを維持し、匿名公開の `/theme-comparison` にAuraとLumoの比較を用意する。既存の部品集をlayoutなしの `/theme-preview` でも公開し、別documentの同一origin iframeへ表示する。AppShellConfiguratorはpreviewの固定query値に応じ、Vaadin提供のAuraまたはLumo stylesheetを一つだけ読み込む。画面の装飾はVaadin標準styleとする。

比較画面は明暗を共通操作で切り替え、狭い画面では縦に配置する。各previewの操作・sample dataは独立し、明暗切替による再読み込みで初期化する。UI filter chainのframe制限はSAMEORIGINとし、他originからの埋め込みを拒否する。REST／Actuator／API資料の制限は維持する。

**影響:** 共通の部品集を使い、全タブを両テーマで試せる。実browserでstylesheetの分離、操作、明暗、mobile表示を検証する。

<a id="adr-021"></a>
## ADR-021: 標準認証の差し替えと境界の検証

**状態:** Accepted

**背景:** 標準認証の置換条件はBootのauto-configurationで評価する必要がある。ブラウザーからのREST操作には実際のCSRF token受け渡しが必要であり、context path配下への配置も考慮する。認可・transactionの両方を書き忘れた実装や、非推奨宣言によるcompiler警告抑制も品質検査の対象とする。

**決定:** LocalAuthenticationConfigurationをBootのAutoConfiguration.importsで登録し、利用者のUserDetailsService定義後に条件評価する。登録順の制御はBootのauto-configuration機構に委ねる。

REST・API資料・UIのCSRF設定にはSpring Security標準のspa()を使用する。XSRF-TOKEN cookieとX-XSRF-TOKEN headerを使用し、springdocのCSRF連携を有効にする。login／logoutでtokenを更新し、Vaadin内部通信の扱いはVaadinSecurityConfigurerに委ねる。CSRF保護を有効に保つ。

RESTのLocationはServletUriComponentsBuilderで現在のrequestから生成する。OpenAPIのserver URLはspringdocの標準生成を使い、context pathを含む配置先を反映する。proxy配下のforwarded headerは信頼境界を確認して運用設定する。

公開Command／Queryの実装methodを起点に、transactionとMethod Securityの両方をArchUnitで検証する。検査対象は公開APIの実装method全体とする。QueryはreadOnlyを要求する。非推奨APIのcompile検査に加え、手書きsourceのDeprecated annotationをCheckstyleで禁止し、Javadocのみのdeprecated宣言も-Xlint:dep-annと-Werrorで拒否する。

**影響:** 独自filter／token endpoint／URL resolverを増やさず、認証設定の差し替え、実browserのSwagger操作、context path付きLocation、annotationの欠落を検証する。公開APIを段階的に廃止する案件ではDeprecated宣言の許可と検出方式を別途判断する。

参照: [Boot auto-configuration](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html)、[Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)、[springdoc properties](https://springdoc.org/properties)。

<a id="adr-022"></a>
## ADR-022: 案件拡張を妨げない生成と検証

**状態:** Accepted

**背景:** sampleの個数・名称・未実装状態を共通品質条件にすると、API・migration・Job・schemaの通常追加や案件名の変更が失敗する。生成対象のsample限定も案件の資料・SQL型の欠落を招く。

**決定:** 共通検査はmodule境界、認可、transaction、migration整合性、配布境界を検証する。名称・件数・登録内容は各機能の契約testで扱う。sample固有の契約はsample側のtestに置き、sample削除・変更時に一緒に更新する。特定機能を無効化するtestは、その条件をtest自身で指定する。

OpenAPIの出力testは生成物の取得・保存・保護を確認し、個別APIの契約は各APIのtestで検証する。公開対象はweb.rest配下を維持する。共通エラーschemaはsample非依存で登録し、共通応答の適用は未定義のstatusに限定する。system主体の権限一覧は標準GrantedAuthorityで複数featureに対応する。jOOQは複数schemaを標準生成し、PostgreSQL内部schema、public、systemを除く業務schemaを対象とする。生成先はjooq配下のschema別packageとし、単一schemaでも同じ配置を維持する。tblsは内部schemaとFlyway履歴以外のtableを対象とする。

配布検査は実際のmain／test／codegen出力とJARを照合し。architecture検査のbase packageはApplicationから取得する。完全修飾名はimportによる衝突解消ができない箇所やframeworkの文字列式等に限定し、長いmethodは意味のまとまりで空行・目的を説明するコメントを入れる。

**影響:** 業務機能の追加時も品質規約と案件固有の権限・公開範囲を保持する。新schema・API・migrationを追加した構成でも生成と検査が成立することを確認する。

<a id="adr-023"></a>
## ADR-023: 資料の正本統合と関係に集中したER図

**状態:** Accepted。ADR-015の手書き補足配置・PDF入力・ER表示を本判断で置き換える。PDF生成は[ADR-024](#adr-024)で廃止。

**背景:** PDF専用の技術概要は既存ガイドと重複する。全カラムを含むER図は大きくなり、関係の把握を妨げる。

**決定:** DBの補足はdatabase.md、設計理由はADRへ集約する。projectDocumentationはarchitecture.mdを直接PDF化する。Mermaid CLI標準のMarkdown変換で本文中の図をSVGへ変換し、PandocとLuaLaTeXで組版する。PDF用の表示metadataのみdocker/docsへ置き、本文の正本はarchitecture.mdとする。出力project.pdfと既存のrelease連携を維持する。

ER描画はtbls標準のMermaid出力をMermaid CLIでSVGへ変換する。ER図はtblsのshowColumnTypesで関係カラムへ絞り、全カラム・制約の詳細はtable別Markdownで読む。SVGは拡大しても文字が劣化しない関係図として埋め込む。全体図と直接隣接tableの図を提供し、対象tableは実DBのschemaから取得する。大規模化時はtbls標準Viewpointsで業務単位の図を追加する。

**影響:** ガイド内の図はその場で保守する。PDFの対象はアーキテクチャ資料とする。MarkdownとERの変換には導入済みtoolの標準機能を使用する。

<a id="adr-024"></a>
## ADR-024: PDF資料生成を標準機能から除外する

**状態:** Accepted。ADR-015／016／023のPDF生成・配布に関する決定を置き換える。

**背景:** 設計・開発資料の配布形式はMarkdownとMermaidとする。

**決定:** 標準の資料生成toolchainはtblsとMermaid CLIで構成する。documentationはDB資料とOpenAPIを生成し、releaseArtifactsはJAR、DB資料ZIP、OpenAPI YAMLを収集する。MarkdownとMermaidによる設計・開発資料、tblsとMermaid CLIによるER SVG生成は維持する。

**影響:** 資料用imageの構成をDB資料とER描画に必要なtoolに限定する。ガイドはMarkdownとして参照し、業務機能としての帳票出力は案件の要件に応じて判断する。

<a id="adr-025"></a>
## ADR-025: Gradleコマンドの責務を分離する

**状態:** Accepted。ADR-014／016の検証task構成を更新する。

**背景:** 開発者がcommand名から実行範囲を判断できるように、個別実行・テスト集約・静的検査・配布物生成の責務を明確にする。

**決定:** unitTestは単体、integrationTestは結合、architectureTestは構造のtestを実行する。testはこれらとunit＋integrationのJaCoCo reportを集約する。checkは整形・静的解析・配布境界を検査する。buildはassemble・check・testを含む。codegen subprojectもunitTest／test／checkの役割を揃える。

Gradle Java pluginが提供するtestはTest型のまま集約に使い、直接実行するtest sourceを空にする。実際のJUnit実行は専用taskへ移す。実装にはGradle標準のtask型を使用する。集約task自身には対象classがなく、依存する各testの結果で成否を判定する。testReportは3種類の結果を一つのHTML reportへ集約し、testから実行する。個別class選択はunitTest等の--testsで行う。

releaseArtifactsはJAR・資料の生成と収集を担当する。CIとreleaseはbuildを明示的に実行する。apiDocumentation内の検査は生成資料の契約検証として資料生成と一体にし、資料出力の責務をapiDocumentationへ集約する。

**影響:** checkでも解析・配布境界に必要なcompileとjOOQ生成、JAR作成は実行する。test実行とは区別する。個別testのreportと集約coverageはbuild/reportsへ出力し、buildとreleaseの品質保証は維持する。
