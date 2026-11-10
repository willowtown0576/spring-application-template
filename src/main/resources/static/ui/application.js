const themeStorageKey = 'spring-application-template.theme';
try {
    if (localStorage.getItem(themeStorageKey) === 'dark') document.documentElement.dataset.bsTheme = 'dark';
} catch {
    // Storageを利用できない場合も、初期表示とページ内のテーマ切り替えを提供する。
}

document.addEventListener('htmx:configRequest', event => {
    const token = document.cookie.split('; ').find(cookie => cookie.startsWith('XSRF-TOKEN='));
    const header = document.querySelector('meta[name="csrf-header"]');
    if (token && header) event.detail.headers[header.content] = decodeURIComponent(token.substring('XSRF-TOKEN='.length));
});
document.addEventListener('htmx:beforeSwap', event => {
    if ([400, 403, 422, 500].includes(event.detail.xhr.status)) {
        event.detail.shouldSwap = true;
        event.detail.isError = false;
        if (event.detail.xhr.status !== 422) {
            event.detail.target = document.getElementById('ui-error');
            event.detail.swapOverride = 'innerHTML';
            event.detail.target.hidden = false;
        }
    }
});
document.addEventListener('htmx:beforeRequest', () => {
    document.getElementById('ui-error').hidden = true;
});
for (const eventName of ['htmx:sendError', 'htmx:timeout']) {
    document.addEventListener(eventName, () => {
        const error = document.getElementById('ui-error');
        error.textContent = '通信に失敗しました。接続を確認して再度操作してください。';
        error.hidden = false;
    });
}
document.addEventListener('alpine:init', () => {
    Alpine.data('workspaceShell', () => ({
        dark: document.documentElement.dataset.bsTheme === 'dark',
        init() {
            window.addEventListener('pageshow', event => {
                if (event.persisted) {
                    try { this.dark = localStorage.getItem(themeStorageKey) === 'dark'; }
                    catch { /* 戻る操作でもStorageの拒否によって画面を停止しない。 */ }
                }
            });
            this.$watch('dark', value => {
                try { localStorage.setItem(themeStorageKey, value ? 'dark' : 'light'); }
                catch { /* Storageが拒否されても現在の表示は切り替える。 */ }
            });
        }
    }));
    Alpine.data('sampleGallery', () => ({
        items: [
            {id: 1, name: '画面レイアウトの確認', status: '未着手'},
            {id: 2, name: 'フォームの確認', status: '確認中'},
            {id: 3, name: '一覧の確認', status: '完了'}
        ],
        nextId: 4, query: '', search: '', currentPage: 1,
        editing: false, editingId: null, name: '', status: '未着手', error: '',
        detail: null, removing: null, message: '',
        get filtered() { return this.items.filter(item => item.name.includes(this.search)); },
        get pages() { return Math.max(1, Math.ceil(this.filtered.length / 2)); },
        get visible() { return this.filtered.slice((this.currentPage - 1) * 2, this.currentPage * 2); },
        create() {
            this.editingId = null; this.name = ''; this.status = '未着手';
            this.error = ''; this.editing = true;
        },
        edit(item) {
            this.editingId = item.id; this.name = item.name; this.status = item.status;
            this.error = ''; this.editing = true;
        },
        save() {
            if (!this.name.trim() || Array.from(this.name).length > 100) {
                this.error = '名前を1〜100文字で入力してください。'; return;
            }
            const item = {id: this.editingId ?? this.nextId++, name: this.name, status: this.status};
            if (this.editingId === null) this.items.push(item);
            else this.items = this.items.map(existing => existing.id === item.id ? item : existing);
            this.detail = null; this.editing = false; this.search = ''; this.query = '';
            this.currentPage = this.pages; this.message = '保存しました。';
        },
        remove() {
            this.items = this.items.filter(item => item.id !== this.removing.id);
            this.removing = null; this.detail = null;
            this.currentPage = Math.min(this.currentPage, this.pages); this.message = '削除しました。';
        }
    }));
});
