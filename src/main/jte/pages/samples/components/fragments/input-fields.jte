<section class="card border-0 shadow-sm rounded-4" x-data="{extra: false, notification: ''}" aria-label="入力部品">
    <div class="card-body p-4">
        <h2 class="h5 fw-semibold mb-1">入力部品</h2>
        <p class="small text-body-secondary mb-4">用途に合った入力欄で、正確なデータ入力をサポート</p>
        <div class="row">
            <div class="col-md-6">
                @template.components.field(id = "example-email", label = "メールアドレス", type = "email")
                @template.components.field(id = "example-date", label = "日付", type = "date")
                @template.components.field(id = "example-quantity", label = "数量", type = "number")
            </div>
            <div class="col-md-6">
                <label class="form-label" for="example-note">備考</label>
                <textarea id="example-note" class="form-control mb-3" rows="3"></textarea>
                <label class="form-check">
                    <input
                        class="form-check-input"
                        type="checkbox"
                        x-model="extra"
                        aria-controls="example-extra">追加項目を表示</label>
                <div id="example-extra" x-show="extra" x-transition x-cloak class="mt-3">
                    <label class="form-label" for="example-extra-text">追加情報</label>
                    <input id="example-extra-text" class="form-control">
                </div>
            </div>
        </div>
        <button class="btn btn-outline-primary mt-4" x-bind:class="{'text-white': dark}" x-on:click="notification = '確認しました。'">入力内容を確認</button>
        <p class="mt-3" role="status" x-text="notification"></p>
        <details class="border-top mt-4 pt-3 small">
            <summary>データの保存について</summary>
            <p>入力内容はDBに保存されません。</p>
        </details>
    </div>
</section>
