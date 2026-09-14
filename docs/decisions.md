# Decision Ledger

この文書は `spring-application-starter` における設計判断の**唯一の正本（Source of Truth）**である。

`AGENTS.md`、`README.md`、`docs/architecture.md`、`docs/database.md`、`docs/implementation-plan.md` その他の設計資料は、この文書と矛盾してはならない。

設計変更が発生した場合は、必ず次の順序で反映する。

1. `docs/decisions.md` を更新する。
2. 影響を受ける派生ドキュメントを更新する。
3. 必要なら実装を変更する。
4. テスト・静的解析・ドキュメント生成で整合性を確認する。

派生ドキュメントだけを修正して、新しい設計判断を確定してはならない。

---

# 1. Status

各Decisionは以下のStatusを持つ。

- **Fixed**
  starterの標準として決定済み。

- **Project-specific**
  案件ごとに決定する。

- **Deferred**
  必要性は認識しているが、実際に必要になるまで確定しない。

- **Verify-on-implementation**
  方針は決まっているが、具体APIや設定方法を実装時点の公式仕様で確認する。

- **Rejected**
  検討したが採用しない。

- **Superseded**
  過去のDecisionが後続Decisionによって置き換えられた。

---

# 2. Meta Decisions

## D-000 Decision Ledger

**Status:** Fixed

**Decision**

`docs/decisions.md` を設計判断の唯一の正本とする。

**Concrete files**

```text
docs/decisions.md
```

**Rules**

- 設計変更は最初にこの文書へ反映する。
- `AGENTS.md` 等はこの文書から派生させる。
- 過去のDecisionを単に消さず、必要に応じて `Rejected` / `Superseded` として履歴を残す。

---

## D-001 AGENTS.md

**Status:** Fixed

**Decision**

rootに**単一の `AGENTS.md`** を置く。

nested `AGENTS.md` は作らない。

**Rationale**

Codexが複数のinstruction sourceを解釈する必要をなくすため。

**Concrete files**

```text
AGENTS.md
```

---

# 3. Starter Purpose and Principles

## D-010 Purpose

**Status:** Fixed

**Decision**

Java / Springによる小規模〜中規模の業務システムを、一人または少人数で高品質かつ高速に構築するための個人用starterとする。

starterは「空の未完成品」ではなく、**業務固有機能だけが存在しない、技術基盤として完成したapplication**を目指す。

---

## D-011 Quality over shortcuts

**Status:** Fixed

**Decision**

実装速度のために品質を犠牲にしない。

速度は、設計判断や品質管理を事前に標準化・自動化することで得る。

優先順位:

1. 正しさ
2. 型安全性
3. データ整合性
4. セキュリティ
5. 責務の明確さ
6. 依存方向の明確さ
7. 保守性
8. 単純さ
9. 実装速度

---

## D-012 Code quality target

**Status:** Fixed

**Decision**

コードは公開しても恥ずかしくない品質を基準とする。

starterだからという理由で仮実装や雑な設計を許容しない。

---

## D-013 YAGNI

**Status:** Fixed

**Decision**

将来使うかもしれないという理由だけで抽象化・framework・extension pointを作らない。

ただし以下をYAGNIの名目で削らない。

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

# 4. Technology Stack

## D-020 Java

**Status:** Fixed

**Decision**

Java 25を使用する。

---

## D-021 Gradle

**Status:** Fixed

**Decision**

以下を使用する。

- Gradle Kotlin DSL
- Gradle Wrapper
- Gradle Java Toolchain
- Gradle Version Catalog

Groovy DSLはstarter標準にしない。

global Gradle installationを要求しない。

---

## D-022 Spring Boot

**Status:** Fixed / Verify-on-implementation

**Decision**

Spring Boot 4.1.x系を前提とする。

具体minor / patchは実装時に公式情報を確認する。

---

## D-023 Vaadin

**Status:** Fixed

**Decision**

Vaadin 25 / Vaadin Flowを標準UI technologyとする。

React / Angular / Vue / TypeScript中心のfrontend stackはbaselineにしない。

---

## D-024 Database

**Status:** Fixed

**Decision**

PostgreSQLを使用する。

---

## D-025 Database access

**Status:** Fixed

**Decision**

- Flyway
- jOOQ

を使用する。

JPA / Hibernateは使用しない。

---

## D-026 Spring baseline technologies

**Status:** Fixed

**Decision**

以下を利用可能なbaselineとする。

- Spring MVC
- Spring Security
- Jakarta Validation
- Spring Batch
- Spring Scheduling
- Spring RestClient
- Spring HTTP Service Client
- Spring Boot Actuator
- Spring Modulith

---

## D-027 Test and quality tools

**Status:** Fixed

**Decision**

- JUnit
- AssertJ
- Testcontainers
- Spotless
- Checkstyle
- SpotBugs
- JaCoCo
- ArchUnit

を標準利用する。

---

## D-028 Documentation tools

**Status:** Fixed

**Decision**

- springdoc-openapi
- tbls
- Markdown
- Mermaid
- Pandoc
- LuaLaTeX

を標準とする。

---

## D-029 Repository automation

**Status:** Fixed

**Decision**

- GitHub Actions
- Renovate

を利用する。

---

# 5. Dependency Policy

## D-030 Dependency priority

**Status:** Fixed

**Decision**

新しい機能が必要な場合は以下の順に検討する。

1. JDK標準API
2. Spring標準機能
3. 既存導入済みlibrary
4. 十分成熟した外部library
5. 独自実装

---

## D-031 Default exclusions

**Status:** Fixed

**Decision**

以下はstarter baselineに含めない。

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

---

# 6. Version Management

## D-040 Version ownership

**Status:** Fixed

**Decision**

- Java → Gradle Toolchain
- Gradle → Wrapper
- explicit dependencies/plugins → Version Catalog
- Spring ecosystem → Spring Boot dependency management / BOM
- Vaadin ecosystem → Vaadin BOM
- Docker images → explicit pinned version
- application version → Gradle project version

versionをbuild script各所へ散在させない。

---

## D-041 Docker latest

**Status:** Rejected

**Decision**

Docker imageで `latest` を使用しない。

---

## D-042 Renovate

**Status:** Fixed

**Decision**

以下を追跡する。

- Gradle dependencies
- Gradle plugins
- Gradle Wrapper
- Docker images
- GitHub Actions

Spring Boot / Vaadin等のmajor upgradeをblind auto-mergeしない。

---

# 7. Repository Structure

## D-050 Logical project structure

**Status:** Fixed

**Decision**

完成時の論理構成を以下とする。

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

このツリーは論理構成を表す。

空directoryを構造維持だけのために大量生成しない。

---

## D-051 Generated output

**Status:** Fixed

**Decision**

`build/` はGit管理しない。

主なoutput:

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

# 8. Docker

## D-060 Docker directory

**Status:** Fixed

**Decision**

Docker関連設定は `docker/` に集約する。

```text
docker/
├── compose.yaml
└── docs/
    └── Dockerfile
```

---

## D-061 Single Compose

**Status:** Fixed

**Decision**

Compose fileは一つだけにする。

```text
docker/compose.yaml
```

用途はCompose profileで切り替える。

---

## D-062 Compose profiles

**Status:** Fixed

**Decision**

基本profile:

```text
dev
docs
```

---

## D-063 dev profile

**Status:** Fixed

**Decision**

開発用PostgreSQLを提供する。

persistent named volumeを使用する。

---

## D-064 docs profile

**Status:** Fixed

**Decision**

documentation生成用の:

- temporary PostgreSQL
- documentation tool container

を提供する。

---

## D-065 Documentation image

**Status:** Fixed

**Decision**

`docker/docs/Dockerfile` に可能な限り以下を集約する。

- tbls
- Mermaid CLI
- Pandoc
- LuaLaTeX
- 日本語PDF生成に必要な環境

tool versionは再現性のため可能な限りpinする。

font fileをrepositoryへ安易にcommitしない。

---

## D-066 compose.docs.yaml

**Status:** Superseded

**Previous**

documentation用の別Compose fileを持つ。

**Current**

D-061によりComposeは一つ。

---

## D-067 Application Dockerfile

**Status:** Rejected as baseline

**Decision**

application image用独自Dockerfileはbaselineでは作らない。

Spring Boot Buildpacksを第一選択とする。

---

# 9. Development Environment

## D-070 Dev Container

**Status:** Rejected

**Decision**

starter標準では使用しない。

---

## D-071 Host requirements

**Status:** Fixed

**Decision**

hostに必要:

- JDK 25
- Git
- Docker
- editor / IDE

applicationはhost JVMで実行する。

---

## D-072 Node/npm

**Status:** Fixed / Verify-on-implementation

**Decision**

Vaadinが必要とするNode等は、可能な限りVaadin/build tooling側で管理する。

global Node/npm管理をdeveloperへ要求しない方向を優先する。

---

# 10. Gradle as Unified Command Surface

## D-080 Gradle responsibility

**Status:** Fixed

**Decision**

Gradleをdeveloper向けの単一command surfaceとする。

developerは通常、以下を直接操作しなくてよいようにする。

- docker compose
- Flyway CLI
- jOOQ CLI
- tbls
- Mermaid CLI
- Pandoc
- npm

---

## D-081 Main developer commands

**Status:** Fixed

**Decision**

主に覚えるcommand:

```bash
./gradlew bootRun
./gradlew test
./gradlew check
./gradlew documentation
./gradlew build
```

