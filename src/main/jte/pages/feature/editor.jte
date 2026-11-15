@import dev.template.application.web.ui.UiPage
@import dev.template.application.web.ui.FeatureViews.Editor
@import dev.template.application.web.ui.FeatureViews.Row

@param UiPage page
@param Editor editor

@template.layouts.application(page = page, title = editor.creating() ? "データ登録" : "データ編集", content = @`
    @template.components.page-heading(section = "DATA MANAGEMENT", title = editor.creating() ? "データ登録" : "データ編集",
        description = "入力した行をまとめて保存します。エラーがある場合は全行を保存せず、入力内容を保持します。")
    <div class="card border-0 shadow-sm rounded-4">
        <div class="card-body p-4">
            <form id="feature-editor" method="post" action="${page.contextPath()}/features/save"
                x-data="{count: ${editor.rows().size()}, dirty: ${editor.dirty()}}"
                x-on:input="dirty = true" x-on:change="dirty = true"
                x-on:submit="dirty = false"
                x-on:beforeunload.window="if (dirty) { $event.preventDefault(); $event.returnValue = ''; }"
                x-on:click.window="if (dirty &amp;&amp; $event.target.closest('a[href]') &amp;&amp; !confirm('未保存の変更があります。移動しますか？')) { $event.preventDefault(); $event.stopPropagation(); }">
                <input type="hidden" name="${page.csrfName()}" value="${page.csrfToken()}">
                <input type="hidden" name="creating" value="${editor.creating()}">
                <input type="hidden" name="requestId" value="${editor.requestId().toString()}">
                @template.pages.feature.fragments.filter-fields(filter = editor.filter())
                @if(!editor.error(0).isEmpty())
                    <div class="alert alert-danger" role="alert">${editor.error(0)}</div>
                @endif
                @if(editor.reviewing())
                    <div class="alert alert-warning">入力値と最新の保存内容を比較してください。「確認した内容で再編集」を押した後、入力を調整して保存します。</div>
                @endif
                <div x-ref="rows">
                    @for(int index = 0; index < editor.rows().size(); index++)
                        @template.pages.feature.fragments.editor-row(row = editor.rows().get(index), creating = editor.creating(), error = editor.error(index + 1), reviewing = editor.reviewing())
                    @endfor
                </div>
                @if(editor.creating())
                    <template x-ref="emptyRow">
                        @template.pages.feature.fragments.editor-row(row = new Row(null, "", "ACTIVE", 1, null), creating = true)
                    </template>
                    <button id="feature-add-row" type="button" class="btn btn-outline-secondary mb-4 btn-action-wide" x-bind:class="{'text-white': dark}" x-bind:disabled="count >= 100"
                        x-on:click="$refs.rows.appendChild($refs.emptyRow.content.cloneNode(true)); count++; dirty = true">行を追加</button>
                    <span class="small text-body-secondary ms-2">最大100行</span>
                @endif
                <div class="d-flex flex-wrap justify-content-end gap-2 border-top pt-4">
                    @if(editor.reviewing())
                        @if(editor.confirmable())
                            <button id="feature-confirm" type="submit" class="btn btn-success text-white btn-action-wide" formaction="${page.contextPath()}/features/confirm">確認した内容で再編集</button>
                        @endif
                    @else
                        <button id="feature-save" type="submit" class="btn btn-action-wide ${editor.creating() ? "btn-primary" : "btn-success text-white"}">まとめて保存</button>
                    @endif
                    @if(editor.conflicted())
                        <button id="feature-refresh" type="submit" class="btn btn-outline-primary btn-action-wide" x-bind:class="{'text-white': dark}" formaction="${page.contextPath()}/features/refresh" formnovalidate>最新内容を確認</button>
                    @endif
                    <a class="btn btn-outline-secondary btn-action" x-bind:class="{'text-white': dark}" href="${editor.filter().url(page.contextPath() + "/features")}">一覧へ戻る</a>
                </div>
            </form>
        </div>
    </div>
`)
