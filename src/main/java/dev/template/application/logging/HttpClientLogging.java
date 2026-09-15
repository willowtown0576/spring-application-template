package dev.template.application.logging;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Bootが構築するHTTP clientへ相関ID伝播と診断用metadataのログを追加する。 */
@Configuration(proxyBeanMethods = false)
class HttpClientLogging {
    private static final Logger LOG = LoggerFactory.getLogger(HttpClientLogging.class);

    /**
     * 記録対象をmethod・host・status・所要時間に限定する。
     *
     * @return HTTP interceptorを登録するcustomizer
     */
    @Bean
    RestClientCustomizer outgoingLogging() {
        return builder -> builder.requestInterceptor((request, body, execution) -> {
            final String correlation = MDC.get(CorrelationFilter.KEY);
            if (correlation != null)
                request.getHeaders().set(CorrelationFilter.HEADER, correlation);
            final long start = System.nanoTime();
            try {
                final var response = execution.execute(request, body);
                LOG.info("HTTP outgoing method={} host={} status={} durationMs={}", request.getMethod(),
                    request.getURI().getHost(), response.getStatusCode().value(),
                    (System.nanoTime() - start) / 1_000_000);
                return response;
            } catch (final IOException | RuntimeException exception) {
                TechnicalErrors.log(LOG, "HTTP outgoing", exception);
                throw exception;
            }
        });
    }
}
