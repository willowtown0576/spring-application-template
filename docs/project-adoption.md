# 案件開始チェックリスト

このstarterを案件のapplicationへ変更するためのチェックリスト。識別子、サンプル、認証・権限、DB、UI／API、外部環境、資料、検証を対象とする。設計の正本は[ADR](decisions.md)、日常の実装手順は[開発ガイド](developer-guide.md)。

## 使い方と着手順

- **変更必須**: 案件の値・実装へ変更する。
- **判断必須**: 案件の要件に照らして決める。現状を採用できる項目もあり、現状を採用する判断も有効とする。
- **条件付き**: 記載条件に該当するときだけ変更する。追加対象は条件に該当する機能とする。
- 各項目は、変更または判断と完了条件の確認が済んだら完了とする。該当しない場合は、案件のIssue等に理由を記載する。
- 本書は再利用する手順書として未チェックのまま保持する。案件の実施状況・担当・証跡はIssue等で管理し、製品資料は共通の手順書として扱う。secretの管理先は案件のsecret管理基盤とする。

| 時点 | 終える範囲 |
|---|---|
| 案件repositoryの作成前 | A: 採用条件・入力値 |
| 最初の案件DB起動前 | B: 名称、C: ローカル環境、D: migrationの扱い |
| 最初の業務機能を実装するとき | D: feature／DB、E: 認証・認可、F: UI／API |
| 該当機能を追加するとき | G: 外部連携・Batch等 |
| チーム開発開始前 | H: CI、I: 資料、J: 開発開始の検証 |
| 本番公開前 | H: 配布先・運用、J: リリース前の検証 |

本書の「もれなく」は、現行starterに存在する設定・実装の変更箇所と、接続が必要な案件環境を対象とする。個別業務の画面・データ・外部サービスの仕様は要件定義で追加する。

## A. 採用条件と入力値

- [ ] **A1 判断必須 — 採用元を確定する。** 採用するtemplateのtag／commit、案件repositoryのowner／name／公開範囲、default branchを決める。ローカルだけの未commit変更やsecret、生成物を案件へ持ち込まない。完了条件: 案件がどのsourceから作られたか特定でき、push先がtemplate元でない。
- [ ] **A2 判断必須 — 利用・配布条件を決める。** 現行rootにはLICENSEがない。案件のsource公開方針、権利者表記、配布時の依存license表示を確認し、必要なLICENSE／NOTICEを用意する。Vaadinの追加部品を選ぶ場合も採用条件を確認する。
- [ ] **A3 判断必須 — 技術基盤を採用できるか確認する。** Java 25、Spring Boot 4.1系、Vaadin 25、PostgreSQL、Docker利用、JVM実行、Modular Monolithの前提を案件環境と照合する。差異があれば先に案件ADRへ記載する。versionやlibraryの変更は案件の技術要件に基づいて判断する。
- [ ] **A4 判断必須 — 次の値を確定する。** repository名、Gradle project名、group、base package、生成tool package、application識別名、画面表示名、初期version、資料image名。各値は用途に応じて個別に決める。入力先と変換関係はBの表を使う。
- [ ] **A5 判断必須 — sampleの扱いを決める。** welcome／部品集／テーマ比較と、DBを使うfeature sampleを別々に「開発中保持・業務へ転用・削除」から選ぶ。公開可否と削除期限も決め、実装はD／Fへ反映する。

## B. 名称・packageの置換

**名称置換は下表を一組として行う。** `application`、`feature`、`system`、`starter` の置換対象は用途ごとに選択する。package名、業務名、Spring設定key、framework metadataは別の意味を持つ。

