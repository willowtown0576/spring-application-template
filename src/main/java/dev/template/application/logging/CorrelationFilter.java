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

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationFilter extends OncePerRequestFilter {
  static final String HEADER = "X-Correlation-ID";
  static final String KEY = "correlationId";
  private static final Logger LOG = LoggerFactory.getLogger(CorrelationFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String previous = MDC.get(KEY);
    MDC.put(KEY, UUID.randomUUID().toString());
    response.setHeader(HEADER, MDC.get(KEY));
    long start = System.nanoTime();
    int status = 500;
    try {
      chain.doFilter(request, response);
      status = response.getStatus();
    } finally {
      LOG.info(
          "HTTP incoming method={} status={} durationMs={}",
          request.getMethod(),
          status,
          (System.nanoTime() - start) / 1_000_000);
      if (previous == null) MDC.remove(KEY);
      else MDC.put(KEY, previous);
    }
  }
}
