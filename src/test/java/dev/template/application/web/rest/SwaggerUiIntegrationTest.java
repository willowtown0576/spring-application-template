package dev.template.application.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;
import dev.template.application.Application;
import java.net.URI;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** context path配下でフォーム認証・Swagger・RESTのCSRF連携を実browserで検証する。 */
@Tag("integration")
@Testcontainers
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.docker.compose.enabled=false", "server.address=127.0.0.1", "vaadin.productionMode=true",
    "server.servlet.context-path=/app", "springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true",
    "starter.security.user.operations-read=true"})
class SwaggerUiIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(System.getProperty("test.postgres.image"));

    private final int port;

    /**
     * 隔離serverのportを受け取る。
     * @param port 起動したHTTP port
     */
    @Autowired
    SwaggerUiIntegrationTest(final @LocalServerPort int port) {
        this.port = port;
    }

    /** login後のtokenでSwaggerから作成・検索でき、tokenなし・不正token・logout後の操作を拒否する。 */
    @Test
    void swaggerUsesCsrfAndPreservesContextPath() {
        try (final var playwright = Playwright.create();
            final var browser = playwright.chromium().launch();
            final var context = browser.newContext()) {
            final var page = context.newPage();
            final String base = "http://127.0.0.1:" + port + "/app";

            // 実フォームで認証し、認証前後のCSRF tokenを比較する。
            page.navigate(base + "/login");
            final String beforeLogin = context.cookies().stream().filter(cookie -> cookie.name.equals("XSRF-TOKEN"))
                .findFirst().orElseThrow().value;
            page.locator("input[name=username]").fill("test-user");
            page.locator("input[name=password]").fill("test-password-12345");
            page.locator("vaadin-button[theme~=submit]").click();
            page.waitForURL(base + "/");
            page.navigate(base + "/swagger-ui/index.html");
            final String token = context.cookies().stream().filter(cookie -> cookie.name.equals("XSRF-TOKEN"))
                .findFirst().orElseThrow().value;
            assertThat(token).isNotEqualTo(beforeLogin);

            // Swaggerの標準操作で作成し、context path付きLocationから取得する。
            page.locator(".opblock-post .opblock-summary").click();
            page.locator(".opblock-post .try-out__btn").click();
            page.locator(".opblock-post textarea").fill("{\"name\":\"Swagger sample\"}");
            final var response = page.waitForResponse(
                candidate -> candidate.url().endsWith("/api/v1/features")
                    && candidate.request().method().equals("POST"),
                () -> page.locator(".opblock-post button.execute").click());
            assertThat(response.status()).isEqualTo(201);
            assertThat(response.url()).isEqualTo(base + "/api/v1/features");
            final String location = response.headerValue("Location");
            assertThat(URI.create(location).getPath()).startsWith("/app/api/v1/features/");
            assertThat(context.request().get(location).status()).isEqualTo(200);

            // sessionだけでは更新できず、CSRF headerが必要であることを確認する。
            assertThat(
                context.request()
                    .post(base + "/api/v1/features", RequestOptions.create()
                        .setHeader("Content-Type", "application/json").setData("{\"name\":\"Rejected\"}"))
                    .status())
                .isEqualTo(403);
            assertThat(context.request()
                .post(base + "/api/v1/features",
                    RequestOptions.create().setHeader("Content-Type", "application/json")
                        .setHeader("X-XSRF-TOKEN", "invalid").setData("{\"name\":\"Rejected\"}"))
                .status()).isEqualTo(403);

            // logout後は古いtokenを使っても業務操作へアクセスできない。
            assertThat(context.request()
                .post(base + "/logout", RequestOptions.create().setHeader("X-XSRF-TOKEN", token).setMaxRedirects(0))
                .status()).isBetween(300, 399);
            assertThat(context.request().get(location).status()).isEqualTo(401);
            assertThat(
                context.request()
                    .post(base + "/api/v1/features",
                        RequestOptions.create().setHeader("Content-Type", "application/json")
                            .setHeader("X-XSRF-TOKEN", token).setData("{\"name\":\"Rejected\"}"))
                    .status())
                .isEqualTo(401);
        }
    }
}