---

# 11. Project Initialization

## D-090 Base package

**Status:** Fixed

**Decision**

starterでは仮base packageを使用する。

```text
dev.template.application
```

---

## D-091 Replacement targets

**Status:** Fixed

**Decision**

案件開始時に置換する。

- project name
- Gradle group
- base package
- application name

---

## D-092 Initializer

**Status:** Deferred

**Decision**

独自initializer framework/scriptは現時点では作らない。

Codexによる置換で十分ならそれを使う。

---

# 12. Architecture

## D-100 Architecture style

**Status:** Fixed

**Decision**

```text
Feature-oriented Modular Monolith
+
Command / Query separation
+
Ports and Adapters where meaningful
```

---

## D-101 Starter feature

**Status:** Fixed

**Decision**

starterではliteral name `feature` の最小sampleを置く。

架空business domainを大量に作らない。

---

## D-102 Package structure

**Status:** Fixed

**Decision**

```text
feature/
├── api/
│   ├── command/
│   └── query/
└── internal/
    ├── command/
    ├── query/
    ├── usecase/
    ├── domain/
    └── infrastructure/
```

`api.event` は必要な場合だけ追加する。

---

# 13. Spring Modulith

## D-110 Module boundary

**Status:** Fixed

**Decision**

Spring Modulithをmodule boundaryの標準機構とする。

---

## D-111 Named Interfaces

**Status:** Fixed / Verify-on-implementation

**Decision**

以下をNamed Interfaceとして公開する。

- `api.command`
- `api.query`
- 必要時 `api.event`

`package-info.java` + `@NamedInterface` を基本候補とする。

---

## D-112 Cross-feature access

**Status:** Fixed

**Allowed**

```text
order.internal
→ inventory.api.command
```

**Forbidden**

```text
order.internal
→ inventory.internal
```

---

## D-113 Module cycles

**Status:** Fixed

**Decision**

禁止。

Spring Modulith `verify()` で検証する。

---

## D-114 ArchUnit overlap

**Status:** Fixed

**Decision**

Spring Modulithで表現できるruleをArchUnitへ重複実装しない。

---

# 14. Package Visibility

## D-120 Visibility

**Status:** Fixed

**Decision**

- feature public API → `public`
- internal implementation → package-private第一選択
- `protected` → 継承を明確に意図した場合のみ

Spring Beanだからという理由だけでpublicにしない。

---

# 15. Command Architecture

## D-130 Command flow

**Status:** Fixed

**Decision**

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

---

## D-131 Repository interface

**Status:** Fixed

**Decision**

Repository interfaceは `feature.internal.usecase` に置く。

---

## D-132 Command implementation responsibilities

**Status:** Fixed

**Decision**

`internal.command`:

- public Command API実装
- transaction boundary
- input adaptation
- usecase invocation
- Result返却

business logicを大量に置かない。

---

# 16. Query Architecture

## D-140 Query flow

**Status:** Fixed

**Decision**

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

---

## D-141 Query/domain separation

**Status:** Fixed

**Decision**

Query pathはusecase/domainを通さない。

read modelとSQL最適化を優先する。

---

## D-142 DataSource interface

**Status:** Fixed

**Decision**

interface:

```text
feature.internal.query
```

implementation:

```text
feature.internal.infrastructure
```

---

## D-143 Command / Query asymmetry

**Status:** Fixed

**Decision**

Commandはdomain-oriented。

QueryはSQL/read-model-oriented。

意図的な非対称設計とする。

---

# 17. Transaction

## D-150 Command transaction

**Status:** Fixed

**Decision**

`internal.command` がtransaction boundary。

```java
@Transactional
```

---

## D-151 Query transaction

**Status:** Fixed

**Decision**

`internal.query`:

```java
@Transactional(readOnly = true)
```

---

## D-152 Usecase/domain

**Status:** Fixed

**Decision**

transaction-awareにしない。

---

## D-153 Cross-feature updates

**Status:** Fixed

**Decision**

親Commandから対象featureのpublic Command APIを呼ぶ。

他feature schemaへの直接:

- INSERT
- UPDATE
- DELETE

は禁止。

---

## D-154 Propagation

**Status:** Fixed

**Decision**

default `REQUIRED`。

`REQUIRES_NEW` は明確な理由がある場合のみ。

---

## D-155 Self invocation

**Status:** Fixed

**Decision**

transactional self invocationへ依存しない。

---

# 18. Result / Failure / Exception

## D-160 Result model

**Status:** Fixed

**Decision**

```text
Result<S,F>
├── Success<S,F>
└── Failure<S,F>
```

variant名は `Success` / `Failure`。

---

## D-161 Typed Failure

**Status:** Fixed

**Decision**

Failure reasonは型付き。

message stringだけでbusiness failureを表現しない。

---

## D-162 Command return

**Status:** Fixed

**Decision**

Commandは原則Resultを返す。

---

## D-163 Query return

**Status:** Fixed

**Decision**

expected failureが意味を持つ場合のみResult。

単純list/searchは値を直接返してよい。

---

## D-164 Exception classification

**Status:** Fixed

**Decision**

- expected business outcome → Failure
- unexpected technical failure → Exception
- impossible internal state → Exception

---

## D-165 Exception wrapping

**Status:** Fixed

**Decision**

既存Spring/jOOQ exceptionが十分意味を持つ場合はwrapしない。

custom exceptionは意味を追加する場合のみ。

---

## D-166 Rollback-safe conversion

**Status:** Fixed

**Decision**

transactional method内部でExceptionをcatchしてFailureを返し、partial commitを起こしてはならない。

Exception → Failure変換はrollbackが保証される位置で行う。

---

## D-167 Adapter handling

**Status:** Fixed

**Decision**

- REST Failure → HTTP status / ProblemDetail
- Vaadin Failure → user-level handling
- Batch Failure → job semanticsに応じた結果
- unexpected Batch Exception → Job failure / non-zero semantics
- unexpected web Exception → common error handler + logging

---

# 19. ID

## D-170 UUID

**Status:** Fixed

**Decision**

UUID v7。

---

## D-171 Representation

**Status:** Fixed

**Decision**

PostgreSQL:

```text
uuid
```

Java:

```text
java.util.UUID
```

---

## D-172 Domain IDs

**Status:** Fixed

**Decision**

必要に応じてfeature-specific Value Object。

Query DTOではraw UUID可。

---

## D-173 Generation location

**Status:** Fixed

**Decision**

application/domain側で生成する。

DB auto incrementはbaselineにしない。

---

## D-174 UUID v7 implementation

**Status:** Fixed (D-591)

**Decision**

uuid-creatorのTimeOrderedEpochFactoryへClockを注入する。独自generatorは作らない。

---

# 20. Date and Time

## D-180 Instant

**Status:** Fixed

**Decision**

```text
Instant ↔ timestamptz
```

---

## D-181 Business date

**Status:** Fixed

```text
LocalDate ↔ date
```

---

## D-182 Time only

**Status:** Fixed

```text
LocalTime ↔ time
```

---

## D-183 Regional time

**Status:** Fixed

`ZonedDateTime + ZoneId`。

---

## D-184 External offset time

**Status:** Fixed

`OffsetDateTime`。

内部では必要に応じInstantへ変換。

---

## D-185 LocalDateTime

**Status:** Rejected for real instants

**Decision**

実時刻表現に使わない。

---

## D-186 Clock

**Status:** Fixed

**Decision**

current timeはinjectable `Clock`。

production:

```java
Clock.systemUTC()
```

test:

```java
Clock.fixed(...)
```

OS default timezoneへ依存しない。

---

# 21. Database Ownership

## D-190 Schema per feature

**Status:** Fixed

**Decision**

featureごとにPostgreSQL schemaを所有する。

---

## D-191 Cross-feature writes

**Status:** Fixed

**Decision**

他feature schemaへの直接writeは禁止。

---

## D-192 Cross-feature reads

**Status:** Fixed

**Decision**

cross-feature JOINは禁止しない。

DB JOINが適切ならJava側へ無理に分解しない。

---

## D-193 Cross-feature query architecture

**Status:** Deferred

**Candidates**

- multi-schema JOIN
- View
- Materialized View
- projection table
- dedicated cross-feature query module

必要になるまで命名・構造を固定しない。

---

## D-194 Technical schema

**Status:** Deferred / Project-specific

**Decision**

必要時に `system` 等を使用可能。

用途:

- Spring Batch metadata
- Spring Modulith event publication metadata

必須作成ではない。

---

# 22. Database Naming and Types

## D-200 Naming

**Status:** Fixed

snake_case。

---

## D-201 Constraints

**Status:** Fixed

```text
PK    pk_<table>
FK    fk_<table>_<referenced_table>
UK    uk_<table>_<column...>
Index ix_<table>_<column...>
Check ck_<table>_<purpose>
```

---

## D-202 ID columns

**Status:** Fixed

own PK:

```text
id
```

reference:

```text
<target>_id
```

---

## D-203 Nullability

**Status:** Fixed

default `NOT NULL`。

absence自体に意味がある場合のみnullable。

---

## D-204 Numeric

**Status:** Fixed

```text
numeric(p,s) ↔ BigDecimal
```

moneyへfloat/doubleを使用しない。

---

## D-205 Strings

**Status:** Fixed

- bounded semantic string → `varchar(n)`
- unbounded description → `text`

ValidationとDB constraintを整合。

---

## D-206 Boolean

**Status:** Fixed

