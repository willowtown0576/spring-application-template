package dev.template.application.logging;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class HttpClientLogging {
  private static final Logger LOG = LoggerFactory.getLogger(HttpClientLogging.class);

  @Bean
  RestClientCustomizer outgoingLogging() {
    return builder ->
        builder.requestInterceptor(
            (request, body, execution) -> {
              String correlation = MDC.get(CorrelationFilter.KEY);
              if (correlation != null)
                request.getHeaders().set(CorrelationFilter.HEADER, correlation);
              long start = System.nanoTime();
              try {
                var response = execution.execute(request, body);
                LOG.info(
                    "HTTP outgoing method={} host={} status={} durationMs={}",
                    request.getMethod(),
                    request.getURI().getHost(),
                    response.getStatusCode().value(),
                    (System.nanoTime() - start) / 1_000_000);
                return response;
              } catch (IOException | RuntimeException exception) {
                TechnicalErrors.log(LOG, "HTTP outgoing", exception);
                throw exception;
              }
            });
  }
}
