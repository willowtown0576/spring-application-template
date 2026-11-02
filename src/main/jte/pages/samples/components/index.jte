@import dev.template.application.web.ui.UiPage

@param UiPage page

@template.layouts.application(page = page, title = "部品集", content = @`
    @template.components.page-heading(section = "COMPONENTS", title = "画面コンポーネント集",
        description = "検索、確認、登録。業務を支える画面と操作のスタンダード。")
    <div class="alert bg-primary-subtle text-primary-emphasis border-0 rounded-3 small mb-4" role="note">
        <strong>操作サンプル</strong>　架空のデータを使用します。変更はブラウザー内だけに反映され、再読み込みで初期化されます。
    </div>
    @template.pages.samples.components.fragments.search-list()
    @template.pages.samples.components.fragments.input-fields()
`)
