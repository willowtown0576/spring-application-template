# Starter完成条件の検証

[AGENTS.md](../AGENTS.md) §80の全46項目を、実装と検証結果へ対応付ける。設計判断は[decisions.md](decisions.md)、phase別の詳細履歴は[implementation-plan.md](implementation-plan.md)が正本となる。

検証日: 2026-09-14。ローカルはmacOS / Java 25 / Docker Linux arm64。GitHub検証はUbuntu 24.04 / Java 25 / Linux amd64。

| 完成条件 | 実装・検証証跡 |
| --- | --- |
| Java 25 / Gradle buildが再現可能 | Java 25 / Wrapper 9.7.0、`clean check build --no-build-cache` 成功。Toolchain / Version Catalogを使用。 |
| Gradle Wrapperが利用可能 | Java 25 / Wrapper 9.7.0、`clean check build --no-build-cache` 成功。Toolchain / Version Catalogを使用。 |
| Java Toolchainが設定されている | Java 25 / Wrapper 9.7.0、`clean check build --no-build-cache` 成功。Toolchain / Version Catalogを使用。 |
| Version Catalogでversion管理されている | Java 25 / Wrapper 9.7.0、`clean check build --no-build-cache` 成功。Toolchain / Version Catalogを使用。 |
| Spring Boot applicationが起動可能 | 配布JARとOCIを一時DBで起動。health UP、REST / UI / info / OpenAPIの未認証401を確認。 |
| Vaadinが利用可能 | Chromium E2Eで作成・取得・入力不正・認可・共通error通知を検証。production frontendをJARへ同梱。 |
| REST foundationが利用可能 | 配布JARとOCIを一時DBで起動。health UP、REST / UI / info / OpenAPIの未認証401を確認。 |
| Spring Security foundationが利用可能 | 配布JARとOCIを一時DBで起動。health UP、REST / UI / info / OpenAPIの未認証401を確認。 |
| PostgreSQL development environmentが利用可能 | `docker/compose.yaml` 一つにdev / docs profile。Phase 2でbootRunとvolume保持、Phase 7で一時DB破棄を確認。 |
| Docker関連設定が `docker/` に集約されている | `docker/compose.yaml` 一つにdev / docs profile。Phase 2でbootRunとvolume保持、Phase 7で一時DB破棄を確認。 |
| Compose fileが一つ | `docker/compose.yaml` 一つにdev / docs profile。Phase 2でbootRunとvolume保持、Phase 7で一時DB破棄を確認。 |
| `dev` / `docs` profilesが利用可能 | `docker/compose.yaml` 一つにdev / docs profile。Phase 2でbootRunとvolume保持、Phase 7で一時DB破棄を確認。 |
| `docker/docs/Dockerfile` でdocumentation toolchainを再現可能 | 固定tool imageでDB Markdown / 8 ER SVG / 日本語PDF 2ページを生成。Phase 7で図・全文を目視検証。 |
| Flyway migrationが動作 | Flyway V1 / V2とTestcontainers、独立codegen / docs DBを使用。clean buildとDB統合test成功。 |
| development/test/codegen/documentation DBが分離されている | Flyway V1 / V2とTestcontainers、独立codegen / docs DBを使用。clean buildとDB統合test成功。 |
| jOOQ code generationが再現可能 | Flyway V1 / V2とTestcontainers、独立codegen / docs DBを使用。clean buildとDB統合test成功。 |
| generated sourceをGit管理していない | ルート限定の`/build/` / `/bin/`で生成物を除外。手書き`src/codegen`はGit管理。 |
| Spring Modulith boundaryを検証可能 | `check`でUnit 12 / Integration 42 / Architecture 7、Spotless / Checkstyle / SpotBugs / JaCoCo成功。 |
| Unit / Integration / Architecture Testが分離されている | `check`でUnit 12 / Integration 42 / Architecture 7、Spotless / Checkstyle / SpotBugs / JaCoCo成功。 |
| PostgreSQL Testcontainersが動作 | Flyway V1 / V2とTestcontainers、独立codegen / docs DBを使用。clean buildとDB統合test成功。 |
| Spotlessが動作 | `check`でUnit 12 / Integration 42 / Architecture 7、Spotless / Checkstyle / SpotBugs / JaCoCo成功。 |
| Checkstyleが動作 | `check`でUnit 12 / Integration 42 / Architecture 7、Spotless / Checkstyle / SpotBugs / JaCoCo成功。 |
| SpotBugsが動作 | `check`でUnit 12 / Integration 42 / Architecture 7、Spotless / Checkstyle / SpotBugs / JaCoCo成功。 |
| JaCoCo reportを生成可能 | `check`でUnit 12 / Integration 42 / Architecture 7、Spotless / Checkstyle / SpotBugs / JaCoCo成功。 |
| `./gradlew check` がquality gateとして機能 | `check`でUnit 12 / Integration 42 / Architecture 7、Spotless / Checkstyle / SpotBugs / JaCoCo成功。 |
| logging / MDC correlation ID foundationが存在 | MDC復元、機密値非出力、Command outcome、outgoing correlationをtestで検証。 |
| Actuatorが利用可能 | 配布JARとOCIを一時DBで起動。health UP、REST / UI / info / OpenAPIの未認証401を確認。 |
| Hikari metricsが観測可能 | 認可済みのActuator requestでHikariCP metricsを取得。 |
| HTTP Service Client foundationが利用可能 | HTTP Service Groupの実通信・read timeout・retryなしと、必須Durationの起動時validationを検証。 |
| ConfigurationPropertiesが型安全かつvalidation可能 | HTTP Service Groupの実通信・read timeout・retryなしと、必須Durationの起動時validationを検証。 |
| configuration不足をstartup testで検出可能 | HTTP Service Groupの実通信・read timeout・retryなしと、必須Durationの起動時validationを検証。 |
| graceful shutdownが有効 | Boot標準graceful shutdownを有効化。JAR / OCIの終了を確認。負荷中のdrainは運用環境で検証。 |
| file I/O policyが実装可能 | D-400〜D-409のadapter / Port / file safety方針。案件のstorage要件がないため実装を追加しない。 |
| Batchを必要時に利用可能 | JDBC metadataとJobの完了・重複拒否・失敗後再実行を検証。production Jobは未登録。 |
| Schedulingを必要時に利用可能 | Boot schedulerでtask実行を検証。業務の定期処理は未登録。 |
| DB documentationを生成可能 | 固定tool imageでDB Markdown / 8 ER SVG / 日本語PDF 2ページを生成。Phase 7で図・全文を目視検証。 |
| OpenAPI documentationを生成可能 | springdoc YAML生成test成功。v1契約、ops:read、default公開無効を検証。 |
| Markdown / Mermaid / Pandoc / LuaLaTeXでproject PDFを生成可能 | 固定tool imageでDB Markdown / 8 ER SVG / 日本語PDF 2ページを生成。Phase 7で図・全文を目視検証。 |
| `./gradlew documentation` でdocumentation一式を生成可能 | 固定tool imageでDB Markdown / 8 ER SVG / 日本語PDF 2ページを生成。Phase 7で図・全文を目視検証。 |
| executable JARを生成可能 | executable JARの起動とfrontend同梱、test用認証・Playwright非同梱を確認。 |
| OCI imageを生成可能 | 固定Paketo builder / run imageでlocal linux/arm64とGitHub linux/amd64の生成成功。localでnon-root（1002:1001）起動を確認。 |
| GitHub Actions CIが動作 | [CI run 34794141586](https://github.com/willowtown0576/spring-application-template/actions/runs/34794141586) がUbuntu 24.04で成功。 |
| Renovateがdependency updateを検出可能 | Renovate 44.83.0公式imageでstrict validationとextract dry-run成功。11設定ファイル / 62依存参照を検出。Appの定期PR実行は未検証。 |
| release foundationが存在 | 4成果物の収集とtag一致 / 不一致 / 不正 / snapshot拒否をローカル検証。[Release run 34794156551](https://github.com/willowtown0576/spring-application-template/actions/runs/34794156551) のbuild / upload成功、publishはskip。 |
| 不要なbusiness sampleが存在しない | sampleはliteral featureの作成・取得のみ。domain固有sample、cache / retry / event等の先行frameworkなし。 |
| speculative abstractionが存在しない | sampleはliteral featureの作成・取得のみ。domain固有sample、cache / retry / event等の先行frameworkなし。 |

## 公開・案件接続の範囲

GitHub Releaseの実公開とOCI registryへのpushは実行していない。application versionは0.1.0-SNAPSHOTのまま維持する。認証provider・production secret・deployment・branch保護・Renovate Appの接続は案件側の設定となる。Windowsは実行環境がなく未検証。
