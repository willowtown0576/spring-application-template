package dev.template.application.web.ui;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** HTML画面の共通表示情報を提供する。 */
@ControllerAdvice(assignableTypes = {UiController.class, FeatureUiController.class, FeatureDeletionUiController.class})
class UiModel {
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

}
