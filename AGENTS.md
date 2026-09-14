# AGENTS.md

このファイルは `spring-application-starter` における **Codex向けの単一の実装指示書** である。

このリポジトリでは、rootの `AGENTS.md` だけを使用する。

nested `AGENTS.md` は作成しない。

設計判断の唯一の正本は:

```text
docs/decisions.md
```

である。

`AGENTS.md`、`README.md`、`docs/architecture.md`、`docs/database.md`、`docs/implementation-plan.md`、実装のいずれかと `docs/decisions.md` が矛盾する場合は、`docs/decisions.md` を優先する。

新しい設計判断を行う場合は、先に `docs/decisions.md` を更新する。

---

# 1. Project Purpose

このリポジトリは、Java / Spring を使用した小規模〜中規模の業務システムを、一人または少人数で高品質かつ高速に構築するための個人用starterである。

このstarterは「最低限動く雛形」ではない。

**業務固有機能だけが存在しない、技術基盤として完成したapplication** を目指す。

速度は品質を犠牲にして得るものではない。

設計判断、品質管理、build、test、database、documentation、CIなどをあらかじめ標準化・自動化することで、案件開始後の開発速度を上げる。

コードは、公開しても恥ずかしくない品質を基準とする。

---

# 2. Priority

設計・実装では原則として以下の順序を優先する。

1. 正しさ
2. 型安全性
3. データ整合性
4. セキュリティ
5. 責務の明確さ
6. 依存方向の明確さ
7. 保守性
8. 単純さ
9. 実装速度

短期的な実装速度を理由に上位原則を崩してはならない。

---

# 3. YAGNI

将来使うかもしれないという理由だけで、以下を先行実装しない。

- abstraction
- framework
- extension point
- wrapper
- generic utility
- infrastructure

ただし、以下をYAGNIの名目で削除してはならない。

- type safety
- transaction safety
- security
- data integrity
- module boundary
- meaningful validation
- meaningful tests
- required error handling
- reproducible build
- reproducible documentation generation

---

# 4. Design Style

重視するもの:

- Javaらしい自然な設計
- 静的型付け
- 不変性
- nullを安易に許容しない設計
- 明確な責務
- 小さなpublic API
- feature単位のmodule boundary
- explicit SQL
- PostgreSQLの適切な利用
- JDK標準機能
- Spring標準機能
- 十分成熟したlibrary
- automationによる品質保証

避けるもの:

- 独自framework
- speculative abstraction
- giant utility
- stringly typed design
- 無意味なwrapper
- 無意味なDTO変換
- 不要なinterface
- 不要な継承
- framework typeのdomainへの流入
- layerを跨ぐだけのexception wrapping
- 「将来必要になるかもしれない」という理由だけの実装

---

# 5. Technology Stack

基本stack:

- Java 25
- Gradle Kotlin DSL
- Gradle Wrapper
- Gradle Java Toolchain
- Gradle Version Catalog
- Spring Boot 4.1.x
- Spring MVC
- Spring Security
- Jakarta Validation
- Spring Batch
- Spring Scheduling
- Spring RestClient
- Spring HTTP Service Client
- Spring Boot Actuator
- Spring Modulith
- Vaadin 25 / Vaadin Flow
- PostgreSQL
- Flyway
- jOOQ
- JUnit
- AssertJ
- Testcontainers
- Spotless
- Checkstyle
- SpotBugs
- JaCoCo
- ArchUnit
- springdoc-openapi
- tbls
- Markdown
- Mermaid
- Pandoc
- LuaLaTeX
- GitHub Actions
- Renovate

---

# 6. Do Not Include by Default

以下はbaselineに含めない。

- Spring Data JPA
- Hibernate
- Redis
- Kafka
- RabbitMQ
- JMS
- Quartz
- Spring Integration
- Mail
- OAuth2
- OIDC
- SAML
- LDAP
- React
- Angular
- Vue
- TypeScript中心のfrontend
- generic master-data import framework
- generic cache framework
- generic retry framework
- generic workflow framework
- giant custom exception hierarchy

必要性が案件で明確になった場合のみ追加する。

---

# 7. Dependency Policy

新しい機能が必要な場合は、以下の順に検討する。

1. JDK標準API
2. Spring標準機能
3. 既に導入済みのlibrary
4. 十分成熟した外部library
5. 独自実装

便利という理由だけでdependencyを増やさない。

既存標準機能で十分なら独自frameworkを作らない。

---

# 8. Version Management

version管理をbuild script各所へ散在させない。

- Java
  → Gradle Java Toolchain
- Gradle
  → Gradle Wrapper
- explicit dependency / plugin
  → Version Catalog
- Spring ecosystem
  → Spring Boot dependency management / BOM
- Vaadin ecosystem
  → Vaadin BOM
- Docker image
  → explicit pinned version
- application version
  → Gradle project version

Docker imageで `latest` を使わない。

Renovateで以下を追跡する。

- Gradle dependencies
- Gradle plugins
- Gradle Wrapper
- Docker images
- GitHub Actions

Spring Boot / Vaadin等のmajor upgradeをblind auto-mergeしない。

---

# 9. Project Structure

完成時の論理構成は以下を基準とする。

