@import dev.template.application.web.ui.UiPage

@param UiPage page

@template.layouts.application(page = page, title = "ホーム", content = @`
    @template.components.page-heading(section = "WORKSPACE", title = "今日の業務を、ここから。",
        description = "利用する機能を選択して、作業を始めましょう。")
    <section class="card border-0 shadow-sm rounded-4 mb-4" aria-labelledby="workspace-title">
        <div class="card-body p-4 p-lg-5">
            <div class="row g-4 align-items-center">
                <div class="col-lg-8">
                    <span class="badge bg-primary-subtle text-primary-emphasis mb-3">業務を始める</span>
                    <h2 id="workspace-title" class="h3 fw-semibold mb-3">必要な情報と操作へ、<br class="d-none d-sm-block">迷わずアクセス。</h2>
                    <p class="text-body-secondary mb-0">データの検索、内容の確認、登録・更新まで。<br class="d-none d-sm-block">一貫した操作で日々の業務を進められます。</p>
                </div>
                <div class="col-lg-4">
                    <div class="bg-body-tertiary border rounded-3 p-4">
                        <p class="small fw-semibold text-body-secondary mb-2">QUICK ACCESS</p>
                        <p class="fw-semibold mb-3">データ管理</p>
                        <a id="welcome-features" class="btn btn-primary w-100" href="${page.contextPath()}/features">検索・一覧を開く <span aria-hidden="true">→</span></a>
                    </div>
                </div>
            </div>
        </div>
    </section>
    <div class="row g-4">
        <div class="col-md-6">
            <section class="card border-0 shadow-sm rounded-4 h-100">
                <div class="card-body p-4 d-flex flex-column align-items-start">
                    <span class="badge bg-success-subtle text-success-emphasis mb-3">DATA MANAGEMENT</span>
                    <h2 class="h5 fw-semibold mb-2">データの登録・更新</h2>
                    <p class="text-body-secondary mb-4">新しいデータを登録し、一覧から選択した内容を編集します。複数行の入力・更新にも対応しています。</p>
                    <a class="btn btn-outline-primary mt-auto w-100" x-bind:class="{'text-white': dark}" href="${page.contextPath()}/features/new">新しいデータを登録 <span aria-hidden="true">→</span></a>
                </div>
            </section>
        </div>
        <div class="col-md-6">
            <section class="card border-0 shadow-sm rounded-4 h-100">
                <div class="card-body p-4 d-flex flex-column align-items-start">
                    <span class="badge bg-secondary-subtle text-secondary-emphasis mb-3">COMPONENT LIBRARY</span>
                    <h2 class="h5 fw-semibold mb-2">画面コンポーネント集</h2>
                    <p class="text-body-secondary mb-4">一覧・詳細・入力フォームの共通部品と操作を確認できます。ここでの編集はブラウザー内だけに反映されます。</p>
                    <a id="welcome-gallery" class="btn btn-outline-secondary mt-auto w-100" x-bind:class="{'text-white': dark}" href="${page.contextPath()}/components">部品集を開く <span aria-hidden="true">→</span></a>
                </div>
            </section>
        </div>
    </div>
`)