| 初期値／対象 | 変更箇所と連動先 |
|---|---|
| `spring-application-starter` | [settings.gradle.kts](../settings.gradle.kts)のrootProject.name、[application.yml](../src/main/resources/application.yml)のspring.application.name、[.tbls.yml](../.tbls.yml)のname、README／docsの現行説明・JAR実行例 |
| `dev.template` | [root build.gradle.kts](../build.gradle.kts)のgroup。base packageと別に決定する。codegenは生成tool用の内部subprojectとして扱う |
| `dev.template.application` | src/main/java・src/test/javaのdirectory、package／import／完全修飾名、[JooqCodegen](../codegen/src/main/java/dev/template/build/JooqCodegen.java)のtarget package、application.ymlのspringdoc.packages-to-scan、[AutoConfiguration.imports](../src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)の完全修飾class名、docs中の現行package説明 |
| `dev/template/application` | Java sourceのdirectory。SpotBugs／JaCoCoは`**/jooq/**`を除外し、verifyProductionJarは実class出力から検査するため対象は実際のclass出力から決まる |
| package依存の検査 | [ArchitectureTest](../src/test/java/dev/template/application/ArchitectureTest.java)のpackage規約とApplication.classから導出する解析範囲。[SpotBugs filter](../config/spotbugs/exclude.xml)のclass名。package構造の規約を変える場合はpatternも追従 |
| `dev.template.build`／`dev/template/build` | codegen/src/main/javaのdirectoryとpackage、root build.gradle.ktsのJooqCodegen／DocumentationMigrationのmainClass。application packageへ混ぜず、生成toolを配布から隔離する |
| `Application Starter`等の表示 | [StarterLayout](../src/main/java/dev/template/application/web/ui/StarterLayout.java)、[WelcomeView](../src/main/java/dev/template/application/web/ui/WelcomeView.java)、[LoginView](../src/main/java/dev/template/application/web/ui/LoginView.java)、[ComponentGalleryView](../src/main/java/dev/template/application/web/ui/ComponentGalleryView.java)の表示文言／PageTitleと対応UI test |
| `0.1.0-SNAPSHOT` | root build.gradle.ktsのversion、docsのJAR名・release例。APIのmajor versionとは別管理 |
| `spring-application-starter-docs:1` | root build.gradle.ktsのdocumentationImage、[docker/compose.yaml](../docker/compose.yaml)のdocs-tools.image、[renovate.json](../renovate.json)のignoreDeps。複数案件でtoolchainを変更するなら共有tagの衝突を避ける |
| `starter.security.user` | 条件付きの名称変更。application.yml、[LocalAuthenticationConfiguration](../src/main/java/dev/template/application/security/LocalAuthenticationConfiguration.java)のConfigurationProperties、src/test/resources/application.properties、認証設定test。APP_USER_*は環境変数契約であり別途判断 |
| `starter-docs-db-` | root build.gradle.ktsの一時Compose project prefix。UUIDで隔離される技術名なので保持可能 |
| `starter-*`のUI ID／`APPLICATION STARTER / UI EXAMPLES` | sampleを残す場合の内部IDと表示文言を区別する。ID変更時はFeatureUiIntegrationTestのselectorも更新する |

- [ ] **B1 変更必須 — 案件識別子を反映する。** 上表のproject、group、application package、application識別名、表示名を置換する。生成toolも案件namespaceへ揃える。完了条件: Java、生成型、設定、画面、資料が同じ案件を指す。
- [ ] **B2 判断必須 — version・技術名を確定する。** 初期version、資料image名、設定prefix、UI内部ID、一時Compose prefixの採用／変更を決める。変更する場合は表の連動先を全て更新する。
- [ ] **B3 変更必須 — 検査対象の追従を確認する。** ArchitectureTestの解析元とverifyProductionJarは実classから自動追従する。SpotBugs filterのclass名を更新し、package規約自体を変える場合だけArchUnitや除外patternも変更する。完了条件: 新packageが実際に解析され、生成型以外が誤って除外されず、配布class検査も新しいclassを要求する。
- [ ] **B4 変更必須 — directoryとsourceを同時に変える。** applicationとcodegen双方のmain／test・package-info.java・完全修飾名を確認する。build/generated-src/jooq等の更新は再生成で行う。
- [ ] **B5 判断必須 — repositoryと成果物名を確認する。** checkout先directory名とremoteを案件へ合わせる。JAR／OCI名はproject.name／versionに追従する。release workflowのGH_REPOはgithub.repositoryから取得するため、元ownerの文字列置換は不要。資料ZIP／OpenAPIの汎用file名は保持可能。

## C. ローカル環境と設定

