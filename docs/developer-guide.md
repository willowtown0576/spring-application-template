# 開発ガイド

[architecture](architecture.md)で責務と依存方向、[ADR](decisions.md)で採用判断を確認する。設計判断が変わる場合はADRを先に追加する。

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

最小sampleは次の契約を持つ。

| API | 契約 |
|---|---|
| `FeatureCommands.create(CreateFeatureParam)` | 成功はResult.SuccessにUUID v7。不正nameはFailure(INVALID_NAME) |
| `FeatureQueries.find(FindFeatureParam)` | OptionalのFeatureResult。未検出はempty |
| `CreateFeatureParam.name` | 1〜100 Unicode code point。null、ASCII spaceのみ、NUL、不正surrogateを拒否 |
| Param自体／検索ID | nullは呼び出し側の誤りとして拒否 |

nameは入力文字列をそのまま保存し、同名を許可する。100個の補助平面文字も100 code pointであり、UTF-16のString.length()とは異なる。

`common.Result<S,F>` は成功／業務失敗の契約、`FeatureResult` は参照内容のrecord。呼び出し側はsealed switchでvariantを処理する。技術障害はResultへ包まずExceptionのまま伝播させる。

DataSourceは内部FeatureDataを返し、Query実装が公開FeatureResultへ変換する。HTTP・HTML画面固有の型はweb adapterへ配置する。

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
| GET `/api/v1/features/{id}` | 200＋`{"id":"UUID","name":"sample"}`、未登録404 |
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
| `layouts/application.jte` | HTML全体、ナビ、header、本文枠 |
| `components/` | page-heading、field、error-message等の共通部品 |
| `pages/home/index.jte` | home全体 |
| `pages/auth/login.jte` | login画面 |
| `pages/feature/index.jte` | DB連携画面全体 |
| `pages/feature/fragments/panel.jte` | 初回描画と部分更新で共用するDB操作領域 |
| `pages/samples/components/index.jte` | 公開部品集全体 |
| `pages/samples/components/fragments/` | 部品集固有の検索一覧・入力部品 |

新しい業務画面は`pages/<機能>/`に追加し、入口は`index.jte`、詳細・編集は`detail.jte`・`edit.jte`等の役割名とする。画面固有の断片は同じ機能の`fragments/`に置き、ページ全体の初回描画とhtmx応答で同じtemplateを呼ぶ。共通部品は`components/`に置き、ページからは共通部品と自機能の断片を使用する。短いHTMLだけを理由に分割しない。公開URLとtemplate pathは独立して扱う。

Controllerは`pages/feature/index`等の拡張子なしのview名を返し、jteからは`@template.layouts.application(...)`や`@template.pages.feature.fragments.panel(...)`で呼ぶ。templateは4スペースでインデントし、HTMLの入れ子とjteの条件分岐を揃える。

### URLと表示


| URL | 用途 |
|---|---|
| `/` | 匿名home |
| `/components` | 匿名部品集。架空dataはブラウザー内だけに保持 |
| `/login` | 標準フォーム認証 |
| `/features` | 認証必須の作成フォーム |
| `/features/find` | 認証必須のID検索 |

共通layout、field等のjte部品とBootstrapの標準class・variantで外観を統一する。標準の配布資産はBootstrap CSSとhtmx・Alpine.jsで構成し、開閉等の動作はAlpineへ配置する。検索一覧は検索条件・件数・table・ページング、詳細は項目表示と操作、入力はlabel・必須表示・項目error・保存／キャンセルを揃える。固有の装飾を各画面へ追加せず、必要なstyleはstatic/uiへ集約する。

[ADR-029](decisions.md#adr-029)に従い、共通shellは濃色サイドナビ・header・本文を持つ。狭幅ではナビを開閉し、現在地はaria-currentで示す。page-heading.jteで見出しと説明を揃え、検索条件・一覧・補助操作を余白とcardで区切る。状態badgeは名称とBootstrapの意味色を併用する。

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
