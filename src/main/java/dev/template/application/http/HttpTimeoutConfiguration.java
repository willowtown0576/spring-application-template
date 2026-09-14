package dev.template.application.http;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HttpTimeoutConfiguration.Timeouts.class)
class HttpTimeoutConfiguration {
  @Validated
  @ConfigurationProperties("spring.http.clients")
  record Timeouts(
      @NotNull @DurationMin(millis = 1) Duration connectTimeout,
      @NotNull @DurationMin(millis = 1) Duration readTimeout) {}
}