```text
spring-application-starter/
├── AGENTS.md
├── README.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── .gitignore
├── .gitattributes
├── .tbls.yml
├── renovate.json
│
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│
├── config/
│   └── checkstyle/
│       └── checkstyle.xml
│
├── docker/
│   ├── compose.yaml
│   └── docs/
│       └── Dockerfile
│
├── docs/
│   ├── decisions.md
│   ├── architecture.md
│   ├── database.md
│   ├── implementation-plan.md
│   ├── project/
│   │   ├── index.md
│   │   └── diagrams/
│   └── database/
│       ├── generated/
│       └── notes/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── dev/template/application/
│   │   │       ├── Application.java
│   │   │       ├── feature/
│   │   │       │   ├── api/
│   │   │       │   │   ├── command/
│   │   │       │   │   └── query/
│   │   │       │   └── internal/
│   │   │       │       ├── command/
│   │   │       │       ├── query/
│   │   │       │       ├── usecase/
│   │   │       │       ├── domain/
│   │   │       │       └── infrastructure/
│   │   │       ├── web/
│   │   │       │   ├── ui/
│   │   │       │   └── rest/
│   │   │       ├── batch/
│   │   │       ├── scheduling/
│   │   │       ├── security/
│   │   │       └── logging/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/
│   │           └── migration/
│   │
│   ├── test/
│   │   └── java/
│   ├── integrationTest/
│   │   └── java/
│   └── architectureTest/
│       └── java/
│
└── .github/
    └── workflows/
        ├── ci.yml
        └── release.yml
```

これは論理構成である。

空directoryを構造維持だけのために大量生成しない。

`build/` はGit管理しない。

主なgenerated output:

```text
build/
├── generated-src/
│   └── jooq/
└── documentation/
    ├── database/
    ├── api/
    └── project/
```

---

# 10. Docker

Docker関連設定は `docker/` に集約する。

```text
docker/
├── compose.yaml
└── docs/
    └── Dockerfile
```

Compose fileは一つだけにする。

```text
docker/compose.yaml
```

用途はprofileで切り替える。

基本profile:

```text
dev
docs
```

## dev

用途:

- development PostgreSQL

persistent named volumeを使用する。

## docs

用途:

- documentation用temporary PostgreSQL
- documentation tool container

## docker/docs/Dockerfile

可能な限り以下を一つのimageへ集約する。

- tbls
- Mermaid CLI
- Pandoc
- LuaLaTeX
- 日本語PDF生成に必要な環境

tool versionは可能な限りpinする。

font fileをrepositoryへ安易にcommitしない。

`compose.docs.yaml` のような用途別Compose fileを追加しない。

application image用独自Dockerfileはbaselineでは作らない。

application container imageはSpring Boot Buildpacksを第一選択とする。

---

# 11. Development Environment

Dev Containerはbaselineでは使用しない。

hostに必要なもの:

- JDK 25
- Git
- Docker
- editor / IDE

global Gradleは不要。

Gradle Wrapperを使う。

applicationはhost JVMで実行する。

Dockerは主に以下に使う。

- development PostgreSQL
- jOOQ code generation DB
- documentation DB
- documentation toolchain

Vaadinが必要とするNode等は、可能な限りVaadin / build tooling側で管理する。

developerへglobal Node/npm管理を要求しない方向を優先する。

---

# 12. Gradle as the Unified Command Surface

Gradleをdeveloper向けの単一command surfaceとする。

developerが通常直接操作しなくてよいようにする対象:

- Docker Compose
- Flyway CLI
- jOOQ CLI
- tbls
- Mermaid CLI
- Pandoc
- npm

developerが主に覚えるcommand:

```bash
./gradlew bootRun
./gradlew test
./gradlew check
./gradlew documentation
./gradlew build
```

---

# 13. Project Initialization

starterでは仮base packageを使用する。

```text
dev.template.application
```

案件開始時に以下を置換する。

- project name
- Gradle group
- base package
- application name

独自initializer frameworkは現時点では作らない。

Codexによる一括置換で十分ならそれを使う。

initializer scriptは必要性が明確になった場合のみ追加する。

---

# 14. Architecture

基本architecture:

```text
Feature-oriented Modular Monolith
+
Command / Query separation
+
Ports and Adapters where meaningful
```

layerだけでapplication全体を分割しない。

starterではliteral name `feature` の最小sampleを使う。

架空business domainを大量に作らない。

---

# 15. Spring Modulith

Spring Modulithをmodule boundaryの標準機構とする。

feature public API:

```text
feature.api.command
feature.api.query
```

必要な場合のみ:

```text
feature.api.event
```

を追加する。

`api.event` は全featureへ常設しない。

公開packageはNamed Interfaceとして扱う。

`package-info.java` + `@NamedInterface` を基本候補とする。

許可:

```text
order.internal
→ inventory.api.command
```

禁止:

```text
order.internal
→ inventory.internal
```

module cycleは禁止。

Spring Modulith `verify()` をmodule boundary検証のsource of truthとする。

Spring Modulithで表現できるruleをArchUnitへ重複実装しない。

---

# 16. Package Visibility

`public` は本当に外部公開が必要な型だけに使用する。

- feature public API
  → public
- internal implementation
  → package-private第一選択
- protected
  → 継承を明確に意図する場合のみ

Spring Beanだからという理由だけでpublicにしない。

---

# 17. Command Architecture

基本flow:

```text
feature.api.command
        ↓
feature.internal.command
        ↓
feature.internal.usecase
        ↓
feature.internal.domain
        ↓
Repository interface
        ↑
feature.internal.infrastructure
```

## internal.command

責務:

- public Command API implementation
- transaction boundary
- input adaptation
- usecase invocation
- Result返却

business logicを大量に置かない。

## internal.usecase

責務:

- business operation orchestration
- domain coordination
- Repository port定義

Repository interfaceは:

```text
feature.internal.usecase
```

へ置く。

usecaseはtransaction frameworkを認識しない。

Spring Security型を認識しない。

## internal.domain

責務:

- Entity
- Value Object
- domain rule
- invariant

以下へ依存しない。

