<section x-data="sampleGallery" class="card border-0 shadow-sm rounded-4 mb-4" aria-label="検索一覧">
    <div class="card-body p-4">
        <div class="d-flex justify-content-between flex-wrap gap-3 mb-3">
            <div>
                <h2 class="h5 fw-semibold mb-1">検索一覧</h2>
                <p class="small text-body-secondary mb-0">条件を指定して、必要なデータを絞り込む</p>
            </div>
            <button id="sample-create" class="btn btn-primary" x-on:click="create">新規作成</button>
        </div>
        <form x-on:submit.prevent="search = query; currentPage = 1"
            class="row g-3 align-items-end bg-body-tertiary border rounded-3 p-3 pb-4 mt-0 mb-4 mx-0">
            <div class="col-sm-6">
                <label class="form-label" for="sample-search">名前</label>
                <input id="sample-search" class="form-control" x-model="query">
            </div>
            <div class="col-sm-6 d-flex gap-2">
                <button class="btn btn-primary" type="submit">検索</button>
                <button
                    class="btn btn-outline-secondary" x-bind:class="{'text-white': dark}"
                    type="button"
                    x-on:click="query = ''; search = ''; currentPage = 1">クリア</button>
            </div>
        </form>
        <div class="d-flex align-items-center gap-2 mb-3">
            <h3 class="h6 mb-0 fw-semibold">検索結果</h3>
            <span
                id="sample-count"
                class="badge bg-secondary-subtle text-secondary-emphasis rounded-pill"
                role="status"
                x-text="filtered.length + '件'"></span>
        </div>
        <div class="table-responsive border rounded-3 mb-3">
            <table id="sample-grid" class="table table-hover align-middle mb-0">
                <thead>
                    <tr>
                        <th scope="col" class="small text-body-secondary fw-semibold bg-body-tertiary py-3 px-3">名前</th>
                        <th scope="col" class="small text-body-secondary fw-semibold bg-body-tertiary py-3 px-3">状態</th>
                        <th scope="col" class="small text-body-secondary fw-semibold bg-body-tertiary py-3 px-3">操作</th>
                    </tr>
                </thead>
                <tbody>
                    <template x-for="item in visible" x-bind:key="item.id">
                        <tr>
                            <td class="fw-medium py-3 px-3" x-text="item.name"></td>
                            <td class="py-3 px-3">
                                <span class="badge rounded-pill"
                                    x-bind:class="{
                                        'bg-secondary-subtle text-secondary-emphasis': item.status === '未着手',
                                        'bg-primary-subtle text-primary-emphasis': item.status === '確認中',
                                        'bg-success-subtle text-success-emphasis': item.status === '完了'
                                    }"
                                    x-text="item.status"></span>
                            </td>
                            <td class="py-3 px-3">
                                <div class="d-flex flex-wrap gap-2">
                                    <button
                                        class="btn btn-sm btn-outline-primary" x-bind:class="{'text-white': dark}"
                                        x-on:click="detail = item">詳細</button>
                                    <button
                                        class="btn btn-sm btn-outline-secondary" x-bind:class="{'text-white': dark}"
                                        x-on:click="edit(item)">編集</button>
                                    <button
                                        class="btn btn-sm btn-outline-danger" x-bind:class="{'text-white': dark}"
                                        x-on:click="removing = item">削除</button>
                                </div>
                            </td>
                        </tr>
                    </template>
                </tbody>
            </table>
        </div>
        <div x-show="filtered.length === 0" class="text-center bg-body-tertiary rounded-3 p-4 mb-3">
            <p class="fw-semibold mb-1">該当するデータがありません。</p>
            <p class="small text-body-secondary mb-0">検索条件を変更するか、クリアして再度検索してください。</p>
        </div>
        <nav class="d-flex align-items-center justify-content-end gap-3 pt-2" aria-label="ページング">
            <button
                class="btn btn-outline-secondary" x-bind:class="{'text-white': dark}"
                x-bind:disabled="currentPage === 1"
                x-on:click="currentPage--">前へ</button>
            <span x-text="currentPage + ' / ' + pages"></span>
            <button
                class="btn btn-outline-secondary" x-bind:class="{'text-white': dark}"
                x-bind:disabled="currentPage >= pages"
                x-on:click="currentPage++">次へ</button>
        </nav>
        <section x-show="editing" x-cloak class="bg-body-tertiary border rounded-3 mt-4 p-4" aria-label="入力フォーム">
            <h3 class="h5" x-text="editingId === null ? '新規作成' : '編集'"></h3>
            <form x-on:submit.prevent="save">
                <label class="form-label" for="sample-name">名前 <span class="text-danger">必須</span></label>
                <input
                    id="sample-name"
                    class="form-control"
                    x-model="name"
                    required
                    maxlength="100"
                    x-bind:aria-invalid="error !== ''"
                    aria-describedby="sample-name-error">
                <p id="sample-name-error" class="text-danger" x-text="error" role="alert"></p>
                <label class="form-label" for="sample-status">状態</label>
                <select id="sample-status" class="form-select mb-3" x-model="status">
                    <option>未着手</option>
                    <option>確認中</option>
                    <option>完了</option>
                </select>
                <div class="d-flex gap-2">
                    <button id="sample-save" class="btn btn-primary" type="submit">保存</button>
                    <button
                        class="btn btn-outline-secondary" x-bind:class="{'text-white': dark}"
                        type="button"
                        x-on:click="editing = false">キャンセル</button>
                </div>
            </form>
        </section>
        <section x-show="detail !== null" x-cloak class="bg-body-tertiary border rounded-3 mt-4 p-4" aria-label="詳細">
            <h3 class="h5">詳細</h3>
            <dl>
                <dt>名前</dt>
                <dd id="sample-detail-name" x-text="detail?.name">
                </dd>
                <dt>状態</dt>
                <dd x-text="detail?.status">
                </dd>
            </dl>
            <button class="btn btn-outline-secondary" x-bind:class="{'text-white': dark}" x-on:click="detail = null">詳細を閉じる</button>
        </section>
        <section x-show="removing !== null" x-cloak class="alert alert-warning mt-3" aria-label="削除確認">
            <p>
                <span x-text="removing?.name"></span>を削除しますか？</p>
            <div class="d-flex gap-2">
                <button class="btn btn-danger" x-on:click="remove">削除する</button>
                <button class="btn btn-outline-secondary" x-bind:class="{'text-white': dark}" x-on:click="removing = null">キャンセル</button>
            </div>
        </section>
        <p class="mt-3 mb-0" role="status" x-text="message"></p>
    </div>
</section>
