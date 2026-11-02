package dev.template.application.web.ui;

import dev.template.application.common.Result;
import dev.template.application.feature.api.command.CreateFeatureParam;
import dev.template.application.feature.api.command.FeatureCommands;
import dev.template.application.feature.api.query.FeatureQueries;
import dev.template.application.feature.api.query.FindFeatureParam;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** 標準HTMLと部分更新を描画し、業務操作をfeature公開APIへ委譲する。 */
@Controller
class UiController {
    /** Featureの公開更新API。 */
    private final FeatureCommands commands;
    /** Featureの公開参照API。 */
    private final FeatureQueries queries;

    /**
     * 業務APIを受け取る。
     * @param commands 更新API
     * @param queries 参照API
     */
    UiController(final FeatureCommands commands, final FeatureQueries queries) {
        this.commands = commands;
        this.queries = queries;
    }

    /**
     * request種別に対応する描画単位を選択する。
     * @param request 現在のrequest
     * @return template名
     */
    private static String featureTemplate(final HttpServletRequest request) {
        return "true".equals(request.getHeader("HX-Request")) ? "pages/feature/fragments/panel" : "pages/feature/index";
    }

    /**
     * 公開部品集を表示する。
     * @return template名
     */
    @GetMapping("/components")
    String components() {
        return "pages/samples/components/index";
    }

    /**
     * 名前を作成し、通常requestにはページ、htmxには入力領域を返す。
     * @param name 入力された名前
     * @param model 表示情報
     * @param request 現在のrequest
     * @param response HTTP応答
     * @return template名
     */
    @PostMapping("/features")
    String create(@RequestParam(defaultValue = "") final String name, final Model model,
        final HttpServletRequest request, final HttpServletResponse response) {
        final FeaturePage feature = switch (commands.create(new CreateFeatureParam(name))) {
            case Result.Success(var id) -> new FeaturePage("", id.toString(), "作成しました。", "", "");
            case Result.Failure(var failure) -> switch (failure) {
                case INVALID_NAME -> new FeaturePage(name, "", "", "名前を1〜100文字で入力してください。", "");
            };
        };
        if (!feature.nameError().isEmpty()) {
            response.setStatus(422);
        }
        model.addAttribute("feature", feature);
        return featureTemplate(request);
    }

    /**
     * DB連携画面を表示する。
     * @param model 表示情報
     * @return template名
     */
    @GetMapping("/features")
    String features(final Model model) {
        model.addAttribute("feature", new FeaturePage("", "", "", "", ""));
        return "pages/feature/index";
    }

    /**
     * IDの形式を確認して公開Queryで検索する。
     * @param id 入力ID
     * @param model 表示情報
     * @param request 現在のrequest
     * @param response HTTP応答
     * @return template名
     */
    @GetMapping("/features/find")
    String find(@RequestParam(defaultValue = "") final String id, final Model model, final HttpServletRequest request,
        final HttpServletResponse response) {
        final UUID requested;
        try {
            requested = UUID.fromString(id);
            if (!requested.toString().equalsIgnoreCase(id)) {
                throw new IllegalArgumentException("Non-canonical UUID");
            }
        } catch (final IllegalArgumentException exception) {
            response.setStatus(422);
            model.addAttribute("feature", new FeaturePage("", id, "", "", "正しいUUIDを入力してください。"));
            return featureTemplate(request);
        }
        final String message = queries.find(new FindFeatureParam(requested)).map(feature -> "名前: " + feature.name())
            .orElse("見つかりませんでした。");
        model.addAttribute("feature", new FeaturePage("", id, message, "", ""));
        return featureTemplate(request);
    }

    /**
     * 標準フォーム認証の画面を表示する。
     * @param error 認証失敗の有無
     * @param model 表示情報
     * @return template名
     */
    @GetMapping("/login")
    String login(@RequestParam(required = false) final String error, final Model model) {
        model.addAttribute("failed", error != null);
        return "pages/auth/login";
    }

    /**
     * context pathと遅延CSRF tokenを各HTMLへ渡す。
     * @param request 現在のrequest
     * @param authentication 現在の認証
     * @return 共通表示情報
     */
    @ModelAttribute("page")
    UiPage page(final HttpServletRequest request, final Authentication authentication) {
        final CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return new UiPage(request.getContextPath(),
            authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken),
            token.getParameterName(), token.getHeaderName(), token.getToken());
    }

    /**
     * 公開ホームを表示する。
     * @return template名
     */
    @GetMapping("/")
    String welcome() {
        return "pages/home/index";
    }
}