- Vaadin
- Spring MVC
- SecurityContext
- jOOQ generated classes

## internal.infrastructure

責務:

- jOOQ Repository implementation
- external HTTP implementation
- storage implementation
- other technical adapter

business decisionを置かない。

---

# 18. Query Architecture

基本flow:

```text
feature.api.query
        ↓
feature.internal.query
        ↓
DataSource interface
        ↑
feature.internal.infrastructure
        ↓
jOOQ / PostgreSQL
```

Query pathは意図的にusecase/domainを通さない。

Queryはread modelとSQL最適化を優先する。

Query DTOはdomain entityから独立してよい。

DataSource interface:

```text
feature.internal.query
```

implementation:

```text
feature.internal.infrastructure
```

Commandはdomain-oriented。

QueryはSQL/read-model-oriented。

この非対称性は意図的な設計である。

---

# 19. Transactions

Command transaction boundary:

```text
feature.internal.command
```

```java
@Transactional
```

Query transaction boundary:

```text
feature.internal.query
```

```java
@Transactional(readOnly = true)
```

usecase/domainへ `@Transactional` を置かない。

cross-feature updateは、親Commandから対象featureのpublic Command APIを呼ぶ。

他feature schemaへの直接:

- INSERT
- UPDATE
- DELETE

は禁止。

default propagationは `REQUIRED`。

`REQUIRES_NEW` は明確な理由がある場合のみ。

transactional self invocationへ依存しない。

---

# 20. Result / Failure / Exception

期待されるbusiness failureはtyped Resultで表現する。

概念:

```text
Result<S,F>
├── Success<S,F>
└── Failure<S,F>
```

variant名は:

```text
Success
Failure
```

Failure reasonは型付きにする。

message stringだけでbusiness failureを表現しない。

Commandは原則Resultを返す。

Queryはexpected failureが意味を持つ場合のみResultを使う。

単純list/searchは値を直接返してよい。

分類:

```text
expected business outcome
→ Failure

unexpected technical failure
→ Exception

impossible internal state
→ Exception
```

既存Spring/jOOQ exceptionが十分意味を持つ場合は無意味にwrapしない。

custom exceptionは意味を追加する場合のみ作る。

layerを跨ぐだけのcustom exceptionは作らない。

重要:

`@Transactional` method内部でExceptionをcatchしてFailureを返すことでpartial commitを起こしてはならない。

Exception → Failure変換はrollback semanticsを壊さない位置で行う。

adapter handling:

- REST Failure
  → HTTP status / ProblemDetail
- Vaadin Failure
  → user-level handling
- Batch Failure
  → job semanticsに応じた終了状態
- unexpected Batch Exception
  → Job failure
- unexpected Web Exception
  → common error handler + logging

---

# 21. ID

IDはUUID v7を標準とする。

PostgreSQL:

```text
uuid
```

Java:

```text
java.util.UUID
```

domainでは必要に応じてfeature-specific Value Objectで包む。

例:

```java
record FeatureId(UUID value) {}
```

Query DTOではraw UUIDを使ってよい。

IDはapplication/domain側で生成する。

DB auto incrementをbaselineにしない。

自table PK:

```text
id
```

reference:

```text
<target>_id
```

UUID v7の具体実装は実装時点のJava 25標準APIを確認する。

標準で不足する場合は成熟libraryを優先する。

独自generatorを安易に作らない。

---

# 22. Date and Time

real instant:

```text
Java: Instant
PostgreSQL: timestamptz
```

business date:

```text
Java: LocalDate
PostgreSQL: date
```

time only:

```text
Java: LocalTime
PostgreSQL: time
```

regional calculation:

```text
ZonedDateTime + ZoneId
```

external offset timestamp:

```text
OffsetDateTime
```

必要に応じて内部でInstantへ変換する。

実時刻表現に `LocalDateTime` を使わない。

内部基準はUTC。

current timeはinjectable `Clock` を使う。

production:

```java
Clock.systemUTC()
```

test:

```java
Clock.fixed(...)
```

OS/system default timezoneへ依存しない。

---

# 23. PostgreSQL Schema Ownership

原則としてfeatureごとにPostgreSQL schemaを所有する。

例:

```text
Java:
inventory

PostgreSQL:
inventory
```

他feature schemaへの直接writeは禁止。

cross-feature updateは対象featureのpublic Command APIを通す。

cross-feature readはDB JOINを禁止しない。

一覧・検索・report等でDB JOINが適切ならDBで行う。

architecture上の形式美だけを理由にJava側へ分解しない。

cross-feature queryの具体構造は必要になるまで固定しない。

候補:

- multi-schema JOIN
- View
- Materialized View
- projection table
- dedicated cross-feature query module

technical schemaは必要時のみ作る。

例:

```text
system
```

用途候補:

- Spring Batch metadata
- Spring Modulith event publication metadata

---

# 24. Database Naming

PostgreSQL identifierはsnake_case。

constraint/index naming:

```text
PK    pk_<table>
FK    fk_<table>_<referenced_table>
UK    uk_<table>_<column...>
Index ix_<table>_<column...>
Check ck_<table>_<purpose>
```

own PK:

```text
id
```

reference column:

```text
<target>_id
```

defaultは `NOT NULL`。

absence自体に意味がある場合のみnullable。

---

# 25. Database Types

money / decimal:

```text
PostgreSQL: numeric(p,s)
Java: BigDecimal
```

moneyへfloat/doubleを使用しない。

bounded semantic string:

```text
varchar(n)
```

unbounded description:

```text
text
```

Jakarta ValidationのlengthとDB constraintを整合させる。

booleanは本当に二値しか存在しない概念にのみ使用する。

