@import dev.template.application.web.ui.UiPage
@import dev.template.application.web.ui.FeaturePage

@param UiPage page
@param FeaturePage feature

<section id="feature-panel" class="row g-4" aria-label="データ操作">
    <div class="col-lg-6">
        <div class="card border-0 shadow-sm rounded-4 h-100">
            <div class="card-body p-4">
                <span class="badge bg-primary-subtle text-primary-emphasis mb-3">登録</span>
                <h2 class="h5 fw-semibold mb-2">新しいデータを作成</h2>
                <p class="small text-body-secondary mb-4">名前を入力して、データを登録します。</p>
                <form
                    method="post"
                    action="${page.contextPath()}/features"
                    hx-post="${page.contextPath()}/features"
                    hx-target="#feature-panel"
                    hx-swap="outerHTML"
                    hx-disabled-elt="find button">
                    <input type="hidden" name="${page.csrfName()}" value="${page.csrfToken()}">
                    <label class="form-label" for="feature-name">名前 <span class="text-danger">必須</span></label>
                    <input
                        id="feature-name"
                        class="form-control ${feature.nameError().isEmpty() ? "" : "is-invalid"}"
                        name="name"
                        value="${feature.name()}"
                        required
                        aria-invalid="${!feature.nameError().isEmpty()}"
                        aria-describedby="feature-name-error">
                    <div id="feature-name-error" class="invalid-feedback">${feature.nameError()}</div>
                    <button id="feature-create" type="submit" class="btn btn-primary mt-4">作成</button>
                </form>
            </div>
        </div>
    </div>
    <div class="col-lg-6">
        <div class="card border-0 shadow-sm rounded-4 h-100">
            <div class="card-body p-4">
                <span class="badge bg-secondary-subtle text-secondary-emphasis mb-3">参照</span>
                <h2 class="h5 fw-semibold mb-2">IDから検索</h2>
                <p class="small text-body-secondary mb-4">登録済みのIDを指定して、内容を確認します。</p>
                <form
                    method="get"
                    action="${page.contextPath()}/features/find"
                    hx-get="${page.contextPath()}/features/find"
                    hx-target="#feature-panel"
                    hx-swap="outerHTML"
                    hx-disabled-elt="find button">
                    <label class="form-label" for="feature-id">ID</label>
                    <input
                        id="feature-id"
                        class="form-control ${feature.idError().isEmpty() ? "" : "is-invalid"}"
                        name="id"
                        value="${feature.id()}"
                        required
                        aria-invalid="${!feature.idError().isEmpty()}"
                        aria-describedby="feature-id-error">
                    <div id="feature-id-error" class="invalid-feedback">${feature.idError()}</div>
                    <button id="feature-find" type="submit" class="btn btn-outline-primary mt-4" x-bind:class="{'text-white': dark}">取得</button>
                </form>
            </div>
        </div>
    </div>
    <p id="feature-result" class="col-12" role="status">${feature.message()}</p>
</section>
