package dev.template.application.web.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.ScreenshotAnimations;
import dev.template.application.Application;
import dev.template.application.feature.api.authorization.FeatureAuthority;
import dev.template.application.feature.api.command.CreateFeatureParam;
import dev.template.application.feature.api.command.FeatureCommands;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** 実ブラウザーでVaadinの入力・権限制御・エラー秘匿を検証する。認証sessionはテスト専用構成で供給する。 */
@ExtendWith(OutputCaptureExtension.class)
@Testcontainers
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.docker.compose.enabled=false", "server.address=127.0.0.1", "vaadin.productionMode=true"})
@Import(FeatureUiIntegrationTest.TestSessions.class)
@Tag("integration")
@MockitoSpyBean(types = FeatureCommands.class)
class FeatureUiIntegrationTest {
    /** Springの接続先として共有する、このtest class専用のPostgreSQL。 */
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(System.getProperty("test.postgres.image"));

    /** 認可・transaction境界を持つFeatureの公開更新API。 */
    private final FeatureCommands commands;

    /** 実ブラウザーから接続するtest用HTTP serverの動的port。 */
    private final int port;

    /**
     * 起動済みHTTP serverのportを受け取る。
     *
     * @param port ランダムに割り当てられたport
     * @param commands Commandのspy
     */
    @Autowired
    FeatureUiIntegrationTest(@LocalServerPort final int port, final FeatureCommands commands) {
        this.port = port;
        this.commands = commands;
    }

    /** 追加部品の入力制約・選択・メニュー・折りたたみを匿名ブラウザーで検証する。 */
    @Test
    void additionalComponentsValidateInputAndRespondToSelection() {
        try (final Playwright playwright = Playwright.create();
            final Browser browser = playwright.chromium().launch();
            final BrowserContext context = browser.newContext()) {
            final Page page = context.newPage();
            page.navigate("http://127.0.0.1:" + port + "/components");

            // 用途別fieldの制約を確認する。
            page.getByText("数値・時刻", new Page.GetByTextOptions().setExact(true)).click();
            page.locator("#example-email input").fill("invalid");
            page.locator("#example-quantity input").fill("11");
            page.locator("#example-email input").click();
            assertThat(page.locator("#example-email")).hasAttribute("invalid", "");
            assertThat(page.locator("#example-quantity")).hasAttribute("invalid", "");
            page.locator("#example-email input").fill("sample@example.com");
            page.locator("#example-quantity input").fill("2");
            page.locator("#example-email input").click();
            assertThat(page.locator("#example-quantity")).not().hasAttribute("invalid", "");
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/components-typed.png"))
                .setFullPage(true));

            // 選択結果の表示を確認する。
            page.getByText("選択部品", new Page.GetByTextOptions().setExact(true)).click();
            page.locator("#example-channels").getByText("モバイル", new Locator.GetByTextOptions().setExact(true)).click();
            assertThat(page.locator("#example-selection-result")).hasText("確認対象: モバイル");
            page.setViewportSize(390, 844);
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(Path.of("build/reports/ui/components-selection-mobile.png")).setFullPage(true));