将来状態が増え得るものを安易にboolean化しない。

---

# 26. Audit and Delete

audit column standard names:

```text
created_at
updated_at
created_by
updated_by
```

全tableへ機械的に追加しない。

必要なtableだけ使用する。

audit actor assignmentの具体方式は案件依存。

default deleteはphysical delete。

以下を自動追加しない。

```text
deleted
deleted_at
is_deleted
```

soft deleteは明確な要件がある場合のみ。

履歴要件は履歴modelとして設計し、soft deleteで代用しない。

---

# 27. Foreign Keys

同一feature内ではDB constraintを積極的に利用する。

cross-feature FKは案件依存。

module autonomyとdata integrityを比較して判断する。

UUID referenceのみを保持する設計も許容する。

---

# 28. Flyway

Flyway migrationをschema変更履歴のsource of truthとする。

適用済migrationを安易に編集しない。

変更は新migrationとして追加する。

developmentの基本flow:

```text
./gradlew bootRun
→ development PostgreSQL
→ Flyway migrate
→ application start
```

Spring Boot Docker Compose integrationを利用する方向。

productionも原則application startup時にmigrationする。

ただし運用要件によりmigrationだけ先に実行する方式を許容する。

migration directoryのfeature別構成は、実装時点のFlyway公式仕様を確認して決める。

独自migration loaderを作らない。

Flyway history tableの配置は未確定。

common history tableを第一候補とするが、実装前に確定する。

---

# 29. Sample and Master Data

starterの起動・動作確認に必要な最小限のsample/system dataはFlywayで登録してよい。

generic business master-data import frameworkは作らない。

以下のようなbusiness masterは案件固有。

- 商品
- 顧客
- 倉庫

大量・高頻度更新のmaster dataをFlyway標準管理しない。

---

# 30. Database Environments

以下4用途を明確に分離する。

## Development DB

Docker Compose PostgreSQL。

persistent named volume。

## Test DB

Testcontainers PostgreSQL。

isolated / ephemeral。

## jOOQ Codegen DB

temporary PostgreSQL。

```text
empty DB
→ Flyway
→ jOOQ code generation
→ destroy
```

development DBを使わない。

## Documentation DB

temporary PostgreSQL。

```text
empty DB
→ Flyway
→ tbls
→ destroy
```

development DBを使わない。

---

# 31. jOOQ

JPA/Hibernateは使わない。

SQL accessはjOOQを標準とする。

generation flow:

```text
temporary empty PostgreSQL
        ↓
Flyway migrate
        ↓
jOOQ code generation
        ↓
build/generated-src/jooq/
        ↓
compileJava
        ↓
temporary DB destroy
```

generated packageはschema単位で明確に分離する。

具体package名は実装時にbase packageとの整合を見て決める。

generated sourceはGitへcommitしない。

jOOQ generated codeは原則:

```text
feature.internal.infrastructure
```

からのみ参照する。

必要ならArchUnitで検証する。

Gradle inputs/outputsを設定し、migrationに変更がない場合の不要なcode generationを避ける。

---

# 32. Database Documentation

DB documentationはFlyway SQLの文字列解析から生成しない。

実際にmigrationされたPostgreSQL schemaをsourceとする。

flow:

```text
temporary PostgreSQL
        ↓
Flyway migrate
        ↓
tbls
        ↓
Markdown / ER diagram
        ↓
temporary DB destroy
```

table/column descriptionはPostgreSQL COMMENTを活用する。

```sql
COMMENT ON TABLE ...;
COMMENT ON COLUMN ...;
```

tbls config:

```text
.tbls.yml
```

manual notes:

```text
docs/database/notes/
```

reviewable generated docs候補:

```text
docs/database/generated/
```

このdirectoryをGit管理するかはrepository policyとして未確定。

build artifact:

```text
build/documentation/database/
```

generated fileを手編集しない。

ER diagramはSVGを優先する。

---

# 33. REST

REST adapter:

```text
web.rest
```

Controllerはfeature public APIのみ呼ぶ。

禁止:

```text
web.rest
→ feature.internal
```

REST-specific request/response DTOは `web.rest` に置いてよい。

HTTP concernをfeature API/domainへ漏らさない。

URLはnoun-oriented / plural。

例:

```text
GET    /api/v1/features
GET    /api/v1/features/{id}
POST   /api/v1/features
```

non-CRUD domain operationは必要に応じてaction subresourceを使う。

例:

```text
POST /api/v1/orders/{id}/cancel
```

---

# 34. REST Errors

基本status:

```text
200 success
201 created
204 no content
400 malformed / validation
401 unauthenticated
403 unauthorized
404 not found
409 conflict
422 semantic business rejection
500 unexpected error
```

REST errorはSpring `ProblemDetail` を標準とする。

global `ApiResponse<T>` のようなresponse wrapperを作らない。

feature Failureはweb adapterでHTTP statusへ変換する。

unexpected Exceptionはcommon handlerで500へ変換する。

必要な場合だけstable machine-readable error codeを追加する。

JSONはSpring/Jackson defaultsを基本とする。

- camelCase
- ISO-8601
- unnecessary custom serializerを避ける

---

# 35. REST API Versioning

major versionをURL pathで表現する。

```text
/api/v1
/api/v2
```

breaking changeだけmajor bump。

compatible additionではversionを増やさない。

future v2 packageを先行作成しない。

Spring MVC標準API versioningを優先する。

custom version resolverを作らない。

deprecation / sunsetも可能な限りSpring / HTTP標準mechanismを優先する。

具体APIは実装時点の公式仕様を確認する。

---

# 36. Pagination and Sort

pagination/sortはREST固有ではなくQuery APIのtyped conceptとして扱う。

