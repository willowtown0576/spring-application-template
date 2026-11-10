# 資料の生成と更新

設計判断は[ADR-015／017／023／024](decisions.md#adr-015)。手書きのMarkdown／Mermaidと、実DB／Controller metadataから資料を生成する。製品資料には現在の設計・仕様・手順を記載する。

内部処理・task依存・cache・障害調査は[ツールチェーン保守ガイド](toolchain-maintenance.md)を参照する。

## 手書き資料の更新

READMEは起動・開発の入口、docs/index.mdは目的別案内、ADRは判断の正本。各ガイドは現在の手順と責務を説明する。設計変更はADRを先に更新し、説明の重複による矛盾を避ける。

| 変更対象 | 編集するsource |
|---|---|
| 設計判断 | docs/decisions.mdにADRを追加 |
| setup・開発・運用 | 対応するdocsのガイド、必要に応じREADME |
| DB構造・説明 | 新しいFlyway migrationのDDL／COMMENT |
| DB手書き補足 | docs/database.md、判断理由はADR |
| API契約 | Controller／DTOのmetadata、FeatureOpenApiConfigurationの説明・例とAPI test |
| 構成図・シーケンス図 | 対応するガイド内のMermaid block |
| AIの前提 | root AGENTS.md。設計の本文はdocsへ置く |

## 生成command

```bash
./gradlew documentation
```

| 個別task | 出力 |
|---|---|
| databaseDocumentation | build/documentation/database。Markdown／ER SVG |
| apiDocumentation | build/documentation/api/v1/openapi.yaml |

資料生成の実行入口はdocumentationとする。資料だけの変更なら該当taskを実行できる。全ての出力はGit管理外。生成物の修正はsourceの更新と再生成によって行う。

## toolchainとDB

[Dockerfile](../docker/docs/Dockerfile)にtbls、Mermaid CLI、日本語fontをまとめる。image／Node／Mermaidを固定し、OS packageはDebian snapshot、npmはlockfileとnpm ciで再現する。hostへの各toolやfontのinstallは不要。

DB資料は一意のCompose projectの一時PostgreSQLにFlywayを適用し、tblsが実schemaを読む。ERはtblsがMarkdownへ出力したMermaid blockをMermaid CLIでSVGへ描画する。dataは資料用DB内に隔離する。資料用credentialはtask内で生成し、成功・失敗時とも一時DBを破棄する。

DB資料の対象は実schemaから取得する。.tbls.ymlで内部schemaとFlyway履歴を除外し、業務schemaの追加を自動反映する。

API資料はTestcontainersとMockMvcで起動したapplicationから取得する。通常のOpenAPI／Swagger UIは無効。有効化した環境でもops:readが必要であり、その保護も生成testで確認する。

### APIを追加する場合

web.rest配下にControllerとMapping、入出力DTOを追加するとspringdocが認識する。個別Controllerの登録一覧は不要。別packageへ配置する場合はapplication.ymlのpackages-to-scanを更新する。操作説明、条件別の応答、ResponseEntityの複数のbody型等は@Operation／@ApiResponse／@Schemaで補足する。項目の型だけでは伝わらない利用契約・関連項目・具体例はfeature別のOpenApiCustomizerで補う。Feature sampleはFeatureOpenApiConfigurationを使用し、業務APIへHTTP資料用annotationを持ち込まない。共通の400／401／403／500は未定義の場合だけ追加し、個別APIの定義を優先する。

`./gradlew apiDocumentation` は環境変数の追加なしで実行できる。ApiDocumentationTestは取得・保護・ファイル出力を担当する。FeatureApiDocumentationTestはsample契約の検査であり、sampleを変更・削除するときに一緒に更新する。新APIの契約testを追加し、出力YAMLを確認する。

### DBのSVGを読む

READMEのREADME-1.svgは全体の外部キー関係、table別SVGは対象tableと直接隣接するtableの関係を示す。関係カラムに絞り、全カラム・型・制約・COMMENTはMarkdownの表で確認する。関連のあるtable間に関係線を表示する。SVGは静的な関係図であり、ブラウザーで直接開いて拡大できる。

tblsは図の表示カラム、関係の距離、Viewpointsによる業務単位の分割、Mermaid／PlantUML／DOT出力にも対応する。tableが増えた場合は全体図へ情報を詰め込まず、.tbls.ymlにViewpointsを定義する。標準機能の詳細は[tbls公式設定](https://github.com/k1LoW/tbls/tree/v1.96.0#er-diagram)を参照する。

## 生成後の確認

- DB: table／column／constraint／COMMENTとERがmigrationの内容に一致すること。
- API: path、入出力schema、statusと認証・CSRFの説明がControllerの契約に一致すること。
- Markdown: 相対link、ADR anchor、commandとfile名が有効であること。

初回image buildにはnetwork、時間、disk領域が必要。生成に失敗した場合は失敗したtaskのログを確認し、空や古い出力を成功した資料として扱わない。

## APIの説明・具体例

[ADR-034](decisions.md#adr-034)に従い、生成YAMLだけでも次を判断できる情報を記載する。

- 各操作の目的、必要権限、必須項目、既定値、文字数・件数・versionの制約。
- 登録・参照・更新でIDと名前が対応する、架空の業務データによる入力・応答例。
- 複数操作の二行以上の例、各行で異なる更新内容、行番号・項目・理由を持つエラー例。
- version競合時の最新値確認と再編集、再送キーの再利用条件、変更なしの扱い。
- 検索条件・sort・方向・件数とcursorの関係、前後移動・終端・空結果・閲覧中の変動。
- session・CSRF cookie/headerの取得と送信、認証・権限・入力不正・競合・技術障害のstatus。

例のUUIDは架空の値とし、稼働DBへseed dataを要求しない。ID・version・cursorは実際の応答から取得して使用する。cursor例は実装と同じcodecで生成する。FeatureApiDocumentationTestは生成メタデータから入力例を取得し、隔離DBの実APIへ送って契約との一致を確認する。

OpenAPI YAMLの取得・保存はUTF-8とし、日本語の説明を文字化けさせない。生成物を編集する修正は行わず、sourceと契約testを更新してapiDocumentationを再実行する。
