@import gg.jte.Content
@import dev.template.application.web.ui.UiPage

@param UiPage page
@param String title
@param Content content

@template.layouts.base(page = page, title = title, content = @`
    @template.components.navigation(page = page, title = title)
    <div class="min-vh-100 d-flex flex-column">
        <header class="sticky-top bg-body border-bottom shadow-sm">
            <div class="container-xxl px-3 px-md-4 py-3">
                <div class="d-flex align-items-center justify-content-between flex-wrap gap-3">
                    <div class="d-flex align-items-center gap-3">
                        <button id="starter-menu-toggle" type="button" class="btn btn-outline-primary btn-action"
                            x-bind:class="{'text-white': dark}" data-bs-toggle="offcanvas" data-bs-target="#starter-navigation"
                            aria-controls="starter-navigation" x-cloak>
                            <span aria-hidden="true">☰</span> メニュー
                        </button>
                        <a class="text-decoration-none text-body fw-bold lh-sm" href="${page.contextPath()}/">Spring Application<br class="d-sm-none"> Template<span class="text-primary">.</span></a>
                    </div>
                    <div class="d-flex align-items-center gap-3 ms-auto">
                        <label class="form-check form-switch small mb-0">
                            <input id="starter-dark-mode" class="form-check-input" type="checkbox" x-model="dark">
                            ダークモード
                        </label>
                        <form method="post" action="${page.contextPath()}/logout" class="m-0">
                            <input type="hidden" name="${page.csrfName()}" value="${page.csrfToken()}">
                            <button id="starter-account" class="btn btn-sm btn-outline-secondary btn-action" x-bind:class="{'text-white': dark}" type="submit">ログアウト</button>
                        </form>
                    </div>
                </div>
            </div>
        </header>
        <noscript>
            <nav class="container-xxl px-4 py-3 d-flex flex-wrap gap-3 border-bottom" aria-label="ページ移動">
                <a href="${page.contextPath()}/">ホーム</a>
                <a href="${page.contextPath()}/features">データ管理</a>
                <a href="${page.contextPath()}/components">コンポーネント</a>
            </nav>
        </noscript>
        <main id="main" class="container-xxl px-3 px-md-4 py-4 py-lg-5 flex-grow-1">
            <nav aria-label="現在地" class="mb-4">
                <ol class="breadcrumb small mb-0">
                    <li class="breadcrumb-item"><a class="text-body-secondary" href="${page.contextPath()}/">ワークスペース</a></li>
                    <li class="breadcrumb-item active" aria-current="page">${title}</li>
                </ol>
            </nav>
            <div id="ui-error" class="alert alert-danger" role="alert" hidden></div>
            ${content}
        </main>
        <footer class="border-top py-3">
            <div class="container-xxl px-3 px-md-4 d-flex flex-wrap justify-content-between gap-2 small text-body-secondary">
                <span>Spring Application Template</span><span>業務ワークスペース</span>
            </div>
        </footer>
    </div>
`)
