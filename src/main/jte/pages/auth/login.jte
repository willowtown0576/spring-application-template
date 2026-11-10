@import dev.template.application.web.ui.UiPage

@param UiPage page
@param boolean failed
@param boolean loggedOut = false

@template.layouts.base(page = page, title = "ログイン", content = @`
    <main id="main" class="min-vh-100 d-flex align-items-center">
        <div class="container py-5">
            <div class="row justify-content-center">
                <div class="col-sm-10 col-md-8 col-lg-6 col-xl-4">
                    <div class="text-center mb-4">
                        <span class="d-inline-flex align-items-center justify-content-center bg-primary text-white rounded-4 p-3 fs-3 fw-bold mb-3" aria-hidden="true">S.</span>
                        <p class="fw-bold fs-5 mb-1">Spring Application Template</p>
                        <p class="small text-body-secondary mb-0">日々の業務を、ひとつの場所に。</p>
                    </div>
                    <section class="card border-0 shadow-sm rounded-4" aria-labelledby="login-title">
                        <div class="card-body p-4 p-lg-5">
                            <p class="small fw-semibold text-primary mb-3">WORKSPACE</p>
                            <h1 id="login-title" class="h4 fw-bold mb-2">ログイン</h1>
                            <p class="text-body-secondary mb-4">利用者名とパスワードを入力して、業務を開始してください。</p>
                            @if(failed)
                                <div id="login-error" class="alert alert-danger" role="alert">利用者名またはパスワードが正しくありません。</div>
                            @endif
                            @if(loggedOut)
                                <div class="alert alert-success" role="status">ログアウトしました。</div>
                            @endif
                            <form method="post" action="${page.contextPath()}/login">
                                <input type="hidden" name="${page.csrfName()}" value="${page.csrfToken()}">
                                @template.components.field(id = "username", label = "利用者名", required = true)
                                @template.components.field(id = "password", label = "パスワード", type = "password", required = true)
                                <button id="login-submit" class="btn btn-primary w-100 mt-2" type="submit">ログイン</button>
                            </form>
                        </div>
                    </section>
                    <p class="small text-center text-body-secondary mt-4 mb-0">認証後に、利用する機能を選択できます。</p>
                </div>
            </div>
        </div>
    </main>
`)
