@import dev.template.application.web.ui.UiPage
@import dev.template.application.web.ui.FeatureViews
@import dev.template.application.web.ui.FeatureViews.Filter
@import dev.template.application.feature.api.query.FeatureDetails

@param UiPage page
@param Filter filter
@param FeatureDetails item

@template.layouts.application(page = page, title = "データ詳細", content = @`
    @template.components.page-heading(section = "DATA MANAGEMENT", title = "データ詳細", description = "保存された内容を確認します。")
    <div class="card border-0 shadow-sm rounded-4">
        <div class="card-body p-4">
            <dl class="row mb-4">
                <dt class="col-sm-3">名前</dt><dd class="col-sm-9 text-break">${item.name()}</dd>
                <dt class="col-sm-3">状態</dt><dd class="col-sm-9">${FeatureViews.status(item.status())}</dd>
                <dt class="col-sm-3">更新日時（日本時間）</dt><dd class="col-sm-9">${FeatureViews.time(item.updatedAt())}</dd>
            </dl>
            <div class="d-flex flex-wrap justify-content-end gap-2">
                <a class="btn btn-success text-white btn-action" href="${filter.url(page.contextPath() + "/features/edit")}&ids=${item.id().toString()}">編集</a>
                <a id="feature-delete" class="btn btn-danger text-white btn-action" href="${filter.url(page.contextPath() + "/features/delete")}&ids=${item.id().toString()}">削除</a>
                <a class="btn btn-outline-secondary btn-action" x-bind:class="{'text-white': dark}" href="${filter.url(page.contextPath() + "/features")}">一覧へ戻る</a>
            </div>
        </div>
    </div>
`)