本当に二値だけの概念に使用。

状態が増え得るものを安易にboolean化しない。

---

# 23. Audit / Delete

## D-210 Audit names

**Status:** Fixed

必要なtableのみ:

```text
created_at
updated_at
created_by
updated_by
```

---

## D-211 Audit assignment

**Status:** Project-specific

具体的な付与方式は案件要件で決定。

---

## D-212 Delete default

**Status:** Fixed

physical delete。

---

## D-213 Soft delete

**Status:** Rejected as baseline

以下を自動追加しない。

```text
deleted
deleted_at
is_deleted
```

履歴要件は履歴modelとして設計する。

---

# 24. Foreign Keys

## D-220 Same-feature FK

**Status:** Fixed

DB constraintを積極利用。

---

## D-221 Cross-feature FK

**Status:** Project-specific

module autonomyとdata integrityを比較して決定。

UUID referenceのみも許容。

---

# 25. Flyway

## D-230 Source of truth

**Status:** Fixed

schema変更履歴はFlyway migration。

---

## D-231 Applied migrations

**Status:** Fixed

適用済migrationを安易に変更しない。

新しい変更は新migration。

---

## D-232 Development migration

**Status:** Fixed / Verify-on-implementation

```text
./gradlew bootRun
→ dev PostgreSQL
→ Flyway
→ application start
```

Spring Boot Docker Compose integrationを利用する方向。

---

## D-233 Production migration

**Status:** Fixed

原則application startup時。

案件の運用要件によってmigration先行実行も許容。

---

## D-234 Migration layout

**Status:** Fixed (Phase 2; D-581)

`db/migration/<feature>/` 配下をFlyway標準の再帰scanで読み込む。versionはapplication全体で一意にする。

独自migration loaderは作らない。

---

## D-235 Flyway history table

**Status:** Fixed (Phase 2; D-581)

**Decision**

`public.flyway_schema_history` をapplication全体の共通履歴とする。

---

# 26. Master / Sample Data

## D-240 Minimal starter data

**Status:** Fixed

起動・動作確認に必要な最小sample/system dataのみFlywayで登録可。

---

## D-241 Generic master import

**Status:** Rejected

CSV + Spring Batch等によるgeneric master importをbaseline化しない。

---

## D-242 Business master

**Status:** Project-specific

商品・顧客・倉庫等は案件固有。

---

# 27. Database Environments

## D-250 Development DB

**Status:** Fixed

Docker Compose PostgreSQL。

persistent named volume。

---

## D-251 Test DB

**Status:** Fixed

Testcontainers PostgreSQL。

isolated / ephemeral。

---

## D-252 jOOQ codegen DB

**Status:** Fixed

temporary PostgreSQL。

```text
empty DB
→ Flyway
→ jOOQ code generation
→ destroy
```

---

## D-253 Documentation DB

**Status:** Fixed

temporary PostgreSQL。

```text
empty DB
→ Flyway
→ tbls
→ destroy
```

---

## D-254 Four DB purposes

**Status:** Fixed

明確に分離する。

1. development
2. test
3. jOOQ code generation
4. documentation

---

# 28. jOOQ

## D-260 Generated source

**Status:** Fixed

```text
build/generated-src/jooq/
```

---

## D-261 Generated packages

**Status:** Fixed

schema単位で明確に分離されたgenerated package hierarchyとする。

具体package名は実装時にbase packageとの整合を見て決定。

---

## D-262 Git

**Status:** Fixed

generated sourceはcommitしない。

---

## D-263 Usage boundary

**Status:** Fixed

jOOQ generated codeは原則 `internal.infrastructure` からのみ参照。

ArchUnitで検証可能。

---

## D-264 Reproducibility

**Status:** Fixed

development DBをcodegen sourceにしない。

---

## D-265 Incrementality

**Status:** Fixed

Gradle inputs / outputsを設定し、migrationに変更がない場合の不要なcodegenを避ける。

---

# 29. Database Documentation

## D-270 Source

**Status:** Fixed

migrated PostgreSQL schemaをsourceとする。

Flyway SQL自体を独自parseしない。

---

## D-271 PostgreSQL comments

**Status:** Fixed

```sql
COMMENT ON TABLE ...
COMMENT ON COLUMN ...
```

をdescription sourceとして利用。

---

## D-272 tbls

**Status:** Fixed

`.tbls.yml`。

---

## D-273 ER format

**Status:** Fixed

SVG優先。

---

## D-274 Manual notes

**Status:** Fixed

```text
docs/database/notes/
```

---

## D-275 Generated review docs

**Status:** Project-specific

```text
docs/database/generated/
```

をschema変更reviewのためGit管理する案を許容する。

Git管理を必須とはまだ固定しない。

---

## D-276 Build output

**Status:** Fixed

```text
build/documentation/database/
```

はbuild artifact。

---

# 30. REST

## D-280 REST location

**Status:** Fixed

```text
web.rest
```

---

## D-281 Dependency boundary

**Status:** Fixed

REST Controller → feature public API。

feature internalへ直接依存しない。

---

## D-282 HTTP DTO

**Status:** Fixed

REST-specific request/response DTOは `web.rest` に置ける。

---

## D-283 URLs

**Status:** Fixed

noun-oriented / plural。

---

## D-284 Non-CRUD actions

**Status:** Fixed

必要に応じaction subresourceを使用。

---

## D-285 HTTP status

**Status:** Fixed

基本:

- 200
- 201
- 204
- 400
- 401
- 403
- 404
- 409
- 422
- 500

---

## D-286 Errors

**Status:** Fixed

Spring `ProblemDetail`。

---

## D-287 API response wrapper

**Status:** Rejected

global `ApiResponse<T>` 等を作らない。

---

## D-288 JSON

**Status:** Fixed

Spring/Jackson defaultsを基本。

- camelCase
- ISO-8601
- unnecessary serializerを避ける

---

# 31. API Versioning

## D-290 Version form

**Status:** Fixed

```text
/api/v1
/api/v2
```

major version。

---

## D-291 Breaking changes

**Status:** Fixed

breaking changeだけmajor bump。

---

## D-292 Future packages

**Status:** Rejected

実在しないv2 packageを先行作成しない。

---

## D-293 Spring standard versioning

**Status:** Fixed / Verify-on-implementation

Spring MVC標準API versioningを優先する。

custom resolverを作らない。

---

## D-294 Deprecation / Sunset

**Status:** Fixed / Verify-on-implementation

API廃止予告は可能な限りSpring / HTTP標準のdeprecation / sunset mechanismを優先する。

独自仕組みを先に作らない。

---

# 32. Pagination / Sort

## D-300 Ownership

**Status:** Fixed

pagination/sortはQuery API側のtyped conceptとして扱う。

REST/Vaadinはadapter。

---

## D-301 Offset/limit

**Status:** Project-specific

小〜中規模業務システムでは第一候補。

強制はしない。

---

## D-302 Slice/Page

**Status:** Project-specific

total不要 → Slice的model。

total必要 → Page。

---

## D-303 Keyset

**Status:** Project-specific

deep pagination / large datasetでjOOQ seek/keysetを検討。

---

## D-304 Typed sort

**Status:** Fixed

任意文字列をDB columnへ直結しない。

query-specific enum等へ変換。

---

## D-305 Spring Data Pageable

**Status:** Rejected as baseline

paginationだけのためにSpring Dataを追加しない。

---

# 33. Vaadin

## D-310 UI location

**Status:** Fixed

```text
web.ui
```

---

## D-311 Dependency boundary

**Status:** Fixed

Vaadin View → feature public API。

---

## D-312 Framework types

**Status:** Fixed

Vaadin-specific typeをdomain/usecaseへ渡さない。

---

# 34. Security

## D-320 Spring Security

**Status:** Fixed

baselineに含める。

---

## D-321 Authentication

**Status:** Project-specific

具体方式は固定しない。

---

## D-322 Method Security

**Status:** Fixed

business authorizationの中心。

`@EnableMethodSecurity`。

---

## D-323 Authorization boundary

**Status:** Fixed

主に:

- internal.command
- internal.query

で認可。

---

## D-324 Web route security

**Status:** Fixed

粗いentry restriction。

Method Securityを本認可とする。

---

## D-325 Authority

**Status:** Fixed

RoleよりAuthority中心。

例:

```text
inventory:read
inventory:write
```

---

## D-326 Role

**Status:** Fixed

Authorityの集合として扱う。

---

## D-327 Authority constants

**Status:** Fixed

stringを各所へ散在させない。

feature単位の定義を許容。

全featureを巨大global classへ集約しない。

---

## D-328 SecurityContext boundary

**Status:** Fixed

domain/usecaseから:

- SecurityContextHolder
- Authentication
- GrantedAuthority

を直接使わない。

---

## D-329 Current user

**Status:** Fixed

security layerでbusiness/application-friendly typeへ変換して渡す。

---

## D-330 Batch/Scheduler actor

**Status:** Fixed

fake Authenticationを作らない。

必要ならbusiness Actor型を設計。

---

## D-331 REST security status

**Status:** Fixed

- unauthenticated → 401
- unauthorized → 403

---

## D-332 Actuator security

**Status:** Fixed

公開endpointを最小限にする。

healthは必要に応じ匿名可。

---

# 35. External HTTP

## D-340 Client priority

**Status:** Fixed

1. HTTP Service Client
2. RestClient
3. WebClient — reactive/streamingが本当に必要な場合

RestTemplateは新規採用しない。

