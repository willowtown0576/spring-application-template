package dev.template.application.common;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** application共通のUTC Clockを提供する。 */
@Configuration(proxyBeanMethods = false)
class CommonConfiguration {
    /**
     * 本番の実時刻をUTCで供給する。
     *
     * @return UTCを使用するClock
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
