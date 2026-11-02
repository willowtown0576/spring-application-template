@import gg.jte.Content
@import dev.template.application.web.ui.UiPage

@param UiPage page
@param String title
@param Content content

<!DOCTYPE html>
<html lang="ja" data-bs-theme="light" x-data="{dark: false, menuOpen: false}" x-bind:data-bs-theme="dark ? 'dark' : 'light'">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="csrf-header" content="${page.csrfHeader()}">
        <meta name="htmx-config" content='{"historyCacheSize":0,"allowEval":false,"includeIndicatorStyles":false}'>
        <title>${title} | Spring Application</title>
        <link rel="stylesheet" href="${page.contextPath()}/vendor/bootstrap.min.css">
        <link rel="stylesheet" href="${page.contextPath()}/ui/application.css">
        <script defer src="${page.contextPath()}/vendor/htmx.min.js"></script>
        <script defer src="${page.contextPath()}/ui/application.js"></script>
        <script defer src="${page.contextPath()}/vendor/cdn.min.js"></script>
    </head>
    <body hx-history="false" class="bg-body-tertiary">
        <a class="visually-hidden-focusable" href="#main">本文へ移動</a>
        <div class="container-fluid">
            <div class="row min-vh-100">
                <aside class="col-lg-3 col-xl-2 bg-dark text-white p-0" data-bs-theme="dark" aria-label="ワークスペース">
                    <div class="d-flex flex-column h-100 p-3 p-lg-4">
                        <div class="d-flex align-items-center justify-content-between gap-3 mb-lg-5">
                            <a class="text-decoration-none text-white" href="${page.contextPath()}/">
                                <span class="d-block fw-bold fs-5">Spring Application<span class="text-primary">.</span></span>
                                <span class="small text-white-50">業務ワークスペース</span>
                            </a>
                            <button id="starter-menu-toggle" type="button" class="btn btn-sm btn-outline-light d-lg-none"
                                x-on:click="menuOpen = !menuOpen" x-bind:aria-expanded="menuOpen"
                                aria-controls="starter-navigation" x-cloak>メニュー</button>
                        </div>
                        <nav id="starter-navigation" class="d-lg-block mt-4 mt-lg-0" aria-label="メインナビゲーション" x-show="menuOpen">
                            <p class="small text-uppercase text-white-50 mb-3">Workspace</p>
                            <ul class="nav nav-pills flex-column gap-2">
                                <li class="nav-item">
                                    <a class="nav-link px-3 py-3 ${title.equals("ホーム") ? "active bg-white bg-opacity-10" : "text-white-50"}"
                                        aria-current="${title.equals("ホーム") ? "page" : null}"
                                        href="${page.contextPath()}/">ホーム</a>
                                </li>
                                <li class="nav-item">
                                    <a
                                        id="starter-gallery"
                                        class="nav-link px-3 py-3 ${title.equals("部品集") ? "active bg-white bg-opacity-10" : "text-white-50"}"
                                        aria-current="${title.equals("部品集") ? "page" : null}"
                                        href="${page.contextPath()}/components">コンポーネント</a>
                                </li>
                                <li class="nav-item">
                                    <a
                                        class="nav-link px-3 py-3 ${title.equals("データ管理") ? "active bg-white bg-opacity-10" : "text-white-50"}"
                                        aria-current="${title.equals("データ管理") ? "page" : null}"
                                        href="${page.contextPath()}/features">データ管理</a>
                                </li>
                            </ul>
                        </nav>
                        <div class="d-none d-lg-block mt-auto pt-5">
                            <div class="border-top border-secondary pt-4">
                                <p class="small text-white-50 mb-1">Spring Application</p>
                                <p class="small text-white-50 mb-0">日々の業務を、ひとつの場所に。</p>
                            </div>
                        </div>
                    </div>
                </aside>
                <div class="col-lg-9 col-xl-10 p-0 d-flex flex-column">
                    <header class="bg-body border-bottom px-4 px-xl-5 py-3">
                        <div class="d-flex align-items-center justify-content-between flex-wrap gap-3">
                            <nav aria-label="現在地">
                                <ol class="breadcrumb small mb-0">
                                    <li class="breadcrumb-item text-body-secondary">ワークスペース</li>
                                    <li class="breadcrumb-item active" aria-current="page">${title}</li>
                                </ol>
                            </nav>
                            <div class="d-flex align-items-center gap-3">
                                <label class="form-check form-switch small mb-0">
                                    <input id="starter-dark-mode" class="form-check-input" type="checkbox" x-model="dark">
                                    ダークモード
                                </label>
                                @if(page.authenticated())
                                    <form method="post" action="${page.contextPath()}/logout" class="m-0">
                                        <input type="hidden" name="${page.csrfName()}" value="${page.csrfToken()}">
                                        <button id="starter-account" class="btn btn-sm btn-outline-secondary" x-bind:class="{'text-white': dark}" type="submit">ログアウト</button>
                                    </form>
                                @else
                                    <a
                                        id="starter-account"
                                        class="btn btn-sm btn-outline-secondary" x-bind:class="{'text-white': dark}"
                                        href="${page.contextPath()}/login">ログイン</a>
                                @endif
                            </div>
                        </div>
                    </header>
                    <main id="main" class="px-4 px-xl-5 py-4 py-lg-5 flex-grow-1">
                        <div class="container-xxl px-0">
                            <div id="ui-error" class="alert alert-danger" role="alert" hidden></div>
                            ${content}
                        </div>
                    </main>
                </div>
            </div>
        </div>
    </body>
</html>
