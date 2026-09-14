package dev.template.application.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class HttpTimeoutConfigurationIntegrationTest {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(HttpTimeoutConfiguration.class);

  @Test
  void missingTimeoutsFailStartup() {
    runner.run(context -> assertThat(context).hasFailed());
  }

  @ParameterizedTest
  @ValueSource(strings = {"0s", "-1s", "invalid"})
  void invalidTimeoutsFailStartup(String value) {
    runner
        .withPropertyValues(
            "spring.http.clients.connect-timeout=" + value, "spring.http.clients.read-timeout=1s")
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void durationsBindToValidatedConfiguration() {
    runner
        .withPropertyValues(
            "spring.http.clients.connect-timeout=2s", "spring.http.clients.read-timeout=10s")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context.getBean(HttpTimeoutConfiguration.Timeouts.class).connectTimeout())
                  .isEqualTo(Duration.ofSeconds(2));
            });
  }
}