- [ ] **C1 変更必須 — 開発者ごとに起動設定を与える。** [local-env.properties.example](../config/local-env.properties.example)をGit管理外のlocal-env.propertiesへコピーし、DEV_DB_PASSWORDとAPP_USER_NAME／APP_USER_PASSWORDを設定する。passwordは12 code point以上・72 UTF-8 bytes以下。初期値や実credentialをtemplateへ埋め込まない。外部UserDetailsServiceへ変更済みならE2の契約に従う。
- [ ] **C2 判断必須 — 開発DB名・user・volumeの扱いを決める。** Composeのdev用POSTGRES_DB／USERは`application`、volume keyは`postgres-data`。保持してよいがCompose project名・volumeは案件ごとに分離する。directory名やCOMPOSE_PROJECT_NAMEも確認する。既存volumeのpasswordは設定file変更だけでは変わらない。
- [ ] **C3 判断必須 — 同時起動と接続設定を決める。** 8080が競合する場合はSERVER_PORT、必要時は待受addressを設定する。開発DBはloopbackの動的portを使う。既存環境変数がlocal fileより優先されること、JARはlocal fileを読まないことを確認する。
- [ ] **C4 判断必須 — チームの実行環境を合わせる。** JDK 25／Gradle JVM、Docker、依存・image・Chromium取得経路を確認する。network制限がある場合だけMaven／container／browserの取得設定を追加する。GradleはWrapper、NodeはVaadinのbuild toolingで管理する。
- [ ] **C5 条件付き — versionを変えるなら管理元を同時更新する。** Javaはroot／codegenのToolchainとCI／Releaseのsetup-java、GradleはWrapperとchecksum、明示依存はCatalog。PostgreSQLはCatalogとCompose両service、資料toolchainはDockerfile／lockfileを合わせる。BOM管理のversionはBOMに集約する。

## D. 業務feature・DB・生成型

- [ ] **D1 判断必須 — migrationの出発点を確定する。** V1はfeature sample、V2はBatch metadata。適用済みmigrationは履歴として保持し、新migrationで変更する。未配布・未適用の案件初期schemaを再編する場合だけ、その前提と手順をADRで決める。開発volumeの削除は必要なdataの確認とbackupの後に行う。
- [ ] **D2 変更必須 — 業務機能を定義する。** literal名featureとname／UUIDだけのsampleを、実際の業務module・公開Command／Query・Param／Result／Failure・domain規則へ転用するか、別featureとして実装する。業務仕様は公開契約・domain規則・契約testで具体化する。
- [ ] **D3 変更必須 — module境界を整合させる。** feature側のNamedInterfaceとallowedDependencies、web/package-info.javaのfeature::command／queryを更新する。ArchitectureTestはapplication全体のmoduleを検証するため、他moduleのinternal参照とcycleを検出できることを確認する。
- [ ] **D4 変更必須 — DB契約を実装する。** 所有schema、table／column／constraint／index、COMMENT、Validation、ID・日時・金額型を定義し、domainとDBの制約を合わせる。Queryの一覧・検索条件、sort、pagination、必要なJOIN／COUNTは実要件に合わせる。
- [ ] **D5 条件付き — schemaを追加・変更したら生成結果を確認する。** JooqCodegenはmigration適用後の業務schemaを自動生成し、target`dev.template.application.jooq`配下をschema別に分ける。内部schema、public、systemは除外する。新technical schemaでは生成・資料対象に含めるかを判断する。生成した型はinfrastructureからだけ使用する。
- [ ] **D6 条件付き — sample撤去を連動させる。** 下表の参照を全て整理し、業務の代替testを用意する。品質taskは業務の代替testとともに有効に保つ。
- [ ] **D7 判断必須 — audit・履歴・削除規則を決める。** 必要なtableだけcreated_at等を採用し、利用者／systemの識別子を記録する方式を決める。業務監査台帳は監査要件に応じて設計する。physical deleteのままでよいか、履歴の保持と参照権限も判断する。
- [ ] **D8 判断必須 — 本番DBを準備する。** 接続先、DB／user、TLS、network、migration実行権限、application実行権限、接続数上限を決める。起動時Flywayを使うか事前migrationに分けるか決め、既存DBがある場合はbaseline／移行を別途設計する。
- [ ] **D9 条件付き — 初期dataを用意する。** 起動に必要なsystem dataと業務masterの投入・更新・再実行時の扱いを決める。import方式はdataの規模と更新頻度から選び、credentialは実行環境のsecret管理基盤から供給する。

