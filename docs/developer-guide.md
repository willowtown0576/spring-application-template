# 開発ガイド

[architecture](architecture.md)で責務と依存方向、[ADR](decisions.md)で採用判断を確認する。設計判断が変わる場合はADRを先に追加する。

## 作業単位とブランチ

[ADR-031](decisions.md#adr-031)に従い、一つの変更目的を専用branchで完結させる。新しい作業は既定branchを基点に作業branchを作り、目的に必要な実装・設定・資料・testをまとめる。同じ目的の修正は同じbranchで扱い、別目的の変更は分ける。既定branchでは直接編集せず、merge済みbranchは新しい作業に使わない。AI向けの具体的な規則は[root AGENTS.md](../AGENTS.md#作業ブランチ)を参照する。

## featureを実装する

1. `api.command`／`api.query` に公開interface、用途別Param record、Failure／read modelを定義する。package-info.javaでNamed Interfaceを宣言する。
2. `internal.domain` に不変条件とValue Object、`internal.usecase` に業務調整とRepository Portを置く。
3. `internal.command.XxxCommandsImpl` で入力検証・認可・transaction・UseCase呼び出しを行う。
4. `internal.query.XxxQueriesImpl` で認可・read-only transaction・内部データから公開結果への変換を行う。DataSource Portはinternal.queryに置く。
5. `internal.infrastructure` にRepository／DataSourceのjOOQ実装を置く。外部HTTPやstorageの実装もこの境界に閉じる。
6. `web.rest`／`web.ui` 等のadapterから公開APIを使う。
7. 境界値、DB制約、rollback、認可、module境界を変更に応じて検証する。

feature間の依存は公開APIを通る非循環構造とする。interfaceやwrapperは業務契約・技術境界に必要な箇所へ配置する。

## 公開契約と失敗

Featureの操作名と入力型は同じ語彙・単複を使う。[ADR-033](decisions.md#adr-033)に従い、同じ操作へcreate／register等の同義語を混在させない。入力型は操作＋対象＋Param、HTTP型は操作＋対象＋Request／Responseで命名する。

| Command | 入力型 | 成功値 |
|---|---|---|
| `create` | `CreateFeatureParam` | 作成ID |
| `createMany` | `CreateFeaturesParam` | 入力順の作成ID列 |
| `update` | `UpdateFeatureParam` | 対象ID |
| `updateMany` | `UpdateFeaturesParam` | 実際に変更したID列 |
| `delete` | `DeleteFeatureParam` | 履歴へ保存して削除したID |
| `deleteMany` | `DeleteFeaturesParam` | 入力順の削除ID列 |

すべて`Result<成功値, FeatureCommandFailure>`を返す。失敗型は単件・複数で共有し、Issueのrow／field／reasonを使う。単件のrowは1、複数は入力順の1始まり、操作全体の問題は0。業務拒否の名前・構造を登録・更新・削除で分けない。

| Query | 入力型 | 戻り値 |
|---|---|---|
| `findById` | `FindFeatureByIdParam` | `Optional<FeatureDetails>`。未検出はempty |
| `search` | `SearchFeaturesParam` | `FeatureSearchPage`。itemsと前後の境界 |

`FeatureDetails`は一件の参照内容、`FeatureSearchPage`は検索結果全体、`FeatureSearchCursor`はページ境界、`FeatureSortField`は並び項目を表す。`FeatureStatus`は更新・参照で共用するため`api.model`に配置する。Command入力・Query条件・HTTP request・read modelの役割を型名とpackageで区別する。

複数入力のrowsには単件の入力型を使う。複数登録の実行主体・操作キーは`CreateFeaturesParam`、複数更新の行集合は`UpdateFeaturesParam`が保持する。1〜100行を全件検証し、一つのtransactionで処理する。Param自体／検索IDのnullは呼び出し側の誤りとして拒否する。nameは1〜100 Unicode code point、null・ASCII spaceのみ・NUL・不正surrogateは業務拒否とする。

削除の対象は`FeatureDeletionTarget(id, version)`で表す。`DeleteFeatureParam`は認証主体と一件のtarget、`DeleteFeaturesParam`は認証主体とtargets集合を持つ。HTTP adapterは認証主体をprincipalから取得する。複数削除も1〜100件の全件成功または全件拒否とし、履歴と元データを同じtransactionで移動する。

nameは入力文字列をそのまま保存し、同名を許可する。100個の補助平面文字も100 code pointであり、UTF-16のString.length()とは異なる。

`common.Result<S,F>` は成功／業務失敗の契約、`FeatureDetails` は参照内容のrecord。呼び出し側はsealed switchでvariantを処理する。技術障害はResultへ包まずExceptionのまま伝播させる。

DataSourceは内部FeatureDataを返し、Query実装が公開FeatureDetailsへ変換する。HTTP・HTML画面固有の型はweb adapterへ配置する。

## Beanと不変性

application実装Beanは@Service／@Repository／@Componentとconstructor injectionを使う。UseCaseでは登録用@Serviceだけを許可し、transaction、Security、SQLの制御は各adapterで担う。domainはSpring非依存とする。

UUID生成器はSupplier<UUID>としてUseCaseへ注入する。時刻はClock、testは固定Clock／固定IDを使う。Clock.systemUTC等のJDK型やSecurityFilterChain／RestClientCustomizer等の構成は@Beanを使用する。

再代入しないlocal variable、parameter、fieldにはfinalを付ける。record component等は言語仕様の暗黙finalを使用する。変更可能fieldは必要性を説明し、Checkstyle例外を対象へ限定する。部品集の行採番はブラウザー内のsample識別用であり、業務UUIDとは異なる。

testもconstructor injectionを基本とし、MockitoSpyBeanはclassに宣言してconstructorで受け取る。Beanの可視性は利用範囲に応じて決める。別packageから使用する必要のあるPort等はpublicにできるが、Modulithの公開契約とは区別する。

## JavaDoc・format・import

- 手書きの型とmethodには責務・契約を説明するJavaDocを付ける。parameter、return、必要な例外条件を説明する。フィールドとenum定数にも保持する値・依存先・用途を説明するJavaDocを付け、privateとtestも対象にする。serialVersionUIDはCheckstyle標準の除外対象、record componentは型の@paramで説明する。
- Overrideは `{@inheritDoc}`、test／lifecycleは条件と期待結果を記載する。
- コメントは、現在の仕様・責務・制約の理由を単独で理解できる説明とする。
- Java formatはSpotlessのEclipse JDT。基本4スペース、継続行は追加4スペース、行幅120。設定の正本は[formatter profile](../config/formatter/eclipse-java.xml)。
- `./gradlew spotlessApply` はimportOrder／removeUnusedImportsも実行する。Checkstyleでもunused／redundant importを検査する。継承したnested type等、意味上の冗長importは自動検出に限界があるためreviewする。
- 利用するAPIは採用versionの推奨APIとする。全JavaCompileで `-Xlint:deprecation -Xlint:removal -Xlint:dep-ann -Werror` を適用する。main／test／codegenとも推奨APIへ置き換え、`@SuppressWarnings` のdeprecation／removal／all指定による回避を禁止する。警告を隠す `@Deprecated` 宣言も手書きsourceでは禁止し、Javadocだけのdeprecated宣言はdep-annで拒否する。
- SpotlessのSort Membersでmemberを下表の順に整列する。同じ分類ではpublic→protected→package→private、同じ可視性では名前順とする。method・nested typeも対象になる。
- CheckstyleのDeclarationOrderでフィールド・constructor・methodの配置とフィールドの可視性順を検査する。名前順を含むソート結果との一致はspotlessCheckで検査する。
- 順序に依存する初期化はconstructorまたはinitializerに代入順を明示する。enum定数もソートされるため、宣言順に意味のあるenumを含むfileは理由を添えた `// @SortMembers:doNotSortFields=true` でフィールド・enum定数・initializerの並べ替えを止め、フィールドを手動で規約順に配置する。この場合もDeclarationOrderとJavaDoc検査を適用する。

| 順番 | member |
|---|---|
| 1 | static field |
| 2 | static initializer |
| 3 | instance field |
| 4 | instance initializer |
| 5 | constructor |
| 6 | static method |
| 7 | instance method |
| 8 | nested type |

## REST

Controllerは公開Command／Queryを呼び、HTTP DTOからParamへ変換する。成功応答は標準JSON、エラー応答はProblemDetailとする。

| 例 | 成功・失敗 |
|---|---|
| POST `/api/v1/features`、`{"name":"sample"}` | 201＋Location＋`{"id":"UUID"}` |
| GET `/api/v1/features/{id}` | 200＋id／name／status／updatedAt／version、未登録404 |
| GET `/api/v1/features` | name、status、sort、descending、size、cursor、previousで検索。items／previous／nextを返す |
| PUT `/api/v1/features/{id}` | name／status／versionで単件更新。成功200＋ID |
| POST `/api/v1/features/bulk` | requestId／rows（name／status）で一括登録。成功200＋入力順ID配列 |
| DELETE `/api/v1/features/{id}?version=1` | 確認時のversionで一件を履歴退避して削除。200＋ID |
| POST `/api/v1/features/bulk-delete` | targets（id／version）を全件検査し履歴退避・削除。200＋入力順ID配列 |
| PUT `/api/v1/features/bulk` | id／name／status／versionの配列で一括更新。成功200＋変更ID配列 |
| 更新競合／操作キーの内容不一致 | 409＋ProblemDetail.issues（row、field、reason） |
| 行入力の業務制約違反 | 422＋ProblemDetail.issues。rowは1始まり、全体は0 |
| 不正JSON／UUID、name欠落／null | 400 |
| 認証なし／権限不足・CSRF拒否 | 401／403 |
| 名前の業務制約違反 | 422 |
| 予期しない障害 | 内部情報を伏せた500 |

API major versionはSpring MVC標準のpath segmentで解決する。major versionの更新は非互換変更時とする。pagination／sortを追加する場合はQuery側の型付き契約を先に決め、COUNTの実行は総件数を必要とするQueryに限定する。

## HTML画面

Spring MVC Controllerはweb.uiに置き、feature公開APIを呼ぶ。jteはsrc/main/jteに配置し、表示モデルはweb.uiのrecordを使用する。業務判断・認可・transactionは公開API境界で実行する。UI構成の判断は[ADR-028](decisions.md#adr-028)を参照する。

### テンプレートの配置

[ADR-030](decisions.md#adr-030)に従い、共通部分と機能別の画面を分離する。

| 配置 | 責務 |
|---|---|
| `layouts/base.jte` | HTML head・配布asset・ブラウザー共通状態 |
| `components/navigation.jte` | ボタンで開閉する左ナビ |
| `layouts/application.jte` | 認証後のナビ、header、現在地、本文枠、footer |
| `components/` | page-heading、field、error-message等の共通部品 |
| `pages/home/index.jte` | home全体 |
| `pages/auth/login.jte` | login画面 |
| `pages/feature/index.jte` | DB連携画面全体 |
| `pages/feature/fragments/list.jte` | 初回描画とhtmxで共用する検索・一覧領域 |
| `pages/feature/editor.jte`、`detail.jte` | 登録・更新フォーム、詳細 |
| `pages/feature/delete.jte` | 名前・件数を示す削除確認と拒否理由 |
| `pages/feature/fragments/editor-row.jte`、`filter-fields.jte` | 編集行と戻り先の検索条件 |
| `pages/samples/components/index.jte` | 認証後の部品集全体 |
| `pages/samples/components/fragments/` | 部品集固有の検索一覧・入力部品 |

新しい業務画面は`pages/<機能>/`に追加し、入口は`index.jte`、詳細・編集は`detail.jte`・`edit.jte`等の役割名とする。画面固有の断片は同じ機能の`fragments/`に置き、ページ全体の初回描画とhtmx応答で同じtemplateを呼ぶ。共通部品は`components/`に置き、ページからは共通部品と自機能の断片を使用する。短いHTMLだけを理由に分割しない。公開URLとtemplate pathは独立して扱う。

Controllerは`pages/feature/index`等の拡張子なしのview名を返し、jteからは`@template.layouts.application(...)`や`@template.pages.feature.fragments.list(...)`で呼ぶ。templateは4スペースでインデントし、HTMLの入れ子とjteの条件分岐を揃える。

### URLと表示


| URL | 用途 |
|---|---|
| `/` | 認証後のhome。各機能への入口 |
| `/components` | 認証後の部品集。架空dataはブラウザー内だけに保持 |
| `/login` | 標準フォーム認証 |
| `/features` | 認証必須の条件検索・一覧。検索／前後移動はhtmx部分更新 |
| `/features/detail` | 詳細。ID・version・登録日時を本文に表示しない |
| `/features/new`、`/features/edit` | 単件／複数登録と選択行の編集 |
| GET／POST `/features/delete` | GETで選択対象を確認し、POSTで履歴退避・削除。GETだけでは変更しない |
| POST `/features/save` | 一括保存。成功後は条件を維持して先頭ページへredirect |
| POST `/features/refresh`、`/features/confirm` | 競合時の最新値比較と、確認済みversionでの再編集 |

共通layout、field等のjte部品とBootstrapの標準class・variantで外観を統一する。標準の配布資産はBootstrap CSS・JavaScriptとhtmx・Alpine.jsで構成する。Bootstrap標準部品の動作はそのdata属性で設定し、業務画面内の一時状態はAlpineへ配置する。検索一覧は検索条件・件数・table・ページング、詳細は項目表示と操作、入力はlabel・必須表示・項目error・保存／キャンセルを揃える。固有の装飾を各画面へ追加せず、必要なstyleはstatic/uiへ集約する。

[ADR-035](decisions.md#adr-035)と[ADR-039](decisions.md#adr-039)に従い、ログインはlayouts/base.jteを使い、業務ナビを表示しない。認証後の画面はlayouts/application.jteでheader・現在地・本文・footerを統一する。左ナビは全画面幅で初期状態を閉じ、メニューボタンから開く。aside内のnavをBootstrap Offcanvasで表示し、Escape・focus制御・背景表示・スクロール抑止はBootstrapへ委ねる。ナビの現在地はaria-currentで示す。page-heading.jteで見出しと説明を揃え、検索条件・一覧・補助操作を余白とcardで区切る。状態badgeは名称とBootstrapの意味色を併用する。

通常のURLとform送信を基本に、htmxで通信を伴う領域だけ更新する。UI endpointはHTMLを返し、外部連携RESTを内部通信の必須経路にしない。入力エラーは422のHTML、業務権限不足は403、技術障害は秘匿した500で返す。UI用adviceを優先し、他のMVCエラーは既存のProblemDetailで返す。CSRFはフォームhiddenと、SPA用cookieを読み取るhtmx headerで送信する。context pathはUiPageから生成する。業務画面の履歴snapshot保存は無効にする。

Alpine.jsは表示切替・入力補助等のブラウザー内の一時状態を担当する。永続化・業務検証はサーバーで実施する。同じ部品の開閉をBootstrap JSとAlpineで重複管理しない。htmxが置換するDOM内のAlpine状態は初期化されるため、保持が必要な状態は置換範囲外へ置く。共通の処理はstatic/ui/application.jsへ配置する。部品集はDBを使用せず、検索・ページング・詳細・編集・削除の操作例を示す。

jteはgenerateJteでbuild配下へJavaを生成してcompileする。IDEではGradle buildを使用する。templateを変更したらclassesまたはbootRunを再実行する。ブラウザー資産のversionはCatalogで管理し、prepareUiAssetsで必要なファイルのみ生成resourceへ展開する。ブラウザーはCDNへ接続しない。

UI変更時はキーボード操作、入力エラー、明暗、mobileでのはみ出し、部分更新を実browserで確認する。`./gradlew integrationTest --tests '*FeatureUiIntegrationTest'`で確認でき、captureはbuild/reports/uiへ出力する。

## 可読性

型名はimportして記述する。同名型の衝突やSpEL／AOP等の文字列式で必要な場合に限り完全修飾名を使う。数十行になるmethodやtestは、準備・操作・検証、入力・保存・表示等の意味のまとまりで空行を入れ、意図が読み取りにくい境界には目的や制約をコメントする。自明な処理の逐語説明や、行数を減らすだけのhelper分割は避ける。

## 変更を検証する

```bash
./gradlew spotlessApply build
```

DB変更は[DBガイド](database.md)、test追加は[検証ガイド](testing.md)、設定・認証・外部HTTP・Batchは[運用ガイド](operations.md)を参照する。資料sourceが変わる場合は[資料生成](documentation.md)も行う。

## Featureの検索・一括操作

契約の判断は[ADR-032](decisions.md#adr-032)を参照する。保存する名前は加工しない。検索語だけ前後の空白を除去し、部分一致を大小文字を区別せず検索する。`%`と`_`は文字そのものとして扱う。全角と半角は区別する。statusはACTIVE／INACTIVE、未指定は全状態。登録時の既定値はACTIVE。

sortはNAME／UPDATED_ATのいずれか一つ、descendingはtrue／false。既定値はUPDATED_AT降順、sizeは20（1〜100）。同値は非表示のIDで順序を確定する。cursorはweb adapterが条件と境界を符号化したtokenで、API利用者は中身を解釈せず返されたprevious／nextを使う。previous tokenへ移動するときだけprevious=trueを渡す。条件・並び順・件数を変える場合はcursorとpreviousを外す。COUNT、OFFSET、ページ番号は使用しない。並行更新により行の表示位置は変わる。

登録時はversion=1、created_at=updated_at。名前または状態が実際に変わる行だけversionを1増やし、同じ操作の変更行には共通のupdated_atを設定する。日時はUTCのmicrosecondで保存し、画面は更新日時だけを日本時間の秒単位で表示する。更新入力には読取時のversionを必ず送る。

複数更新は一覧の同一ページで選択した行を専用フォームで編集する。異なる値を各行へ入力でき、一行でも不正・未検出・競合なら全行を保存しない。入力は失敗時にも保持する。競合時は最新値を比較し、利用者が明示的に確認した後に再編集する。確認後の再競合も同じ検査で拒否する。Alpine.jsは行の追加・削除、選択状態、未保存の移動確認を担当する。

複数登録のrequestIdは操作ごとに発行したUUIDを使い、通信再送時は同じ値と内容を送る。HTTP adapterは認証主体からactorを取得する。サーバーはactor／requestIdと入力順を含むhash・結果IDを同じtransactionで保存する。同一キーで内容を変えた場合は409。キー記録は自動削除せず、保持・削除方針を変更する案件は重複登録が起こり得る期間を含めてADRで定める。

## 履歴退避を伴う削除

[ADR-036](decisions.md#adr-036)に従い、削除はfeature.feature_historyへの保存とfeature.featureからの除去を同じtransactionで行う。historyには削除直前の全カラム、共通deleted_at、認証主体のdeleted_byを保存する。statusの無効化とは異なり、通常検索・ID参照の対象から外れる。versionとupdated_atは削除で変更しない。

UIは詳細画面の削除、または一覧の現在ページで選択した対象を確認画面に渡す。確認対象の名前・件数を見て明示的に確定する。競合時は確認したversionを維持し、再取得した最新内容を確認し直す。成功後は検索条件を維持して一覧先頭へ戻る。権限・CSRF・version・対象重複・件数制限はサーバーで検証する。

削除済みの対象への再送は404。履歴を追加・上書きしない。登録の再送台帳は削除せず、同じ登録操作を再送しても元のIDを返すだけで復活させない。履歴参照・復元APIと画面、登録・更新ごとの監査履歴は提供しない。保管・復元方針を拡張する場合は案件のADRで契約を定める。

## ナビ・一括操作・テーマ

[ADR-039](decisions.md#adr-039)に従い、左ナビはBootstrap Offcanvasの標準slideと背景表示で開閉する。data-bs-toggle／data-bs-targetとdata-bs-dismissで操作し、独自の開閉状態やanimationは持たない。表示中はBootstrapがrole=dialogとaria-modalを付け、focusを内部へ移して保持する。Escape・背景クリック・閉じるボタンで閉じ、起点のメニューボタンへfocusを戻す。prefers-reduced-motionへの対応もBootstrap標準を使用する。

一括編集・削除は検索結果の見出しと登録操作に続けて、表より上へ配置する。選択件数と操作方法を示し、未選択時は操作を無効にする。条件変更・ページ移動で選択を解除する。各行にも編集と確認画面へ進む削除を置く。編集はBootstrap success、削除はdangerの背景・白文字を用い、登録のprimaryと区別する。

workspaceShellのdarkはlocalStorageのspring-application-template.themeにlight／darkとして保存する。application.jsをCSSより前に読み込み、保存値を初回描画前に適用する。Alpine初期値にも同じ値を使い、戻る操作のpageshowでも復元する。Storage拒否時は例外で停止せず、現在ページの切り替えを提供する。保存対象は表示設定のみとし、業務データや認証情報を含めない。

操作ボタンの幅と配置はADR-038に従う。共通CSSの`btn-action`（8rem）、`btn-action-wide`（13rem）、`btn-action-row`（5rem）を使い、ラベルに依存しない幅とする。検索条件とデータ操作を見出しと余白で分け、操作群は右揃え・折り返し可能にする。

HTML要素は動作に合わせて選ぶ。画面移動はhref付きa、ページ内の処理はtype=button、入力確定はform内のtype=submitとする。部品集の入力確認もformの標準検証を使用する。sectionには用途を示す見出し、tableにはcaptionを設け、視覚的な見出しが重複するcaptionはvisually-hiddenを使用する。

検索語は100 Unicode code pointまでとし、ブラウザーでもcode point数を検証する。HTMLのmaxlengthは補助文字のUTF-16長を考慮して200とする。htmxの400応答は共通のエラー通知欄に表示する。検索条件を含むリンクは名前をURI変数として厳密にエンコードし、`+`等の予約文字を画面遷移で維持する。
