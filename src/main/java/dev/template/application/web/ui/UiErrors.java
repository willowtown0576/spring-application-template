package dev.template.application.web.ui;

import dev.template.application.logging.TechnicalErrors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/** HTML操作の拒否と技術障害を、内部情報を含まない通知へ変換する。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice(assignableTypes = UiController.class)
class UiErrors {
    /** 秘匿済みの診断情報の出力先。 */
    private static final Logger LOG = LoggerFactory.getLogger(UiErrors.class);

    /**
     * Method Securityの拒否を表示する。
     * @param model 表示情報
     * @return 通知template
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    String denied(final Model model) {
        model.addAttribute("message", "操作する権限がありません。");
        return "components/error-message";
    }

    /**
     * 技術障害の詳細を伏せて表示する。
     * @param exception 予期しない障害
     * @param model 表示情報
     * @return 通知template
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    String unexpected(final Exception exception, final Model model) {
        TechnicalErrors.log(LOG, "Unexpected UI error", exception);
        model.addAttribute("message", "処理に失敗しました。");
        return "components/error-message";
    }
}