### sample変更時の連動箇所

| 対象 | 更新・撤去するもの |
|---|---|
| 業務Java | feature配下の公開API／authorization／内部実装、各package-info.java、利用するadapter |
| DB／SQL型 | 新migration、JooqCodegenのschema／package、Repository／DataSourceのgenerated import |
| 認証・system | LocalAuthenticationConfigurationの権限付与、SystemActorの権限。SystemActorは`List<GrantedAuthority>`で複数featureの権限を保持できる |
| REST／UI | FeatureController、FeatureView、StarterLayout／WelcomeViewのDB連携リンク、OpenApiConfigurationのtitle／説明 |
| 品質設定 | ArchitectureTestのpackage規約、SpotBugs filterのCreateFeatureUseCase例外。gallery削除時はCheckstyleのComponentGalleryView.nextId例外も削除 |
| test | FeatureNameTest、CreateFeatureUseCaseTest、DatabaseIntegrationTest、WithTestUser、LocalAuthenticationConfigurationTest、SystemExecutionTest、FeatureUiIntegrationTest、ApiDocumentationTestのsample依存 |
| DB資料 | .tbls.ymlのname／desc／exclude、DB guide／ER説明 |
| プロジェクト資料 | README、architecture、developer-guide、operations、図のsample説明とURL |

## E. 認証・認可・実行主体

- [ ] **E1 判断必須 — 認証方式を採用する。** 現行は明示設定された1利用者のInMemoryUserDetailsManager＋フォーム／session認証。利用者数、個別監査、退職・無効化、password変更、SSO、MFA等の要件と照合する。1利用者方式で足りる場合は保持できるが、実運用credentialを設定する。
- [ ] **E2 条件付き — 認証を差し替える。** UserDetailsServiceを提供すると標準利用者設定はbackoffする。独自AuthenticationProvider／外部認証を追加するだけで同じbackoffになるとは限らないため、LocalAuthenticationConfigurationの条件、SecurityConfiguration、LoginView／logout、application.ymlの自動構成除外、APP_USER_*、testを一組で更新する。置換先でも認証・認可を必須とする。
- [ ] **E3 変更必須 — 業務権限を定義する。** feature:read／writeを各業務Authorityと利用者への付与規則に置き換える。業務操作の本認可はCommand／QueryのMethod Securityとする。必要ならtenant／所有者／行単位の認可を追加し、権限不足testも用意する。
- [ ] **E4 判断必須 — system主体の権限を確定する。** 現行BATCHはfeature参照・更新、SCHEDULERは参照だけ。実Job／定期処理が必要とする最小権限へ変更する。system: namespaceを利用者と分離し、システム主体は信頼されたadapterのコードで選択する。
- [ ] **E5 判断必須 — 運用権限を分離する。** ops:readとAPP_USER_OPERATIONS_READの付与先、Actuator／API資料への到達範囲を決める。全利用者に運用権限を配らない。
- [ ] **E6 判断必須 — sessionと公開境界を設定する。** HTTPS、cookieのSecure／HttpOnly／SameSite、session有効期限、CSRF、login失敗時の制限、proxy越しのURLを実配置で確認する。別originのREST clientが必要ならCORS・認証・CSRF方式をそのclient向けに設計し、clientごとの信頼境界に応じて保護設定を適用する。
- [ ] **E7 条件付き — 複数instanceならsession・処理分担を設計する。** Vaadinのserver側UI state、再起動時のsession、load balancerの振り分け、Batch／Schedulerの多重起動を確認する。Redis等の採用は配置要件に基づいて判断する。

## F. UI・REST・公開sample

