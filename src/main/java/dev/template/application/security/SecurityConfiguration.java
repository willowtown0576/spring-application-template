package dev.template.application.security;

import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults;
import org.springframework.security.web.SecurityFilterChain;

/** URLごとの粗い入口制御とMethod Securityを有効化する。標準フォーム認証を使用する。 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
class SecurityConfiguration {
    /**
     * 型付き認可annotationの値をSpring標準の式テンプレートで展開する。
     *
     * @return 標準テンプレート設定
     */
    @Bean
    static AnnotationTemplateExpressionDefaults templateExpressionDefaults() {
        return new AnnotationTemplateExpressionDefaults();
    }

    /**
     * health以外のActuatorを運用Authorityで保護する。
     *
     * @param http Springのfilter chain builder
     * @param errors 共通Securityエラー出力
     * @return 対象URLのfilter chain
     * @throws Exception filter chainを構築できない場合
     */
    @Bean
    @Order(0)
    SecurityFilterChain actuatorSecurity(final HttpSecurity http, final SecurityErrors errors) throws Exception {
        return http.securityMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeHttpRequests(requests -> requests.requestMatchers(EndpointRequest.to(HealthEndpoint.class))
                .permitAll().anyRequest().hasAuthority(OperationsAuthority.READ.getAuthority()))
            .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(errors).accessDeniedHandler(errors))
            .build();
    }

    /**
     * OpenAPIとSwagger UIを運用Authorityで保護する。
     *
     * @param http Springのfilter chain builder
     * @param errors 共通Securityエラー出力
     * @return 対象URLのfilter chain
     * @throws Exception filter chainを構築できない場合
     */
    @Bean
    @Order(1)
    SecurityFilterChain documentationSecurity(final HttpSecurity http, final SecurityErrors errors) throws Exception {
        return http.csrf(csrf -> csrf.spa())
            .securityMatcher("/v3/api-docs/**", "/v3/api-docs.yaml", "/swagger-ui/**", "/swagger-ui.html")
            .authorizeHttpRequests(
                requests -> requests.anyRequest().hasAuthority(OperationsAuthority.READ.getAuthority()))
            .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(errors).accessDeniedHandler(errors))
            .build();
    }

    /**
     * REST入口を認証必須にし、操作権限はCommand/Queryに委譲する。
     *
     * @param http Springのfilter chain builder
     * @param errors 共通Securityエラー出力
     * @return 対象URLのfilter chain
     * @throws Exception filter chainを構築できない場合
     */
    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(final HttpSecurity http, final SecurityErrors errors) throws Exception {
        return http.csrf(csrf -> csrf.spa()).securityMatcher("/api/**")
            .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
            .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(errors).accessDeniedHandler(errors))
            .logout(logout -> logout.disable()).build();
    }

    /**
     * HTML画面の入口、標準フォーム認証、部分更新の認証切れを制御する。
     *
     * @param http Springのfilter chain builder
     * @return UI用filter chain
     * @throws Exception filter chainを構築できない場合
     */
    @Bean
    SecurityFilterChain uiSecurityFilterChain(final HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.spa())
            .authorizeHttpRequests(requests -> requests.requestMatchers("/login", "/vendor/**", "/ui/**", "/error")
                .permitAll().requestMatchers("/actuator/**").denyAll().anyRequest().authenticated())
            .formLogin(login -> login.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
            .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, exception) -> {
                final String login = request.getContextPath() + "/login";
                if ("true".equals(request.getHeader("HX-Request"))) {
                    response.setHeader("HX-Redirect", login);
                    response.setStatus(401);
                } else {
                    response.sendRedirect(login);
                }
            }).accessDeniedHandler((request, response, exception) -> {
                if ("true".equals(request.getHeader("HX-Request"))) {
                    if (request.getUserPrincipal() == null) {
                        response.setHeader("HX-Redirect", request.getContextPath() + "/login");
                        response.setStatus(401);
                    } else {
                        response.setStatus(403);
                        response.setContentType("text/html;charset=UTF-8");
                        response.getWriter().write("操作を確認できませんでした。画面を再読み込みしてください。");
                    }
                } else {
                    response.sendError(403);
                }
            })).build();
    }
}
