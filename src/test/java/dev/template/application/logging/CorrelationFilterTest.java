package dev.template.application.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(OutputCaptureExtension.class)
class CorrelationFilterTest {
  @Test
  void generatesTrustedIdAndRestoresMdcEvenOnFailure(CapturedOutput output) {
    var request = new MockHttpServletRequest("GET", "/private-person?token=secret-token");
    request.addHeader(CorrelationFilter.HEADER, "untrusted-secret-header");
    var response = new MockHttpServletResponse();
    MDC.put(CorrelationFilter.KEY, "outer");
    try {
      assertThatThrownBy(
              () ->
                  new CorrelationFilter()
                      .doFilter(
                          request,
                          response,
                          (req, res) -> {
                            assertThat(MDC.get(CorrelationFilter.KEY))
                                .isEqualTo(response.getHeader(CorrelationFilter.HEADER));
                            assertThat(UUID.fromString(MDC.get(CorrelationFilter.KEY))).isNotNull();
                            throw new IOException("private-exception");
                          }))
          .isInstanceOf(IOException.class);
      assertThat(MDC.get(CorrelationFilter.KEY)).isEqualTo("outer");
      assertThat(output.getAll())
          .contains("status=500")
          .doesNotContain(
              "secret-token", "private-person", "untrusted-secret-header", "private-exception");
    } finally {
      MDC.remove(CorrelationFilter.KEY);
    }
  }

  @Test
  void exceptionLoggingKeepsFramesButOmitsMessages(CapturedOutput output) {
    TechnicalErrors.log(
        LoggerFactory.getLogger(TechnicalErrors.class),
        "test operation",
        new IllegalStateException("secret-password", new IOException("secret-token")));
    assertThat(output.getAll())
        .contains("IllegalStateException", "IOException", "CorrelationFilterTest")
        .doesNotContain("secret-password", "secret-token");
  }
}
