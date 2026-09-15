package dev.template.application.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.template.application.Application;
import dev.template.application.security.OperationsAuthority;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** 隔離DBで起動したapplicationからOpenAPIを取得し、公開契約と保護設定を検証する。 */
@Tag("documentation")
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(classes = Application.class, properties = {"spring.docker.compose.enabled=false",
    "springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true"})
class ApiDocumentationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(System.getProperty("test.postgres.image"));

    private final MockMvc mvc;

    /**
     * テスト対象のapplication依存を受け取る。
     *
     * @param mvc 検証に使用するMockMvc
     */
    @Autowired
    ApiDocumentationTest(final MockMvc mvc) {
        this.mvc = mvc;
    }

    /**
     * API資料を有効にし、運用権限による保護と公開schemaを検証してOpenAPI YAMLを出力する。
     *
     * @throws Exception HTTP操作またはテスト環境の処理に失敗した場合
     */
    @Test
    void exportsVersionedContractAndRequiresOperationsAuthority() throws Exception {
        mvc.perform(get("/v3/api-docs.yaml")).andExpect(status().isUnauthorized());
        mvc.perform(get("/v3/api-docs.yaml").with(user("user"))).andExpect(status().isForbidden());

        // UIの入口にも同じ運用権限を要求する。
        mvc.perform(get("/swagger-ui.html")).andExpect(status().isUnauthorized());
        mvc.perform(get("/swagger-ui.html").with(user("user"))).andExpect(status().isForbidden());
        mvc.perform(get("/swagger-ui.html").with(user("operator").authorities(OperationsAuthority.READ)))
            .andExpect(status().is3xxRedirection());

        // 公開契約全体を取得する。個別APIの仕様は各featureのtestで検証する。
        final String yaml = mvc
            .perform(get("/v3/api-docs.yaml").with(user("operator").authorities(OperationsAuthority.READ)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(yaml).contains("openapi:", "paths:").doesNotContain("/actuator", "/test-session");

        // 取得に成功した資料だけを出力する。
        final Path output = Path.of(System.getProperty("documentation.api.output"));
        Files.createDirectories(Objects.requireNonNull(output.getParent()));
        Files.writeString(output, yaml);
    }
}
