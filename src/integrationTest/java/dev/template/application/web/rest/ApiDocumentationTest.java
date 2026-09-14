package dev.template.application.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.template.application.Application;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Tag("documentation")
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(
    classes = Application.class,
    properties = {
      "spring.docker.compose.enabled=false",
      "springdoc.api-docs.enabled=true",
      "springdoc.swagger-ui.enabled=true"
    })
class ApiDocumentationTest {
  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(System.getProperty("test.postgres.image"));

  @Autowired private MockMvc mvc;

  @Test
  void exportsVersionedContractAndRequiresOperationsAuthority() throws Exception {
    mvc.perform(get("/v3/api-docs.yaml")).andExpect(status().isUnauthorized());
    mvc.perform(get("/v3/api-docs.yaml").with(user("user"))).andExpect(status().isForbidden());
    mvc.perform(get("/swagger-ui.html")).andExpect(status().isUnauthorized());
    mvc.perform(get("/swagger-ui.html").with(user("user"))).andExpect(status().isForbidden());
    mvc.perform(
            get("/swagger-ui.html")
                .with(user("operator").authorities(new SimpleGrantedAuthority("ops:read"))))
        .andExpect(status().is3xxRedirection());
    mvc.perform(
            get("/v3/api-docs")
                .with(user("operator").authorities(new SimpleGrantedAuthority("ops:read"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths.length()").value(2))
        .andExpect(
            jsonPath(
                    "$.paths['/api/v1/features'].post.responses['201'].content['application/json'].schema['$ref']")
                .value("#/components/schemas/CreatedFeature"))
        .andExpect(
            jsonPath(
                    "$.paths['/api/v1/features'].post.responses['403'].content['application/problem+json'].schema['$ref']")
                .value("#/components/schemas/ProblemDetail"))
        .andExpect(
            jsonPath("$.components.schemas.CreateRequest.properties.name.maxLength").value(100));
    String yaml =
        mvc.perform(
                get("/v3/api-docs.yaml")
                    .with(user("operator").authorities(new SimpleGrantedAuthority("ops:read"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(yaml)
        .contains(
            "/api/v1/features", "CreatedFeature", "CreateRequest", "FeatureView", "422", "404")
        .doesNotContain("/actuator", "/test-session", "/features/{version}");
    Path output = Path.of(System.getProperty("documentation.api.output"));
    Files.createDirectories(java.util.Objects.requireNonNull(output.getParent()));
    Files.writeString(output, yaml);
  }
}
