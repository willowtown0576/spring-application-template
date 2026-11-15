package dev.template.application.web.ui;

import dev.template.application.common.Result;
import dev.template.application.feature.api.command.CreateFeatureParam;
import dev.template.application.feature.api.command.CreateFeaturesParam;
import dev.template.application.feature.api.command.FeatureCommandFailure;
import dev.template.application.feature.api.command.FeatureCommands;
import dev.template.application.feature.api.command.UpdateFeatureParam;
import dev.template.application.feature.api.command.UpdateFeaturesParam;
import dev.template.application.feature.api.model.FeatureStatus;
import dev.template.application.feature.api.query.FeatureQueries;
import dev.template.application.feature.api.query.FeatureSortField;
import dev.template.application.feature.api.query.FindFeatureByIdParam;
import dev.template.application.web.FeatureSearchCursorCodec;
import dev.template.application.web.ui.FeatureViews.Editor;
import dev.template.application.web.ui.FeatureViews.Filter;
import dev.template.application.web.ui.FeatureViews.Listing;
import dev.template.application.web.ui.FeatureViews.Row;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 条件検索・詳細・入力フォームを公開APIにつなぐHTML adapter。 */
@Controller
class FeatureUiController {
    /** 更新API。 */
    private final FeatureCommands commands;
    /** HTTPページ境界。 */
    private final FeatureSearchCursorCodec cursors;
    /** 参照API。 */
    private final FeatureQueries queries;

    /**
     * 公開APIとページ境界を受け取る。
     * @param commands 更新API
     * @param queries 参照API
     * @param cursors ページ境界
     */
    FeatureUiController(final FeatureCommands commands, final FeatureQueries queries,
        final FeatureSearchCursorCodec cursors) {
        this.commands = commands;
        this.queries = queries;
        this.cursors = cursors;
    }

    /**
     * 繰り返し項目を行に変換する。名前に含まれるカンマを分割しない。
     * @param values 入力
     * @return 入力モデル
     */
    private static Editor editor(final MultiValueMap<String, String> values) {
        final List<String> names = values.getOrDefault("rowName", List.of());
        final List<String> statuses = values.getOrDefault("rowStatus", List.of());
        final List<String> ids = values.getOrDefault("rowId", List.of());
        final List<String> versions = values.getOrDefault("rowVersion", List.of());
        final boolean creating = Boolean.parseBoolean(values.getFirst("creating"));
        if (names.size() > 100 || names.size() != statuses.size()
            || (!creating && (ids.size() != names.size() || versions.size() != names.size()))) {
            throw new IllegalArgumentException("Invalid rows");
        }
        final List<Row> rows = new ArrayList<>();
        for (int index = 0; index < names.size(); index++) {
            rows.add(new Row(creating ? null : UUID.fromString(ids.get(index)), names.get(index), statuses.get(index),
                creating ? 1 : Long.parseLong(versions.get(index)), null));
        }
        return new Editor(creating, UUID.fromString(value(values, "requestId", "")), filter(values), rows, List.of(),
            false, true);
    }

    /**
     * 検索条件を復元する。
     * @param values 入力
     * @return 検索条件
     */
    private static Filter filter(final MultiValueMap<String, String> values) {
        final String status = value(values, "status", "");
        if (!status.isEmpty() && status(status) == null) {
            throw new IllegalArgumentException("Invalid status");
        }
        return new Filter(value(values, "name", "").strip(), status,
            FeatureSortField.valueOf(value(values, "sort", "UPDATED_AT")),
            Boolean.parseBoolean(value(values, "descending", "true")), Integer.parseInt(value(values, "size", "20")));
    }

