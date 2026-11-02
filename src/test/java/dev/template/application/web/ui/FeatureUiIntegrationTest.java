package dev.template.application.web.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import dev.template.application.Application;
import dev.template.application.feature.api.authorization.FeatureAuthority;
import dev.template.application.feature.api.command.CreateFeatureParam;
import dev.template.application.feature.api.command.FeatureCommands;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.file.Path;
import java.util.List;
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

/** 実ブラウザーでHTML画面の入力・権限制御・エラー秘匿を検証する。認証sessionはテスト専用構成で供給する。 */
@ExtendWith(OutputCaptureExtension.class)
@Testcontainers
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.docker.compose.enabled=false", "server.address=127.0.0.1"})
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

    /**
     * 通常フォーム認証、htmxのDB操作、入力拒否、技術障害と権限拒否を検証する。
     * @param output 検証中のログ
     */
    @Test
    void browserCreatesFindsValidatesAndEnforcesAuthorities(final CapturedOutput output) {
        try (final Playwright playwright = Playwright.create();
            final Browser browser = playwright.chromium().launch();
            final BrowserContext context = browser.newContext()) {
            final String base = "http://127.0.0.1:" + port;
            final Page page = context.newPage();
            page.navigate(base + "/features");
            assertThat(page).hasURL(base + "/login");
            page.locator("#username").fill("test-user");
            page.locator("#password").fill("wrong-password");
            page.locator("#login-submit").click();
            assertThat(page.locator("#login-error")).isVisible();
            page.locator("#username").fill("test-user");
            page.locator("#password").fill("test-password-12345");
            page.locator("#login-submit").click();
            page.waitForURL(base + "/");
            page.navigate(base + "/features");
            page.locator("#feature-name").fill("   ");
            page.locator("#feature-create").click();
            assertThat(page.locator("#feature-name")).hasAttribute("aria-invalid", "true");
            page.locator("#feature-name").fill("画面から作成<script>alert(1)</script>");
            page.locator("#feature-create").click();
            assertThat(page.locator("#feature-result")).hasText("作成しました。");
            final String id = page.locator("#feature-id").inputValue();
            page.locator("#feature-find").click();
            assertThat(page.locator("#feature-result")).hasText("名前: 画面から作成<script>alert(1)</script>");
            page.locator("#feature-id").fill("invalid");
            page.locator("#feature-find").click();
            assertThat(page.locator("#feature-id")).hasAttribute("aria-invalid", "true");
            page.locator("#feature-id").fill("0199417c-0000-7000-8000-000000000099");
            page.locator("#feature-find").click();
            assertThat(page.locator("#feature-result")).hasText("見つかりませんでした。");

            Mockito.doThrow(new IllegalStateException("ui-private-secret"))
                .when((FeatureCommands) AopTestUtils.getUltimateTargetObject(commands))
                .create(new CreateFeatureParam("error-check"));
            page.locator("#feature-name").fill("error-check");
            page.locator("#feature-create").click();
            assertThat(page.locator("#ui-error")).containsText("処理に失敗しました。");
            assertThat(output.getAll()).doesNotContain("ui-private-secret");
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/features.png")));

            try (final BrowserContext reader = browser.newContext()) {
                reader.request().get(base + "/test-session");
                final Page readPage = reader.newPage();
                readPage.navigate(base + "/features");
                readPage.locator("#feature-id").fill(id);
                readPage.locator("#feature-find").click();
                assertThat(readPage.locator("#feature-result")).containsText("画面から作成");
                readPage.locator("#feature-name").fill("拒否する入力");
                readPage.locator("#feature-create").click();
                assertThat(readPage.locator("#ui-error")).containsText("操作する権限がありません。");
                reader.clearCookies();
                readPage.locator("#feature-create").click();
                assertThat(readPage).hasURL(base + "/login");
            }
            page.locator("#starter-account").click();
            page.waitForURL(base + "/");
            assertThat(context.request().get(base + "/api/v1/features/" + id).status()).isEqualTo(401);
            page.navigate(base + "/features");
            assertThat(page).hasURL(base + "/login");
        }
    }

    /** 公開部品集の検索・ページング・詳細・入力・削除と明暗・狭幅を実ブラウザーで確認する。 */
    @Test
    void galleryUsesStandardComponentsAndLocalInteractions() {
        try (final Playwright playwright = Playwright.create();
            final Browser browser = playwright.chromium().launch();
            final BrowserContext context = browser.newContext()) {
            final Page page = context.newPage();
            page.navigate("http://127.0.0.1:" + port + "/");
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/welcome.png")));
            page.locator("#welcome-gallery").click();
            assertThat(page.locator("#sample-count")).hasText("3件");
            assertThat(page.locator("#starter-gallery")).hasAttribute("aria-current", "page");
            assertThat(page.locator("#starter-navigation")).isVisible();
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/components-light.png"))
                .setFullPage(true));
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("次へ").setExact(true)).click();
            assertThat(page.locator("#sample-grid")).containsText("一覧の確認");
            page.locator("#sample-create").click();
            page.locator("#sample-name").fill("   ");
            page.locator("#sample-save").click();
            assertThat(page.locator("#sample-name-error")).containsText("名前を1〜100文字");
            page.locator("#sample-name").fill("追加行");
            page.locator("#sample-save").click();
            assertThat(page.locator("#sample-count")).hasText("4件");
            page.locator("#sample-search").fill("追加行");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("検索").setExact(true)).click();
            assertThat(page.locator("#sample-count")).hasText("1件");
            page.locator("#sample-grid").getByText("詳細", new Locator.GetByTextOptions().setExact(true)).click();
            assertThat(page.locator("#sample-detail-name")).hasText("追加行");
            page.locator("#sample-grid").getByText("編集", new Locator.GetByTextOptions().setExact(true)).click();
            page.locator("#sample-name").fill("編集行");
            page.locator("#sample-save").click();
            page.locator("#sample-search").fill("編集行");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("検索").setExact(true)).click();
            page.locator("#sample-grid").getByText("削除", new Locator.GetByTextOptions().setExact(true)).click();
            page.getByText("キャンセル", new Page.GetByTextOptions().setExact(true)).last().click();
            assertThat(page.locator("#sample-count")).hasText("1件");
            page.locator("#sample-grid").getByText("削除", new Locator.GetByTextOptions().setExact(true)).click();
            page.getByText("削除する", new Page.GetByTextOptions().setExact(true)).click();
            assertThat(page.locator("#sample-count")).hasText("0件");
            page.getByLabel("追加項目を表示").check();
            assertThat(page.locator("#example-extra-text")).isVisible();
            page.locator("#starter-dark-mode").check();
            assertThat(page.locator("html")).hasAttribute("data-bs-theme", "dark");
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/components-dark.png"))
                .setFullPage(true));
            page.setViewportSize(390, 844);
            assertThat(page.locator("#starter-navigation")).isHidden();
            page.locator("#starter-menu-toggle").click();
            assertThat(page.locator("#starter-navigation")).isVisible();
            assertThat(page.locator("#starter-menu-toggle")).hasAttribute("aria-expanded", "true");
            page.locator("#starter-menu-toggle").click();
            assertThat(page.locator("#starter-navigation")).isHidden();
            assertThat(page.evaluate("document.documentElement.scrollWidth <= window.innerWidth")).isEqualTo(true);
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/components-mobile.png"))
                .setFullPage(true));
            page.reload();
            assertThat(page.locator("#sample-count")).hasText("3件");
        }
    }

    /** JavaScriptを無効にした通常フォームでも作成・検索できることを検証する。 */
    @Test
    void standardFormsWorkWithoutJavaScript() {
        try (final Playwright playwright = Playwright.create();
            final Browser browser = playwright.chromium().launch();
            final BrowserContext context = browser
                .newContext(new Browser.NewContextOptions().setJavaScriptEnabled(false))) {
            final String base = "http://127.0.0.1:" + port;
            context.request().get(base + "/test-session?write=true");
            final Page page = context.newPage();
            page.navigate(base + "/features");
            page.locator("#feature-name").fill("通常フォーム");
            page.locator("#feature-create").click();
            assertThat(page.locator("#feature-result")).hasText("作成しました。");
            page.locator("#feature-find").click();
            assertThat(page.locator("#feature-result")).hasText("名前: 通常フォーム");
            assertThat(context.request().post(base + "/features").status()).isEqualTo(403);
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