REST/Vaadinはadapter。

案件ごとの候補:

- offset / limit
- Slice
- Page
- keyset / seek

小〜中規模業務システムではoffset/limitを第一候補にできるが、starterで強制しない。

total countが不要ならCOUNT(*)を必須にしない。

total/page countが必要な場合だけPage的modelを使う。

deep pagination / large datasetではjOOQ seek/keysetを検討する。

sortは型安全にする。

外部から受け取った任意文字列をそのままDB columnへ渡さない。

query-specific enum等へ変換する。

pagination目的だけでSpring Data `Pageable` / `Sort` を導入しない。

---

# 37. Vaadin

Vaadin Viewは:

```text
web.ui
```

に配置する。

Vaadin Viewはfeature public APIのみ呼ぶ。

feature internalへ直接依存しない。

Vaadin-specific typeをdomain/usecaseへ持ち込まない。

不要なfrontend ecosystemを追加しない。

---

# 38. Security

Spring Securityをbaselineに含める。

authentication mechanism自体は案件依存。

starterでは以下を固定しない。

- local authentication
- OIDC
- SAML
- LDAP

security-specific implementationは:

```text
security
```

へ隔離する。

Method Securityをbusiness authorizationの中心とする。

`@EnableMethodSecurity` を使用する。

認可の主な境界:

```text
internal.command
internal.query
```

web route securityはcoarse-grained entry restriction。

business operationの本認可はMethod Security。

---

# 39. Authority

RoleよりAuthority中心を基本とする。

例:

```text
inventory:read
inventory:write
order:read
order:write
```

RoleはAuthorityの集合として扱う。

Authority stringを各所へ散在させない。

feature単位の定義を許容する。

全featureのAuthorityを巨大global classへ集約しない。

---

# 40. SecurityContext Boundary

domain/usecaseから以下を直接使用しない。

- `SecurityContextHolder`
- `Authentication`
- `GrantedAuthority`

current userがbusiness上必要な場合はsecurity layerでapplication-friendly typeへ変換する。

例:

```text
Spring Authentication
        ↓
security
        ↓
CurrentUser / Actor / UserId
        ↓
Command
```

Batch/Scheduling用にfake Authenticationを作らない。

必要ならbusiness Actor型を設計する。

---

# 41. REST / Vaadin / Actuator Security

Vaadin route restrictionやREST URL securityはcoarse-grained restrictionとして使用する。

UIを隠すだけでsecurityを成立させない。

Command/Query boundaryでも認可する。

REST:

```text
unauthenticated → 401
unauthorized    → 403
```

Actuatorは公開を最小限にする。

healthは必要に応じて匿名公開可。

その他endpointは認証またはnetwork restriction等で保護する。

---

# 42. External HTTP Client

優先順位:

```text
1. HTTP Service Client
2. RestClient
3. WebClient
```

WebClientはreactive / streamingが本当に必要な場合のみ。

RestTemplateは新規採用しない。

Spring Boot標準のHTTP Service Group機能が利用可能なら優先する。

独自HTTP client frameworkを作らない。

---

# 43. External HTTP Boundary

外部API DTOは:

```text
feature.internal.infrastructure
```

へ閉じる。

external DTOをそのまま:

- domain
- usecase
- feature public API

へ流さない。

business側は外部HTTP APIそのものではなく、自身が必要とするPortへ依存する。

infrastructureがHTTP clientでそのPortを実装する。

---

# 44. External HTTP Timeouts and Retry

timeoutは明示する。

最低限:

- connect timeout
- read timeout

値はConfigurationProperties / environment variableから設定する。

retryはdefaultでは行わない。

特に以下を機械的にretryしない。

- POST
- PUT
- PATCH

idempotency保証とfailure semanticsが明確な場合だけ個別に検討する。

---

# 45. External HTTP Errors

business上意味のあるexpected external response:

```text
→ Failureへ変換可
```

network failure:

```text
→ Exception
```

timeout:

```text
→ Exception
```

5xx:

```text
→ Exception
```

unexpected protocol error:

```text
→ Exception
```

既存Spring/HTTP exceptionが十分意味を持つ場合は無意味にwrapしない。

API key / bearer token等はclient configuration側で扱う。

usecase/domainへcredentialを持ち込まない。

---

# 46. External HTTP Logging

必要な場合は横断的に以下を記録する。

- target service
- operation
- HTTP status
- duration
- correlation ID
- exception

request / response body全文を標準logにしない。

MDC correlation IDを必要に応じoutgoing HTTPへ伝播する。

具体header名は案件仕様に応じる。

---

# 47. Logging

通常のbusiness codeにlogger callを書かないことを基本とする。

Loggingはcross-cutting concern。

top-level:

```text
logging
```

主に:

- AOP
- filter
- interceptor

を使う。

対象:

- Command start/end/result/exception
- Query start/end/exception
- incoming HTTP request
- outgoing HTTP request
- Batch execution
- correlation ID

推奨level:

```text
Command start/end → INFO
Query start/end   → DEBUG
expected Failure  → INFO基本
unexpected error  → ERROR
```

WARNは意味上warningの場合のみ。

---

# 48. Logging Security

以下をlogしない。

- password
- API token
- access token
- secret
- credential
- unnecessary PII

request / response objectを無条件に全文dumpしない。

external API bodyも標準では全文dumpしない。

correlation IDはMDCで管理する。

package-based pointcutを第一候補とする。

不要なmarker annotation frameworkを先に作らない。

business codeからのdirect logger callは絶対禁止ではない。

ただしAOPでは意味を表現できないtechnical eventに限定する。

---

# 49. Configuration