---

## D-341 HTTP Service Groups

**Status:** Fixed / Verify-on-implementation

Spring Boot標準group configurationを利用できる場合は優先。

---

## D-342 External DTOs

**Status:** Fixed

`internal.infrastructure` に閉じる。

---

## D-343 Business Port

**Status:** Fixed

business側は外部HTTP APIそのものではなく、自身が必要とするPortへ依存。

---

## D-344 Timeout

**Status:** Fixed

明示する。

- connect timeout
- read timeout

---

## D-345 Retry

**Status:** Fixed

default retryなし。

---

## D-346 Mutation retries

**Status:** Fixed

POST / PUT / PATCHの機械的retryは禁止。

idempotency保証時のみ検討。

---

## D-347 Error mapping

**Status:** Fixed

expected business response → Failure変換可。

network / timeout / 5xx / unexpected protocol → Exception。

---

## D-348 Authentication

**Status:** Fixed

API key / token等はclient configuration側。

business layerへcredentialを持ち込まない。

---

## D-349 Correlation propagation

**Status:** Fixed

必要に応じMDC correlation IDをoutgoing HTTPへ伝播。

header名は案件仕様に応じる。

---

## D-350 External HTTP logging

**Status:** Fixed

横断的に必要な場合、以下を記録対象とする。

- target service
- operation
- HTTP status
- duration
- correlation ID
- exception

request / response body全文を標準logにしない。

---

# 36. Logging

## D-360 Business logger calls

**Status:** Fixed

通常のbusiness codeへlogger callを書かない。

---

## D-361 Mechanism

**Status:** Fixed

AOP / filter / interceptor中心。

---

## D-362 Command log level

**Status:** Fixed

start/end → INFO。

---

## D-363 Query log level

**Status:** Fixed

start/end → DEBUG。

---

## D-364 Failure level

**Status:** Fixed

expected Failure → INFO基本。

意味上warningならWARN。

---

## D-365 Exception level

**Status:** Fixed

unexpected Exception → ERROR + stack trace。

---

## D-366 Sensitive information

**Status:** Fixed

以下をlogしない。

- password
- token
- secret
- credential
- unnecessary PII

---

## D-367 Payload dump

**Status:** Rejected

request/response objectの全文dumpを標準にしない。

---

## D-368 Correlation ID

**Status:** Fixed

MDCで管理。

---

## D-369 Pointcuts

**Status:** Fixed

package-based pointcutを第一候補。

不要なmarker annotation frameworkを先に作らない。

---

## D-370 Direct logger exceptions

**Status:** Fixed

AOPでは意味を表現できないtechnical eventのみ例外的に直接logger可。

---

# 37. Configuration

## D-380 Environment-dependent values

**Status:** Fixed

environment variableから注入。

---

## D-381 application.yml

**Status:** Fixed

以下を置いてよい。

- stable configuration
- environment variable mapping
- safe default

secretは書かない。

---

## D-382 Secrets

**Status:** Fixed

repositoryへcommitしない。

JAR / Docker双方でenvironment-based configuration modelを共通利用する。

---

## D-383 ConfigurationProperties

**Status:** Fixed

```java
@ConfigurationProperties
@Validated
record ...
```

を第一選択。

---

## D-384 @Value

**Status:** Fixed

scattered `@Value` を避ける。

---

## D-385 Typed config

**Status:** Fixed

可能な限り:

- Duration
- URI
- numeric type
- boolean

等を使用。

---

## D-386 Startup validation

**Status:** Fixed

必須設定不足はapplication startup failure。

configuration validationについてtestも用意する。

---

## D-387 Defaults

**Status:** Fixed

安全なdefaultのみ。

---

## D-388 Environment profile files

**Status:** Fixed

environment別YAMLを大量複製しない。

---

# 38. HikariCP

## D-390 Pool

**Status:** Fixed

Spring Boot標準HikariCP。

---

## D-391 Tuning

**Status:** Project-specific

starterでは細かくtuneしない。

---

## D-392 Observability

**Status:** Fixed

Actuator/Micrometerでpool状態を観測可能にする。

---

# 39. File I/O

## D-400 Boundary

**Status:** Fixed

upload/downloadはweb adapter concern。

---

## D-401 Framework types

**Status:** Fixed

business APIへ以下を不用意に漏らさない。

- MultipartFile
- HttpServletResponse
- Vaadin StreamResource
- Path
- raw InputStream

technical I/O representationをbusiness contractにしない。

---

## D-402 Storage port

**Status:** Fixed

persistent storageはPort経由。

implementationはinfrastructure。

---

## D-403 Storage implementation

**Status:** Project-specific

例:

- local filesystem
- S3-compatible storage

---

## D-404 Filename safety

**Status:** Fixed

user filenameを保存pathとして信用しない。

内部名はUUID等で生成。

---

## D-405 File validation

**Status:** Fixed

必要に応じ:

- size
- MIME type
- extension
- content

を検証。

---

## D-406 Temporary files

**Status:** Fixed

適切なtemp領域を使用。

処理後削除。

---

## D-407 Large files

**Status:** Fixed

不要に全内容をmemoryへ展開しない。

---

## D-408 CSV

**Status:** Fixed

baseline:

- UTF-8
- comma separated
- quotingを正しく扱う
- LF / CRLFの双方を受容

独自CSV parserを作らない。

必要ならApache Commons CSV等を利用。

---

## D-409 Excel / business PDF

**Status:** Project-specific

baselineには含めない。

---

# 40. Cache

## D-410 Baseline

**Status:** Fixed

cacheなし。

---

## D-411 Dependencies

**Status:** Rejected as baseline

最初から:

- Spring Cache
- Caffeine
- Redis

を追加しない。

---

## D-412 Introduction

**Status:** Project-specific

performance measurement後に必要箇所へ導入。

---

# 41. Events / Async

## D-420 Default feature communication

**Status:** Fixed

同期public Command / Query API。

---

## D-421 Spring Modulith Event

**Status:** Fixed

時間的に分離可能な副作用に使用可能。

---

## D-422 Event overuse

**Status:** Rejected

疎結合という理由だけで全処理をevent化しない。

---

## D-423 api.event

**Status:** Fixed

必要なfeatureのみ持つ。

---

## D-424 Async

**Status:** Fixed

defaultは同期。

必要性がある処理だけ非同期化。

---

## D-425 Async considerations

**Status:** Fixed

- transaction
- failure handling
- retry
- ordering
- duplicate execution
- recovery

を検討。

---

# 42. Mail / Notification

## D-430 Mail baseline

**Status:** Rejected

starterにMail dependencyを入れない。

---

## D-431 Notification architecture

**Status:** Project-specific

必要時にnotification feature/module + Port。

business featureからtransport implementationを直接呼ばない。

---

# 43. Batch / Scheduling

## D-440 Batch

**Status:** Fixed

top-level adapter:

```text
batch
```

---

## D-441 Batch dependency

**Status:** Fixed

Batch → feature public API。

---

## D-442 Batch use cases

**Status:** Fixed

以下が必要な処理でSpring Batch。

- restartability
- step
- chunk
- job history

---

## D-443 Sample jobs

**Status:** Rejected

意味のないsample Jobを作らない。

---

## D-444 Job auto-run

**Status:** Rejected

web application startup時に全Jobを自動実行しない。

---

## D-445 Batch result semantics

**Status:** Fixed

- unexpected Exception → Job failure
- expected Failure → job use caseの意味に応じて終了状態へmapping

---

## D-446 Scheduling

**Status:** Fixed

top-level:

```text
scheduling
```

単純定期処理は `@Scheduled`。

---

## D-447 Quartz

**Status:** Rejected as baseline

---

# 44. Testing

## D-450 Unit tests

**Status:** Fixed

対象:

- domain
- usecase

Spring contextなし。

JUnit + AssertJ。

---

## D-451 Integration tests

**Status:** Fixed

PostgreSQL Testcontainers。

対象:

- Repository
- DataSource
- Flyway
- jOOQ
- constraints
- sorting
- pagination
- joins
- boundary cases

---

## D-452 H2

**Status:** Rejected

PostgreSQL代替integration DBに使わない。

---

## D-453 Architecture tests

**Status:** Fixed

Spring Modulith + ArchUnit。

---

## D-454 ArchUnit scope

**Status:** Fixed

Modulithで表現しづらいtechnical rulesのみ。

例:

- web → jOOQ禁止
- domain → Vaadin禁止
- domain → Spring MVC禁止
- security → feature.internal禁止
- generated jOOQ → infrastructure以外禁止

---

## D-455 REST tests

**Status:** Fixed

必要に応じMockMvc。

---

## D-456 Vaadin E2E

**Status:** Fixed

重要flowのみ。

---

## D-457 SpringBootTest

**Status:** Fixed

wiring/context/security等の少数smoke test。

---

## D-458 Source sets

**Status:** Fixed

```text
src/test/java
src/integrationTest/java
src/architectureTest/java
```

---

## D-459 Task names

**Status:** Fixed

```text
test
integrationTest
architectureTest
```

---

## D-460 Test class names

**Status:** Fixed

```text
FooTest
FooIntegrationTest
ArchitectureTest
```

---

# 45. Code Quality

## D-470 Spotless

**Status:** Fixed

formatting標準。

---

## D-471 Checkstyle

**Status:** Fixed

意味のあるcoding conventionsを守る。

Javaを不自然にするほど細かくしない。

---

## D-472 SpotBugs