- [ ] **F1 変更必須 — 案件の入口を作る。** `/`のWelcomeViewを案件のhomeへ置換するか、開発用保持の期限を決める。StarterLayoutのタイトル・ナビ、LoginView、PageTitle、空状態・エラー・ヘルプ文言を案件へ合わせる。リンク先は実際に提供するrouteと一致させる。
- [ ] **F2 判断必須 — 公開sampleの扱いを確定する。** `/components`、`/theme-comparison`、`/theme-preview`は匿名公開で、メニューを消すだけではアクセスできる。削除または案件向けのroute制限を行い、本番で残すものは公開理由を明確にする。DB sampleの`/features`／`/api/v1/features`も別に判断する。
- [ ] **F3 判断必須 — テーマを選ぶ。** 通常画面はStarterShellでAura、比較iframeだけLumo。Lumoを通常テーマにする場合はshell、標準variant、明暗切替、UI testを更新する。documentごとにAuraまたはLumoを選択し、styleはVaadin標準に統一する。
- [ ] **F4 条件付き — 比較機能を削除したら付随設定も整理する。** ThemeComparisonView、ComponentGalleryViewのRouteAlias／preview向けBeforeEnter、StarterShellのquery分岐、ナビ／資料／testを整理する。UI chainのSAMEORIGINはiframe比較用なので、埋め込みが不要ならframe拒否へ戻す。AppShellConfiguratorを残す場合は標準stylesheetの指定を残す。
- [ ] **F5 判断必須 — 言語・時刻・端末を確定する。** 日本語表示、lang／Locale、DatePicker等の入力形式、業務timezone／通貨、mobileとkeyboard操作を要件に合わせる。内部時刻のUTC／Clockと業務の表示timezoneを区別する。
- [ ] **F6 変更必須 — 業務REST契約と資料を合わせる。** FeatureControllerのpath／DTO／status、OpenApiConfigurationの共通説明と各APIのmetadata、FeatureApiDocumentationTest等のsample契約testを実仕様へ変更する。RESTを提供しない場合はsampleを撤去し、API資料task／release添付の扱いをADRで決める。
- [ ] **F7 条件付き — APIの構造を変える場合は周辺も追従する。** 現行は`/api/v1`、WebConfigurationはpath segment 1でmajorを解決する。prefix／major変更時はsecurityMatcher、URL／test、OpenAPI出力先、releaseArtifactsを確認する。API versionとapplication versionは独立して管理する。
- [ ] **F8 判断必須 — OpenAPI公開を決める。** API_DOCUMENTATION_ENABLEDのdefaultはfalse。有効化環境とops:readの付与先を決め、認証・権限・CSRFの実仕様を資料へ反映する。単に表示を有効にするだけでは運用権限は付かない。

## G. 要件がある場合だけ追加する機能

- [ ] **G1 条件付き — 外部HTTP接続を設定する。** 対象service、base URL、credentialの取得先、HTTP_CONNECT_TIMEOUT／HTTP_READ_TIMEOUT、group別差異、Failure変換を定義する。HTTP Service Client／注入RestClient.Builderを利用し、相関IDと秘匿loggingを維持する。retryは冪等性が保証できる操作だけにする。
- [ ] **G2 条件付き — Batch Jobを実装する。** 標準には稼働Jobがない。起動手段、JobParameters、再実行／restart、chunk／transaction、失敗通知と履歴保持を決める。例外messageがframeworkログとmetadataに保存されることを踏まえ、秘匿・参照権限・保持期間を決める。BatchLoggingのlistenerを登録し、実worker内でSystemExecutionを適用する。Jobの起動方式とmetadataのFlyway管理を変更する場合は、運用要件と移行手順をADRへ記載する。
- [ ] **G3 条件付き — 定期処理を実装する。** 標準には実行する@Scheduled処理がない。schedule／timezone、重複実行防止、再実行、終了待ち、例外通知を決め、SystemActor.SCHEDULERの許可範囲で公開APIを呼ぶ。
- [ ] **G4 条件付き — file入出力を実装する。** storage、容量、MIME／内容検査、名前の扱い、認可、保持・削除、temporary fileの後始末を決める。CSV／Excel／業務PDFは実要件に応じて採用し、UI型・生pathをdomainへ流さない。
- [ ] **G5 条件付き — event／非同期／cache等を選ぶ。** 必要性、整合性、失敗時の回復、実測した性能制約をADRへ記載する。同期公開APIを基本とし、frameworkやPortは実装対象の処理に合わせて追加する。

## H. CI・配布・運用環境

