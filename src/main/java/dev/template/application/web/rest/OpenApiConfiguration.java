package dev.template.application.web.rest;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/** 有効化された環境だけでOpenAPIと共通エラー仕様を公開する。 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
class OpenApiConfiguration {
    /**
     * APIの基本情報を設定し、server URLはspringdocのrequestに基づく生成に委ねる。
     *
     * @param environment application名を持つ起動設定
     * @return OpenAPI基本モデル
     */
    @Bean
    OpenAPI applicationOpenApi(final Environment environment) {
        return new OpenAPI()
            .info(new Info().title(environment.getRequiredProperty("spring.application.name")).version("1").description(
                "Form login at /login establishes a session; logout invalidates it. State-changing requests require a valid CSRF token. Each operation requires its declared authorities. Errors use ProblemDetail (400, 401, 403, 500)."))
            .components(new Components().schemas(ModelConverters.getInstance().read(ProblemDetail.class)));
    }

    /**
     * filterや共通handlerが返すエラー応答を全操作へ記載する。
     *
     * @return 共通エラー仕様のcustomizer
     */
    @Bean
    OpenApiCustomizer commonErrors() {
        return api -> api.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
            // 個別APIの説明・schemaがある場合は、その契約を優先する。
            for (final HttpStatus status : List.of(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED,
                HttpStatus.FORBIDDEN, HttpStatus.INTERNAL_SERVER_ERROR)) {
                operation.getResponses().putIfAbsent(Integer.toString(status.value()),
                    new ApiResponse().description(status.getReasonPhrase())
                        .content(new Content().addMediaType("application/problem+json",
                            new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail")))));
            }
        }));
    }
}
