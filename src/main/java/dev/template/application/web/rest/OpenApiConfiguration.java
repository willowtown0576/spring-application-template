package dev.template.application.web.rest;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
class OpenApiConfiguration {
  @Bean
  OpenAPI applicationOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Feature API")
                .version("1")
                .description(
                    "Authentication is supplied by the project. Create requires feature:write and a valid CSRF token; find requires feature:read. Errors use ProblemDetail (400, 401, 403, 500)."))
        .servers(List.of(new Server().url("/")));
  }

  @Bean
  OpenApiCustomizer commonErrors() {
    return api ->
        api.getPaths()
            .values()
            .forEach(
                path ->
                    path.readOperations()
                        .forEach(
                            operation ->
                                List.of(
                                        HttpStatus.BAD_REQUEST,
                                        HttpStatus.UNAUTHORIZED,
                                        HttpStatus.FORBIDDEN,
                                        HttpStatus.INTERNAL_SERVER_ERROR)
                                    .forEach(
                                        status ->
                                            operation
                                                .getResponses()
                                                .addApiResponse(
                                                    Integer.toString(status.value()),
                                                    new ApiResponse()
                                                        .description(status.getReasonPhrase())
                                                        .content(
                                                            new Content()
                                                                .addMediaType(
                                                                    "application/problem+json",
                                                                    new MediaType()
                                                                        .schema(
                                                                            new Schema<>()
                                                                                .$ref(
                                                                                    "#/components/schemas/ProblemDetail"))))))));
  }
}
