package dev.template.application.web.rest;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Spring MVC標準のpath segmentによるmajor API version解決を設定する。 */
@Configuration(proxyBeanMethods = false)
class WebConfiguration implements WebMvcConfigurer {
    /** {@inheritDoc} */
    @Override
    public void configureApiVersioning(final ApiVersionConfigurer configurer) {
        configurer.usePathSegment(1);
    }
}
