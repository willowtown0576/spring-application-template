package dev.template.application.web.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.template.application.Application;
import dev.template.application.security.OperationsAuthority;
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

/** Feature sampleの公開schemaを検証する。sample変更時に契約と一緒に更新する。 */
@Tag("documentation")
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(classes = Application.class, properties = {"spring.docker.compose.enabled=false",
    "springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true"})
class FeatureApiDocumentationTest {
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
    FeatureApiDocumentationTest(final MockMvc mvc) {
        this.mvc = mvc;
    }

    /**
     * sampleの作成・検索契約がOpenAPIに反映されることを確認する。
     * @throws Exception HTTP操作に失敗した場合
     */
    @Test
    void describesFeatureContract() throws Exception {
        mvc.perform(get("/v3/api-docs").with(user("operator").authorities(OperationsAuthority.READ)))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.paths['/api/v1/features'].post.responses['201'].content['application/json'].schema['$ref']")
                    .value("#/components/schemas/CreatedFeature"))
            .andExpect(jsonPath(
                "$.paths['/api/v1/features'].post.responses['403'].content['application/problem+json'].schema['$ref']")
                .value("#/components/schemas/ProblemDetail"))
            .andExpect(jsonPath("$.components.schemas.CreateRequest.properties.name.maxLength").value(100));
    }
}
