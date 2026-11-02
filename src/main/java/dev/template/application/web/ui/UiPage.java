package dev.template.application.web.ui;

/**
 * 共通レイアウトの表示情報。
 * @param contextPath 配置先のcontext path
 * @param authenticated 認証済み利用者か
 * @param csrfName CSRFのフォーム項目名
 * @param csrfHeader CSRFのheader名
 * @param csrfToken CSRF token
 */
public record UiPage(String contextPath, boolean authenticated, String csrfName, String csrfHeader, String csrfToken) {
}
