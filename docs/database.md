# Database

この文書は [Decision Ledger](decisions.md) の派生資料である。開発DB・migration・codegen・DB統合テストはPhase 2で実装済み。documentation用DBと生成taskはPhase 7で実装済み。進捗は [実装計画](implementation-plan.md) を参照する。

## 所有権とデータ整合性

PostgreSQL schemaは原則feature単位で所有する。他feature schemaへの直接writeは禁止し、public Command APIを通す。readのcross-feature JOINは許可する。具体的なcross-feature query構成とcross-feature FKは必要になるまで固定しない（D-190〜D-194、D-220〜D-221）。

identifierはsnake_case、own PKは `id`、参照は `<target>_id`。defaultはNOT NULL、削除はphysical delete。audit columnは必要なtableにだけ追加し、soft deleteや履歴用columnを機械的に追加しない（D-200〜D-213）。

| 概念 | PostgreSQL | Java |
| --- | --- | --- |
| ID | `uuid` | `UUID`（application/domain側でv7生成） |
| 実時刻 | `timestamptz` | `Instant` |
| 業務日付 | `date` | `LocalDate` |
| 時刻のみ | `time` | `LocalTime` |
| 金額・小数 | `numeric(p,s)` | `BigDecimal` |
| 長さ制限のある文字列 | `varchar(n)` | `String`（ValidationとDB制約を整合） |
| 長さ制限のない説明 | `text` | `String` |

constraint/index名は `pk_<table>`、`fk_<table>_<referenced_table>`、`uk_<table>_<column...>`、`ix_<table>_<column...>`、`ck_<table>_<purpose>` とする。

## 4用途のDB

| 用途 | 実行環境 | lifecycle |
| --- | --- | --- |
| Development | Docker Compose `dev` profile | persistent named volume |
| Test | PostgreSQL Testcontainers | test用に隔離し、終了後破棄 |
| jOOQ codegen | temporary PostgreSQL | empty DB → Flyway → codegen → 破棄 |
| Documentation | Docker Compose `docs` profileのtemporary PostgreSQL | empty DB → Flyway → tbls → 破棄 |

開発DBをcodegenやdocumentationに流用しない（D-250〜D-265）。Docker設定は一つの `docker/compose.yaml` に集約する。

PostgreSQLは `postgres:18.6`。ComposeとVersion Catalogのimageは同時に更新する。開発DBは `DEV_DB_PASSWORD` を必須とし、hostにはloopbackの動的portだけを公開する。PostgreSQL 18のnamed volume mount先は `/var/lib/postgresql`。

## FlywayとjOOQ

Flyway migrationをschema変更履歴の正本とし、適用済migrationの変更は避け、新migrationを追加する。開発時は `bootRun` によるCompose起動・Flyway適用・application起動を接続する。productionも原則startup時に適用する（D-230〜D-235）。

jOOQはmigration済みの一時DBから `build/generated-src/jooq/` に生成し、compileJavaへ接続する。schemaごとのgenerated packageを分離し、generated sourceはcommitしない。Gradle inputs/outputsで不要な再生成を避け、一時DBは失敗時も後始末する。

migrationは `src/main/resources/db/migration/feature/V1__create_feature.sql` から開始し、feature別directoryを標準再帰scanする。version番号はapplication全体で一意とする。共通historyは `public.flyway_schema_history`、feature schemaはSQLで作成する（D-581）。

最小table `feature.feature` はapplicationが付与するUUIDの `id` と100文字以内の `name` を持つ。NOT NULL・PK・ASCII spaceだけの名前を拒否するcheckをDBで保証する。sample dataは登録しない。

`src/codegen/java/` の小さな実行classをGradle JavaExecから呼び、Testcontainers → Flyway → jOOQ GenerationToolを実行する。generated packageは `dev.template.application.jooq.feature`。jOOQ標準命名によりtable参照は `Feature.FEATURE_` となる。generated codeは手書きコード用Checkstyle / SpotBugsの対象外とする。

Flyway / jOOQ / JDBC driver / TestcontainersはapplicationとcodegenでBoot BOMに揃える。変更のないcodegenはUP-TO-DATEとなり、一時DBを起動しない。compileからも同じtaskを呼ぶ。

Integration TestはServiceConnectionで別containerへ接続し、Composeを無効化する。Dockerが使えない場合はskipせず失敗する。testはtransaction rollbackで隔離し、migrationの再適用不要・COMMENT・generated typeによる読み書き・DB境界値を検証する。

## DB documentation

SQL文字列を独自parseせず、実際にmigrationしたPostgreSQLをtblsで参照する。table/columnの説明はPostgreSQL COMMENTから取得する。設定は `.tbls.yml`、ERはSVGを優先する（D-270〜D-276）。

出力は `build/documentation/database/`、手書き補足は `docs/database/notes/`。生成ファイルは手編集しない。生成物はGit管理せず、`docs/database/generated/` は使用しない（D-631）。

`./gradlew databaseDocumentation` を `documentation` に集約する。一意のCompose projectとtmpfsを使用し、成功・失敗時とも一時DBを削除する。固定tool versionはD-630を参照する。project PDF用のMermaid CLI・Pandoc・LuaLaTeX・日本語環境はtblsとともに `docker/docs/Dockerfile` に可能な限り集約する。

## 実装前に確定する事項

- 案件でevent publicationが必要になった場合のModulith metadata配置。


これらは公式仕様を確認したうえで、先に `decisions.md` の対象Decisionへ反映する。認証方式、audit actor、cross-feature FK、pool tuning等の案件依存事項は一括して固定しない。

Phase 3ではschemaを変更せず、上記tableへの作成とID取得をpublic APIから接続した。名前の長さはPostgreSQLと同じUnicode code point数で検証する。UTF-16のcode unit数とは異なるため、adapterの長さ制限もこの契約に揃える。transaction rollbackとread-onlyはDB統合テストで検証済み。

## Spring Batch metadata

Phase 6の `system/V2__create_batch_metadata.sql` はSpring Batch 6.0.5公式PostgreSQL schemaを基にする。6 tableと3 sequenceをsystem schemaへ作成する。framework互換性のため、数値ID・timestamp・nullable column等は公式仕様を維持し、業務tableのUUID / Instant規約を機械的に適用しない。constraint名はrepository規約に合わせている。

Bootの `spring.batch.jdbc.table-prefix=system.BATCH_` と `initialize-schema=never` により、初期化をFlywayへ一本化する。jOOQ生成対象はfeature schemaだけを維持する。Batch version更新時は公式schema差分を確認し、変更が必要なら新migrationを追加する。

Job instance・execution・step履歴はPostgreSQLに保存する。Integration Testで正常終了、完了済みinstanceの重複実行拒否、失敗から同じinstanceへの再実行を検証する。JobParameters / execution contextへcredentialや不要なPIIを保存しない（D-623）。
