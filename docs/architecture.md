# Architecture

この文書は [Decision Ledger](decisions.md) の派生資料である。記述は完成時の設計であり、実装状況は [実装計画](implementation-plan.md) を参照する。

## Moduleと依存方向

Feature-oriented Modular Monolithを採用する（D-100〜D-114）。starterではliteral name `feature` の最小sampleを使用する。

`feature.api.command` / `feature.api.query` をNamed Interfaceとして公開する。REST、Vaadin、Batch、Schedulingおよび他featureはこのpublic APIを呼び、`feature.internal` を直接参照しない。module cycleとmodule boundaryはSpring Modulith `verify()` で検証する。

## CommandとQuery

| 経路 | 責務 |
| --- | --- |
| `api.command` → `internal.command` | 公開契約、入力適応、transaction boundary、認可 |
| `internal.command` → `internal.usecase` → `internal.domain` | 業務操作の調整、不変条件、domain rule |
| `internal.infrastructure` → `internal.usecase` のRepository interface | jOOQ等による永続化portの実装 |
| `api.query` → `internal.query` | read model取得、read-only transaction、認可 |
| `internal.infrastructure` → `internal.query` のDataSource interface | SQLによるread model取得 |

Queryはusecase/domainを通さない（D-130〜D-155）。domain/usecaseはtransaction frameworkやSpring Security型に依存しない。jOOQ generated typeは原則 `internal.infrastructure` だけで使用する。

公開が必要な型だけを `public` にする。internal implementationはpackage-privateを第一選択とし、packageを跨ぐ必要がある型の可視性は実装時に判断する（D-120）。

## 結果とtransaction

Commandは原則 `Result<S,F>` を返す。variantは `Success` / `Failure`、Failure reasonは型付きにする。Queryはexpected failureに意味がある場合だけResultを使う（D-160〜D-167）。

想定する業務上の不成立はFailure、予期しない技術障害・不可能な内部状態はExceptionとする。transaction内でExceptionを捕捉してFailureに変換し、部分commitを発生させない。他featureへの更新はpublic Command APIを呼び、default propagationは `REQUIRED` とする。

## Webとsecurity

RESTは `web.rest`、Vaadinは `web.ui` に配置する。HTTP DTOやVaadin型をbusiness側へ流さない。RESTは `/api/v1` 形式のmajor versionとSpring MVC標準versioningを使用し、errorは `ProblemDetail` に変換する（D-280〜D-312）。

Spring Securityと `@EnableMethodSecurity` をbaselineに含める。business authorizationは主にCommand/Query boundaryでAuthority単位に行う。route securityは入口の制限とし、RESTの未認証は401、権限不足は403。認証方式自体は案件依存とする（D-320〜D-332）。

## 横断的な技術基盤

- 外部HTTPはHTTP Service Client、RestClientの順で検討する。外部DTOはinfrastructureに閉じ、connect/read timeoutを明示する。default retryは行わない（D-340〜D-350）。
- LoggingはAOP/filter/interceptorを中心に実装する。CommandはINFO、QueryはDEBUG、予期しない障害はERROR。correlation IDはMDCで管理し、credentialやpayload全文を記録しない（D-360〜D-370）。
- 環境依存値はenvironment variableから注入する。custom設定は型付き・validation付きConfigurationPropertiesを第一選択とし、必須設定不足を起動時に検出する（D-380〜D-388）。
- 実時刻はInstantとinjectable Clock、内部基準はUTC。UUID v7はuuid-creatorのTimeOrderedEpochFactoryへClockを注入して生成する（D-170〜D-186）。
- Batch / Schedulingはpublic APIを呼ぶadapter。意味のないJobや定期処理は作らず、起動時に全Jobを自動実行しない（D-440〜D-447）。
- 同期public APIをfeature間通信の基本とする。event / async / cache / file storage等は必要性に応じて導入し、先行frameworkは作らない。

## 品質保証

Unit / Integration / Architecture Testを分離する。Unit TestはSpring contextなし、DB統合テストはPostgreSQL Testcontainersを使用する。ArchUnitはModulithで表現しにくい技術的依存制約だけを検証する（D-450〜D-460）。

`check` にSpotless、Checkstyle、SpotBugs、各test、関連JaCoCo taskを接続する。coverageは可視化に使い、数値達成だけのtestは作らない（D-470〜D-475）。

## Phase 3の実装

`FeatureCommands.create` → `DefaultFeatureCommands` → `CreateFeatureUseCase` → `Feature` / `FeatureName` → `FeatureRepository` を接続した。Commandは不正入力をwrite前にtyped Failureへ変換し、usecaseはJDKのSupplierでIDを生成してRepositoryへ渡す。

`FeatureQueries.find` → `DefaultFeatureQueries` → `FeatureDataSource` はdomainを経由せず、Optionalのread modelを返す。`JooqFeatureStore` が両portを実装する。Spring implementationはpackage-privateで、packageを跨ぐport / domain / usecase型だけをpublicにしている。