- [ ] **H1 判断必須 — 案件repositoryの管理を設定する。** GitHubのアクセス権、Actions利用可否、default branch、merge規則・必須checkを設定する。現行CIは全branch push／PR、job名check。main固定のworkflow置換は不要だが、repository側の保護は別途必要。
- [ ] **H2 判断必須 — CI実行条件を確定する。** Ubuntu 24.04／JDK 25／Docker／Chromium依存installが可能か確認する。runner変更時はPlaywright依存・Docker権限・architectureを検証する。通常のbuildはtest専用credentialと一時DBを使うため、本番APP_USER_*／DB secretをCIへ渡す必要はない。
- [ ] **H3 判断必須 — 依存更新を接続する。** 案件repositoryへRenovate App等を接続し、renovate.jsonの対象・group・自動merge無効・資料image除外名を確認する。外部Appの接続は案件repository上で確認する。
- [ ] **H4 判断必須 — releaseの公開先・公開条件を確定する。** 現行は`v*` tagで検証後GitHub Releaseを公開し、workflow_dispatchはbuildとartifact保存を担当する。versionはsuffixなしSemVerとtag一致が必要。GitHub Releaseを使わない案件はpublish jobと成果物の受け渡しを変更する。公開を伴わない検証にはworkflow_dispatchを使う。
- [ ] **H5 判断必須 — 配布先を構築する。** JAR／OCI、実行基盤、registry／image名、network、DNS／TLS、起動・停止・更新・切り戻し手順を決める。bootBuildImageはローカル生成までで、registry push／deploymentは未提供。必要な認証とCI処理を案件側で追加する。
- [ ] **H6 変更必須 — 実行環境にsecretを設定する。** 既存PostgreSQLのSPRING_DATASOURCE_URL／USERNAME／PASSWORDと、選んだ認証方式の設定を安全な供給元から渡す。ローカル用設定fileは開発環境に限定する。開発・検証・本番の値と更新方法を分離する。
- [ ] **H7 判断必須 — 容量と可用性を決める。** JVM memory／CPU、DB poolと最大接続、timeout、probe、graceful shutdown（現行phase timeout 30s）、一時disk、instance数を配置先で確認する。負荷試験の結果で設定し、推測だけでpoolを増やさない。
- [ ] **H8 判断必須 — 監視と障害対応を用意する。** health／info／metricsの公開先、log収集・保持・参照権限、通知先、相関IDでの調査、DB／Batch障害時の手順を決める。log項目は診断に必要な非機密情報に限定する。
- [ ] **H9 判断必須 — backup／restoreを実証する。** DB・必要なfileの保持期間、復旧目標、backup取得と復元手順、migration失敗時の扱いを決め、検証環境で復旧を試す。過去JARへ戻すだけでDB変更まで戻るとは考えない。
- [ ] **H10 判断必須 — report／成果物の扱いを決める。** Actions reportは14日、release-assets artifactは7日、GitHub Release添付は別管理。案件の保存期間と閲覧権限に合わせる。UI capture・API／DB資料には動作確認用dataを使用する。

## I. ドキュメント・AIの前提

- [ ] **I1 変更必須 — READMEと案内を案件向けに書く。** 目的、開発環境、起動URL、公開機能、設定名、build／release手順、問い合わせ先を現行実装へ合わせる。templateの紹介を案件の仕様として残さない。
- [ ] **I2 変更必須 — DB／APIのsourceを合わせる。** .tbls.ymlのname／desc／exclude、Flyway COMMENT、OpenAPI metadata、docs/architecture.mdの本文、Mermaidのmodule／schemaを更新する。生成物はGit管理外とし、修正はsourceからの再生成で行う。
- [ ] **I3 判断必須 — 案件ADRを確定する。** 継続採用する基盤判断と、認証・module・DB・UI等の変更判断を区別する。現在の仕様は各ガイドへ反映し、SupersededのADRからは現在有効な後継判断を案内する。
- [ ] **I4 判断必須 — AIの前提を維持する。** root AGENTS.mdのdocs参照、依存境界、品質規約、secret禁止、「最初から存在していたコードとして書く」規則を維持する。案件固有の必要事項だけ追加し、指示はroot AGENTS.md、作業記録はIssueへ集約する。
- [ ] **I5 条件付き — 運用責任と外部設定の正本を案内する。** 配置先・権限付与・障害対応・backup／releaseの管理先をdocsから参照できるようにする。docsにはsecretの管理先と取得手順を記載する。

