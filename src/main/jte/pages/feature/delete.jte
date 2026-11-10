@import dev.template.application.web.ui.UiPage
@import dev.template.application.web.ui.FeatureDeletionPage

@param UiPage page
@param FeatureDeletionPage deletion

@template.layouts.application(page = page, title = "データ削除", content = @`
    @template.components.page-heading(section = "DATA MANAGEMENT", title = "削除内容の確認",
        description = "対象の名前と件数を確認してください。確認するだけではデータは削除されません。")
    <section class="card border-0 shadow-sm rounded-4" aria-labelledby="feature-delete-title">
        <div class="card-body p-4">
            <h2 id="feature-delete-title" class="h5 fw-semibold mb-3">削除対象</h2>
            <div class="alert alert-warning">
                <p class="fw-semibold mb-1">${deletion.rows().size()}件のデータを削除します。</p>
                <p class="mb-0">削除直前の内容・削除日時・実行者を履歴へ保存し、通常の一覧と検索から取り除きます。画面からの復元はできません。</p>
            </div>
            @if(!deletion.issues().isEmpty())
                <div class="alert alert-danger" role="alert">
                    <p class="fw-semibold mb-1">この操作では、どの対象も削除していません。</p>
                    <p class="mb-0">${deletion.error(0)} 最新の内容を確認し直してから、削除を判断してください。</p>
                </div>
            @endif
            <form method="post" action="${page.contextPath()}/features/delete">
                <input type="hidden" name="${page.csrfName()}" value="${page.csrfToken()}">
                @template.pages.feature.fragments.filter-fields(filter = deletion.filter())
                <div class="table-responsive" role="region" aria-label="削除するデータ" tabindex="0">
                    <table class="table align-middle">
                        <caption class="visually-hidden">削除するデータ</caption>
                        <thead class="table-active"><tr><th scope="col">名前</th><th scope="col">確認結果</th></tr></thead>
                        <tbody>
                            @for(int index = 0; index < deletion.rows().size(); index++)
                                <tr>
                                    <td class="text-break">
                                        ${deletion.rows().get(index).name()}
                                        <input type="hidden" name="rowId" value="${deletion.rows().get(index).id().toString()}">
                                        <input type="hidden" name="rowName" value="${deletion.rows().get(index).name()}">
                                        <input type="hidden" name="rowVersion" value="${deletion.rows().get(index).version()}">
                                    </td>
                                    <td class="text-danger">${deletion.error(index + 1)}</td>
                                </tr>
                            @endfor
                        </tbody>
                    </table>
                </div>
                <div class="d-flex flex-wrap justify-content-end gap-2 mt-4">
                    @if(deletion.issues().isEmpty())
                        <button id="feature-delete-confirm" type="submit" class="btn btn-danger btn-action-wide">確認した${deletion.rows().size()}件を削除</button>
                    @else
                        <a id="feature-delete-refresh" class="btn btn-outline-primary btn-action-wide" x-bind:class="{'text-white': dark}"
                            href="${deletion.refreshUrl(page.contextPath() + "/features/delete")}">最新の内容を確認</a>
                    @endif
                    <a id="feature-delete-cancel" class="btn btn-outline-secondary btn-action" x-bind:class="{'text-white': dark}"
                        href="${deletion.filter().url(page.contextPath() + "/features")}">一覧へ戻る</a>
                </div>
            </form>
        </div>
    </section>
`)
