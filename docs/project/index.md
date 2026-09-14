---
title: "Spring Application Starter"
subtitle: "技術基盤ガイド"
lang: ja
documentclass: ltjsarticle
mainfont: Noto Serif CJK JP
CJKmainfont: Noto Serif CJK JP
CJKsansfont: Noto Sans CJK JP
sansfont: Noto Sans CJK JP
monofont: DejaVu Sans Mono
geometry: margin=22mm
fontsize: 11pt
colorlinks: true
---

# 目的と設計

小規模・中規模の業務システムを、少人数で高品質に構築するための技術基盤です。業務固有の機能を追加する前に、設計、データ整合性、認可、検証と資料生成の手順を揃えます。

設計判断の正本は `docs/decisions.md` です。詳細は `docs/architecture.md`、DB方針は `docs/database.md`、実装状況は `docs/implementation-plan.md` を参照してください。

![公開APIを境界とした依存方向](diagrams/architecture.svg){width=95%}

CommandはUsecaseとDomainを経由し、transaction内で更新します。Queryはread modelを取得し、read-only transactionを使用します。RESTとVaadinは公開APIだけを呼びます。

# 開発と品質保証

必要な環境はJDK 25、Git、Docker、editorまたはIDEです。Gradle Wrapperを使用し、global GradleやNode/npmの管理を要求しません。

| コマンド | 内容 |
|---|---|
| `./gradlew bootRun` | 開発DBの起動とapplication実行 |
| `./gradlew check` | test・静的解析・formatの検証 |
| `./gradlew bootJar` | 配布用JARの生成 |
| `./gradlew documentation` | DB・API・project資料の生成 |

開発DBのpasswordは `DEV_DB_PASSWORD` で指定します。認証方式は案件側で接続し、作成は `feature:write`、取得は `feature:read` を要求します。

\newpage

# データベース

PostgreSQL schemaはfeature単位で所有します。Flyway migrationが変更履歴の正本です。jOOQ sourceは一時DBへmigrationを適用して生成し、開発DBを流用しません。

最小sampleは `feature.feature` です。UUID v7のIDと100文字以内の名前を保持します。Spring Batchの履歴は `system` schemaへ保存します。metadataの型はframework仕様に従います。

DB資料はmigration済みの一時PostgreSQLをtblsで参照し、MarkdownとSVGとして生成します。手書きの補足は `docs/database/notes/` に置き、生成物を直接編集しません。

# 運用基盤

healthは匿名で確認でき、infoとmetricsは `ops:read` を要求します。HTTP timeoutは環境変数で設定し、不正値は起動時に拒否します。correlation IDでHTTPと処理のログを関連付けます。

BatchはJDBCによる履歴と再実行を利用できます。Jobの自動起動は無効です。Schedulingも利用可能ですが、業務Jobや定期処理は案件側で追加します。

# 資料の再生成

`documentation` は3つのtaskを集約します。DB資料は実schema、OpenAPIはSpring MVC metadata、本文書はMarkdownとMermaidから生成します。すべての成果物は `build/documentation/` に出力します。

生成用DBは処理後に破棄されます。通常のbuildは、PDFやDB資料の生成を要求しません。最新の検証結果と残作業は実装計画に記録します。
