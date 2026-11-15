package dev.template.application.web.rest;

import dev.template.application.common.Result;
import dev.template.application.feature.api.command.CreateFeatureParam;
import dev.template.application.feature.api.command.CreateFeaturesParam;
import dev.template.application.feature.api.command.DeleteFeatureParam;
import dev.template.application.feature.api.command.DeleteFeaturesParam;
import dev.template.application.feature.api.command.FeatureCommandFailure;
import dev.template.application.feature.api.command.FeatureCommands;
import dev.template.application.feature.api.command.FeatureDeletionTarget;
import dev.template.application.feature.api.command.UpdateFeatureParam;
import dev.template.application.feature.api.command.UpdateFeaturesParam;
import dev.template.application.feature.api.model.FeatureStatus;
import dev.template.application.feature.api.query.FeatureDetails;
import dev.template.application.feature.api.query.FeatureQueries;
import dev.template.application.feature.api.query.FeatureSortField;
import dev.template.application.feature.api.query.FindFeatureByIdParam;
import dev.template.application.web.FeatureSearchCursorCodec;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** HTTPとfeature公開APIのadapter。Paramを作り、業務FailureをHTTP statusへ変換する。 */
@RestController
@RequestMapping(path = "/api/{version}/features", version = "v1")
class FeatureController {
    /** 認可・transaction境界を持つFeatureの公開更新API。 */
    private final FeatureCommands commands;
    /** HTTPページ境界の変換。 */
    private final FeatureSearchCursorCodec cursors;
    /** 認可・read-only transaction境界を持つFeatureの公開参照API。 */
    private final FeatureQueries queries;

    /**
     * 公開APIだけを受け取る。
     *
     * @param commands 更新API
     * @param queries 参照API
     * @param cursors ページ境界の変換
     */
    FeatureController(final FeatureCommands commands, final FeatureQueries queries,
        final FeatureSearchCursorCodec cursors) {
        this.cursors = cursors;
        this.commands = commands;
        this.queries = queries;
    }

    /**
     * 公開APIの結果をHTTPへ写す。
     * @param result 実行結果
     * @return 成功またはProblemDetail
     */
    private static ResponseEntity<?> response(final Result<?, FeatureCommandFailure> result) {
        return switch (result) {
            case Result.Success(var value) -> ResponseEntity.ok(value);
            case Result.Failure(var failure) -> {
                final HttpStatus status = failure.issues().stream()
                    .anyMatch(issue -> issue.reason() == FeatureCommandFailure.Reason.CONFLICT
                        || issue.reason() == FeatureCommandFailure.Reason.REPLAY_MISMATCH)
                            ? HttpStatus.CONFLICT
                            : failure.issues().stream()
                                .anyMatch(issue -> issue.reason() == FeatureCommandFailure.Reason.NOT_FOUND)
                                    ? HttpStatus.NOT_FOUND
                                    : HttpStatus.UNPROCESSABLE_CONTENT;
                final var problem = ProblemDetail.forStatusAndDetail(status, "Feature operation was rejected.");
                problem.setProperty("issues", failure.issues());
                yield ResponseEntity.status(status).body(problem);
            }
        };
    }

