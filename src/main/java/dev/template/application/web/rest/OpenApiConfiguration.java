package dev.template.application.web.rest;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import java.util.Map;
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
        final Components components = new Components().schemas(ModelConverters.getInstance().read(ProblemDetail.class));
        return new OpenAPI()
            .info(new Info().title(environment.getRequiredProperty("spring.application.name")).version("1")
                .description("""
                    業務データの登録・条件検索・更新・削除API。参照はfeature:read、登録・更新・削除はfeature:writeが必要です。

                    認証: GET /loginで画面とCSRF情報を取得し、POST /loginへusername/passwordをフォーム送信します。
                    フォームの_csrf、またはXSRF-TOKEN cookieと同じ値のX-XSRF-TOKEN headerを付けてログインしてください。
                    認証後はJSESSIONIDと最新のXSRF-TOKEN cookieを保持します。登録・更新・削除にはXSRF-TOKENと一致するX-XSRF-TOKEN headerも必要です。
                    ログイン前のCSRF tokenを使い続けないでください。GETにはCSRF headerは不要。POST /logoutでsessionを無効化します。
                    Swagger UIも同じブラウザーのログインsessionを使います。API利用者名・passwordは実行環境の認証設定に従います。

                    例は架空の設備保守契約です。例のIDが稼働DBに存在することは保証しません。ID・version・cursorは実際の応答から取得します。
                    名前の同一性は一意制約ではありません。登録はversion=1、実変更だけversionが1増加し、更新日時が変わります。
                    日時はUTCのISO 8601形式。HTTP 200/201は成功、400は形式不正、401は未認証、403は権限またはCSRF不足、
                    404は未検出、409は競合・再送内容不一致、422は業務入力不正、500は技術障害です。
                    エラーはapplication/problem+json。業務拒否にはissues（行番号・項目・理由）が付き、内部障害の詳細は公開しません。
                    """))
            .components(components
                .addSecuritySchemes("SessionCookie",
                    new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE)
                        .name("JSESSIONID").description("/loginで確立した認証sessionのcookie。"))
                .addSecuritySchemes("CsrfCookie",
                    new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE)
                        .name("XSRF-TOKEN").description("認証後の最新のCSRF cookie。書き込み時はheaderと対で送信する。"))
                .addSecuritySchemes("CsrfHeader",
                    new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER)
                        .name("X-XSRF-TOKEN").description("最新のXSRF-TOKEN cookieと同じ値。書き込み時に必須。")))
            .addSecurityItem(new SecurityRequirement().addList("SessionCookie"));
    }

    /**
     * filterや共通handlerが返すエラー応答を全操作へ記載する。
     *
     * @return 共通エラー仕様のcustomizer
     */
    @Bean
    OpenApiCustomizer commonErrors() {
        return api -> {
            final Schema<?> problem = api.getComponents().getSchemas().get("ProblemDetail");
            // 抽出後に補完し、Springがトップレベルへ展開する拡張項目の階層と一致させる。
            problem.getProperties().remove("properties");
            problem.setAdditionalProperties(true);
            problem.getProperties().get("instance").setFormat("uri-reference");
            api.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                // 個別APIの説明・schemaがある場合は、その契約を優先する。
                for (final HttpStatus status : List.of(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED,
                    HttpStatus.FORBIDDEN, HttpStatus.INTERNAL_SERVER_ERROR)) {
                    operation.getResponses().putIfAbsent(Integer.toString(status.value()),
                        new ApiResponse().description(switch (status) {
                            case BAD_REQUEST -> "JSON・UUID・enum・検索条件等の形式不正。入力を修正して再送。";
                            case UNAUTHORIZED -> "未認証またはsession失効。ログイン後に再送。";
                            case FORBIDDEN -> "必要な権限、または有効なCSRF cookie/headerが不足。";
                            default -> "予期しない技術障害。更新操作はrollbackされ、詳細は公開しない。";
                        }).content(new Content().addMediaType("application/problem+json",
                            new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))
                                .addExamples("error",
                                    new Example().summary(status.getReasonPhrase())
                                        .value(Map.of("type", "about:blank", "title", status.getReasonPhrase(),
                                            "status", status.value(), "detail", switch (status) {
                                                case UNAUTHORIZED -> "Authentication is required.";
                                                case FORBIDDEN -> "Access is denied.";
                                                case BAD_REQUEST -> "Failed to read request";
                                                default -> "An unexpected error occurred.";
                                            }))))));
                }
            }));
        };
    }
}
