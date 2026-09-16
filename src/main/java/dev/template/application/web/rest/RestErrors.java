package dev.template.application.web.rest;

import dev.template.application.logging.TechnicalErrors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** MVCエラーをProblemDetailへ統一する。Security例外はfilter側の401/403判定に委譲する。 */
@RestControllerAdvice
class RestErrors extends ResponseEntityExceptionHandler {
    /** 予期しないREST障害の秘匿処理済み診断情報の出力先。 */
    private static final Logger LOG = LoggerFactory.getLogger(RestErrors.class);

    /**
     * 認証・認可例外はSecurity側のhandlerへ伝播する。
     *
     * @param exception 元のSecurity例外
     */
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    void propagateSecurityException(final RuntimeException exception) {
        throw exception;
    }

    /**
     * 技術障害を安全にログし、詳細を伏せた500を返す。
     *
     * @param exception 予期しない障害
     * @return 汎用エラー応答
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(final Exception exception) {
        TechnicalErrors.log(LOG, "Unexpected REST error", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }
}