environment-dependent valueはenvironment variableから注入する。

`application.yml` には以下を置いてよい。

- stable configuration
- environment variable mapping
- safe default

secretは書かない。

secretをrepositoryへcommitしない。

JAR実行でもDocker実行でも同じenvironment-based configuration modelを使う。

custom configurationは:

```java
@ConfigurationProperties
@Validated
record ...
```

を第一選択とする。

scattered `@Value` を避ける。

適切な型を使う。

例:

- Duration
- URI
- int
- boolean

必須設定が不足している場合はstartup failureとする。

configuration validationのtestも用意する。

安全でないdefaultを入れない。

environment別YAMLを大量に複製しない。

---

# 50. HikariCP

Spring Boot標準のHikariCPを使う。

starterでは過度にtuningしない。

pool sizeやtimeoutは案件ごとに決める。

接続数を増やせば速くなるとは考えない。

Actuator/Micrometerでpool状態を観測可能にする。

---

# 51. File I/O

file upload/downloadはweb adapter concernとする。

business APIへtechnical/framework-specific typeを不用意に漏らさない。

例:

- `MultipartFile`
- `HttpServletResponse`
- Vaadin `StreamResource`
- `Path`
- raw `InputStream`

persistent storageはPort経由。

implementationはinfrastructure。

storage implementationは案件依存。

候補:

- local filesystem
- S3-compatible storage

---

# 52. File Safety

user-provided filenameを保存pathとして信用しない。

内部保存名はUUID等で生成する。

必要に応じ以下を検証する。

- size
- MIME type
- extension
- content

path traversalを許さない。

temporary fileは適切なtemp directoryを使用する。

処理後に削除する。

大容量fileを不要に全内容memoryへ読み込まない。

CSV baseline:

- UTF-8
- comma separated
- quotingを正しく扱う
- LF / CRLF双方を受容

独自CSV parserを作らない。

必要ならApache Commons CSV等の成熟libraryを使う。

Excel / business PDFは案件要件がある場合だけ追加する。

---

# 53. Cache

starter baselineではcacheを導入しない。

最初から以下を追加しない。

- Spring Cache
- Caffeine
- Redis

performanceをmeasurementした後で必要箇所へ導入する。

cache invalidation / consistency complexityを安易に増やさない。

---

# 54. Events

feature間の通常communicationは同期public APIを基本とする。

```text
public Command / Query API
```

時間的に分離できる副作用ではSpring Modulith Eventを使用可能。

即時結果が必要な処理をeventへ逃がさない。

例:

```text
在庫引当成功が注文確定条件
→ synchronous Command API
```

一方:

```text
注文確定後の通知
→ event候補
```

疎結合という理由だけで全処理をevent化しない。

`api.event` は必要なfeatureだけに作る。

---

# 55. Async

defaultは同期。

必要性がある処理だけ非同期化する。

非同期化する場合は必ず以下を考慮する。

- transaction boundary
- failure handling
- retry
- ordering
- duplicate execution
- recovery

---

# 56. Mail and Notification

Mail dependencyはbaselineに含めない。

必要時はnotification feature/module + Portという形を検討する。

business featureからtransport implementationを直接呼ばない。

例:

- JavaMailSender
- SMS client
- Slack client
- Teams client

generic notification frameworkを先に作らない。

---

# 57. Batch

Spring Batchは利用可能なbaseline technology。

Batchはtop-level adapter。

```text
batch
```

Batchからfeature internalへ直接依存しない。

```text
batch
→ feature public API
```

以下が必要な処理でSpring Batchを使う。

- restartability
- step
- chunk
- job history

意味のないsample Jobを作らない。

web application startup時に全Jobを自動実行しない。

Batch semantics:

```text
unexpected Exception
→ Job failure

expected Failure
→ job use caseの意味に応じて終了状態へmapping
```

---

# 58. Scheduling

Schedulingはtop-level adapter。

```text
scheduling
```

単純な定期処理は:

```java
@Scheduled
```

を使う。

Schedulingからfeature internalへ直接依存しない。

```text
scheduling
→ feature public API
```

Quartzはbaselineに含めない。

---

# 59. Testing

## Unit Test

対象:

- domain
- usecase

Spring contextを起動しない。

JUnit + AssertJ。

fake/mockは必要最小限。

mock-heavy designを避ける。

## Integration Test

PostgreSQL Testcontainersを使う。

対象:

- Repository
- Query DataSource
- Flyway
- jOOQ
- DB constraint
- sorting
- pagination
- joins
- null/boundary behavior

H2は使わない。

本番と同じFlyway migrationを適用する。

## Architecture Test

Spring Modulith + ArchUnit。

Spring Modulith:

- module boundary
- cycle
- allowed dependency

ArchUnit:

Spring Modulithで表現しにくいtechnical rulesだけ。

例:

```text
web → jOOQ 禁止
domain → Vaadin 禁止
domain → Spring MVC 禁止
security → feature.internal 禁止
generated jOOQ → infrastructure以外から参照禁止
```

## REST Test

必要に応じMockMvc。

確認対象:

- validation
- status
- ProblemDetail
- API version
- JSON

## Vaadin E2E

重要flowだけ。

## SpringBootTest

wiring/context/security等の少数smoke testに限定。

---

# 60. Test Source Sets

```text
src/test/java
src/integrationTest/java
src/architectureTest/java
```

Gradle tasks:

```text
test
integrationTest
architectureTest
```

class naming:

```text
FooTest
FooIntegrationTest
ArchitectureTest
```

---

# 61. Code Quality

初期段階から以下を有効にする。

- Spotless
- Checkstyle
- SpotBugs
- JaCoCo
- Spring Modulith verification
- ArchUnit

