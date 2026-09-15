# 技術スタック

採用理由と除外方針は[ADR-001／002](decisions.md#adr-002)を参照する。

| 領域 | 採用技術 | 用途 |
|---|---|---|
| 言語・build | Java 25、Gradle Kotlin DSL、Wrapper、Toolchain | host JVM、再現可能なbuild |
| application | Spring Boot 4.1系、Spring MVC | DI、HTTP、auto-configuration |
| UI | Vaadin 25 core／Flow、Aura | Javaの標準component、responsive画面 |
| 認可・validation | Spring Security、Method Security、Jakarta Validation | 入口と業務操作の保護、型付き設定検証 |
| module | Spring Modulith、ArchUnit | 公開境界・依存規則 |
| DB | PostgreSQL、Flyway、jOOQ、HikariCP | schema履歴、明示的SQL、connection pool |
| ID・時刻 | uuid-creator、JDK Clock／java.time | Clockを注入したUUID v7、UTC |
| 外部HTTP | HTTP Service Client／Group、RestClient | 標準client、timeout、相関ID |
| 定期・一括処理 | Spring Batch JDBC、Spring Scheduling | 実行履歴、案件Job／定期処理の基盤 |
| 観測 | Actuator、Micrometer、SLF4J／Logback、Spring AOP | health、pool metrics、安全な共通ログ |
| test | JUnit、AssertJ、Testcontainers、MockMvc、Playwright | 単体、実DB、API、Chromium UI |
| 静的品質 | Spotless／Eclipse JDT、Checkstyle、SpotBugs、JaCoCo | format、import、規約、欠陥候補、coverage |
| API資料 | springdoc-openapi | Controller metadataからOpenAPI |
| DB資料 | tbls、Mermaid CLI | 実schemaからMarkdown／ER SVG |
| 技術資料 | Markdown、Mermaid | 設計・開発資料と図 |
| 自動化・配布 | GitHub Actions、Renovate、Spring Boot Buildpacks／Paketo | CI、更新PR、JAR／OCI |

## versionの正本

| 対象 | 管理元 |
|---|---|
| Java | [root build](../build.gradle.kts)と[codegen build](../codegen/build.gradle.kts)のToolchain |
| Gradleと配布SHA | [Wrapper properties](../gradle/wrapper/gradle-wrapper.properties) |
| explicit dependency／plugin／BOM／DB・Buildpacks image | [Version Catalog](../gradle/libs.versions.toml) |
| Spring ecosystem | Boot BOM。Modulithは専用BOM |
| Vaadin ecosystem | Vaadin BOM |
| 開発・資料DB image | [Compose](../docker/compose.yaml)。Catalogと同じPostgreSQL tag |
| 資料toolchain | [Dockerfile](../docker/docs/Dockerfile)、[package.json](../docker/docs/package.json)、[lockfile](../docker/docs/package-lock.json) |
| application version | [root build.gradle.kts](../build.gradle.kts)のversion |
| GitHub Actions | [.github/workflows](../.github/workflows/ci.yml)の固定SHA |

patch versionの管理元は上表の設定fileとする。更新時はBOMとframeworkの互換性、migration、生成型、UI、資料生成を確認する。Renovateは更新を提案し、mergeはreview後に実施する。

## 案件の要件で選択する技術

JPA／Hibernate ORM、message broker、Redis、Quartz、Mail、外部認証provider、別のfrontend framework、汎用cache／retry／workflow／master import基盤は案件の要件に応じて採用する。採用条件は[ADR-013](decisions.md#adr-013)を参照する。Hibernate ValidatorはJakarta Validationの実装として利用する。