**Status:** Fixed

static bug analysis。

---

## D-473 JaCoCo

**Status:** Fixed

coverage visibility。

coverage percentageを目的化しない。

---

## D-474 Quality gate

**Status:** Fixed

```bash
./gradlew check
```

---

## D-475 check content

**Status:** Fixed

少なくとも:

- Spotless check
- Checkstyle
- SpotBugs
- unit tests
- integration tests
- architecture tests
- relevant JaCoCo tasks

---

# 46. Documentation

## D-480 Markdown

**Status:** Fixed

human-authored documentationのsource of truth。

---

## D-481 Mermaid

**Status:** Fixed

diagram標準。

---

## D-482 Mermaid conversion

**Status:** Fixed

Pandocへfenced Mermaidを直接任せず、SVG等へ事前render。

---

## D-483 PDF

**Status:** Fixed

Pandoc + LuaLaTeX。

日本語を再現可能に生成。

---

## D-484 Project source

**Status:** Fixed

```text
docs/project/
```

---

## D-485 Project output

**Status:** Fixed

```text
build/documentation/project/
```

---

## D-486 Documentation aggregate

**Status:** Fixed

```text
documentation
├── databaseDocumentation
├── apiDocumentation
└── projectDocumentation
```

---

## D-487 Normal build

**Status:** Fixed

heavy documentation generationを通常 `build` に含めない。

---

# 47. OpenAPI

## D-490 Tool

**Status:** Fixed

springdoc-openapi。

---

## D-491 Annotation policy

**Status:** Fixed

Controller / Validation metadataを活用しannotation soupを避ける。

---

## D-492 Swagger UI

**Status:** Fixed

dev/localで利用可。

productionでは無条件公開しない。

---

## D-493 Versioned output

**Status:** Fixed

例:

```text
build/documentation/api/v1/openapi.yaml
```

---

## D-494 Spring REST Docs

**Status:** Rejected as baseline

---

# 48. Actuator / Observability

## D-500 Baseline

**Status:** Fixed

- health
- info
- metrics

---

## D-501 Micrometer

**Status:** Fixed

Boot標準を利用。

---

## D-502 Prometheus

**Status:** Project-specific

---

## D-503 OpenTelemetry

**Status:** Project-specific

---

## D-504 Structured JSON logging

**Status:** Project-specific

---

# 49. Graceful Shutdown

## D-510 Graceful shutdown

**Status:** Fixed

Spring Boot標準を使用。

独自shutdown frameworkを作らない。

---

# 50. Release and Deployment

## D-520 Canonical artifact

**Status:** Fixed

Spring Boot executable JAR。

```bash
./gradlew bootJar
```

---

## D-521 OCI image

**Status:** Fixed

```bash
./gradlew bootBuildImage
```

---

## D-522 Application version

**Status:** Fixed

Gradle project versionを正本とする。

SemVerを基本とする。

---

## D-523 API vs app version

**Status:** Fixed

別物として扱う。

---

## D-524 Git tag

**Status:** Fixed

例:

```text
v1.0.0
```

---

## D-525 Deployment platform

**Status:** Project-specific

候補:

- JAR + systemd
- Docker
- ECS
- Kubernetes
- etc.

---

## D-526 Production Compose

**Status:** Rejected as baseline

---

# 51. GitHub Actions

## D-530 Build logic

**Status:** Fixed

Gradleをbuild truthとする。

Actionsへbuild logicを複製しない。

---

## D-531 CI

**Status:** Fixed

基本:

```text
checkout
→ JDK 25
→ Gradle
→ ./gradlew check
```

---

## D-532 Release workflow

**Status:** Fixed

Git tagをtriggerとしてrelease可能にする。

---

## D-533 Release assets

**Status:** Fixed

必要に応じ:

- executable JAR
- OpenAPI
- DB documentation
- project documentation PDF

をGitHub Releaseへ添付。

---

# 52. README / Docs Roles

## D-540 README

**Status:** Fixed

利用者向け入口。

含める:

- purpose
- prerequisites
- initialization
- project structure概要
- main commands
- bootRun
- check
- documentation
- build / release概要

---

## D-541 architecture.md

**Status:** Fixed

architecture詳細。

---

## D-542 database.md

**Status:** Fixed

database / Flyway / jOOQ / DB documentation詳細。

---

## D-543 implementation-plan.md

**Status:** Fixed

phase単位の実装順序。

---

# 53. Codex / Ponytail

## D-550 Codex read order

**Status:** Fixed

変更前に:

1. `AGENTS.md`
2. relevant docs
3. current implementation

を読む。

---

## D-551 Phase discipline

**Status:** Fixed

指定されたimplementation phaseだけを実装。

先のphaseへ勝手に進まない。

---

## D-552 Validation workflow

**Status:** Fixed

変更後:

1. formatter
2. relevant tests
3. static analysis
4. 必要なら `./gradlew check`
5. failure修正
6. changed files確認
7. result summary

---

## D-553 Ponytail role

**Status:** Fixed

general YAGNI / simplification review。

project-specific ruleのsource of truthではない。

---

## D-554 Ponytail review

**Status:** Fixed

削減対象:

- speculative abstraction
- unused extension point
- redundant wrapper
- unnecessary DTO
- unused configuration
- needless indirection

削減禁止:

- type safety
- security
- transaction safety
- data integrity
- module boundary
- meaningful tests
- necessary validation

---

# 54. Verify at Implementation Time

## D-560 Version-sensitive items

**Status:** Fixed

以下は実装時に公式documentationを確認する。

- Spring Boot / Spring Framework APIs
- Spring MVC API Versioning
- HTTP Service Client / Service Groups
- Vaadin 25 Node/build management
- Java 25 UUID v7 support
- Flyway migration locations
- jOOQ Gradle integration
- springdoc + Spring Boot 4
- Spring Modulith event publication metadata
- Spring Batch metadata
- Spring Boot Docker Compose integration
- `bootBuildImage`

古いblog記事をblind copyしない。

---

# 55. Explicit Rejections / Superseded Decisions

| ID    | Decision                                          | Status               |
| ----- | ------------------------------------------------- | -------------------- |
| R-001 | Dev Container baseline                            | Rejected             |
| R-002 | JPA / Hibernate baseline                          | Rejected             |
| R-003 | Redis baseline                                    | Rejected             |
| R-004 | Cache baseline                                    | Rejected             |
| R-005 | Generic master-data import batch                  | Rejected             |
| R-006 | Generic notification framework                    | Rejected             |
| R-007 | Multiple Compose files                            | Rejected             |
| R-008 | `compose.docs.yaml`                               | Superseded           |
| R-009 | Application Dockerfile baseline                   | Rejected             |
| R-010 | RestTemplate                                      | Rejected             |
| R-011 | Spring Data Pageable dependency                   | Rejected as baseline |
| R-012 | H2 for PostgreSQL integration tests               | Rejected             |
| R-013 | Soft delete default                               | Rejected             |
| R-014 | Everything as events                              | Rejected             |
| R-015 | Async by default                                  | Rejected             |
| R-016 | Logger in every business class                    | Rejected             |
| R-017 | Global custom API response envelope               | Rejected             |
| R-018 | Giant custom exception hierarchy                  | Rejected             |
| R-019 | Heavy docs generation as normal build requirement | Rejected             |
| R-020 | Production Compose as starter standard            | Rejected             |

---

# 56. Deferred / Project-specific Decisions

以下は意図的に固定しない。

- authentication mechanism
- cross-feature query module structure
- pagination方式
- total countの有無
- audit actor assignment implementation
- cross-feature FK
- cache implementation
- retry policy per operation
- file storage implementation
- mail / notification transport
- Hikari pool sizing
- observability backend
- Spring Batch metadata schema placement
- Spring Modulith metadata schema placement
- production deployment platform
- exact project initialization automation
- `docs/database/generated/` のGit管理有無

---

# 57. Phase 1 Implementation Decisions

## D-570 Initial build versions

**Status:** Fixed

2026-09-13のPhase 1では以下を採用する。以後のversion更新はVersion Catalog / Wrapperを正本とし、この表は初回選定の記録とする。