## J. 完了条件と検証

### 開発開始時

- [ ] **J1 — 置換漏れを検索する。** 次の検索結果を意味ごとにreviewする。保持する技術名・ADRの履歴・本チェックリスト内の初期値例は除外理由を判断し、置換はその識別子の用途に基づいて判断する。

```bash
rg -n --hidden -g '!.git/**' -g '!.gradle/**' -g '!build/**' -g '!codegen/build/**' -g '!node_modules/**' -g '!target/**' -g '!src/main/frontend/generated/**' -g '!config/local-env.properties' 'dev[./]template|spring-application-starter|Application Starter|APPLICATION STARTER|Feature API|feature:|starter\.security' .
rg -n 'feature|application\.jooq|SystemExecution' src codegen/src config .tbls.yml build.gradle.kts
```

- [ ] **J2 — clean状態からbuildする。** 開発アプリを停止し、下記を実行する。src/main／testとcodegenのcompile、非推奨API検査、format、静的解析、DB／UI／認可、Modulith、配布境界が成功する。Dockerはtestの必須実行条件とする。

```bash
./gradlew spotlessApply clean build documentation
```

- [ ] **J3 — 生成内容を読む。** jOOQの対象schema／package、DB資料のtable／COMMENT／ER、APIのpath／schema／status、Markdown linkを確認する。build成功だけでは内容の妥当性は保証されない。
- [ ] **J4 — 案件UI／APIを動かす。** bootRunでhome・login／logout・業務操作・未認証・権限不足・validation・明暗・mobileを確認する。削除したsample URLは使えず、残した公開機能だけ匿名で動くことを確認する。
- [ ] **J5 — testと解析を業務へ移す。** sample専用testの置換後も、commit／rollback、Method Security、system context復元、入力境界、module境界、生成型の参照範囲、配布物へのtest混入禁止を検証できることを確認する。
- [ ] **J6 — クリーンな案件checkoutでも実行する。** Git管理外の必要設定を手順どおり供給し、CI／別開発環境でbuildできることを確認する。再現に必要なsourceと手順はrepositoryで管理する。

### リリース前

- [ ] **J7 — versionと成果物を検証する。** [release手順](operations.md)に沿ってverifyReleaseVersion、build、releaseArtifacts、bootBuildImageを実行し、案件名・version・schema・公開APIが正しいことを確認する。
- [ ] **J8 — 配置先で受け入れる。** 本番相当のJAR／OCIと設定でDB migration、認証・権限、TLS／proxy／session、監視、終了・更新、backup復元を確認する。配置先の接続・動作は当該環境で確認する。

## そのまま利用できる基盤

次は、案件要件が変わらない限り変更する必要はない。名前の追従やsampleへの参照除去は行うが、品質条件は維持する。

- Gradle Wrapper／Toolchain／BOM／Catalog、Spotless、unused import／JavaDoc／final検査、非推奨APIのcompileエラーと抑制禁止。
- feature単位のModulith境界、Command／Query分離、Method Security、transaction、typed Result、UUID v7、Clock／UTC。
- 開発・test・codegen・資料DBの分離、Flyway履歴、生成source非管理、JARからtest／生成toolを除く検査。
- 標準のProblemDetail、CSRF、secret非記録、相関ID、共通HTTP timeout、graceful shutdown。
- 未使用のBatch／Scheduling基盤。不要と判断して取り除く場合だけ、依存・metadata・設定・test・資料を一組で変更する。
- test用credentialと一時DB名`documentation`。用途ごとに隔離された値を使用する。資料DB名を変える必要がある場合はCompose、GradleのJDBC URL、DocumentationMigration、TBLS_DSN、healthcheckを同時に変更する。

## 自動化の選択肢（未採用）

**Bの識別子置換とD6のsample撤去は、手作業だけでは煩雑になりやすい。** directory移動、文字列に埋まったpackage、生成型検査、権限、資料が連動するためである。特に複数案件で繰り返す名称置換は自動化の効果がある。業務仕様・認可・既存DB移行・公開範囲の判断は、自動化しても人が確定する。

