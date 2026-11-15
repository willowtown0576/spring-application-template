@import dev.template.application.web.ui.UiPage
@import dev.template.application.web.ui.FeatureViews
@import dev.template.application.web.ui.FeatureViews.Listing
@import dev.template.application.feature.api.query.FeatureSortField
@import dev.template.application.feature.api.model.FeatureStatus

@param UiPage page
@param Listing listing

<section id="feature-results" aria-label="検索と一覧" x-data="{selected: []}">
    <div class="card border-0 shadow-sm rounded-4 mb-4">
        <div class="card-body p-4">
            <h2 class="h5 fw-semibold mb-3">検索条件</h2>
            <form role="search" aria-label="データ検索" method="get" action="${page.contextPath()}/features"
                hx-get="${page.contextPath()}/features" hx-target="#feature-results" hx-swap="outerHTML"
                class="row g-3 align-items-end">
                <div class="col-md-4">
                    <label class="form-label" for="search-name">名前</label>
                    <input id="search-name" class="form-control" name="name" value="${listing.filter().name()}" placeholder="名前の一部を入力" maxlength="200"
                        x-on:input="$el.setCustomValidity(Array.from($el.value).length > 100 ? '名前は100文字以内で入力してください。' : '')">
                </div>
                <div class="col-md-2">
                    <label class="form-label" for="search-status">状態</label>
                    <select id="search-status" class="form-select" name="status">
                        <option value="" selected="${listing.filter().status().isEmpty()}">すべて</option>
                        <option value="ACTIVE" selected="${listing.filter().status().equals("ACTIVE")}">有効</option>
                        <option value="INACTIVE" selected="${listing.filter().status().equals("INACTIVE")}">無効</option>
                    </select>
                </div>
                <div class="col-md-2">
                    <label class="form-label" for="search-sort">並び順</label>
                    <select id="search-sort" class="form-select" name="sort">
                        <option value="UPDATED_AT" selected="${listing.filter().sort() == FeatureSortField.UPDATED_AT}">更新日時</option>
                        <option value="NAME" selected="${listing.filter().sort() == FeatureSortField.NAME}">名前</option>
                    </select>
                </div>
                <div class="col-md-2">
                    <label class="form-label" for="search-direction">方向</label>
                    <select id="search-direction" class="form-select" name="descending">
                        <option value="true" selected="${listing.filter().descending()}">降順</option>
                        <option value="false" selected="${!listing.filter().descending()}">昇順</option>
                    </select>
                </div>
                <div class="col-md-2">
                    <label class="form-label" for="search-size">表示件数</label>
                    <select id="search-size" class="form-select" name="size">
                        @if(listing.filter().size() != 20 && listing.filter().size() != 50 && listing.filter().size() != 100)
                            <option value="${listing.filter().size()}" selected>${listing.filter().size()}件</option>
                        @endif
                        @for(int size : new int[]{20, 50, 100})
                            <option value="${size}" selected="${listing.filter().size() == size}">${size}件</option>
                        @endfor
                    </select>
                </div>
                <div class="col-12 d-flex flex-wrap justify-content-end gap-2">
                    <button class="btn btn-primary btn-action" type="submit">検索</button>
                    <a class="btn btn-outline-secondary btn-action" x-bind:class="{'text-white': dark}" href="${page.contextPath()}/features">条件をクリア</a>
                </div>
            </form>
        </div>
    </div>
    <div class="card border-0 shadow-sm rounded-4">
        <div class="card-body p-4">
            <div class="d-flex flex-wrap justify-content-between gap-3 mb-3">
                <h2 class="h5 fw-semibold mb-0 align-self-center">検索結果 <span class="badge bg-secondary-subtle text-secondary-emphasis">${listing.items().size()}件</span></h2>
                <div class="w-100 border-top pt-3">
                    <h3 class="h6 fw-semibold mb-3">データの登録</h3>
                    <div class="d-flex flex-wrap justify-content-end gap-2">
                        <a id="feature-new" class="btn btn-primary btn-action" href="${listing.filter().url(page.contextPath() + "/features/new")}">新規登録</a>
                        <a id="feature-bulk-new" class="btn btn-outline-primary btn-action" x-bind:class="{'text-white': dark}" href="${listing.filter().url(page.contextPath() + "/features/new")}&rows=3">複数登録</a>
                    </div>
                </div>
            </div>
            <form method="get" action="${page.contextPath()}/features/edit">
                @template.pages.feature.fragments.filter-fields(filter = listing.filter())
                <div id="feature-bulk-actions" class="bg-body-tertiary border rounded-3 p-3 mb-3">
                    <div class="d-flex flex-wrap align-items-center gap-2 mb-3">
                        <span class="fw-semibold me-2">選択したデータの一括操作</span>
                        <span class="badge bg-secondary-subtle text-secondary-emphasis me-2" x-cloak>
                            <span id="feature-selected-count" x-text="selected.length">0</span>件選択
                        </span>
                    </div>
                    <div class="d-flex flex-wrap justify-content-end gap-2 mb-3">
                        <button id="feature-bulk-edit" type="submit" class="btn btn-success text-white btn-action-wide" x-bind:disabled="selected.length === 0">選択した行を編集</button>
                        <button id="feature-bulk-delete" type="submit" formaction="${page.contextPath()}/features/delete"
                            class="btn btn-danger text-white btn-action-wide" x-bind:disabled="selected.length === 0">選択した行を削除</button>
                    </div>
                    <p class="small text-body-secondary mb-0">行のチェックボックスで対象を選び、まとめて編集・削除できます。選択は現在のページ内で有効です。</p>
                </div>
                <div class="table-responsive" role="region" aria-label="検索結果の表" tabindex="0">
                    <table class="table table-hover align-middle">
                        <caption class="visually-hidden">検索結果</caption>
                        <thead class="table-active text-nowrap"><tr><th scope="col">選択</th><th scope="col">名前</th><th scope="col">状態</th><th scope="col">更新日時（日本時間）</th><th scope="col">操作</th></tr></thead>
                        <tbody>
                            @for(var item : listing.items())
                                <tr>
                                    <td><input type="checkbox" class="form-check-input" name="ids" value="${item.id().toString()}" aria-label="${item.name()}を選択" x-model="selected"></td>
                                    <td><a href="${listing.filter().url(page.contextPath() + "/features/detail")}&id=${item.id().toString()}">${item.name()}</a></td>
                                    <td><span class="badge ${item.status() == FeatureStatus.ACTIVE ? "bg-success-subtle text-success-emphasis" : "bg-secondary-subtle text-secondary-emphasis"}">${FeatureViews.status(item.status())}</span></td>
                                    <td class="text-body-secondary text-nowrap">${FeatureViews.time(item.updatedAt())}</td>
                                    <td class="text-nowrap">
                                        <div class="d-inline-flex gap-2">
                                            <a class="feature-row-edit btn btn-sm btn-success text-white btn-action-row" href="${listing.filter().url(page.contextPath() + "/features/edit")}&ids=${item.id().toString()}">編集</a>
                                            <a class="feature-row-delete btn btn-sm btn-danger text-white btn-action-row" href="${listing.filter().url(page.contextPath() + "/features/delete")}&ids=${item.id().toString()}">削除</a>
                                        </div>
                                    </td>
                                </tr>
                            @endfor
                            @if(listing.items().isEmpty())
                                <tr><td colspan="5" class="text-center text-body-secondary py-5">条件に一致するデータはありません。</td></tr>
                            @endif
                        </tbody>
                    </table>
                </div>

            </form>
            <nav class="d-flex justify-content-end gap-2 mt-4" aria-label="検索結果のページ">
                <a class="btn btn-outline-secondary btn-action" x-bind:class="{'text-white': dark}" href="${listing.filter().url(page.contextPath() + "/features")}">先頭</a>
                @if(listing.previous() != null)
                    <a id="feature-previous" class="btn btn-outline-primary btn-action" x-bind:class="{'text-white': dark}" href="${listing.previous()}" hx-get="${listing.previous()}" hx-target="#feature-results" hx-swap="outerHTML">前へ</a>
                @endif
                @if(listing.next() != null)
                    <a id="feature-next" class="btn btn-outline-primary btn-action" x-bind:class="{'text-white': dark}" href="${listing.next()}" hx-get="${listing.next()}" hx-target="#feature-results" hx-swap="outerHTML">次へ</a>
                @endif
            </nav>
            <p class="small text-body-secondary mt-3 mb-0">更新により表示位置が変わる場合があります。名前はデータベースの文字順で並びます。</p>
        </div>
    </div>
</section>
