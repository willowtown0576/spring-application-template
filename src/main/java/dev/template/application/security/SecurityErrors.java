package dev.template.application.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Security filter内の未認証・認可拒否を、本文に内部情報を含まないProblemDetailへ変換する。 */
@Component
class SecurityErrors implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final JsonMapper mapper;

    /**
     * Boot管理のJSON mapperを使用する。
     *
     * @param mapper ProblemDetailを出力するmapper
     */
    SecurityErrors(final JsonMapper mapper) {
        this.mapper = mapper;
    }

    /** {@inheritDoc} */
    @Override
    public void commence(final HttpServletRequest request, final HttpServletResponse response,
        final AuthenticationException exception) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, "Authentication is required.");
    }

    /** {@inheritDoc} */
    @Override
    public void handle(final HttpServletRequest request, final HttpServletResponse response,
        final AccessDeniedException exception) throws IOException {
        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!new AuthenticationTrustResolverImpl().isAuthenticated(authentication)) {
            write(response, HttpStatus.UNAUTHORIZED, "Authentication is required.");
        } else {
            write(response, HttpStatus.FORBIDDEN, "Access is denied.");
        }
    }

    /**
     * 指定statusのProblemDetailをHTTP responseへ出力する。
     *
     * @param response 出力先
     * @param status 応答status
     * @param detail 公開可能な固定説明
     * @throws IOException responseへ書き込めない場合
     */
    private void write(final HttpServletResponse response, final HttpStatus status, final String detail)
        throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), ProblemDetail.forStatusAndDetail(status, detail));
    }
}
