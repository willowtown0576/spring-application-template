package dev.template.application.web.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import dev.template.application.Application;
import dev.template.application.feature.api.command.FeatureCommands;
import dev.template.application.feature.api.query.FeatureQueries;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@org.junit.jupiter.api.extension.ExtendWith(
    org.springframework.boot.test.system.OutputCaptureExtension.class)
@Testcontainers
@SpringBootTest(
    classes = Application.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.docker.compose.enabled=false",
      "server.address=127.0.0.1",
      "vaadin.productionMode=true"
    })
@Import(FeatureUiIntegrationTest.TestSessions.class)
class FeatureUiIntegrationTest {
  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(System.getProperty("test.postgres.image"));

  @LocalServerPort private int port;

  @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
  private FeatureCommands commands;

  @Test
  void browserCreatesFindsValidatesAndEnforcesAuthorities(
      org.springframework.boot.test.system.CapturedOutput output) {
    try (Playwright playwright = Playwright.create();
        Browser browser = playwright.chromium().launch();
        BrowserContext context = browser.newContext()) {
      String base = "http://127.0.0.1:" + port;
      assertThat(context.request().get(base + "/features").status()).isEqualTo(401);
      context.request().get(base + "/test-session?write=true");
      Page page = context.newPage();
      page.navigate(base + "/features");
      page.locator("#feature-name input").fill("   ");
      page.locator("#feature-create").click();
      assertThat(page.locator("#feature-name")).hasAttribute("invalid", "");
      page.locator("#feature-name input").fill("画面から作成");
      page.locator("#feature-create").click();
      assertThat(page.locator("#feature-result")).hasText("作成しました。");
      String id = page.locator("#feature-id input").inputValue();
      page.locator("#feature-find").click();
      assertThat(page.locator("#feature-result")).hasText("名前: 画面から作成");
      page.locator("#feature-id input").fill("invalid");
      page.locator("#feature-find").click();
      assertThat(page.locator("#feature-id")).hasAttribute("invalid", "");
      page.locator("#feature-id input").fill("0199417c-0000-7000-8000-000000000099");
      page.locator("#feature-find").click();
      assertThat(page.locator("#feature-result")).hasText("見つかりませんでした。");
      org.mockito.Mockito.doThrow(new IllegalStateException("ui-private-secret"))
          .when(
              (FeatureCommands)
                  org.springframework.test.util.AopTestUtils.getUltimateTargetObject(commands))
          .create("error-check");
      page.locator("#feature-name input").fill("error-check");
      page.locator("#feature-create").click();
      assertThat(page.locator("vaadin-notification-card")).containsText("処理に失敗しました。");
      assertThat(output.getAll()).doesNotContain("ui-private-secret");
      page.screenshot(
          new Page.ScreenshotOptions().setPath(Path.of("build/reports/ui/features.png")));
      try (BrowserContext reader = browser.newContext()) {
        reader.request().get(base + "/test-session");
        Page readPage = reader.newPage();
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

  @TestConfiguration(proxyBeanMethods = false)
  static class TestSessions {
    @Bean
    @Order(0)
    SecurityFilterChain testSessionSecurity(HttpSecurity http) throws Exception {
      return http.securityMatcher("/test-session")
          .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
          .build();
    }

    @Bean
    ServletRegistrationBean<HttpServlet> testSessionServlet() {
      return new ServletRegistrationBean<>(
          new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response) {
              var authorities =
                  "true".equals(request.getParameter("write"))
                      ? List.of(
                          new SimpleGrantedAuthority(FeatureQueries.READ),
                          new SimpleGrantedAuthority(FeatureCommands.WRITE))
                      : List.of(new SimpleGrantedAuthority(FeatureQueries.READ));
              var authentication =
                  UsernamePasswordAuthenticationToken.authenticated("ui-test", null, authorities);
              request
                  .getSession()
                  .setAttribute(
                      HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                      new SecurityContextImpl(authentication));
              response.setStatus(204);
            }
          },
          "/test-session");
    }
  }
}
