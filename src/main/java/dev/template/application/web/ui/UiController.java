package dev.template.application.web.ui;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

/** ログインと、認証後のホーム・部品集のHTML画面を選択する。 */
@Controller
class UiController {
    /**
     * 認証後の部品集を表示する。
     * @return template名
     */
    @GetMapping("/components")
    String components() {
        return "pages/samples/components/index";
    }

    /**
     * 標準フォーム認証の画面を表示する。
     * @param error 認証失敗の有無
     * @param logout ログアウト完了の有無
     * @param page 認証状態を含む共通表示情報
     * @param model 表示情報
     * @return template名
     */
    @GetMapping("/login")
    String login(@RequestParam(required = false) final String error,
        @RequestParam(required = false) final String logout, @ModelAttribute("page") final UiPage page,
        final Model model) {
        if (page.authenticated()) {
            return "redirect:/";
        }
        model.addAttribute("loggedOut", logout != null);
        model.addAttribute("failed", error != null);
        return "pages/auth/login";
    }

    /**
     * 認証後のホームを表示する。
     * @return template名
     */
    @GetMapping("/")
    String welcome() {
        return "pages/home/index";
    }
}
