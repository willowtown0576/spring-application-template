package dev.template.application.web.rest;

import dev.template.application.common.Result;
import dev.template.application.feature.api.command.CreateFailure;
import dev.template.application.feature.api.command.CreateFeatureParam;
import dev.template.application.feature.api.command.FeatureCommands;
import dev.template.application.feature.api.query.FeatureQueries;
import dev.template.application.feature.api.query.FeatureResult;
import dev.template.application.feature.api.query.FindFeatureParam;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** HTTPとfeature公開APIのadapter。Paramを作り、業務FailureをHTTP statusへ変換する。 */
@RestController
@RequestMapping(path = "/api/{version}/features", version = "v1")
class FeatureController {
    private final FeatureCommands commands;
    private final FeatureQueries queries;

    /**
     * 公開APIだけを受け取る。
     *
     * @param commands 更新API
     * @param queries 参照API
     */
    FeatureController(final FeatureCommands commands, final FeatureQueries queries) {
        this.commands = commands;
        this.queries = queries;
    }

    /**
     * Featureを作成しLocation付き201、または業務検証エラー422を返す。
     *
     * @param request HTTP入力
     * @return 作成IDまたはProblemDetail
     */
    @PostMapping
    @ApiResponse(responseCode = "201", description = "Created", headers = @Header(name = "Location", schema = @Schema(type = "string", format = "uri-reference")), content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreatedFeature.class)))
    @ApiResponse(responseCode = "422", description = "Invalid feature name", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<?> create(final @Valid @RequestBody CreateRequest request) {
        return switch (commands.create(new CreateFeatureParam(request.name()))) {
            case Result.Success<UUID, CreateFailure>(var id) -> ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}").buildAndExpand(id).toUri())
                .body(new CreatedFeature(id));
            case Result.Failure<UUID, CreateFailure>(var reason) -> switch (reason) {
                case INVALID_NAME -> ResponseEntity.unprocessableContent().body(
                    ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, "Feature name is invalid."));
            };
        };
    }

    /**
     * ID検索を行い、未登録の場合だけ404へ変換する。
     *
     * @param id pathから変換されたUUID
     * @return 画面非依存のQuery結果
     */
    @ApiResponse(responseCode = "200", description = "Found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FeatureResult.class)))
    @ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    FeatureResult find(final @PathVariable UUID id) {
        return queries.find(new FindFeatureParam(id))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feature not found."));
    }

    /**
     * HTTP入力モデル。業務名の厳密な制約はCommand/domainで検証する。
     *
     * @param name 入力された名前
     */
    record CreateRequest(
        @NotNull @Schema(minLength = 1, maxLength = 100, description = "Unicode code points; blank, NUL and malformed surrogates are rejected") String name) {
    }

    /**
     * HTTP作成応答。
     *
     * @param id 作成したFeatureの識別子
     */
    record CreatedFeature(UUID id) {
    }
}