quality gate:

```bash
./gradlew check
```

Checkstyle ruleはJavaを不自然にするほど細かくしない。

JaCoCoはcoverage visibilityのために使う。

coverage percentageを目的化しない。

coverageのためだけのmeaningless testを書かない。

---

# 62. Gradle Tasks

主要tasks:

```text
bootRun
test
integrationTest
architectureTest
check
jooqCodegen
databaseDocumentation
apiDocumentation
projectDocumentation
documentation
build
bootJar
bootBuildImage
```

`check` は少なくとも以下を含める。

- Spotless check
- Checkstyle
- SpotBugs
- unit tests
- integration tests
- architecture tests
- relevant JaCoCo tasks

normal `build` にheavy documentation generationを必須化しない。

必要になれば将来 `releaseArtifacts` 等のaggregate taskを追加してよい。

巨大なcustom release frameworkは作らない。

---

# 63. Project Documentation

human-authored documentationはMarkdownをsource of truthとする。

diagramはMermaidを基本とする。

Mermaid fenced blockをPandocへ直接渡して自動renderされることを期待しない。

PDF生成前にMermaidをSVG等へrenderする。

project documentation source:

```text
docs/project/
```

project documentation output:

```text
build/documentation/project/
```

PDF:

```text
Pandoc + LuaLaTeX
```

日本語を再現可能に生成する。

---

# 64. Documentation Tasks

```text
databaseDocumentation
→ temporary PostgreSQL
→ Flyway
→ tbls

apiDocumentation
→ OpenAPI generation

projectDocumentation
→ Markdown
→ Mermaid → SVG
→ Pandoc / LuaLaTeX
→ PDF
```

aggregate:

```text
documentation
├── databaseDocumentation
├── apiDocumentation
└── projectDocumentation
```

developer command:

```bash
./gradlew documentation
```

---

# 65. OpenAPI

REST documentationはspringdoc-openapiを使う。

Controller / Validation metadataを活用する。

annotation soupを避ける。

Swagger UIはdev/localで利用可。

productionでは無条件公開しない。

API versionごとにoutputを分離可能にする。

例:

```text
build/documentation/api/v1/openapi.yaml
```

Spring REST Docsはbaselineに含めない。

---

# 66. Actuator and Observability

baseline:

- health
- info
- metrics

MicrometerはSpring Boot標準を利用する。

productionでは必要なActuator endpointだけ公開する。

以下は案件依存。

- Prometheus
- OpenTelemetry
- tracing backend
- structured JSON logging

---

# 67. Graceful Shutdown

Spring Boot標準graceful shutdownを使用する。

独自shutdown frameworkを作らない。

deployment/restart時にin-flight requestを不要に切断しない設計を優先する。

---

# 68. Release Artifact

applicationのcanonical artifactはSpring Boot executable JAR。

```bash
./gradlew bootJar
```

OCI image:

```bash
./gradlew bootBuildImage
```

application image用独自Dockerfileはbaselineでは作らない。

Spring Boot Buildpacksを第一選択。

---

# 69. Versioning and Release

application versionはGradle project versionを正本とする。

SemVerを基本とする。

例:

```text
1.0.0
1.1.0
2.0.0
```

Git tag:

```text
v1.0.0
```

application versionとREST API versionは別物。

例:

```text
Application: 1.7.3
REST API:    v1
```

---

# 70. Deployment

deployment platformは案件依存。

候補:

- JAR + systemd
- Docker
- ECS
- Kubernetes
- other platform

production Docker Composeをstarter標準にしない。

development Composeとproduction deployment定義を混在させない。

JAR実行でもDocker実行でも同じenvironment-based configuration modelを使う。

---

# 71. GitHub Actions

Gradleをbuild logicのsource of truthとする。

GitHub Actionsへbuild logicを複製しない。

CI basic flow:

```text
checkout
→ JDK 25
→ Gradle
→ ./gradlew check
```

release workflowはGit tagをtriggerに実行可能とする。

用途例:

- build
- check
- bootJar
- documentation
- bootBuildImage
- GitHub Release

GitHub Releaseへ必要に応じて添付する。

- executable JAR
- OpenAPI
- DB documentation
- project documentation PDF

---

# 72. README

READMEは利用者向け入口。

最低限記載する。

- starter purpose
- prerequisites
- project initialization
- project structure概要
- main commands
- bootRun
- check
- documentation generation
- build / release概要

詳細ruleをREADMEへ重複させすぎない。

---

# 73. docs Roles

```text
docs/decisions.md
→ 設計判断の唯一の正本

docs/architecture.md
→ architecture詳細

docs/database.md
→ database / Flyway / jOOQ / DB documentation詳細

docs/implementation-plan.md
→ phase単位の実装順序

docs/project/
→ 人間向けproject documentation source

docs/database/generated/
→ review可能なgenerated DB docs候補

docs/database/notes/
→ manual database notes
```

---

# 74. Codex Workflow

変更前に必ず:

1. `AGENTS.md` を読む。
2. `docs/decisions.md` を読む。
3. relevant docsを読む。
4. current repositoryを確認する。
5. existing implementationとの整合性を確認する。
6. 指定scopeだけ実装する。

変更後:

1. formatter
2. relevant tests
3. static analysis
4. 必要なら `./gradlew check`
5. failure修正
6. changed files確認
7. implementation summary
8. validation result報告

`docs/implementation-plan.md` の複数phaseを勝手に先行実装しない。

指定phaseが終わったら停止する。

---

# 75. Ponytail

Ponytailはgeneral YAGNI / simplification reviewとして使う。

project-specific ruleのsource of truthではない。

