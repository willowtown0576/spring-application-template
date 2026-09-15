# AI向け実装指示

AI向け指示はrootのAGENTS.mdへ集約する。設計・手順の正本はdocsとし、対象領域のガイドを参照する。

## 読む順序

1. [README](README.md): 目的、起動、開発の入口。
2. [docs/index.md](docs/index.md): 対象領域の資料を選ぶ。
3. [ADR](docs/decisions.md): 設計判断の正本。資料・実装が矛盾した場合はこちらを優先する。
4. 対象領域のガイド、既存実装、呼び出し元、関連test。

主な参照先は[architecture](docs/architecture.md)、[開発規約](docs/developer-guide.md)、[DB](docs/database.md)、[検証](docs/testing.md)、[運用](docs/operations.md)、[資料生成](docs/documentation.md)。

案件への転用は[案件開始チェックリスト](docs/project-adoption.md)を使い、名称・sample・権限・生成型・検査・資料の連動先を確認する。自動化案は採用済み機能と区別し、専用initializerや外部設定処理は方式の採用後に実装する。

## 成果物の書き方

- **生成物は会話の成果物ではなく、最初から存在していたコードとして書くこと。**
- コード、JavaDoc、コメント、README、docsは、会話を知らない開発者が理解できる現在の契約・責務・理由・手順を書く。
- 説明は提供する機能・担当する処理・適用範囲を直接示す。制約は必要性を確認し、許可する対象・条件・配置先として明記する。過去の会話との対比は作業報告で扱う。
- 製品の説明は、現在の仕様・責務・手順として記述する。作業経緯・進捗・実行結果の記録先は会話、Issue、CI reportとする。
- ADRには設計上の課題、選択理由、影響、後継判断を記録する。
- 依存関係・構成・処理フローの図はMermaidで記述する。コマンドやコードは対応言語のコードブロック、項目の比較は表を使う。
- 自明な処理の言い換えでコメントを増やさない。既存JavaDoc規約を守り、境界条件・失敗時の意味を説明する。

## 変更の前提

- 新しい設計判断は実装前にADRへ記載する。手順や契約を変えたら関連docsも合わせる。
- 既存の未commit変更を読み、利用者の変更を保持する。変更範囲は依頼の達成に必要な実装・設定・資料とする。
- 正しさ、型安全性、整合性、security、責務、依存方向を優先する。単純化においてもvalidation、transaction、test、再現性を品質条件として保持する。
- JDK、Spring、導入済みlibraryの順で標準機能を調べる。独自実装は標準機能と既存libraryで要件を満たせない場合に検討する。
- feature公開API、Method Security、transaction、生成SQL型の参照境界を維持する。詳細はADRとarchitectureに従う。
- dependency／plugin／imageのversionは既存の管理元へ置く。version依存のAPIを変更する場合は採用versionの公式仕様を確認する。
- secret、ローカル設定、生成source、生成資料、test reportはGit管理外とする。適用済みschemaの変更は新migrationで行う。
- 部品集はVaadin標準component・theme・variantを使う。装飾と配色はVaadin提供のstyleへ統一する。

## 検証と完了報告

- formatは `./gradlew spotlessApply`。完了前に `./gradlew build` を実行する。
- unitTestは単体、testは単体・結合・アーキテクチャとcoverage、checkは静的検査、buildはcheck・testと成果物作成。
- 変更に応じた意味のあるtestを更新する。詳細なtask・report・保証範囲は[検証ガイド](docs/testing.md)を参照する。
- DB／API sourceや資料toolchainを変えた場合は該当資料taskを実行する。ER図は描画結果、UIは実ブラウザーで表示と操作を確認する。
- 品質検査を有効に保ち、検出した問題を修正する。DockerはDB testの必須実行条件とする。
- 成功した検査、失敗した検査、未確認の環境依存事項を区別して報告する。報告は実際に得られた実行結果を根拠とする。
- 補助skillの適用時も本書とADRの品質要件を満たす。
