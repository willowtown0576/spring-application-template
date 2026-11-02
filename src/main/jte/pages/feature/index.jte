@import dev.template.application.web.ui.UiPage
@import dev.template.application.web.ui.FeaturePage

@param UiPage page
@param FeaturePage feature

@template.layouts.application(page = page, title = "データ管理", content = @`
    @template.components.page-heading(section = "DATA MANAGEMENT", title = "データ管理",
        description = "データを登録し、IDから保存された内容を確認できます。")
    @template.pages.feature.fragments.panel(page = page, feature = feature)
`)
