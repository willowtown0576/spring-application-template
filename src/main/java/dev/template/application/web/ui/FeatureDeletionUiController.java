package dev.template.application.web.ui;

import dev.template.application.common.Result;
import dev.template.application.feature.api.command.DeleteFeaturesParam;
import dev.template.application.feature.api.command.FeatureCommandFailure;
import dev.template.application.feature.api.command.FeatureCommands;
import dev.template.application.feature.api.command.FeatureDeletionTarget;
import dev.template.application.feature.api.model.FeatureStatus;
import dev.template.application.feature.api.query.FeatureQueries;
import dev.template.application.feature.api.query.FeatureSortField;
import dev.template.application.feature.api.query.FindFeatureByIdParam;
import dev.template.application.feature.api.query.SearchFeaturesParam;
import dev.template.application.web.ui.FeatureDeletionPage.Row;
import dev.template.application.web.ui.FeatureViews.Filter;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 対象名・件数の確認後にだけ履歴退避を伴う削除を実行するHTML adapter。 */
@Controller
class FeatureDeletionUiController {
    /** 履歴保存と削除の公開API。 */
    private final FeatureCommands commands;
    /** 現在値を確認する公開API。 */
    private final FeatureQueries queries;

    /**
     * 認可とtransactionを持つ公開APIを受け取る。
     * @param commands 更新API
     * @param queries 参照API
     */
    FeatureDeletionUiController(final FeatureCommands commands, final FeatureQueries queries) {
        this.commands = commands;
        this.queries = queries;
    }

    /**
     * 現在の内容を取得し、削除せずに確認画面を表示する。
     * @param values 選択したID
     * @param filter 戻り先条件
     * @param model 表示情報
     * @return 確認画面
     */
    @GetMapping("/features/delete")
    String confirm(@RequestParam final MultiValueMap<String, String> values,
        @ModelAttribute("filter") final Filter filter, final Model model) {
        final List<String> ids = values.getOrDefault("ids", List.of());
        if (ids.isEmpty() || ids.size() > 100) {
            throw new IllegalArgumentException("Select 1-100 targets");
        }
        final List<Row> rows = new ArrayList<>();
        final List<FeatureCommandFailure.Issue> issues = new ArrayList<>();
        for (int index = 0; index < ids.size(); index++) {
            final UUID id = UUID.fromString(ids.get(index));
            final var current = queries.findById(new FindFeatureByIdParam(id));
            rows.add(current.map(item -> new Row(item.id(), item.name(), item.version()))
                .orElseGet(() -> new Row(id, "対象が見つかりません", 0)));
            if (current.isEmpty()) {
                issues.add(new FeatureCommandFailure.Issue(index + 1, "id", FeatureCommandFailure.Reason.NOT_FOUND));
            }
        }
        model.addAttribute("deletion", new FeatureDeletionPage(filter, rows, issues));
        return "pages/feature/delete";
    }

    /**
     * 確認したversionで削除し、拒否時はその確認内容を保持する。
     * @param values 確認済み対象
     * @param filter 戻り先条件
     * @param principal 認証主体
     * @param model 表示情報
     * @param response HTTP応答
     * @param redirect 完了通知
     * @return 一覧へのredirectまたは確認画面
     */
    @PostMapping("/features/delete")
    String delete(@RequestParam final MultiValueMap<String, String> values,
        @ModelAttribute("filter") final Filter filter, final Principal principal, final Model model,
        final HttpServletResponse response, final RedirectAttributes redirect) {
        final List<String> ids = values.getOrDefault("rowId", List.of());
        final List<String> names = values.getOrDefault("rowName", List.of());
        final List<String> versions = values.getOrDefault("rowVersion", List.of());
        if (ids.size() > 100 || ids.size() != names.size() || ids.size() != versions.size()) {
            throw new IllegalArgumentException("Invalid targets");
        }
        final List<Row> rows = new ArrayList<>();
        for (int index = 0; index < ids.size(); index++) {
            rows.add(new Row(UUID.fromString(ids.get(index)), names.get(index), Long.parseLong(versions.get(index))));
        }
        final var result = commands.deleteMany(new DeleteFeaturesParam(principal.getName(),
            rows.stream().map(row -> new FeatureDeletionTarget(row.id(), row.version())).toList()));
        return switch (result) {
            case Result.Success(var deleted) -> {
                redirect.addFlashAttribute("notice", deleted.size() + "件を履歴へ保存して削除しました。");
                yield "redirect:" + filter.url("/features");
            }
            case Result.Failure(var failure) -> {
                response.setStatus(
                    failure.issues().stream().anyMatch(issue -> issue.reason() == FeatureCommandFailure.Reason.CONFLICT)
                        ? 409
                        : failure.issues().stream()
                            .anyMatch(issue -> issue.reason() == FeatureCommandFailure.Reason.NOT_FOUND) ? 404 : 422);
                model.addAttribute("deletion", new FeatureDeletionPage(filter, rows, failure.issues()));
                yield "pages/feature/delete";
            }
        };
    }

    /**
     * 一覧と同じ検索条件を検証し、確認画面・結果に引き継ぐ。
     * @param name 名前条件
     * @param status 状態条件
     * @param sort 並び項目
     * @param descending 降順か
     * @param size ページサイズ
     * @return 正規化した条件
     */
    @ModelAttribute("filter")
    Filter filter(@RequestParam(defaultValue = "") final String name,
        @RequestParam(required = false) final FeatureStatus status,
        @RequestParam(defaultValue = "UPDATED_AT") final FeatureSortField sort,
        @RequestParam(defaultValue = "true") final boolean descending,
        @RequestParam(defaultValue = "20") final int size) {
        final var conditions = new SearchFeaturesParam(name, status, sort, descending, size, null, false);
        return new Filter(conditions.name(), status == null ? "" : status.name(), sort, descending, size);
    }
}