            // 狭い画面でもメニューと補足情報を操作できることを確認する。
            page.getByText("表示・メニュー", new Page.GetByTextOptions().setExact(true)).click();
            page.getByText("ヘルプ", new Page.GetByTextOptions().setExact(true)).click();
            assertThat(page.locator("#example-menu-result")).hasText("メニュー項目はクリックやキーボードで選べます。");
            page.getByText("データの保存について", new Page.GetByTextOptions().setExact(true)).click();
            assertThat(
                page.getByText("入力内容はDBに保存されません。画面を再読み込みすると初期化されます。", new Page.GetByTextOptions().setExact(true)))
                .isVisible();
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(Path.of("build/reports/ui/components-display-mobile.png")).setFullPage(true));
        }
    }

    /**
     * 実ブラウザーで作成・取得・不正入力・未検出・技術障害を操作し、権限別の応答と秘密情報の非表示を検証する。
     *
     * @param output テスト中のログ出力
     */
    @Test
    void browserCreatesFindsValidatesAndEnforcesAuthorities(final CapturedOutput output) {
        try (Playwright playwright = Playwright.create();
            Browser browser = playwright.chromium().launch();
            BrowserContext context = browser.newContext()) {
            final String base = "http://127.0.0.1:" + port;
            assertThat(context.request().get(base + "/features").url()).endsWith("/login");
            context.request().get(base + "/test-session?write=true");
            final Page page = context.newPage();
            page.navigate(base + "/features");

            // 入力検証と作成・取得の正常系を確認する。
            page.locator("#feature-name input").fill("   ");
            page.locator("#feature-create").click();
            assertThat(page.locator("#feature-name")).hasAttribute("invalid", "");
            page.locator("#feature-name input").fill("画面から作成");
            page.locator("#feature-create").click();
            assertThat(page.locator("#feature-result")).hasText("作成しました。");
            final String id = page.locator("#feature-id input").inputValue();
            page.locator("#feature-find").click();
            assertThat(page.locator("#feature-result")).hasText("名前: 画面から作成");

            // 形式エラーと未登録IDを区別する。
            page.locator("#feature-id input").fill("invalid");
            page.locator("#feature-find").click();
            assertThat(page.locator("#feature-id")).hasAttribute("invalid", "");
            page.locator("#feature-id input").fill("0199417c-0000-7000-8000-000000000099");
            page.locator("#feature-find").click();
            assertThat(page.locator("#feature-result")).hasText("見つかりませんでした。");

            // 技術障害時の画面・ログが非機密の診断情報に限定されることを確認する。
            Mockito.doThrow(new IllegalStateException("ui-private-secret"))
                .when((FeatureCommands) AopTestUtils.getUltimateTargetObject(commands))
                .create(new CreateFeatureParam("error-check"));
            page.locator("#feature-name input").fill("error-check");
            page.locator("#feature-create").click();
            assertThat(page.locator("vaadin-notification-card")).containsText("処理に失敗しました。");
            assertThat(output.getAll()).doesNotContain("ui-private-secret");
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/features.png")));

            // 参照専用sessionでは取得を許可し、更新を拒否する。
            try (BrowserContext reader = browser.newContext()) {
                reader.request().get(base + "/test-session");
                final Page readPage = reader.newPage();
                readPage.navigate(base + "/features");
                readPage.locator("#feature-id input").fill(id);
                readPage.locator("#feature-find").click();
                assertThat(readPage.locator("#feature-result")).hasText("名前: 画面から作成");
                readPage.locator("#feature-name input").fill("拒否する入力");
                readPage.locator("#feature-create").click();
                assertThat(readPage.locator("#feature-result")).hasText("作成する権限がありません。");
            }
        }
    }

    /** 部品集で空入力を拒否し、作成・検索・編集・削除キャンセルと確定・通知が正しく動作することを検証する。 */
    @Test
    void componentGalleryValidatesEditsFiltersAndConfirmsDeletion() {
        try (final Playwright playwright = Playwright.create();
            final Browser browser = playwright.chromium().launch();
            final BrowserContext context = browser.newContext()) {
            final String base = "http://127.0.0.1:" + port;
            assertThat(context.request().get(base + "/components").status()).isEqualTo(200);
            assertThat(context.request().get(base + "/features").url()).endsWith("/login");
            assertThat(context.request().get(base + "/api/v1/features").status()).isEqualTo(401);
            assertThat(context.request().get(base + "/actuator/metrics").status()).isEqualTo(401);
            final Page page = context.newPage();
            assertThat(page.navigate(base + "/").status()).isEqualTo(200);
            assertThat(page.locator("#welcome-gallery")).isVisible();
            page.screenshot(
                new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/welcome.png")).setFullPage(true));
            page.setViewportSize(390, 844);
            page.screenshot(
                new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/welcome-mobile.png")).setFullPage(true));
            page.setViewportSize(1280, 900);

            // ホームから部品集へ遷移し、一覧と詳細の連動を確認する。
            page.locator("#welcome-gallery").click();
            assertThat(page.locator("#sample-grid")).containsText("画面レイアウトの確認");

            // 作成・編集・削除を一連の操作で確認する。
            page.locator("#sample-create").click();
            page.locator("#sample-save").click();
            assertThat(page.locator("#sample-name")).hasAttribute("invalid", "");
            page.locator("#sample-name input").fill("ブラウザー追加行");
            page.locator("#sample-save").click();
            assertThat(page.locator("#sample-grid")).containsText("ブラウザー追加行");
            page.locator("#sample-search input").fill("ブラウザー追加行");
            assertThat(page.locator("#sample-count")).hasText("1件");
            page.locator("#sample-grid vaadin-button:visible").filter(new Locator.FilterOptions().setHasText("編集"))
                .click();
            page.locator("#sample-name input").fill("ブラウザー編集行");
            page.locator("#sample-save").click();
            page.locator("#sample-search input").fill("ブラウザー編集行");
            assertThat(page.locator("#sample-grid")).containsText("ブラウザー編集行");
            page.locator("#sample-grid vaadin-button:visible").filter(new Locator.FilterOptions().setHasText("削除"))
                .click();
            page.getByText("キャンセル", new Page.GetByTextOptions().setExact(true)).click();
            assertThat(page.locator("#sample-grid")).containsText("ブラウザー編集行");
            page.locator("#sample-grid vaadin-button:visible").filter(new Locator.FilterOptions().setHasText("削除"))
                .click();
            page.getByText("削除する", new Page.GetByTextOptions().setExact(true)).click();
            assertThat(page.locator("#sample-count")).hasText("0件");
            page.locator("#sample-search input").fill("");
            assertThat(page.locator("#sample-count")).hasText("3件");
            page.screenshot(
                new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/components.png")).setFullPage(true));
            page.setViewportSize(390, 844);
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/components-mobile.png"))
                .setFullPage(true));
            page.getByText("入力部品", new Page.GetByTextOptions().setExact(true)).click();
            page.getByText("入力内容を確認", new Page.GetByTextOptions().setExact(true)).click();
            assertThat(page.locator("vaadin-notification-card").last()).containsText("通知方法: 画面内");
        }
    }

    /** 実フォームで失敗・成功・DB操作・logoutを検証し、認証情報が失効することを確認する。 */
    @Test
    void formLoginAndLogoutProtectBusinessOperations() {
        try (Playwright playwright = Playwright.create();
            Browser browser = playwright.chromium().launch();
            BrowserContext context = browser.newContext()) {
            final String base = "http://127.0.0.1:" + port;
            final Page page = context.newPage();
            page.navigate(base + "/features");
            assertThat(page).hasURL(base + "/login");
            page.locator("input[name=username]").fill("test-user");
            page.locator("input[name=password]").fill("wrong-password");
            page.locator("vaadin-button[theme~=submit]").click();
            assertThat(page.locator("vaadin-login-form")).hasAttribute("error", "");
            page.locator("input[name=username]").fill("test-user");
            page.locator("input[name=password]").fill("test-password-12345");
            page.locator("vaadin-button[theme~=submit]").click();

            // 成功したsessionで実際のDB操作を行う。
            page.waitForURL(Pattern.compile(Pattern.quote(base) + "/(?!login).*"));
            page.navigate(base + "/features");
            assertThat(page.locator("#feature-name input")).isVisible();
            page.locator("#feature-name input").fill("authenticated-user");
            page.locator("#feature-create").click();
            assertThat(page.locator("#feature-result")).hasText("作成しました。");
            page.screenshot(
                new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/authenticated-feature.png")));
            page.navigate(base + "/");

            // logout後は同じbrowserでも認証を要求する。
            page.locator("#starter-account").click();
            assertThat(page.locator("#starter-account")).hasText("ログイン");
            assertThat(context.request().get(base + "/api/v1/features/0199417c-0000-7000-8000-000000000099").status())
                .isEqualTo(401);
            page.navigate(base + "/login");
            assertThat(page.locator("input[name=username]")).isVisible();
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/login.png")));
            page.setViewportSize(390, 844);
            assertThat(page.locator("input[name=username]")).isVisible();
            assertThat((Boolean) page.evaluate("document.documentElement.scrollWidth > window.innerWidth")).isFalse();
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/login-mobile.png")));
        }
    }

    /** 共通ナビゲーション、明暗テーマ、一覧詳細のレスポンシブ表示とカード操作を検証する。 */
    @Test
    void richPagesNavigateSwitchThemeAndShowDetails() {
        try (final Playwright playwright = Playwright.create();
            final Browser browser = playwright.chromium().launch();
            final BrowserContext context = browser.newContext()) {
            final Page page = context.newPage();
            page.setViewportSize(1440, 1000);
            page.navigate("http://127.0.0.1:" + port + "/");
            assertThat(page.locator("#welcome-gallery")).isVisible();
            page.screenshot(
                new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/welcome-rich.png")).setFullPage(true));

            // 標準の明暗切替を確認する。
            page.locator("#starter-dark-mode input").check();
            assertThat(page.locator("html")).hasCSS("color-scheme", "dark");
            page.screenshot(
                new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/welcome-dark.png")).setFullPage(true));
            page.locator("#starter-dark-mode input").uncheck();
            page.locator("#welcome-gallery").click();
            page.getByText("一覧＋詳細", new Page.GetByTextOptions().setExact(true)).click();
            page.locator("#example-master-grid").getByText("フォームの確認", new Locator.GetByTextOptions().setExact(true))
                .click();
            assertThat(page.locator("#example-detail-card")).containsText("状態: 未着手");
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/components-detail.png"))
                .setFullPage(true));
            page.setViewportSize(390, 844);

            // mobileでは詳細を閉じて一覧へ戻れることを確認する。
            page.locator("#example-detail-close").scrollIntoViewIfNeeded();
            assertThat(page.locator("#example-detail-close")).isVisible();
            assertThat(page.evaluate("document.documentElement.scrollWidth <= window.innerWidth")).isEqualTo(true);
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(Path.of("build/reports/ui/components-detail-mobile.png")).setFullPage(true));
            page.locator("#example-detail-close").click();
            assertThat(page.locator("#example-detail-card")).hasCount(0);
            page.getByText("カード・操作", new Page.GetByTextOptions().setExact(true)).click();
            page.locator("#example-primary-action").click();
            assertThat(page.locator("vaadin-notification-card").last()).containsText("確認しました。");
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(Path.of("build/reports/ui/components-cards-mobile.png")).setFullPage(true));
            page.setViewportSize(1440, 1000);
            page.locator("#starter-dark-mode input").check();
            assertThat(page.locator("html")).hasCSS("color-scheme", "dark");
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/components-cards-dark.png"))
                .setAnimations(ScreenshotAnimations.DISABLED).setFullPage(true));
            assertThat(page.locator("html")).hasCSS("color-scheme", "dark");
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("ホーム").setExact(true)).click();
            assertThat(page.locator("#welcome-gallery")).isVisible();
        }
    }

    /** 両テーマの資産分離、独立した操作、明暗と狭幅配置を実documentで検証する。 */
    @Test
    void standardThemesRenderIndependentlyAndRemainInteractive() {
        try (final Playwright playwright = Playwright.create();
            final Browser browser = playwright.chromium().launch();
            final BrowserContext context = browser.newContext()) {
            final String base = "http://127.0.0.1:" + port;
            final Page page = context.newPage();
            page.setViewportSize(1720, 1400);
            final var response = page.navigate(base + "/theme-comparison");
            assertThat(response.headers()).containsEntry("x-frame-options", "SAMEORIGIN");
            assertThat(page).hasURL(base + "/theme-comparison");

            // 各テーマのstylesheetと入力状態がdocument単位で独立していることを確認する。
            for (final String theme : List.of("aura", "lumo")) {
                final var frame = page.frameLocator("#preview-" + theme);
                assertThat(frame.locator("#sample-grid")).isVisible();
                assertThat(frame.locator("link[rel=stylesheet][href='" + theme + "/" + theme + ".css']")).hasCount(1);
                final String other = "aura".equals(theme) ? "lumo" : "aura";
                assertThat(frame.locator("link[rel=stylesheet][href='" + other + "/" + other + ".css']")).hasCount(0);
                assertThat(frame.locator("html")
                    .evaluate("element => getComputedStyle(element).getPropertyValue('"
                        + ("aura".equals(theme) ? "--aura-accent-color-light" : "--lumo-primary-color") + "').trim()"))
                    .isNotEqualTo("");
                frame.locator("#sample-search input").fill("存在しない行");
                assertThat(frame.locator("#sample-count")).hasText("0件");
                assertThat(page.frameLocator("#preview-" + other).locator("#sample-count")).hasText("3件");
                frame.locator("#sample-search input").fill("");
                assertThat(frame.locator("#sample-count")).hasText("3件");
                frame.getByText("カード・操作", new FrameLocator.GetByTextOptions().setExact(true)).click();
                frame.locator("#example-primary-action").click();
                assertThat(frame.locator("vaadin-notification-card").last()).containsText("確認しました。");
            }
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/theme-comparison-light.png"))
                .setFullPage(true));

            // 明暗切替時の両documentの更新を確認する。
            page.locator("#comparison-dark-mode input").check();
            for (final String theme : List.of("aura", "lumo")) {
                final var frame = page.frameLocator("#preview-" + theme);
                assertThat(frame.locator("html")).hasCSS("color-scheme", "dark");
                assertThat(frame.locator("#sample-count")).hasText("3件");
            }
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/theme-comparison-dark.png"))
                .setFullPage(true));
            page.setViewportSize(390, 844);
            assertThat(page.evaluate("document.documentElement.scrollWidth <= window.innerWidth")).isEqualTo(true);
            assertThat(page.locator("#preview-lumo").boundingBox().y)
                .isGreaterThan(page.locator("#preview-aura").boundingBox().y);
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(Path.of("build/reports/ui/theme-comparison-mobile.png")).setFullPage(true));

            // 狭い画面で両方のpreviewを確認する。
            page.locator("#preview-lumo").evaluate("element => element.scrollIntoView({block: 'start'})");
            page.screenshot(
                new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/theme-comparison-lumo-mobile.png")));
            for (final String theme : List.of("aura", "lumo")) {
                assertThat(page.frameLocator("#preview-" + theme).locator("html")
                    .evaluate("element => element.scrollWidth <= element.ownerDocument.defaultView.innerWidth"))
                    .isEqualTo(true);
            }
            page.navigate(base + "/");
            assertThat(page.locator("link[href='aura/aura.css']")).hasCount(1);
            assertThat(page.locator("link[href='lumo/lumo.css']")).hasCount(0);
        }
    }

    /** test source専用の認証。権限を持つsessionをブラウザーへ発行する。 */
    @TestConfiguration(proxyBeanMethods = false)
    static class TestSessions {
        /**
         * テストsession発行URLだけを匿名許可する。
         *
         * @param http filter chain builder
         * @return テスト専用filter chain
         * @throws Exception 構成に失敗した場合
         */
        @Bean
        @Order(0)
        SecurityFilterChain testSessionSecurity(final HttpSecurity http) throws Exception {
            return http.securityMatcher("/test-session")
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll()).build();
        }

        /**
         * 指定した型付きAuthorityのsessionを作る。
         *
         * @return テスト専用servlet登録
         */
        @Bean
        ServletRegistrationBean<HttpServlet> testSessionServlet() {
            return new ServletRegistrationBean<>(new HttpServlet() {
                /** {@inheritDoc} */
                @Override
                protected void doGet(final HttpServletRequest request, final HttpServletResponse response) {
                    final var authorities = "true".equals(request.getParameter("write"))
                        ? List.of(FeatureAuthority.READ, FeatureAuthority.WRITE)
                        : List.of(FeatureAuthority.READ);
                    final var authentication = UsernamePasswordAuthenticationToken.authenticated("ui-test", null,
                        authorities);
                    request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        new SecurityContextImpl(authentication));
                    response.setStatus(204);
                }
            }, "/test-session");
        }
    }
}