| Component | Initial version | Official source |
| --- | --- | --- |
| Java Toolchain | 25 | [Gradle compatibility](https://docs.gradle.org/current/userguide/compatibility.html) |
| Gradle Wrapper | 9.7.0 | [Release notes](https://docs.gradle.org/9.7.0/release-notes.html) |
| Spring Boot plugin / BOM | 4.1.1 | [System requirements](https://docs.spring.io/spring-boot/system-requirements.html) |
| Spring Modulith BOM | 2.1.1 | [Reference](https://docs.spring.io/spring-modulith/reference/) |
| Spotless plugin | 8.10.2 | [Plugin Portal](https://plugins.gradle.org/plugin/com.diffplug.spotless/8.10.2) |
| google-java-format | 1.36.1 | [Release](https://github.com/google/google-java-format/releases/tag/v1.36.1) |
| SpotBugs plugin | 6.5.11 | [Plugin Portal](https://plugins.gradle.org/plugin/com.github.spotbugs/6.5.11) |
| SpotBugs engine | 4.10.4 | [Release](https://github.com/spotbugs/spotbugs/releases/tag/4.10.4) |
| Checkstyle | 14.1.0 | [Release](https://github.com/checkstyle/checkstyle/releases/tag/checkstyle-14.1.0) |
| JaCoCo | 0.8.14 | [Release](https://github.com/jacoco/jacoco/releases/tag/v0.8.14) |

Java 25のGradle実行には9.1.0以降が必要。Boot 4.1.1はJava 25とGradle 9.xをサポートする。Wrapperには公式配布物のSHA-256を設定する。

## D-571 Dependency and test configuration

**Status:** Fixed

[Gradle標準platformによるBOM import](https://docs.spring.io/spring-boot/gradle-plugin/managing-dependencies.html)を使用し、dependency-management pluginは追加しない。JUnit / AssertJはBoot BOM、Modulith / ArchUnitはModulith BOMに従う。

Gradle標準source setとTest taskで `test` / `integrationTest` / `architectureTest` を分離する。context smoke testはintegrationTestへ置く。Unit Test対象がないPhase 1ではtestをNO-SOURCEとし、架空のdomainを追加しない。

JaCoCoはUnit / Integration Testを一つのreportへ集約する。構造検証のみのArchitecture Testはcoverage対象に含めない。coverage下限値は設定しない。

## D-572 Phase 1 application scope

**Status:** Fixed

初期project name / application nameは `spring-application-starter`、groupは `dev.template`、base packageは `dev.template.application`、初期application versionは `0.1.0-SNAPSHOT` とする。

Phase 1はSpring Boot core starterだけの非Web applicationとする。main起動後は処理がないため正常終了する。DB / REST / security / Vaadin等は計画の対象phaseで追加する。

Modulith `verify()` をArchitecture Testから実行する。まだfeature moduleがないため、実moduleの境界違反検出とtechnical ArchUnit ruleはPhase 3以降に検証する。空moduleや空package維持用のsampleは作らない。

---

# 58. Phase 2 Implementation Decisions

## D-580 PostgreSQL and development lifecycle

**Status:** Fixed

PostgreSQL imageは `postgres:18.6` にpinする（[公式release](https://www.postgresql.org/docs/release/18.6/)）。ComposeとVersion Catalogのimage指定は同時に更新する。

開発DBは `docker/compose.yaml` のdev profile、loopbackへの動的host port、persistent named volumeを使用する。PostgreSQL 18のvolume mount先は `/var/lib/postgresql`。passwordは `DEV_DB_PASSWORD` を必須とし、repositoryに保存しない。`bootRun` はBoot Docker Compose integrationの標準start-and-stop lifecycleを使用する。

## D-581 Migration and schema ownership

**Status:** Fixed; D-234 / D-235の具体方式を確定

`src/main/resources/db/migration/<feature>/V<global-version>__<description>.sql` に配置する。Flyway標準の再帰scanを使用し、versionはapplication全体で一意とする。historyは `public.flyway_schema_history`、defaultSchemaはpublic。feature schemaはmigration SQLで作成する。

最小tableは `feature.feature`。application側で生成するUUIDの `id` と `name varchar(100)` をNOT NULLで持つ。PKは `pk_feature`、ASCII spaceだけの名前を拒否するcheckは `ck_feature_name_not_blank`。初期data、audit column、unique name制約は追加しない。業務操作はPhase 3で設計する。

参照: [Flyway locations](https://documentation.red-gate.com/flyway/reference/configuration/flyway-namespace/flyway-locations-setting)、[history table](https://documentation.red-gate.com/fd/flyway-schema-history-table-273973417.html)。

## D-582 Code generation

**Status:** Fixed

Gradle標準JavaExec task `jooqCodegen` と独立したcodegen source setを用いる。Testcontainersのtemporary PostgreSQLをtry-with-resourcesで起動し、Flyway migrate後、[jOOQ標準GenerationTool](https://www.jooq.org/doc/3.21/manual/code-generation/codegen-execution/codegen-programmatic/)で生成する。追加Gradle pluginや独自migration loaderは不要。

Flyway / jOOQ / PostgreSQL JDBC / TestcontainersのversionはBoot BOMに従う（初回は12.4.0 / 3.21.7 / 42.7.13 / 2.0.5）。applicationとcodegenで同じmigrationを使用する。

出力は `build/generated-src/jooq/`、schema packageは `dev.template.application.jooq.<schema>`。compileJavaへ接続する。codegen classpath、migration、image、生成設定をtask inputsとして追跡する。生成設定はcodegen source内に置き、そのclassを入力として追跡する。timestampを生成物に含めない。generated codeは手書きコード用Checkstyle / SpotBugsおよびcoverage集計の対象外とする。

## D-583 Database integration verification

**Status:** Fixed

Spring Boot TestcontainersのServiceConnectionでtest DBを注入し、development Composeはtestで無効化する。context smoke testとDB制約テストは同じtest classのcontainerを共有し、testごとにtransaction rollbackする。Docker不在時にtestをskipしない。

Phase 2はgenerated typeの読み書きとDB制約をinfrastructure package内のIntegration Testで検証する。production Repository / APIはPhase 3で実装する。

---

# 59. Phase 3 Implementation Decisions

## D-590 Minimal feature contract

**Status:** Fixed

Commandは `FeatureCommands.create(String name)` → `Result<UUID, CreateFailure>`、Queryは `FeatureQueries.find(UUID id)` → `Optional<FeatureView>` とする。名前不正は `CreateFailure.INVALID_NAME`、未検出Queryはempty。名前の重複は許容する。schema変更や追加CRUDは不要。

Resultは現在の利用箇所である `feature.api.command` にsealed interfaceとして配置し、Success / Failure recordはnullを許さない。共通moduleは必要になるまで作らない。

名前は100 Unicode code point以内、ASCII spaceだけでないことを既存DB制約と揃える。PostgreSQL文字列で表現できないNUL / unpaired surrogateも拒否する。trim・Unicode正規化は行わず入力を保存する。domain Value Objectが不変条件を保証し、Commandがwrite前に同じ検証でFailureを返す。nullの名前もINVALID_NAME、nullのQuery IDはプログラミング上の誤用として拒否する。

## D-591 UUID and time

**Status:** Fixed; D-174を具体化

[Java 25 UUID API](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/UUID.html)にはUUID v7 generatorがないため、[uuid-creator](https://github.com/f4b6a3/uuid-creator) 6.1.1をVersion Catalogで管理する。公開source JARの `TimeOrderedEpochFactory(Clock)` を確認済み。

Command wiringでClock.systemUTC()をbeanとして提供し、TimeOrderedEpochFactoryのcreateをSupplier<UUID>としてusecaseへ渡す。domain/usecaseはlibraryとSpringを認識しない。testではClock.fixed / 固定Supplierを使う。不要な時刻columnや独自generatorは作らない。

## D-592 Wiring and technical module

**Status:** Fixed

Command実装はtransaction boundary、usecaseはRepositoryとSupplierを使った作成、Query実装はread-only transactionとDataSource呼び出しを担当する。Repository / DataSource interfaceは既定packageに配置し、jOOQ adapterは両portを実装する。

packageを跨ぐdomain型・usecase型・portのみpublicとし、Spring implementation/configurationはpackage-private。featureはcommand / query Named Interfaceだけを公開する。

既存generated package `jooq.<schema>` はopenなtechnical moduleとして宣言する。Modulithがfeatureの内部隠蔽とcycleを検証し、ArchUnitがgenerated typeの利用をinfrastructureに制限する。domain/usecaseのSpring・jOOQ依存、Queryのdomain/usecase依存、transaction annotationの配置も検証する。存在しないWeb module向けruleやsampleは追加しない。

## D-594 Static analysis scope

**Status:** Fixed

generated classesはSpotBugsの解析対象から除外するが、手書きinfrastructureの型解決用auxiliary classpathには含める。CreateFeatureUseCase.repositoryに対するEI_EXPOSE_REP2だけをfilterで除外する。DIで共有するRepository portは意図的に同一instanceを保持し、defensive copyを作らないため。その他のbug patternやfieldは除外しない。

## D-593 Transaction verification

**Status:** Fixed

public APIをSpring proxy経由で呼び、test外transactionでのcommitと、Repositoryがwrite後に技術例外を送出した場合のrollbackをPostgreSQLで検証する。Queryの実read-only transactionも検証する。認可はPhase 4で追加する。

---

# 60. Phase 4 Implementation Decisions

## D-600 REST contract and API versioning

**Status:** Fixed

POST `/api/v1/features` は `{ "name": "..." }` を受け取り、201・Location・`{ "id": "..." }` を返す。GET `/api/v1/features/{id}` はFeatureView、未検出は404 ProblemDetail。

Spring MVCの `usePathSegment(1)` とmappingの `version="1"` を使う。標準SemanticApiVersionParserはv prefixを扱えるためcustom resolverは作らない。未知versionは標準の400とする。参照: [MVC versioning](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/api-version.html)、[parser](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/accept/SemanticApiVersionParser.html)。

name欠落/nullはJakarta Validationで400、malformed JSON / UUIDも400。名前のdomain rule違反（blank・長すぎる等）は既存typed Failureから422へ変換する。Unicode code pointによる長さ検証はCommandに任せ、UTF-16長のSize制約で上書きしない。

## D-601 Authentication and authorization baseline

**Status:** Fixed

認証方式は案件依存を維持する。baselineは認証済みrequestを要求し、form login / HTTP Basic / 固定ユーザーを追加しない。BootのUserDetailsServiceAutoConfigurationを除外して自動生成ユーザーも作らない。接続する認証方式を実装するまで外部clientは保護APIを利用できない。正常系の動作確認はSpring Security test supportによるtest用認証で行う。

@EnableMethodSecurityを有効にし、Commandにfeature:write、Queryにfeature:readを@PreAuthorizeで要求する。Authority定数はそれぞれのpublic API interfaceに置く。URLは粗いauthenticated制限のみ。CSRF保護を維持し、未認証は401、認証済み権限不足 / CSRF拒否は403。security filterのerrorもProblemDetailとする。

参照: [Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)、[Boot default user configuration](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/security/autoconfigure/UserDetailsServiceAutoConfiguration.html)。

## D-602 Error handling and verification

**Status:** Fixed

REST error adviceはSpring標準ResponseEntityExceptionHandlerを基礎とする。予期しないExceptionは500 ProblemDetailへ変換し、clientへexception messageやstack traceを返さない。共通handlerでtechnical errorをloggingする。認証・認可例外は500として捕捉せずSecurity filterへ伝播する。

Boot 4の `spring-boot-starter-webmvc-test` / `spring-boot-starter-security-test` でMockMvcへsecurity test supportを接続する。MockMvcは実際のSecurity filterとpublic APIを通し、PostgreSQLで正常応答、validation、version、401/403、CSRF、認可拒否のwrite防止、500とrollbackを検証する。既存のdirect API testにも必要Authorityを付与し、権限なしのdirect呼び出しが拒否されることを別途検証する。

---

# 61. Phase 5 Implementation Decisions

## D-610 Vaadin build

**Status:** Fixed

Vaadin 25.2.6 BOM / Gradle pluginを使用する。Flow用のvaadin-coreとvaadin-springを使い、商用componentやHillaをbaselineへ追加しない。Node/npmはVaadin toolingに管理させ、global installを要求しない。bootJarはplugin標準のproduction frontend buildを使う。Vaadin pluginはSpring Boot pluginの後に適用し、bootJarへのtask接続を有効にする。標準componentだけの構成では公式precompiled bundleを利用する。generated frontend / default indexはGit管理せず、Spotlessは手書きJavaとresourceだけを対象とする。

参照: [Gradle plugin](https://plugins.gradle.org/plugin/com.vaadin)、[Gradle設定](https://vaadin.com/docs/latest/flow/configuration/gradle)、[25へのupgrade](https://vaadin.com/docs/latest/upgrading)。

## D-611 UIとsecurity

**Status:** Fixed

`web.ui` の `/features` に名前による作成とUUIDによる取得を行う最小Viewを置く。公開Command / Query APIのみを使い、入力不正・Failure・未検出を画面で表示する。routeは `@PermitAll` で認証を要求し、業務Authorityは既存Method Securityで強制する。権限拒否を利用者へ表示する。

RESTは専用filter chainで既存401 / 403 / CSRFを維持する。UI側はVaadinSecurityConfigurer標準の内部request / resource / navigation保護を使う。productionへlogin方式やtestユーザーを追加しない。

参照: [Vaadin Security Configurer](https://vaadin.com/docs/latest/flow/security/vaadin-security-configurer)。

## D-612 UI検証

**Status:** Fixed

Playwright Java 1.62.0をIntegration Test限定で採用し、GradleのplaywrightInstall taskから付属CLIでChromiumを取得する。integrationTestはproduction frontend生成とbrowser取得を前提taskとし、実ブラウザーで配布時と同じmodeを検証する。global Node/npmは要求しない（[公式手順](https://playwright.dev/java/docs/browsers)）。重要flowのE2Eは実HTTP serverとPostgreSQLに対するブラウザー操作で検証する。認証はtest source set限定の仕組みで付与し、production artifactへ含めない。作成・取得・validation・認可拒否を検証し、既存REST / transaction / module boundaryのquality gateも維持する。

---

# 62. Phase 6 Implementation Decisions

## D-620 Loggingとcorrelation

**Status:** Fixed

incoming requestごとにapplication生成UUIDのcorrelation IDをMDCへ設定し、X-Correlation-ID response headerとoutgoing HTTPへ伝播する。外部から来た任意headerを信用してlogへ流さない。filter終了時に以前のMDCを復元する。URL path / query / body / headers / arguments / Resultの値は標準logへ出さない。

package-based AOPでCommandはINFO、QueryはDEBUGの開始・終了とdurationを記録する。typed FailureはINFOでvariantだけを記録する。AOPはMethod Securityより内側、transactionより外側に置き、commit失敗を成功として記録しない。予期しない例外はERRORで型とstack frameを記録し、例外message / payloadは出さない。RESTとVaadinにも同じ機密保護を適用する。

## D-621 HTTP設定

**Status:** Fixed

Bootのstarter-restclientとHTTP Service Groupsを使用する。接続先・外部API interfaceは案件でinfrastructureへ追加し、baselineに架空clientを作らない。標準のspring.http.clients.connect-timeout / read-timeoutを2s / 10sの安全なdefaultとenvironment variableで設定し、同じprefixのvalidated ConfigurationProperties recordで必須・正のDurationを検証する。自動retryは追加しない。RestClientCustomizerでcorrelation伝播とmethod / host / status / durationだけのloggingを追加し、Group clientにも適用されることをtestで確認する。

参照: [Boot HTTP clients](https://docs.spring.io/spring-boot/reference/io/rest-client.html)。

## D-622 Actuatorとshutdown

**Status:** Fixed

health / info / metricsだけをWeb公開対象とし、healthのみ匿名許可、info / metricsはops:read Authorityを要求する。health detailは表示しない。Actuator専用filter chainをVaadinより先に置く。HikariCP metricsはBoot / Micrometer標準で取得する。graceful shutdownと30sのphase timeoutを明示する。

参照: [Actuator endpoints](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html)、[graceful shutdown](https://docs.spring.io/spring-boot/reference/web/graceful-shutdown.html)。

## D-623 BatchとScheduling

**Status:** Fixed

Boot starter-batch-jdbcとBOM管理のSpring Batch 6.0.5を使う。restart/historyの基盤としてsystem schemaへ公式PostgreSQL metadata DDLをV2で適用する。framework互換性のためmetadataのcolumn名・型・ID生成は公式仕様を維持し、constraint名はrepository規約に合わせる。table-prefixはsystem.BATCH_、Bootのschema自動初期化はnever。migrationの正本はFlyway、jOOQは引き続きfeature schemaだけを生成する。

JobParametersにはcredentialや不要なPIIを渡さない（metadataに永続化される）。framework launcherのparameter全文INFO logはWARN thresholdで抑え、共通listenerからexecution IDとstatusだけを記録する。Job自動起動は無効。productionにsample Jobを作らず、test限定Jobでmetadataの永続化と終了状態を検証する。JobExecutionListenerは共通logging用Beanとして用意し、案件Jobのbuilderで明示登録する。Schedulingは@EnableSchedulingとBoot標準schedulerを使い、productionに@Scheduled methodを作らない。fake Authenticationは使用しない。

参照: [Boot Batch](https://docs.spring.io/spring-boot/reference/io/spring-batch.html)、[Batch 6.0.5 schema](https://github.com/spring-projects/spring-batch/blob/v6.0.5/spring-batch-core/src/main/resources/org/springframework/batch/core/schema-postgresql.sql)。

file storage、CSV / Excel / PDF、event publication、async、retry、cacheはD-400〜D-425の案件依存条件を維持し、このphaseでは追加しない。

---

# 63. Phase 7 Implementation Decisions

## D-630 Documentation toolchain

**Status:** Fixed

単一Composeへdocs profileを追加する。docs-postgresはhostのloopback動的portとdocs network内だけで公開し、tmpfsを使う。Gradle taskごとに一意のCompose project名を生成し、成功・失敗ともfinallyでdownする。dev DBは起動しない。Compose全profileの変数展開に必要なDEV_DB_PASSWORDはdocs taskだけで一時値を渡す。

docker/docs/Dockerfileへtbls 1.96.0、Mermaid CLI 11.17.0とNode 24.14.0、Pandoc / LuaLaTeX / 日本語font / Chromium / Graphviz / Popplerを集約する。Debian 13.1-slimと日付固定のDebian snapshotでOS packageを固定する。fontはimage内にinstallし、repositoryに含めない。Mermaidのtransitive dependencyはpackage-lock.jsonで固定し、image buildではnpm ciを使う。

## D-631 DB資料

**Status:** Fixed

DB documentation生成物はGit管理しない。build/documentation/databaseへtblsのMarkdown / SVGを生成し、補足はdocs/database/notesへ置く。一時PostgreSQLへFlywayで全migrationを適用した実schemaを唯一の入力とし、SQLの独自解析は行わない。tbls 1.96.0内蔵SVG rendererでは文字幅とfontの不一致を確認したため、tblsが出力するDOTを同じimageのGraphviz dotでSVGへrenderする。関連定義の全文はMarkdownに残し、ER図では非表示とする。参照: [tbls renderer](https://github.com/k1LoW/tbls/blob/v1.96.0/output/gviz/gviz.go)。

## D-632 API資料

**Status:** Fixed

Spring Boot 4対応のspringdoc-openapi 3.1.1を使用する。endpointとSwagger UIはdefault無効、明示的に有効化してもops:readで保護する。REST以外はschema対象へ含めない。Spring MVC標準のmappingにversion="v1"を指定してcanonical pathのprefixを保持し、必要なresponse schema / statusだけをController metadataへ補う。

apiDocumentationは専用tagのSpringBootTest / MockMvcと一時PostgreSQLでspringdoc YAMLを取得し、契約を検証してbuild/documentation/api/v1/openapi.yamlへ出力する。通常checkのintegrationTestではこの生成testを除外する。

参照: [springdoc](https://springdoc.org/)、[tbls](https://github.com/k1LoW/tbls)、[Mermaid CLI](https://github.com/mermaid-js/mermaid-cli)。

## D-633 Project PDF

**Status:** Fixed

docs/project/index.mdとdiagrams/*.mmdをsourceとする。MermaidをSVGへ事前renderし、SVGを参照するMarkdownをPandoc / LuaLaTeXへ渡す。日本語fontはNoto CJKを使用する。出力はbuild/documentation/project、図と日本語PDFを目視検証する。documentationは3生成taskを集約し、通常buildへ接続しない。

---

# 64. Phase 8 Implementation Decisions

## D-640 CIとrelease

**Status:** Fixed

GitHub-hosted Ubuntu 24.04でJDK 25、Gradle Wrapper、Dockerを使う。CIはpush / pull_request / workflow_dispatchでcheckとbuildを実行する。PlaywrightのLinux system dependencyは専用Gradle taskから公式CLIのinstall-deps chromiumを呼ぶ。Actionsは公式releaseのcommit SHAへ固定する。通常jobのpermissionはcontents:read、checkout credentialは保持しない。

release workflowはworkflow_dispatchによるbuild-only検証も提供する。publish jobはtag push時だけ実行する。releaseはv* tagをtriggerとし、Gradle taskでstable SemVerのproject versionとtagの完全一致を要求する。snapshot・不正tag・version不一致はpublish前に失敗する。build jobはcheck、JAR、documentation、Buildpacks image生成を行い、別のpublish jobだけにcontents:writeを付与する。GitHub CLIは既存tagをverifyし、JAR・OpenAPI YAML・DB資料ZIP・project PDFを添付する。OCI registryとdeploymentは案件依存のため自動pushしない。application versionの正本は既存Gradle project versionのまま。

## D-641 Buildpacks

**Status:** Fixed

Boot 4.1.1標準のPaketo Noble Java tiny builderを使用し、builder 0.0.187と対応run image 0.0.130へ固定する。image参照はVersion Catalogへ集約する。JVM majorはJava ToolchainからBootが設定する。application image名はproject name / versionに従い、application Dockerfileは作らない。

## D-642 Dependency updates

**Status:** Fixed

Renovateの標準Gradle / Wrapper / Dockerfile / Compose / Actions / npm managerを利用する。Version Catalog内のDocker imageだけregex managerで補完する。PostgreSQLとPaketo imageはそれぞれ同一PRへまとめ、全updateをreviewしてmergeする。自動mergeは無効。Debian snapshotの日付はtoolchain更新時に手動検証して更新する。

参照: [Boot OCI image](https://docs.spring.io/spring-boot/gradle-plugin/packaging-oci-image.html)、[Paketo builder](https://github.com/paketo-buildpacks/builder-noble-java-tiny/blob/v0.0.187/builder.toml)、[Playwright CI](https://playwright.dev/java/docs/ci)、[Gradle Actions](https://github.com/gradle/actions)、[Renovate](https://docs.renovatebot.com/modules/manager/gradle/)、[GitHub Release CLI](https://cli.github.com/manual/gh_release_create)。

---

# 65. Specification Inventory

派生ドキュメントを作成・再生成する前に、以下を照合する。

## Foundation

- [x] Purpose
- [x] Quality priority
- [x] YAGNI
- [x] Java 25
- [x] Gradle Kotlin DSL
- [x] Wrapper
- [x] Toolchain
- [x] Version Catalog
- [x] Dependency policy
- [x] Version management
- [x] Application version ownership

## Repository

- [x] Single root AGENTS.md
- [x] Project structure
- [x] Generated outputs
- [x] README role
- [x] docs roles

## Docker

- [x] docker directory
- [x] single compose.yaml
- [x] dev profile
- [x] docs profile
- [x] docs Dockerfile
- [x] documentation tools
- [x] pinned tool versions
- [x] no compose.docs.yaml
- [x] no application Dockerfile baseline

## Development

- [x] no Dev Container
- [x] host JVM
- [x] no global Gradle
- [x] Node/npm policy
- [x] Gradle as single command surface

## Architecture

- [x] feature modular monolith
- [x] Spring Modulith
- [x] Named Interfaces
- [x] package visibility
- [x] Command architecture
- [x] Query architecture
- [x] Repository location
- [x] DataSource location
- [x] transaction boundaries
- [x] module cycles
- [x] ArchUnit scope

## Error model

- [x] Result
- [x] Success
- [x] Failure
- [x] typed failures
- [x] Exception policy
- [x] rollback-safe conversion
- [x] REST mapping
- [x] Vaadin mapping
- [x] Batch mapping

## IDs / Time

- [x] UUID v7
- [x] DB UUID
- [x] domain wrapper
- [x] Instant
- [x] LocalDate
- [x] LocalTime
- [x] ZonedDateTime
- [x] OffsetDateTime
- [x] Clock
- [x] UTC

## Database

- [x] PostgreSQL
- [x] schema per feature
- [x] cross-feature writes
- [x] cross-feature reads
- [x] naming
- [x] nullability
- [x] numeric
- [x] strings
- [x] boolean
- [x] audit
- [x] physical delete
- [x] FK policy
- [x] Flyway
- [x] migration history open question
- [x] sample data
- [x] no generic master import

## DB environments

- [x] development DB
- [x] test DB
- [x] codegen DB
- [x] documentation DB

## jOOQ

- [x] code generation
- [x] generated package separation
- [x] generated source location
- [x] no Git commit
- [x] infrastructure-only usage
- [x] incremental Gradle behavior

## DB docs

- [x] PostgreSQL COMMENT
- [x] tbls
- [x] ER SVG
- [x] manual notes
- [x] reviewable generated docs
- [x] build artifact docs

## REST

- [x] REST adapter boundary
- [x] DTO boundary
- [x] URL style
- [x] HTTP status
- [x] ProblemDetail
- [x] no ApiResponse wrapper
- [x] Jackson defaults
- [x] API versioning
- [x] deprecation / sunset
- [x] pagination
- [x] typed sort

## Vaadin

- [x] web.ui
- [x] public API only
- [x] no Vaadin types in business layer

## Security

- [x] Spring Security baseline
- [x] project-specific authentication
- [x] Method Security
- [x] Authority
- [x] Role mapping
- [x] SecurityContext boundary
- [x] CurrentUser / Actor
- [x] REST 401/403
- [x] Actuator security

## HTTP

- [x] HTTP Service Client
- [x] RestClient
- [x] WebClient exceptional use
- [x] no RestTemplate
- [x] Service Groups
- [x] DTO isolation
- [x] business Port
- [x] timeout
- [x] retry policy
- [x] external auth
- [x] correlation propagation
- [x] external call logging

## Logging

- [x] AOP/filter/interceptor
- [x] Command logging
- [x] Query logging
- [x] Failure logging
- [x] Exception logging
- [x] sensitive-data protection
- [x] no payload dump
- [x] MDC
- [x] pointcut strategy
- [x] exceptional direct logger use

## Configuration

- [x] environment variables
- [x] no committed secrets
- [x] ConfigurationProperties
- [x] Validation
- [x] typed values
- [x] startup failure
- [x] startup validation test
- [x] minimal profile duplication

## Database pool

- [x] HikariCP
- [x] project-specific tuning
- [x] Actuator metrics

## File I/O

- [x] adapter boundary
- [x] no framework/technical I/O types in business contract
- [x] storage Port
- [x] filename safety
- [x] file validation
- [x] temp cleanup
- [x] large-file memory behavior
- [x] CSV UTF-8/comma/quote/newlines
- [x] Excel/PDF project-specific

## Cache / Events

- [x] no cache baseline
- [x] no cache dependencies baseline
- [x] synchronous default
- [x] Spring Modulith Events
- [x] no event overuse
- [x] async considerations
- [x] no Mail baseline

## Batch / Scheduling

- [x] Batch adapter
- [x] public API dependency
- [x] correct Batch use cases
- [x] no meaningless sample Job
- [x] no auto-run-all
- [x] Failure / Exception semantics
- [x] Scheduling adapter
- [x] no Quartz baseline

## Testing / Quality

- [x] unit tests
- [x] integration tests
- [x] architecture tests
- [x] no H2
- [x] MockMvc
- [x] Vaadin E2E
- [x] limited SpringBootTest
- [x] source sets
- [x] test naming
- [x] Spotless
- [x] Checkstyle
- [x] SpotBugs
- [x] JaCoCo
- [x] check quality gate

## Documentation

- [x] Markdown
- [x] Mermaid
- [x] Mermaid → SVG
- [x] Pandoc
- [x] LuaLaTeX
- [x] Japanese PDF
- [x] databaseDocumentation
- [x] apiDocumentation
- [x] projectDocumentation
- [x] documentation aggregate
- [x] docs excluded from normal build

## Release

- [x] bootJar
- [x] bootBuildImage
- [x] SemVer
- [x] Git tags
- [x] GitHub Actions
- [x] GitHub Release
- [x] release assets
- [x] project-specific deployment
- [x] no production Compose baseline

## Agent workflow

- [x] Decision Ledger source of truth
- [x] single AGENTS.md
- [x] Codex read order
- [x] one phase at a time
- [x] validation workflow
- [x] Ponytail role
- [x] official-doc verification
