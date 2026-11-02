@import dev.template.application.web.ui.UiPage

@param UiPage page

@template.layouts.application(page = page, title = "ホーム", content = @`
    @template.components.page-heading(section = "HOME", title = "今日の業務を、ここから。",
        description = "必要な情報と操作へ、迷わずアクセスできるワークスペース。")
    <section class="card border-0 shadow-sm rounded-4 mb-5" aria-label="ワークスペースの案内">
        <div class="card-body p-4 p-lg-5">
            <div class="row align-items-center g-4">
                <div class="col-lg-8">
                    <span class="badge bg-primary-subtle text-primary-emphasis rounded-pill mb-3">WORKSPACE</span>
                    <h2 class="h3 fw-semibold mb-3">ひとつの場所から、<br>スムーズに仕事を進める。</h2>
                    <p class="text-body-secondary mb-0">情報を探す、内容を確認する、データを登録する。<br class="d-none d-sm-block">いつもの操作を、一貫した使い心地で。</p>
                </div>
                <div class="col-lg-4">
                    <div class="bg-body-tertiary border rounded-3 p-4">
                        <p class="small fw-semibold text-body-secondary mb-3">QUICK ACCESS</p>
                        <p class="fw-semibold mb-2">データの登録・検索</p>
                        <p class="small text-body-secondary mb-3">必要なデータへ直接アクセス</p>
                        <a
                            class="btn btn-primary w-100"
                            href="${page.contextPath()}/features">データ管理を開く <span
                            aria-hidden="true">→</span></a>
                    </div>
                </div>
            </div>
        </div>
    </section>
    <div class="d-flex align-items-center justify-content-between mb-3">
        <h2 class="h5 fw-semibold mb-0">ワークスペースを使う</h2>
        <span class="small text-body-secondary">機能を選択</span>
    </div>
    <div class="row g-4">
        <div class="col-md-6">
            <section class="card border-0 shadow-sm rounded-4 h-100">
                <div class="card-body p-4 d-flex flex-column align-items-start">
                    <span class="badge bg-primary-subtle text-primary-emphasis mb-4">画面・操作</span>
                    <h3 class="h5 fw-semibold mb-2">画面コンポーネント集</h3>
                    <p class="text-body-secondary mb-4">検索一覧から詳細、入力フォームまで。共通の操作と画面部品を確認できます。</p>
                    <a
                        id="welcome-gallery"
                        class="btn btn-outline-primary mt-auto" x-bind:class="{'text-white': dark}"
                        href="${page.contextPath()}/components">部品集を開く <span
                        aria-hidden="true">→</span></a>
                </div>
            </section>
        </div>
        <div class="col-md-6">
            <section class="card border-0 shadow-sm rounded-4 h-100">
                <div class="card-body p-4 d-flex flex-column align-items-start">
                    <span class="badge bg-secondary-subtle text-secondary-emphasis mb-4">データ・管理</span>
                    <h3 class="h5 fw-semibold mb-2">データ管理</h3>
                    <p class="text-body-secondary mb-4">名前を登録し、IDで内容を検索。保存されたデータを確認できます。</p>
                    <a
                        class="btn btn-outline-secondary mt-auto" x-bind:class="{'text-white': dark}"
                        href="${page.contextPath()}/features">データ管理を開く <span
                        aria-hidden="true">→</span></a>
                </div>
            </section>
        </div>
    </div>
`)
