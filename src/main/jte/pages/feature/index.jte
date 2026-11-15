@import dev.template.application.web.ui.UiPage
@import dev.template.application.web.ui.FeatureViews.Listing

@param UiPage page
@param Listing listing
@param String notice = ""

@template.layouts.application(page = page, title = "データ管理", content = @`
    @template.components.page-heading(section = "DATA MANAGEMENT", title = "データ管理",
        description = "必要なデータを見つけ、内容を確認・更新できます。")
    @if(notice != null && !notice.isEmpty())
        <div class="alert alert-success" role="status">${notice}</div>
    @endif
    @template.pages.feature.fragments.list(page = page, listing = listing)
`)
