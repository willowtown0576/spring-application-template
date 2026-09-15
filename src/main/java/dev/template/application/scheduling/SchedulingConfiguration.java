package dev.template.application.scheduling;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Spring標準Schedulingを有効化する。案件固有の定期処理は別Beanへ定義する。 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
class SchedulingConfiguration {
}
