# ドキュメント

開発の入口は[root README](../README.md)。設計判断の正本は[ADR](decisions.md)。各ガイドは現在の構成と操作方法を説明する。

| 目的 | 資料 |
|---|---|
| 初回起動・ローカル設定 | [初期セットアップ](getting-started.md) |
| 案件への変更箇所・完了条件・自動化の選択肢 | [案件開始チェックリスト](project-adoption.md) |
| 採用技術・versionの管理元 | [技術スタック](technology-stack.md) |
| moduleと依存方向を理解する | [architecture](architecture.md) |
| feature・REST・UIを実装する | [開発ガイド](developer-guide.md) |
| schemaとSQL型を変更する | [DBガイド](database.md) |
| test・静的解析・配布境界を検証する | [検証ガイド](testing.md) |
| 認証・設定・ログ・Batch・配布を扱う | [運用ガイド](operations.md) |
| ビルド・生成ツールの内部動作と保守を理解する | [ツールチェーン保守ガイド](toolchain-maintenance.md) |
| DB・API資料を更新する | [資料生成](documentation.md) |
| 判断の理由・変更方法を確認する | [ADR](decisions.md) |
| AIによる変更の前提を確認する | [AGENTS.md](../AGENTS.md) |

生成物はbuild/documentationへ出力する。詳細は[資料生成](documentation.md)を参照する。
