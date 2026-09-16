package dev.template.application.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 受信リクエストにサーバー生成の相関IDを付与し、終了時に以前のMDCを復元する。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationFilter extends OncePerRequestFilter {
    /** HTTP応答と外向きHTTP通信で相関IDを伝播するheader名。 */
    static final String HEADER = "X-Correlation-ID";
    /** 処理中の相関IDを保持するMDCのkey。 */
    static final String KEY = "correlationId";
    /** 受信HTTP通信のmethod・status・所要時間の出力先。 */
    private static final Logger LOG = LoggerFactory.getLogger(CorrelationFilter.class);

    /** {@inheritDoc} */
    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
        final FilterChain chain) throws ServletException, IOException {
        final String previous = MDC.get(KEY);
        MDC.put(KEY, UUID.randomUUID().toString());
        response.setHeader(HEADER, MDC.get(KEY));
        final long start = System.nanoTime();
        int status = 500;
        try {
            chain.doFilter(request, response);
            status = response.getStatus();
        } finally {
            LOG.info("HTTP incoming method={} status={} durationMs={}", request.getMethod(), status,
                (System.nanoTime() - start) / 1_000_000);
            if (previous == null)
                MDC.remove(KEY);
            else
                MDC.put(KEY, previous);
        }
    }
}