    /**
     * Featureを作成しLocation付き201、または業務検証エラー422を返す。
     *
     * @param request HTTP入力
     * @return 作成IDまたはProblemDetail
     */
    @PostMapping
    @ApiResponse(responseCode = "201", description = "Created", headers = @Header(name = "Location", schema = @Schema(type = "string", format = "uri-reference")), content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreateFeatureResponse.class)))
    @ApiResponse(responseCode = "422", description = "Invalid feature name", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<?> create(final @Valid @RequestBody CreateFeatureRequest request) {
        return switch (commands.create(new CreateFeatureParam(request.name(),
            request.status() == null ? FeatureStatus.ACTIVE : request.status()))) {
            case Result.Success<UUID, FeatureCommandFailure>(var id) -> ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}").buildAndExpand(id).toUri())
                .body(new CreateFeatureResponse(id));
            case Result.Failure<UUID, FeatureCommandFailure>(var failure) -> response(new Result.Failure<>(failure));
        };
    }

    /**
     * 複数行を同じtransactionで登録する。実行主体は認証から取得する。
     * @param request 操作キーと登録行
     * @param principal 認証主体
     * @return 入力順のIDまたは行エラー
     */
    @ApiResponse(responseCode = "200", description = "Created or replayed IDs in input order", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(type = "string", format = "uuid"))))
    @ApiResponse(responseCode = "409", description = "Operation key reused with different content", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Invalid rows", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/bulk")
    ResponseEntity<?> createMany(@RequestBody final CreateFeaturesRequest request, final Principal principal) {
        return response(
            commands.createMany(new CreateFeaturesParam(principal.getName(), request.requestId(), request.rows())));
    }

    /**
     * 一件を履歴へ退避して削除する。
     * @param id 対象ID
     * @param version 確認時のversion
     * @param principal 認証主体
     * @return 対象IDまたは業務拒否
     */
    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "200", description = "Archived and deleted ID", content = @Content(mediaType = "application/json", schema = @Schema(type = "string", format = "uuid")))
    @ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Stale version", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Invalid target", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<?> delete(@PathVariable final UUID id, @RequestParam final long version, final Principal principal) {
        return response(
            commands.delete(new DeleteFeatureParam(principal.getName(), new FeatureDeletionTarget(id, version))));
    }

    /**
     * 複数対象を同じtransactionで履歴へ退避して削除する。
     * @param request 確認済み対象
     * @param principal 認証主体
     * @return 入力順の削除IDまたは業務拒否
     */
    @PostMapping("/bulk-delete")
    @ApiResponse(responseCode = "200", description = "Archived and deleted IDs", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(type = "string", format = "uuid"))))
    @ApiResponse(responseCode = "404", description = "Missing target; no rows deleted", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Stale version; no rows deleted", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Invalid targets", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<?> deleteMany(@RequestBody final DeleteFeaturesRequest request, final Principal principal) {
        return response(commands.deleteMany(new DeleteFeaturesParam(principal.getName(), request.targets())));
    }

    /**
     * ID検索を行い、未登録の場合だけ404へ変換する。
     *
     * @param id pathから変換されたUUID
     * @return 画面非依存のQuery結果
     */
    @ApiResponse(responseCode = "200", description = "Found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FeatureDetails.class)))
    @ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    FeatureDetails findById(final @PathVariable UUID id) {
        return queries.findById(new FindFeatureByIdParam(id))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feature not found."));
    }

    /**
     * 条件検索の結果と前後ページ境界を返す。
     * @param name 名前の部分一致
     * @param status 状態条件
     * @param sort ソート対象
     * @param descending 降順か
     * @param size ページサイズ
     * @param cursor ページ境界
     * @param previous 前ページか
     * @return 検索結果
     */
    @ApiResponse(responseCode = "200", description = "Search page", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SearchFeaturesResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid conditions or cursor", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping
    SearchFeaturesResponse search(@RequestParam(defaultValue = "") final String name,
        @RequestParam(required = false) final FeatureStatus status,
        @RequestParam(defaultValue = "UPDATED_AT") final FeatureSortField sort,
        @RequestParam(defaultValue = "true") final boolean descending,
        @RequestParam(defaultValue = "20") final int size, @RequestParam(required = false) final String cursor,
        @RequestParam(defaultValue = "false") final boolean previous) {
        final var param = cursors.search(name, status, sort, descending, size, cursor, previous);
        final var result = queries.search(param);
        return new SearchFeaturesResponse(result.items(), cursors.encode(result.previous(), param),
            cursors.encode(result.next(), param));
    }

    /**
     * 一件を期待version付きで更新する。
     * @param id 更新対象
     * @param request 入力
     * @return 更新IDまたは行エラー
     */
    @ApiResponse(responseCode = "200", description = "Updated ID", content = @Content(mediaType = "application/json", schema = @Schema(type = "string", format = "uuid")))
    @ApiResponse(responseCode = "409", description = "Stale version", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Invalid row", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/{id}")
    ResponseEntity<?> update(@PathVariable final UUID id, @RequestBody final UpdateFeatureRequest request) {
        return response(
            commands.update(new UpdateFeatureParam(id, request.name(), request.status(), request.version())));
    }

    /**
     * 複数行を一括更新する。一行でも競合した場合は変更しない。
     * @param rows 入力順の更新行
     * @return 変更したIDまたは行エラー
     */
    @ApiResponse(responseCode = "200", description = "Changed IDs", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(type = "string", format = "uuid"))))
    @ApiResponse(responseCode = "409", description = "Stale version; no rows changed", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Missing row; no rows changed", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Invalid rows", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/bulk")
    ResponseEntity<?> updateMany(@RequestBody final List<UpdateFeatureParam> rows) {
        return response(commands.updateMany(new UpdateFeaturesParam(rows)));
    }

    /**
     * HTTP入力モデル。業務名の厳密な制約はCommand/domainで検証する。
     *
     * @param name 入力された名前
     * @param status 状態。省略時は有効
     */
    record CreateFeatureRequest(
        @NotNull @Schema(minLength = 1, maxLength = 100, description = "Unicode code points; blank, NUL and malformed surrogates are rejected") String name,
        FeatureStatus status) {
    }

    /**
     * HTTP作成応答。
     *
     * @param id 作成したFeatureの識別子
     */
    record CreateFeatureResponse(UUID id) {
    }

    /**
     * 複数登録入力。
     * @param requestId 操作キー
     * @param rows 登録行
     */
    record CreateFeaturesRequest(UUID requestId, List<CreateFeatureParam> rows) {
    }

    /**
     * 複数削除入力。実行主体は認証から取得する。
     * @param targets 確認済みのIDとversion
     */
    record DeleteFeaturesRequest(List<FeatureDeletionTarget> targets) {
    }

    /**
     * 検索応答。
     * @param items 取得行
     * @param previous 前ページtoken
     * @param next 次ページtoken
     */
    record SearchFeaturesResponse(List<FeatureDetails> items, String previous, String next) {
    }

    /**
     * 更新入力。
     * @param name 名前
     * @param status 状態
     * @param version 期待version
     */
    record UpdateFeatureRequest(String name, FeatureStatus status, long version) {
    }
}
