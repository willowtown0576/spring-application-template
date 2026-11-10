@import dev.template.application.web.ui.FeatureViews
@import dev.template.application.web.ui.FeatureViews.Row

@param Row row
@param boolean creating
@param String error = ""
@param boolean reviewing = false

<div class="feature-editor-row border rounded-3 p-3 mb-3">
    @if(!creating)
        <input type="hidden" name="rowId" value="${row.id().toString()}">
        <input type="hidden" name="rowVersion" value="${row.version()}">
    @endif
    <div class="row g-3 align-items-end">
        <div class="col-md-7">
            <label class="form-label d-block">名前 <span class="text-danger small">必須</span>
                <input class="form-control mt-2" name="rowName" value="${row.name()}" required aria-invalid="${!error.isEmpty()}">
            </label>
        </div>
        <div class="col-md-3">
            <label class="form-label d-block">状態
                <select class="form-select mt-2" name="rowStatus">
                    @if(!row.status().equals("ACTIVE") && !row.status().equals("INACTIVE"))
                        <option value="${row.status()}" selected>選択してください</option>
                    @endif
                    <option value="ACTIVE" selected="${row.status().equals("ACTIVE")}">有効</option>
                    <option value="INACTIVE" selected="${row.status().equals("INACTIVE")}">無効</option>
                </select>
            </label>
        </div>
        @if(creating)
            <div class="col-md-2 mb-2"><button type="button" class="btn btn-danger text-white btn-action-row" aria-label="行を削除" x-bind:disabled="count <= 1" x-on:click="$el.closest('.feature-editor-row').remove(); count--; dirty = true">削除</button></div>
        @endif
    </div>
    @if(!error.isEmpty())
        <p class="text-danger mb-0" role="alert">${error}</p>
    @endif
    @if(reviewing)
        <div class="alert alert-info mt-3 mb-0">
            @if(row.latest() != null)
                <input type="hidden" name="latestVersion" value="${row.latest().version()}">
                <p class="fw-semibold mb-1">最新の保存内容</p>
                <p class="mb-0">${row.latest().name()} ／ ${FeatureViews.status(row.latest().status())} ／ ${FeatureViews.time(row.latest().updatedAt())}</p>
            @else
                <p class="mb-0">このデータは見つかりません。一覧へ戻って対象を確認してください。</p>
            @endif
        </div>
    @endif
</div>
