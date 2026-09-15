package dev.template.application.http;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/** Boot標準HTTP client timeout設定を型付きで検証し、無期限待機を防ぐ。 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HttpTimeoutConfiguration.Timeouts.class)
class HttpTimeoutConfiguration {
    /**
     * 標準設定と同じprefixを使い、起動時に正のtimeoutを必須化する。
     *
     * @param connectTimeout 接続確立の上限
     * @param readTimeout 応答待ちの上限
     */
    @Validated
    @ConfigurationProperties("spring.http.clients")
    record Timeouts(@NotNull @DurationMin(millis = 1) Duration connectTimeout,
        @NotNull @DurationMin(millis = 1) Duration readTimeout) {
    }
}
