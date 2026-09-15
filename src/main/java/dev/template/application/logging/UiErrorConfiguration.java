package dev.template.application.logging;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Vaadin session共通の予期しないエラー処理を設定する。利用者向けのエラー表示と内部診断情報を分離する。 */
@Configuration(proxyBeanMethods = false)
class UiErrorConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(UiErrorConfiguration.class);

    /**
     * sessionに共通error handlerを設定する。
     *
     * @return Vaadin service初期化listener
     */
    @Bean
    VaadinServiceInitListener uiErrors() {
        return event -> event.getSource()
            .addSessionInitListener(session -> session.getSession().setErrorHandler(error -> {
                TechnicalErrors.log(LOG, "Unexpected UI error", error.getThrowable());
                Notification.show("処理に失敗しました。時間をおいて再度お試しください。");
            }));
    }
}
