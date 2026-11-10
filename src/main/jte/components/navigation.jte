@import dev.template.application.web.ui.UiPage

@param UiPage page
@param String title

<aside id="starter-navigation" tabindex="-1" aria-labelledby="starter-navigation-title"
    class="offcanvas offcanvas-start navigation-drawer bg-dark text-white shadow-lg" data-bs-theme="dark">
    <div class="d-flex flex-column h-100 p-4">
        <div class="d-flex justify-content-between align-items-start gap-3 border-bottom border-secondary pb-4">
            <div>
                <h2 id="starter-navigation-title" class="h5 fw-bold mb-2">ワークスペース</h2>
                <span class="small text-white-50">機能を選択</span>
            </div>
            <button id="starter-menu-close" type="button" class="btn-close" aria-label="メニューを閉じる"
                data-bs-dismiss="offcanvas"></button>
        </div>
        <nav class="py-4 flex-grow-1 overflow-y-auto" aria-label="メインナビゲーション">
            <ul class="nav nav-pills flex-column gap-2">
                <li class="nav-item">
                    <a class="nav-link px-3 py-3 ${title.equals("ホーム") ? "active bg-white bg-opacity-10" : "text-white-50"}"
                        aria-current="${title.equals("ホーム") ? "page" : null}" href="${page.contextPath()}/">ホーム</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link px-3 py-3 ${title.startsWith("データ") ? "active bg-white bg-opacity-10" : "text-white-50"}"
                        aria-current="${title.startsWith("データ") ? "page" : null}" href="${page.contextPath()}/features">データ管理</a>
                </li>
                <li class="nav-item">
                    <a id="starter-gallery" class="nav-link px-3 py-3 ${title.equals("部品集") ? "active bg-white bg-opacity-10" : "text-white-50"}"
                        aria-current="${title.equals("部品集") ? "page" : null}" href="${page.contextPath()}/components">コンポーネント</a>
                </li>
            </ul>
        </nav>
        <div class="border-top border-secondary pt-4 small text-white-50">
            <p class="mb-1">Spring Application Template</p>
            <p class="mb-0">日々の業務を、ひとつの場所に。</p>
        </div>
    </div>
</aside>
