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

DataSourceは内部FeatureDataを返し、Query実装が公開FeatureResultへ変換する。HTTP・Vaadin固有の型はweb adapterへ配置する。

## Beanと不変性

application実装Beanは@Service／@Repository／@Componentとconstructor injectionを使う。UseCaseでは登録用@Serviceだけを許可し、transaction、Security、SQLの制御は各adapterで担う。domainはSpring非依存とする。

UUID生成器はSupplier<UUID>としてUseCaseへ注入する。時刻はClock、testは固定Clock／固定IDを使う。Clock.systemUTC等のJDK型やSecurityFilterChain／RestClientCustomizer等の構成は@Beanを使用する。

再代入しないlocal variable、parameter、fieldにはfinalを付ける。record component等は言語仕様の暗黙finalを使用する。変更可能fieldは必要性を説明し、Checkstyle例外を対象へ限定する。部品集のnextIdはView内の行採番用であり、業務UUIDとは異なる。

testもconstructor injectionを基本とし、MockitoSpyBeanはclassに宣言してconstructorで受け取る。Beanの可視性は利用範囲に応じて決める。別packageから使用する必要のあるPort等はpublicにできるが、Modulithの公開契約とは区別する。

## JavaDoc・format・import

- 手書きの型とmethodには責務・契約を説明するJavaDocを付ける。parameter、return、必要な例外条件を説明する。
- Overrideは `{@inheritDoc}`、test／lifecycleは条件と期待結果を記載する。
- コメントは、現在の仕様・責務・制約の理由を単独で理解できる説明とする。
- Java formatはSpotlessのEclipse JDT。基本4スペース、継続行は追加4スペース、行幅120。設定の正本は[formatter profile](../config/formatter/eclipse-java.xml)。
- `./gradlew spotlessApply` はimportOrder／removeUnusedImportsも実行する。Checkstyleでもunused／redundant importを検査する。継承したnested type等、意味上の冗長importは自動検出に限界があるためreviewする。
- 利用するAPIは採用versionの推奨APIとする。全JavaCompileで `-Xlint:deprecation -Xlint:removal -Xlint:dep-ann -Werror` を適用する。main／test／codegenとも推奨APIへ置き換え、`@SuppressWarnings` のdeprecation／removal／all指定による回避を禁止する。警告を隠す `@Deprecated` 宣言も手書きsourceでは禁止し、Javadocだけのdeprecated宣言はdep-annで拒否する。
- formatの変更範囲は字下げ・改行・import整理とする。memberの構成変更は責務や可読性の改善として判断する。

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

## Vaadin

`/`、`/components`、`/theme-comparison`、`/theme-preview` は匿名公開、`/features` は認証必須。DB連携は公開APIだけを呼ぶ。Binderは入力支援であり、業務規則はCommand／domainでも検証する。

部品集のdataはサーバー上のViewインスタンスのListに保持する。dataの有効範囲は各利用者のViewインスタンスとする。Viewを作り直す再読み込み・再起動で初期値へ戻る。SampleParamは編集項目、SampleItemは行IDを含む値とし、更新時の行IDを維持する。

| 画面例 | 主な標準component |
|---|---|
| テーマ比較 | FormLayout、IFrame。同じ部品集をAura／Lumoで表示 |
| welcome | AppLayout、SideNav、Card、Details、Button |
| 一覧とモーダル | Grid、filter、Dialog、Binder、ConfirmDialog、Notification |
| 入力部品 | RadioButtonGroup、Checkbox、TextArea |
| 数値・時刻 | EmailField、PasswordField、IntegerField、NumberField、TimePicker |
| 選択部品 | Select、MultiSelectComboBox、CheckboxGroup、ListBox |
| 表示・メニュー | MenuBar、Avatar、Tooltip、Icon、Accordion |
| 一覧＋詳細 | MasterDetailLayout、Grid、Card |
| カード・操作 | Card variant、Button variant、ConfirmDialog、AvatarGroup |

Auraの既定styleと標準variantを選ぶ。装飾と配色はVaadin提供のstyleへ統一する。responsive配置はFormLayout／AppLayout／MasterDetailLayoutに委譲し、明暗表示はPage.setColorSchemeを使う。PasswordFieldは表示切替を試す部品とし、入力は動作確認用の架空の値に限定する。値の利用範囲は当該入力欄とする。

`/theme-comparison` は左右の独立したdocumentでAuraとLumoを比較する。全7タブを操作でき、狭幅では縦に並ぶ。「比較をダークモードにする」は両previewを再読み込みするため、入力内容は初期化される。通常画面のテーマはAura。StarterShellがVaadin提供のstylesheetを選び、各documentに一つのテーマを適用する。

UI変更時は対象操作、明暗、mobileでのはみ出しと詳細画面の閉じ方を実browserで確認する。操作例の検証は `./gradlew integrationTest --tests '*FeatureUiIntegrationTest'`。captureはbuild/reports/uiに出力する。

## 可読性

型名はimportして記述する。同名型の衝突やSpEL／AOP等の文字列式で必要な場合に限り完全修飾名を使う。数十行になるmethodやtestは、準備・操作・検証、入力・保存・表示等の意味のまとまりで空行を入れ、意図が読み取りにくい境界には目的や制約をコメントする。自明な処理の逐語説明や、行数を減らすだけのhelper分割は避ける。

## 変更を検証する

```bash
./gradlew spotlessApply build
```

DB変更は[DBガイド](database.md)、test追加は[検証ガイド](testing.md)、設定・認証・外部HTTP・Batchは[運用ガイド](operations.md)を参照する。資料sourceが変わる場合は[資料生成](documentation.md)も行う。
