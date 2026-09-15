package dev.template.application.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/** 共通OpenAPI schemaと適用条件を検証する。 */
class OpenApiConfigurationTest {
    /** 空の定義への共通schema登録と、個別APIの応答定義の優先を検証する。 */
    @Test
    void preservesOperationResponsesAndProvidesIndependentErrorSchema() {
        final var configuration = new OpenApiConfiguration();
        final var api = configuration
            .applicationOpenApi(new MockEnvironment().withProperty("spring.application.name", "project-app"));
        final var rejection = new ApiResponse().description("Operation-specific validation");
        final var operation = new Operation().responses(new ApiResponses().addApiResponse("400", rejection));
        api.setPaths(new Paths().addPathItem("/api/v1/new-resources", new PathItem().post(operation)));

        configuration.commonErrors().customise(api);

        assertThat(api.getInfo().getTitle()).isEqualTo("project-app");
        assertThat(api.getComponents().getSchemas()).containsKey("ProblemDetail");
        assertThat(operation.getResponses().get("400")).isSameAs(rejection);
        assertThat(
            operation.getResponses().get("500").getContent().get("application/problem+json").getSchema().get$ref())
            .isEqualTo("#/components/schemas/ProblemDetail");
    }
}