以下は採用前の比較案であり、実装は方式の選択後に行う。実装方式を選んだ後、必要なADRを追加して実装する。既存ADR-003の「専用initializerは具体的な必要性に基づいて採用する」という方針を維持する。

| 選択肢 | 自動化する内容 | 追加・保守負担 | 向いている条件／限界 |
|---|---|---|---|
| **A: IDE refactor＋AIによる案件ごとの変更** | IDEでpackage／型の参照を変更し、AIが本書の連動箇所・設定・資料・testを更新。diffをreviewして既存Gradleで検証 | 専用tool不要。案件ごとのreviewは必要 | 案件数が少ない、業務への転用内容が毎回異なる場合。変更結果の確認は案件ごとのreviewで行う |
| **B: Gradleの初期化task** | 一度入力した案件名・group・package・表示名を、対象fileとdirectoryへ決定的に反映。残存識別子の検査と既存buildへ接続 | 初期実装とtemplate更新時の置換対象保守。追加runtime依存は不要な構成を選べる | 同じstarterから繰り返し案件を開始する場合。適用範囲は案件開始時の名称設定とする |
| **C: Copierによるparameter付きtemplate** | 質問への回答でfile／directoryを生成し、記録した回答とtemplate versionを使って更新を取り込む | Copier実行環境、template記法への移行、回答file、更新競合のreviewが必要 | 多数の派生案件へ基盤更新も継続配信したい場合。業務変更との競合やDB migrationは個別のreviewと移行設計で扱う |
| **D: GitHub template＋CLI／APIによるrepository設定** | repository作成と、必要ならアクセス・保護規則・環境設定を選択した設定値で適用 | GitHub権限、組織policy、設定処理の保守が必要 | 外部repository設定も繰り返す場合。source内のpackage・業務名の置換は別処理なのでA／B／Cとの併用候補 |

### 選択の目安

- 少数案件でまず使うなら **A**。専用initializerを保守する負担を持たず、変更対象は本書で管理する。
- 名前の置換を繰り返すなら **B** がこのprojectのGradle中心方針に最も合う候補。
- 派生案件への継続的なtemplate更新まで必要なら **C**。Bより広い目的と保守負担を持つ。
- repository作成・管理設定も揃えたいなら **Dを併用**する。GitHubのtemplate作成だけで名称置換が済むとは扱わない。

### Bを選ぶ場合の具体的な範囲案

入力はprojectName、group、basePackage、toolPackage、applicationName、displayName、version、docsImageName。入力fileの項目は上記の識別情報に限定する。

1. 初期値と入力値から、変更予定file・directory・置換結果をdry-runで出力する。
2. project名／Java packageの妥当性、移動先衝突、対象fileの存在、既存変更を確認する。想定と異なるsourceは保持し、差異の理由を報告する。
3. Bの対応表に限定して置換・移動する。操作対象はrepository内の手書きsourceと設定に限定する。
4. 旧識別子の残存を報告し、generated packageと検査対象を含む既存Gradle検証へ接続する。技術名・ADR履歴等の保持は明示的に区別する。
5. sample削除は初期版の名称置換から分け、D6の連動変更をreview可能な差分として扱う。migrationや権限は業務契約と運用要件に基づいて個別に変更する。

task名・引数・保存形式は採用時に決める。現時点で実行できる初期化commandはない。

### 判断対象と公式資料

GitHubはtemplateからrepositoryを作成でき、CLIの`gh repo create --template`も用意している。[GitHub template](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-repository-from-a-template)、[CLI](https://cli.github.com/manual/gh_repo_create)を参照する。

BはGradle標準のtaskとして実装する案であり、方式の選択後に実装する。[Gradle custom tasks](https://docs.gradle.org/current/userguide/implementing_custom_tasks.html)を参照する。

Copierには質問・回答file・project更新の機構がある。更新時の競合処理を含めて採用判断する。[template設定](https://copier.readthedocs.io/en/latest/configuring/)、[project更新](https://copier.readthedocs.io/en/stable/updating/)を参照する。