    /**
     * 状態文字列を公開APIの型へ写す。不正値は業務検証へ渡す。
     * @param value 状態
     * @return 状態。不正値と空はnull
     */
    private static FeatureStatus status(final String value) {
        try {
            return value == null || value.isEmpty() ? null : FeatureStatus.valueOf(value);
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    /**
     * 単値項目を既定値付きで読む。
     * @param values 入力
     * @param name 項目名
     * @param fallback 既定値
     * @return 値
     */
    private static String value(final MultiValueMap<String, String> values, final String name, final String fallback) {
        final String found = values.getFirst(name);
        return found == null ? fallback : found;
    }

    /**
     * 利用者が確認した最新versionで再編集する。DBは変更しない。
     * @param values 編集入力と確認済みversion
     * @param model 表示情報
     * @return 編集画面
     */
    @PostMapping("/features/confirm")
    String confirm(@RequestParam final MultiValueMap<String, String> values, final Model model) {
        final Editor editor = editor(values);
        final List<String> versions = values.getOrDefault("latestVersion", List.of());
        if (editor.creating() || versions.size() != editor.rows().size()) {
            throw new IllegalArgumentException("Latest versions required");
        }
        final List<Row> rows = new ArrayList<>();
        for (int index = 0; index < versions.size(); index++) {
            final Row row = editor.rows().get(index);
            rows.add(new Row(row.id(), row.name(), row.status(), Long.parseLong(versions.get(index)), null));
        }
        model.addAttribute("editor",
            new Editor(false, editor.requestId(), editor.filter(), rows, List.of(), false, true));
        return "pages/feature/editor";
    }

    /**
     * 登録フォームを開く。
     * @param values 戻り先条件と初期行数
     * @param model 表示情報
     * @return 編集画面
     */
    @GetMapping("/features/new")
    String create(@RequestParam final MultiValueMap<String, String> values, final Model model) {
        final int count = Integer.parseInt(value(values, "rows", "1"));
        if (count < 1 || count > 100) {
            throw new IllegalArgumentException("Invalid row count");
        }
        final List<Row> rows = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            rows.add(new Row(null, "", "ACTIVE", 1, null));
        }
        model.addAttribute("editor",
            new Editor(true, UUID.randomUUID(), filter(values), rows, List.of(), false, false));
        return "pages/feature/editor";
    }

    /**
     * 詳細を表示する。
     * @param values 対象と戻り先条件
     * @param model 表示情報
     * @return 詳細画面
     */
    @GetMapping("/features/detail")
    String detail(@RequestParam final MultiValueMap<String, String> values, final Model model) {
        model.addAttribute("item", queries.findById(new FindFeatureByIdParam(UUID.fromString(value(values, "id", ""))))
            .orElseThrow(() -> new IllegalArgumentException("Feature missing")));
        model.addAttribute("filter", filter(values));
        return "pages/feature/detail";
    }

    /**
     * 選択行の編集フォームを開く。
     * @param values 選択IDと戻り先条件
     * @param model 表示情報
     * @return 編集画面
     */
    @GetMapping("/features/edit")
    String edit(@RequestParam final MultiValueMap<String, String> values, final Model model) {
        final List<String> ids = values.getOrDefault("ids", List.of());
        if (ids.isEmpty() || ids.size() > 100) {
            throw new IllegalArgumentException("Select rows");
        }
        final List<Row> rows = ids.stream().map(UUID::fromString)
            .map(id -> queries.findById(new FindFeatureByIdParam(id))
                .orElseThrow(() -> new IllegalArgumentException("Feature missing")))
            .map(item -> new Row(item.id(), item.name(), item.status().name(), item.version(), null)).toList();
        model.addAttribute("editor",
            new Editor(false, UUID.randomUUID(), filter(values), rows, List.of(), false, false));
        return "pages/feature/editor";
    }

    /**
     * 一覧を検索する。条件変更時は境界と選択をリセットする。
     * @param values 検索条件
     * @param request HTTP要求
     * @param model 表示情報
     * @return 全体または一覧部分
     */
    @GetMapping("/features")
    String list(@RequestParam final MultiValueMap<String, String> values, final HttpServletRequest request,
        final Model model) {
        final Filter filter = filter(values);
        final var param = cursors.search(filter.name(), status(filter.status()), filter.sort(), filter.descending(),
            filter.size(), values.getFirst("cursor"), Boolean.parseBoolean(values.getFirst("previous")));
        final var result = queries.search(param);
        final String path = request.getContextPath() + "/features";
        final String previous = cursors.encode(result.previous(), param);
        final String next = cursors.encode(result.next(), param);
        model.addAttribute("listing",
            new Listing(filter, result.items(),
                previous == null ? null : filter.url(path) + "&previous=true&cursor=" + previous,
                next == null ? null : filter.url(path) + "&cursor=" + next));
        return "true".equals(request.getHeader("HX-Request")) ? "pages/feature/fragments/list" : "pages/feature/index";
    }

    /**
     * 保存前に最新値を表示する。入力値と期待versionは維持する。
     * @param values 編集入力
     * @param model 表示情報
     * @return 比較画面
     */
    @PostMapping("/features/refresh")
    String refresh(@RequestParam final MultiValueMap<String, String> values, final Model model) {
        final Editor editor = editor(values);
        if (editor.creating()) {
            throw new IllegalArgumentException("Update required");
        }
        final List<Row> rows = editor.rows().stream().map(row -> new Row(row.id(), row.name(), row.status(),
            row.version(), queries.findById(new FindFeatureByIdParam(row.id())).orElse(null))).toList();
        model.addAttribute("editor",
            new Editor(false, editor.requestId(), editor.filter(), rows, List.of(), true, true));
        return "pages/feature/editor";
    }

    /**
     * 入力を一括保存し、成功時は条件を維持して先頭ページへ戻る。
     * @param values フォーム入力
     * @param principal 認証主体
     * @param model 表示情報
     * @param response HTTP応答
     * @param redirect 完了通知
     * @return 一覧へのredirectまたは入力画面
     */
    @PostMapping("/features/save")
    String save(@RequestParam final MultiValueMap<String, String> values, final Principal principal, final Model model,
        final HttpServletResponse response, final RedirectAttributes redirect) {
        final Editor editor = editor(values);
        final Result<List<UUID>, FeatureCommandFailure> result = editor.creating()
            ? commands.createMany(new CreateFeaturesParam(principal.getName(), editor.requestId(),
                editor.rows().stream().map(row -> new CreateFeatureParam(row.name(), status(row.status()))).toList()))
            : commands.updateMany(new UpdateFeaturesParam(editor.rows().stream()
                .map(row -> new UpdateFeatureParam(row.id(), row.name(), status(row.status()), row.version()))
                .toList()));
        return switch (result) {
            case Result.Success<?, ?> _ -> {
                redirect.addFlashAttribute("notice", "保存しました。");
                yield "redirect:" + editor.filter().url("/features");
            }
            case Result.Failure(var failure) -> {
                response.setStatus(
                    failure.issues().stream().anyMatch(issue -> issue.reason() == FeatureCommandFailure.Reason.CONFLICT
                        || issue.reason() == FeatureCommandFailure.Reason.REPLAY_MISMATCH) ? 409 : 422);
                model.addAttribute("editor", new Editor(editor.creating(), editor.requestId(), editor.filter(),
                    editor.rows(), failure.issues(), false, true));
                yield "pages/feature/editor";
            }
        };
    }
}
