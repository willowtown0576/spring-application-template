package dev.template.application.web.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.ReducedMotion;
import dev.template.application.Application;
import dev.template.application.feature.api.authorization.FeatureAuthority;
import dev.template.application.feature.api.command.FeatureCommands;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.file.Path;
import java.util.ArrayList;
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

    /** 一括操作の位置・配色・行削除と、テーマの遷移／再読込／戻るでの保持を検証する。 */
    @Test
    void actionsAreDiscoverableAndThemeSurvivesNavigation() {
        try (final Playwright playwright = Playwright.create();
            final Browser browser = playwright.chromium().launch();
            final BrowserContext context = browser.newContext()) {
            final String base = "http://127.0.0.1:" + port;
            context.request().get(base + "/test-session?write=true");
            final Page page = context.newPage();
            final List<String> scriptErrors = new ArrayList<>();
            page.onPageError(scriptErrors::add);
            page.navigate(base + "/features/new?rows=2");
            page.locator("[name=rowName]").nth(0).fill("ui-polish-one");
            page.locator("[name=rowName]").nth(1).fill("ui-polish-two");
            page.locator("#feature-save").click();
            page.locator("#search-name").fill("ui-polish-");
            page.waitForResponse(
                response -> response.url().contains("/features?") && response.request().method().equals("GET"),
                () -> page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("検索").setExact(true))
                    .click());
            page.waitForFunction(
                "document.querySelectorAll('.htmx-request, .htmx-swapping, .htmx-settling').length === 0");
            assertThat(page.locator("tbody tr")).hasCount(2);
            assertThat(page.evaluate(
                "document.getElementById('feature-bulk-actions').getBoundingClientRect().top < document.querySelector('table').getBoundingClientRect().top"))
                .isEqualTo(true);
            assertThat(page.evaluate(
                """
                    (() => {
                        const search = document.querySelector('#feature-results button[type=submit]');
                        const create = document.getElementById('feature-new');
                        const clear = search.nextElementSibling;
                        return search.getBoundingClientRect().width === create.getBoundingClientRect().width
                            && search.getBoundingClientRect().width === clear.getBoundingClientRect().width
                            && Math.abs(search.parentElement.getBoundingClientRect().right - clear.getBoundingClientRect().right) <= 16
                            && create.getBoundingClientRect().top > search.getBoundingClientRect().bottom + 24;
                    })()
                    """))
                .isEqualTo(true);
            assertThat(page.locator("#feature-bulk-edit")).isDisabled();
            assertThat(page.locator("#feature-bulk-delete")).isDisabled();
            page.locator("[name=ids]").first().check();
            assertThat(page.locator("#feature-selected-count")).hasText("1");
            assertThat(page.locator("#feature-bulk-edit")).isEnabled();
            assertThat(page.locator("#feature-bulk-delete")).isEnabled();
            assertThat(page.locator(".feature-row-delete")).hasCount(2);
            assertThat(page.locator(".feature-row-edit").first().evaluate("element => getComputedStyle(element).color"))
                .isEqualTo("rgb(255, 255, 255)");
            assertThat(
                page.locator(".feature-row-delete").first().evaluate("element => getComputedStyle(element).color"))
                .isEqualTo("rgb(255, 255, 255)");
            assertThat(page.locator(".feature-row-edit").first()
                .evaluate("element => getComputedStyle(element).backgroundColor")).isEqualTo("rgb(25, 135, 84)");
            assertThat(page.locator(".feature-row-delete").first()
                .evaluate("element => getComputedStyle(element).backgroundColor")).isEqualTo("rgb(220, 53, 69)");
            page.evaluate("window.scrollTo({top: 0, behavior: 'instant'})");
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/features-actions.png"))
                .setFullPage(true));
            page.locator(".feature-row-delete").first().click();
            assertThat(page.locator("tbody tr")).hasCount(1);
            assertThat(page.locator("#feature-delete-confirm")).isVisible();
            page.locator("#feature-delete-cancel").click();
            page.locator("#starter-dark-mode").check();
            assertThat(page.locator("html")).hasAttribute("data-bs-theme", "dark");
            page.evaluate("window.scrollTo({top: 0, behavior: 'instant'})");
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/features-actions-dark.png"))
                .setFullPage(true));
            page.setViewportSize(390, 844);
            assertThat(page.evaluate("document.documentElement.scrollWidth <= window.innerWidth")).isEqualTo(true);
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(Path.of("build/reports/ui/features-actions-mobile.png")).setFullPage(true));
            page.setViewportSize(1280, 720);
            page.locator("tbody td a").first().click();
            assertThat(page.locator("html")).hasAttribute("data-bs-theme", "dark");
            assertThat(page.locator("#starter-dark-mode")).isChecked();
            page.reload();
            assertThat(page.locator("html")).hasAttribute("data-bs-theme", "dark");
            page.navigate(base + "/components");
            assertThat(page.locator("html")).hasAttribute("data-bs-theme", "dark");
            page.locator("#starter-dark-mode").uncheck();
            assertThat(page.locator("html")).hasAttribute("data-bs-theme", "light");
            page.goBack();
            assertThat(page.locator("html")).hasAttribute("data-bs-theme", "light");
            page.reload();
            assertThat(page.locator("#starter-dark-mode")).not().isChecked();
            page.locator("#starter-menu-toggle").click();
            assertThat(page.locator("#starter-navigation")).isFocused();
            page.keyboard().press("Escape");
            page.keyboard().press("Escape");
            assertThat(page.locator("#starter-navigation")).isHidden();
            assertThat(page.locator("#starter-menu-toggle")).isFocused();
            assertThat(scriptErrors).isEmpty();
        }
    }

    /**
     * 認証、複数登録、検索、更新競合、入力保持と権限拒否を実ブラウザーで検証する。
     * @param output 検証中のログ
     */
    @Test
    void browserCreatesFindsValidatesAndEnforcesAuthorities(final CapturedOutput output) {
        try (final Playwright playwright = Playwright.create();
            final Browser browser = playwright.chromium().launch();
            final BrowserContext context = browser.newContext()) {
            final String base = "http://127.0.0.1:" + port;
            final Page page = context.newPage();
            final List<String> scriptErrors = new ArrayList<>();
            page.onPageError(scriptErrors::add);
            page.navigate(base + "/features");
            assertThat(page).hasURL(base + "/login");
            assertThat(page.locator("#starter-navigation")).hasCount(0);
            assertThat(page.locator("#starter-menu-toggle")).hasCount(0);
            page.screenshot(
                new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/login.png")).setFullPage(true));
            page.setViewportSize(390, 844);
            assertThat(page.evaluate("document.documentElement.scrollWidth <= window.innerWidth")).isEqualTo(true);
            page.screenshot(
                new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/login-mobile.png")).setFullPage(true));
            page.setViewportSize(1280, 720);
            page.locator("#username").fill("test-user");
            page.locator("#password").fill("wrong-password");
            page.locator("#login-submit").click();
            assertThat(page.locator("#login-error")).isVisible();
            page.locator("#username").fill("test-user");
            page.locator("#password").fill("test-password-12345");
            page.locator("#login-submit").click();
            page.waitForURL(base + "/");
            page.navigate(base + "/features");
            page.locator("#feature-bulk-new").click();
            assertThat(page.locator(".feature-editor-row")).hasCount(3);
            page.locator("[name=rowName]").nth(0).fill("browser-one,<script>alert(1)</script>");
            page.locator("[name=rowName]").nth(1).fill("   ");
            page.locator("[name=rowName]").nth(2).fill("browser-three");
            page.locator("#feature-save").click();
            assertThat(page.getByRole(AriaRole.ALERT)).containsText("名前を1〜100文字");
            assertThat(page.locator("[name=rowName]").first()).hasValue("browser-one,<script>alert(1)</script>");
            page.onceDialog(dialog -> {
                assertThat(dialog.type()).isEqualTo("confirm");
                dialog.dismiss();
            });
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("一覧へ戻る").setExact(true)).click();
            assertThat(page.locator("[name=rowName]").first()).hasValue("browser-one,<script>alert(1)</script>");
            page.locator("#starter-menu-toggle").click();
            page.onceDialog(dialog -> dialog.dismiss());
            page.locator("#starter-gallery").click();
            assertThat(page.locator("#starter-navigation")).isVisible();
            page.locator("#starter-menu-close").click();
            assertThat(page.locator("#starter-navigation")).isHidden();
            assertThat(page.locator("[name=rowName]").first()).hasValue("browser-one,<script>alert(1)</script>");
            page.locator("[name=rowName]").nth(1).fill("browser-two");
            page.locator("#feature-add-row").click();
            assertThat(page.locator(".feature-editor-row")).hasCount(4);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("行を削除")).last().click();
            assertThat(page.locator(".feature-editor-row")).hasCount(3);
            page.locator("#feature-save").click();
            assertThat(page.getByRole(AriaRole.STATUS)).containsText("保存しました。");
            page.locator("#search-name").fill("browser-");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("検索").setExact(true)).click();
            assertThat(page.locator("tbody tr")).hasCount(3);
            assertThat(page.locator("body").innerText()).doesNotContain("登録日時", "version")
                .doesNotContainPattern("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
            page.locator("[name=ids]").first().check();
            page.locator("[name=ids]").nth(1).check();
            page.locator("#feature-bulk-edit").click();
            assertThat(page.locator(".feature-editor-row")).hasCount(2);
            assertThat(page.locator("nav a[href$='/features']")).hasAttribute("aria-current", "page");
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/features-editor.png"))
                .setFullPage(true));
            page.locator("[name=rowName]").first().fill("browser-updated-a");
            page.locator("[name=rowName]").last().fill("browser-updated-b");
            page.locator("[name=rowStatus]").last().selectOption("INACTIVE");
            page.locator("#feature-save").click();
            assertThat(page.locator("tbody")).containsText("browser-updated-a");
            assertThat(page.locator("tbody")).containsText("browser-updated-b");
            page.locator("tbody a.btn").first().click();
            final String editUrl = page.url();
            final Page other = context.newPage();
            other.navigate(editUrl);
            other.locator("[name=rowName]").fill("browser-other-user");
            other.locator("#feature-save").click();
            page.locator("[name=rowName]").fill("browser-my-input");
            page.locator("#feature-save").click();
            assertThat(page.getByRole(AriaRole.ALERT)).containsText("他の操作で更新");
            assertThat(page.locator("[name=rowName]")).hasValue("browser-my-input");
            page.locator("#feature-refresh").click();
            assertThat(page.locator(".alert-info")).containsText("browser-other-user");
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/features-conflict.png"))
                .setFullPage(true));
            page.locator("#feature-confirm").click();
            assertThat(page.locator("[name=rowName]")).hasValue("browser-my-input");
            page.locator("#feature-save").click();
            assertThat(page.locator("tbody")).containsText("browser-my-input");
            page.screenshot(
                new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/features.png")).setFullPage(true));
            page.locator("#starter-dark-mode").check();
            assertThat(page.locator("#feature-bulk-new")).hasClass(java.util.regex.Pattern.compile(".*text-white.*"));
            page.screenshot(
                new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/features-dark.png")).setFullPage(true));
            page.setViewportSize(390, 844);
            assertThat(page.evaluate("document.documentElement.scrollWidth <= window.innerWidth")).isEqualTo(true);
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/features-mobile.png"))
                .setFullPage(true));
            page.setViewportSize(1280, 720);
            page.navigate(base + "/features?name=browser-&size=1&sort=NAME&descending=false");
            final String firstName = page.locator("tbody td a").first().innerText();
            page.locator("#feature-next").click();
            assertThat(page.locator("tbody td a").first()).not().hasText(firstName);
            page.locator("#feature-previous").click();
            assertThat(page.locator("tbody td a").first()).hasText(firstName);
            page.locator("[name=ids]").check();
            page.locator("#search-status").selectOption("INACTIVE");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("検索").setExact(true)).click();
            assertThat(page.locator("[name=ids]:checked")).hasCount(0);
            try (final BrowserContext reader = browser.newContext()) {
                reader.request().get(base + "/test-session");
                final Page readPage = reader.newPage();
                readPage.navigate(base + "/features/new");
                readPage.locator("[name=rowName]").fill("拒否する入力");
                readPage.locator("#feature-save").click();
                assertThat(readPage.locator("body")).containsText("操作する権限がありません。");
            }
            Mockito.doThrow(new IllegalStateException("ui-private-secret"))
                .when((FeatureCommands) AopTestUtils.getUltimateTargetObject(commands))
                .createMany(org.mockito.ArgumentMatchers.any());
            page.navigate(base + "/features/new");
            page.locator("[name=rowName]").fill("error-check");
            page.locator("#feature-save").click();
            assertThat(page.locator("body")).containsText("処理に失敗しました。");
            assertThat(output.getAll()).doesNotContain("ui-private-secret");
            assertThat(scriptErrors).isEmpty();
        }
    }

    /** 削除の取消・競合・最新内容の再確認と単件／複数の確定を実browserで検証する。 */
    @Test
    void deletionRequiresConfirmationAndRefreshAfterConflict() {
        try (final Playwright playwright = Playwright.create();
            final Browser browser = playwright.chromium().launch();
            final BrowserContext context = browser.newContext()) {
            final String base = "http://127.0.0.1:" + port;
            context.request().get(base + "/test-session?write=true");
            final Page page = context.newPage();
            page.navigate(base + "/features/new?rows=3");
            for (int index = 0; index < 3; index++) {
                page.locator("[name=rowName]").nth(index).fill("archive-ui-" + index);
            }
            page.locator("#feature-save").click();
            page.locator("#search-name").fill("archive-ui-");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("検索").setExact(true)).click();
            assertThat(page.locator("tbody tr")).hasCount(3);
            page.locator("tbody td a").first().click();
            page.locator("#feature-delete").click();
            page.locator("#feature-delete-cancel").click();
            assertThat(page.locator("tbody tr")).hasCount(3);
            page.locator("[name=ids]").nth(0).check();
            page.locator("[name=ids]").nth(1).check();
            page.locator("#feature-bulk-delete").click();
            assertThat(page.locator("tbody tr")).hasCount(2);
            final String id = page.locator("[name=rowId]").first().inputValue();
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/features-delete.png"))
                .setFullPage(true));
            final Page other = context.newPage();
            other.navigate(base + "/features/edit?ids=" + id);
            other.locator("[name=rowName]").fill("archive-ui-other");
            other.locator("#feature-save").click();
            page.locator("#feature-delete-confirm").click();
            assertThat(page.getByRole(AriaRole.ALERT)).containsText("この操作では、どの対象も削除していません。");
            assertThat(page.locator("tbody")).containsText("他の操作で更新");
            assertThat(page.locator("#feature-delete-confirm")).hasCount(0);
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(Path.of("build/reports/ui/features-delete-conflict.png")).setFullPage(true));
            page.locator("#feature-delete-refresh").click();
            assertThat(page.locator("tbody")).containsText("archive-ui-other");
            page.locator("#feature-delete-confirm").click();
            assertThat(page.locator("tbody tr")).hasCount(1);
            assertThat(page.locator("#search-name")).hasValue("archive-ui-");
            page.locator("tbody td a").first().click();
            page.locator("#feature-delete").click();
            page.locator("#feature-delete-confirm").click();
            assertThat(page.locator("tbody")).containsText("条件に一致するデータはありません。");
        }
    }

    /** 公開部品集の検索・ページング・詳細・入力・削除と明暗・狭幅を実ブラウザーで確認する。 */
    @Test
    void galleryUsesStandardComponentsAndLocalInteractions() {
        try (final Playwright playwright = Playwright.create();
            final Browser browser = playwright.chromium().launch();
            final BrowserContext context = browser.newContext()) {
            final Page page = context.newPage();
            context.request().get("http://127.0.0.1:" + port + "/test-session?write=true");
            page.navigate("http://127.0.0.1:" + port + "/");
            assertThat(page).hasTitle("ホーム | Spring Application Template");
            page.screenshot(
                new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/welcome.png")).setFullPage(true));
            page.locator("#welcome-gallery").click();
            assertThat(page.locator("#sample-count")).hasText("3件");
            assertThat(page.locator("#starter-gallery")).hasAttribute("aria-current", "page");
            assertThat(page.locator("#starter-navigation")).isHidden();
            page.locator("#starter-menu-toggle").click();
            assertThat(page.locator("#starter-navigation")).isVisible();
            assertThat(page.locator("#starter-navigation")).isFocused();
            page.keyboard().press("Tab");
            assertThat(page.locator("#starter-menu-close")).isFocused();
            page.waitForFunction("document.getElementById('starter-navigation').getBoundingClientRect().left === 0");
            assertThat(page.locator("#starter-navigation").boundingBox().x).isZero();
            assertThat(page.locator("#starter-navigation").boundingBox().y).isZero();
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/navigation-desktop.png")));
            page.keyboard().press("Escape");
            assertThat(page.locator("#starter-navigation")).isHidden();
            assertThat(page.locator("#starter-menu-toggle")).isFocused();
            assertThat(page.locator("#starter-navigation")).not().hasAttribute("aria-modal", "true");
            page.locator("#starter-menu-toggle").click();
            assertThat(page.locator("#starter-navigation")).isFocused();
            page.keyboard().press("Shift+Tab");
            assertThat(page.locator("#starter-gallery")).isFocused();
            page.keyboard().press("Tab");
            assertThat(page.locator("#starter-menu-close")).isFocused();
            page.locator(".offcanvas-backdrop").click(new Locator.ClickOptions().setPosition(900, 200));
            assertThat(page.locator("#starter-navigation")).isHidden();
            assertThat(page.locator("#starter-menu-toggle")).isFocused();
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
            assertThat(page.getByRole(AriaRole.TABLE, new Page.GetByRoleOptions().setName("検索結果のサンプル").setExact(true)))
                .isVisible();
            page.locator("#example-email").fill("invalid-email");
            page.locator("#sample-input-form button[type=submit]").click();
            assertThat(page.locator("#sample-input-status")).isEmpty();
            assertThat(page.locator("#example-email").evaluate("element => element.validity.typeMismatch"))
                .isEqualTo(true);
            page.locator("#example-email").fill("operations@example.com");
            page.locator("#sample-input-form button[type=submit]").click();
            assertThat(page.locator("#sample-input-status")).hasText("確認しました。");
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
            assertThat(page.locator("#starter-navigation")).hasAttribute("aria-modal", "true");
            assertThat(page.locator("#starter-navigation")).isFocused();
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/navigation-mobile.png")));
            page.locator("#starter-menu-close").click();
            assertThat(page.locator("#starter-navigation")).isHidden();
            assertThat(page.evaluate("document.documentElement.scrollWidth <= window.innerWidth")).isEqualTo(true);
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/components-mobile.png"))
                .setFullPage(true));
            page.reload();
            assertThat(page.locator("#sample-count")).hasText("3件");
        }
    }

    /** 未認証の各画面をloginへ誘導し、認証後はメニューから遷移してlogoutで戻る。 */
    @Test
    void loginHomeNavigationAndLogoutFormOneFlow() {
        try (final Playwright playwright = Playwright.create();
            final Browser browser = playwright.chromium().launch();
            final BrowserContext context = browser.newContext()) {
            final String base = "http://127.0.0.1:" + port;
            final Page page = context.newPage();
            for (final String path : List.of("/", "/components", "/features", "/features/new")) {
                page.navigate(base + path);
                assertThat(page).hasURL(base + "/login");
                assertThat(page.locator("#starter-navigation")).hasCount(0);
            }
            page.locator("#username").fill("test-user");
            page.locator("#password").fill("test-password-12345");
            page.locator("#login-submit").click();
            page.waitForURL(base + "/");
            assertThat(page.locator("#starter-navigation")).isHidden();
            page.navigate(base + "/login");
            page.waitForURL(base + "/");
            page.locator("#starter-menu-toggle").click();
            page.locator("#starter-navigation")
                .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("データ管理").setExact(true)).click();
            page.waitForURL(base + "/features");
            assertThat(page.locator("#starter-navigation")).isHidden();
            page.locator("#starter-account").click();
            page.waitForURL(base + "/login?logout");
            assertThat(page.getByRole(AriaRole.STATUS)).containsText("ログアウトしました。");
            page.navigate(base + "/features");
            assertThat(page).hasURL(base + "/login");
        }
    }

    /** 動作軽減とStorage拒否の環境でも、メニュー・focus・ページ内テーマが利用できる。 */
    @Test
    void reducedMotionAndBlockedStorageRemainUsable() {
        try (final Playwright playwright = Playwright.create();
            final Browser browser = playwright.chromium().launch();
            final BrowserContext context = browser
                .newContext(new Browser.NewContextOptions().setReducedMotion(ReducedMotion.REDUCE))) {
            final String base = "http://127.0.0.1:" + port;
            context.addInitScript(
                "Object.defineProperty(window, 'localStorage', {get() { throw new DOMException('blocked', 'SecurityError'); }});");
            context.request().get(base + "/test-session?write=true");
            final Page page = context.newPage();
            final List<String> scriptErrors = new ArrayList<>();
            page.onPageError(scriptErrors::add);
            page.navigate(base + "/");
            page.locator("#starter-menu-toggle").click();
            assertThat(page.locator("#starter-navigation")).isVisible();
            assertThat(page.locator("#starter-navigation").evaluate(
                "element => element.getAnimations().every(animation => animation.effect.getTiming().duration === 0)"))
                .isEqualTo(true);
            assertThat(
                page.locator(".offcanvas-backdrop").evaluate("element => getComputedStyle(element).transitionDuration"))
                .isEqualTo("0s");
            page.locator("#starter-menu-close").click();
            assertThat(page.locator("#starter-navigation")).isHidden();
            assertThat(page.locator("#starter-menu-toggle")).isFocused();
            page.locator("#starter-dark-mode").check();
            assertThat(page.locator("html")).hasAttribute("data-bs-theme", "dark");
            assertThat(scriptErrors).isEmpty();
        }
    }

    /** 予約文字を含む検索の遷移と、Unicode文字数の検証・400応答の通知を確認する。 */
    @Test
    void searchPreservesReservedCharactersAndReportsInvalidConditions() {
        try (final Playwright playwright = Playwright.create();
            final Browser browser = playwright.chromium().launch();
            final BrowserContext context = browser.newContext()) {
            final String base = "http://127.0.0.1:" + port;
            final String name = "review+C++ & A=B/部品";
            context.request().get(base + "/test-session?write=true");
            final Page page = context.newPage();
            page.navigate(base + "/features/new?rows=2&size=1");
            page.locator("[name=rowName]").nth(0).fill(name + "-one");
            page.locator("[name=rowName]").nth(1).fill(name + "-two");
            page.locator("#feature-save").click();
            page.locator("#search-name").fill(name);
            page.waitForResponse(
                response -> response.url().contains("/features?") && response.request().method().equals("GET"),
                () -> page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("検索").setExact(true))
                    .click());
            page.waitForFunction(
                "document.querySelectorAll('.htmx-request, .htmx-swapping, .htmx-settling').length === 0");
            final String first = page.locator("tbody td a").first().textContent();
            page.locator("tbody td a").first().click();
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("一覧へ戻る").setExact(true)).click();
            assertThat(page.locator("#search-name")).hasValue(name);
            assertThat(page.locator("tbody td a").first()).hasText(first);
            page.waitForResponse(response -> response.url().contains("cursor="),
                () -> page.locator("#feature-next").click());
            page.waitForFunction(
                "document.querySelectorAll('.htmx-request, .htmx-swapping, .htmx-settling').length === 0");
            assertThat(page.locator("#search-name")).hasValue(name);
            assertThat(page.locator("tbody td a").first()).not().hasText(first);
            page.locator("#search-name").fill("a".repeat(101));
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("検索").setExact(true)).click();
            assertThat(page.locator("#search-name").evaluate("element => element.validity.customError"))
                .isEqualTo(true);
            assertThat(page.locator("#ui-error")).isHidden();
            page.locator("#search-name").evaluate("element => element.setCustomValidity('')");
            final var rejected = page.waitForResponse(
                response -> response.url().contains("/features?") && response.request().method().equals("GET"),
                () -> page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("検索").setExact(true))
                    .click());
            assertThat(rejected.status()).isEqualTo(400);
            assertThat(page.locator("#ui-error")).isVisible();
            assertThat(page.locator("#ui-error")).containsText("検索条件または入力形式を確認してください。");
            page.locator("#search-name").fill("😀".repeat(100));
            final var accepted = page.waitForResponse(
                response -> response.url().contains("/features?") && response.request().method().equals("GET"),
                () -> page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("検索").setExact(true))
                    .click());
            assertThat(accepted.status()).isEqualTo(200);
            assertThat(page.locator("#ui-error")).isHidden();
            assertThat(page.locator("tbody")).containsText("条件に一致するデータはありません。");
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
            assertThat(page.getByRole(AriaRole.NAVIGATION, new Page.GetByRoleOptions().setName("ページ移動"))).isVisible();
            page.locator("#feature-new").click();
            page.locator("[name=rowName]").fill("通常フォーム,comma");
            page.locator("#feature-save").click();
            page.locator("#search-name").fill("通常フォーム,comma");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("検索").setExact(true)).click();
            assertThat(page.locator("tbody")).containsText("通常フォーム,comma");
            page.locator("tbody td a").first().click();
            assertThat(page.locator("dd").first()).hasText("通常フォーム,comma");
            page.locator("#feature-delete").click();
            page.locator("#feature-delete-confirm").click();
            assertThat(page.locator("tbody")).containsText("条件に一致するデータはありません。");
            assertThat(context.request().post(base + "/features/save").status()).isEqualTo(403);
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