source of truth:

```text
docs/decisions.md
AGENTS.md
docs/
```

非自明な変更ではPonytail reviewを使ってよい。

削減対象:

- speculative abstraction
- unused extension point
- redundant wrapper
- unnecessary DTO
- unused configuration
- needless indirection

削減してはならない:

- type safety
- security
- transaction safety
- data integrity
- module boundary
- meaningful tests
- necessary validation
- operational safety

---

# 76. Project-Specific / Deferred Decisions

以下はstarterで過度に固定しない。

- authentication mechanism
- exact cross-feature query structure
- exact pagination方式
- total countの有無
- audit actor assignment
- cross-feature FK
- cache implementation
- retry policy
- file storage implementation
- mail / notification transport
- Hikari pool size
- observability backend
- Spring Batch metadata schema placement
- Spring Modulith metadata schema placement
- production deployment platform
- exact project initialization automation
- `docs/database/generated/` のGit管理有無
- exact Flyway history table placement
- exact Flyway migration directory layout
- exact UUID v7 implementation

---

# 77. Verify Official Documentation at Implementation Time

以下はversion依存性が高いため、実装時点の公式documentationを確認する。

- Spring Boot / Spring Framework APIs
- Spring MVC API Versioning
- API deprecation / sunset support
- HTTP Service Client
- HTTP Service Groups
- Vaadin 25 Node/build management
- Java 25 UUID v7 support
- Flyway migration locations
- Flyway history behavior
- jOOQ Gradle integration
- springdoc + Spring Boot 4
- Spring Modulith event publication metadata
- Spring Batch metadata initialization
- Spring Boot Docker Compose integration
- `bootBuildImage`

古いblog記事や過去versionの設定をblind copyしない。

---

# 78. Explicitly Prohibited Practices

以下を行わない。

- nested `AGENTS.md`
- feature internalへのcross-module直接依存
- webからjOOQ直接利用
- domainからVaadin利用
- domainからSpring MVC利用
- domain/usecaseからSecurityContext参照
- 他feature schemaへの直接update
- H2をPostgreSQL integration test代替に利用
- generated jOOQ sourceのGit commit
- development DBをjOOQ codegen sourceに利用
- development DBをDB documentation sourceに利用
- Docker `latest`
- Compose fileの用途別乱立
- `compose.docs.yaml`
- application Dockerfile baseline
- RestTemplate新規利用
- mutation HTTPの無条件retry
- user filenameを保存pathとして利用
- secret / token / password logging
- request/response全文dump
- logger callのbusiness codeへの散在
- giant custom API response wrapper
- giant custom exception hierarchy
- generic cache導入
- generic event-driven architecture化
- async by default
- meaningless sample Batch Job
- all Batch Job auto-run on application startup
- unnecessary frontend toolchain
- Spring Data PageableだけのためのSpring Data導入
- soft delete default
- production deployment方式のstarter固定
- production Compose baseline
- normal `build` へのheavy documentation generation強制
- speculative abstraction
- frameworkの再発明

---

# 79. Decision Checklist

新しい仕組みを追加する前に確認する。

1. 今本当に必要か。
2. JDKで解決できないか。
3. Spring標準機能で解決できないか。
4. 既存dependencyで解決できないか。
5. 成熟したlibraryが存在しないか。
6. module boundaryを壊さないか。
7. type safetyを失わないか。
8. transaction semanticsを壊さないか。
9. DB integrityを壊さないか。
10. securityを弱めないか。
11. operational complexityを増やす価値があるか。
12. `docs/decisions.md` の既存Decisionと矛盾しないか。

明確な利点がなければ、より単純な設計を選ぶ。

---

# 80. Starter Definition of Done

業務固有機能を除き、starter完成時に以下が成立していること。

- Java 25 / Gradle buildが再現可能
- Gradle Wrapperが利用可能
- Java Toolchainが設定されている
- Version Catalogでversion管理されている
- Spring Boot applicationが起動可能
- Vaadinが利用可能
- REST foundationが利用可能
- Spring Security foundationが利用可能
- PostgreSQL development environmentが利用可能
- Docker関連設定が `docker/` に集約されている
- Compose fileが一つ
- `dev` / `docs` profilesが利用可能
- `docker/docs/Dockerfile` でdocumentation toolchainを再現可能
- Flyway migrationが動作
- development/test/codegen/documentation DBが分離されている
- jOOQ code generationが再現可能
- generated sourceをGit管理していない
- Spring Modulith boundaryを検証可能
- Unit / Integration / Architecture Testが分離されている
- PostgreSQL Testcontainersが動作
- Spotlessが動作
- Checkstyleが動作
- SpotBugsが動作
- JaCoCo reportを生成可能
- `./gradlew check` がquality gateとして機能
- logging / MDC correlation ID foundationが存在
- Actuatorが利用可能
- Hikari metricsが観測可能
- HTTP Service Client foundationが利用可能
- ConfigurationPropertiesが型安全かつvalidation可能
- configuration不足をstartup testで検出可能
- graceful shutdownが有効
- file I/O policyが実装可能
- Batchを必要時に利用可能
- Schedulingを必要時に利用可能
- DB documentationを生成可能
- OpenAPI documentationを生成可能
- Markdown / Mermaid / Pandoc / LuaLaTeXでproject PDFを生成可能
- `./gradlew documentation` でdocumentation一式を生成可能
- executable JARを生成可能
- OCI imageを生成可能
- GitHub Actions CIが動作
- Renovateがdependency updateを検出可能
- release foundationが存在
- 不要なbusiness sampleが存在しない
- speculative abstractionが存在しない

この状態を、業務機能実装開始前の標準状態とする。
