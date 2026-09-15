package dev.template.application.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** 不正なHTTP timeout設定が起動時に拒否されることを検証する。 */
@Tag("integration")
class HttpTimeoutConfigurationIntegrationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(HttpTimeoutConfiguration.class);

    /** 必須timeout設定がない構成を起動し、設定検証で起動に失敗することを検証する。 */
    @Test
    void missingTimeoutsFailStartup() {
        runner.run(context -> assertThat(context).hasFailed());
    }

    /**
     * ゼロ・負値・形式不正のtimeoutを設定し、application起動が拒否されることを検証する。
     *
     * @param value 検証するtimeout設定値
     */
    @ParameterizedTest
    @ValueSource(strings = {"0s", "-1s", "invalid"})
    void invalidTimeoutsFailStartup(final String value) {
        runner.withPropertyValues("spring.http.clients.connect-timeout=" + value, "spring.http.clients.read-timeout=1s")
            .run(context -> assertThat(context).hasFailed());
    }

    /** 有効なDurationを設定し、接続・応答timeoutが指定値でbindされることを検証する。 */
    @Test
    void durationsBindToValidatedConfiguration() {
        runner.withPropertyValues("spring.http.clients.connect-timeout=2s", "spring.http.clients.read-timeout=10s")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(HttpTimeoutConfiguration.Timeouts.class).connectTimeout())
                    .isEqualTo(Duration.ofSeconds(2));
            });
    }
}
