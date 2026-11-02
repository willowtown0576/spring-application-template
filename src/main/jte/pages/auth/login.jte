@import dev.template.application.web.ui.UiPage

@param UiPage page
@param boolean failed

@template.layouts.application(page = page, title = "ログイン", content = @`
    <div class="row justify-content-center py-lg-5">
        <div class="col-md-8 col-xl-6">
            <div class="card border-0 shadow-sm rounded-4">
                <div class="card-body p-4 p-lg-5">
                    <p class="small fw-semibold text-body-secondary mb-3">WELCOME BACK</p>
                    <h1 class="h3 fw-bold mb-2">ワークスペースにログイン</h1>
                    <p class="text-body-secondary mb-4">利用者名とパスワードを入力してください。</p>
                    @if(failed)
                        <div id="login-error" class="alert alert-danger" role="alert">利用者名またはパスワードが正しくありません。</div>
                    @endif
                    <form method="post" action="${page.contextPath()}/login">
                        <input type="hidden" name="${page.csrfName()}" value="${page.csrfToken()}">
                        @template.components.field(id = "username", label = "利用者名", required = true)
                        @template.components.field(id = "password", label = "パスワード", type = "password", required = true)
                        <button id="login-submit" class="btn btn-primary w-100 mt-2" type="submit">ログイン</button>
                    </form>
                </div>
            </div>
        </div>
    </div>
`)
