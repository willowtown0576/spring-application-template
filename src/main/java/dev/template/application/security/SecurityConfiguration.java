package dev.template.application.security;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
class SecurityConfiguration {
  static final String OPERATIONS_READ = "ops:read";

  @Bean
  @Order(0)
  SecurityFilterChain actuatorSecurity(HttpSecurity http, SecurityErrors errors) throws Exception {
    return http.securityMatcher(EndpointRequest.toAnyEndpoint())
        .authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers(EndpointRequest.to(HealthEndpoint.class))
                    .permitAll()
                    .anyRequest()
                    .hasAuthority(OPERATIONS_READ))
        .exceptionHandling(
            exceptions -> exceptions.authenticationEntryPoint(errors).accessDeniedHandler(errors))
        .build();
  }

  @Bean
  @Order(1)
  SecurityFilterChain documentationSecurity(HttpSecurity http, SecurityErrors errors)
      throws Exception {
    return http.securityMatcher(
            "/v3/api-docs/**", "/v3/api-docs.yaml", "/swagger-ui/**", "/swagger-ui.html")
        .authorizeHttpRequests(requests -> requests.anyRequest().hasAuthority(OPERATIONS_READ))
        .exceptionHandling(
            exceptions -> exceptions.authenticationEntryPoint(errors).accessDeniedHandler(errors))
        .build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrors errors)
      throws Exception {
    return http.securityMatcher("/api/**")
        .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
        .exceptionHandling(
            exceptions -> exceptions.authenticationEntryPoint(errors).accessDeniedHandler(errors))
        .logout(logout -> logout.disable())
        .build();
  }

  @Bean
  SecurityFilterChain uiSecurityFilterChain(HttpSecurity http, SecurityErrors errors)
      throws Exception {
    return http.with(
            VaadinSecurityConfigurer.vaadin(),
            configurer ->
                configurer
                    .enableExceptionHandlingConfiguration(false)
                    .enableLogoutConfiguration(false))
        .exceptionHandling(
            exceptions -> exceptions.authenticationEntryPoint(errors).accessDeniedHandler(errors))
        .logout(logout -> logout.disable())
        .build();
  }
}