Resultは `feature.api.command` のsealed interfaceで、Success / Failureの値はnullを許容しない。現時点では他featureでの利用がないため共通moduleは追加していない。

Named Interfaceはcommand / queryの2つ。generated jOOQは既存packageを維持したopen technical moduleとし、利用箇所をArchUnitでinfrastructureへ制限する。Architecture Testのimport対象はproduction classであり、custom source setのintegrationTest / architectureTestも明示的に除外する。

DB統合テストではtestの外側transactionを無効にしてpublic APIの実commitを検証する。Repositoryによるinsert後に技術例外を発生させ、DBへ行が残らないことを検証する。QueryではSpringのread-only flagとPostgreSQLのtransaction_read_onlyを両方確認する。

## Phase 4の実装

REST ControllerはFeatureのpublic Command / Query APIだけを呼ぶ。Spring MVCの `usePathSegment(1)` とmappingの `version="v1"` で `/api/v1/features` の作成・取得を公開する。requestの欠落・形式不正は400、domainのINVALID_NAMEは422、未検出は404とする。

Command / Queryのtransaction境界に `@PreAuthorize` を配置し、それぞれpublic APIで定義する `feature:write` / `feature:read` を要求する。security filterは認証を要求し、CSRF保護を維持する。認証providerは案件側で接続し、Bootの自動生成ユーザーは無効にする。

security filterの401 / 403とMVCのerrorをProblemDetailへ統一する。共通adviceは予期しない例外を記録して詳細を伏せた500を返し、認証・認可例外はfilterへ伝播する。MockMvcとPostgreSQLで認可拒否時のwrite防止、技術例外時のrollbackを検証する。Modulithでwebの公開API依存を、ArchUnitでSQLへの直接依存禁止とtransaction境界の認可宣言を検証する（D-600〜D-602）。

## Phase 5の実装

`web.ui.FeatureView` は `/features` に配置し、既存public APIへ作成・取得を委譲する。名前の不変条件はCommandのtyped Failureを表示し、UUIDの入力形式だけをUIで検証する。権限拒否は利用者へ表示し、技術例外はVaadin標準のerror handlingへ伝播する。

RESTのfilter chainは `/api/**` に限定する。残りはVaadinSecurityConfigurerでframework内部通信・静的resource・navigationを保護し、Viewの `@PermitAll` は認証を要求する。業務認可は既存Method Securityを維持する。認証方式とloginは案件側で接続する。

Vaadin BOM / pluginは25.2.6。Flow core / Spring連携だけを追加し、独自frontend frameworkは作らない。Gradle pluginをBoot pluginの後に適用し、production frontendをbootJarへ接続する。標準componentには公式precompiled bundleを利用する。

Integration TestはPlaywright JavaのChromiumを使い、実HTTP server・PostgreSQLを通して画面を検証する。session付与用servletとfilter chainはtest configuration限定で、productionには含まれない。既存のModulith / ArchUnit ruleがVaadin画面にも適用される（D-610〜D-612）。

## Phase 6の実装

logging moduleはHTTP filter、Command / Queryのpackage-based AOP、RestClientCustomizer、Batch listener、Vaadin共通error handlerを提供する。AOPは認可後・transactionの外側で成功／Failure／例外を観測する。HTTPから生成するcorrelation IDをMDCへ設定し、response / outgoing requestへ伝播し、終了時に元のMDCを復元する。生のURLやpayload、Result値、例外messageは標準ログに含めない。

`http.HttpTimeoutConfiguration` はBoot標準prefixをvalidated recordへbindし、接続・読み取りtimeoutの欠落・非正値を起動時に拒否する。案件clientや独自HTTP wrapperは追加しない。HTTP Service GroupにもBoot共通設定が適用され、実通信testでtimeout・correlation・503のretryなしを検証する。

Actuatorの専用filter chainでhealthを匿名公開し、info / metricsにはops:readを要求する。他のendpointは公開対象に含めない。Micrometer / HikariCPとgraceful shutdownはBoot標準のまま使用する。

Spring BatchはJDBC JobRepositoryとFlyway管理のsystem schemaを使い、自動起動を無効にする。共通listenerは案件Jobへ明示登録する。SchedulingはBoot標準schedulerで有効にし、実行処理は案件側で追加する。file / event / async / retry / cacheの採用条件は既存Decisionを維持する（D-620〜D-623）。

## Phase 7の実装

springdocはweb.restのController metadataからOpenAPIを生成する。defaultでは公開せず、有効化時も専用SecurityFilterChainでops:readを要求する。生成testは一時PostgreSQLとMockMvcで認可と契約を検証し、version別YAMLを出力する。

DB資料はFlyway適用後の一時DBをtblsで参照する。project PDFはMarkdownと事前renderしたMermaid SVGから生成する。3つのGradle taskをdocumentationへ集約し、通常buildから独立させる（D-630〜D-633）。
