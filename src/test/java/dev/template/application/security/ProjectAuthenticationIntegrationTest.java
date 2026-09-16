package dev.template.application.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.template.application.Application;
import dev.template.application.feature.api.authorization.FeatureAuthority;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** 開発用認証を無効にしたapplicationで案件の認証storeとHTTP認可を検証する。 */
@SpringBootTest(classes = Application.class, properties = {"spring.docker.compose.enabled=false",
    "starter.security.local.enabled=false"})
@AutoConfigureMockMvc
@Import(ProjectAuthenticationIntegrationTest.ProjectUsers.class)
@Testcontainers
@Tag("integration")
class ProjectAuthenticationIntegrationTest {
    /** Springの接続先として共有する、このtest class専用のPostgreSQL。 */
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(System.getProperty("test.postgres.image"));

    /** test対象のBeanと設定を取得するapplication context。 */
    private final ApplicationContext context;
    /** 実際のMVC構成とSecurity filterを通してHTTP契約を検証するclient。 */
    private final MockMvc mvc;

    /**
     * 実際のfilter chainと認証Beanを検証するためのcontextを受け取る。
     * @param mvc HTTP操作
     * @param context application context
     */
    @Autowired
    ProjectAuthenticationIntegrationTest(final MockMvc mvc, final ApplicationContext context) {
        this.mvc = mvc;
        this.context = context;
    }

    /**
     * 残存する開発credentialを拒否し、案件の利用者でsession認証と権限制御を適用する。
     * @throws Exception HTTP操作に失敗した場合
     */
    @Test
    void onlyProjectCredentialsCanAuthenticate() throws Exception {
        assertThat(context.getBeansOfType(LocalAuthenticationConfiguration.UserSettings.class)).isEmpty();
        assertThat(context.containsBean("localUsers")).isFalse();
        mvc.perform(post("/login").with(csrf()).param("username", "test-user").param("password", "test-password-12345"))
            .andExpect(unauthenticated());
        mvc.perform(post("/login").with(csrf()).param("username", "project-user").param("password", "wrong-password"))
            .andExpect(unauthenticated());

        final var response = mvc.perform(
            post("/login").with(csrf()).param("username", "project-user").param("password", "project-password-12345"))
            .andExpect(authenticated().withUsername("project-user")).andReturn();
        final var session = (MockHttpSession) response.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(get("/api/v1/features/0199417c-0000-7000-8000-000000000099").session(session))
            .andExpect(status().isNotFound());
        mvc.perform(get("/actuator/metrics").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/logout").with(csrf()).session(session)).andExpect(status().is3xxRedirection());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/v1/features/0199417c-0000-7000-8000-000000000099")).andExpect(status().isUnauthorized());
    }

    /** 案件の通常Configurationによる利用者storeをtest用classpathだけに配置する。 */
    @TestConfiguration(proxyBeanMethods = false)
    static class ProjectUsers {
        /**
         * 開発用設定と異なるcredentialを持つ案件利用者を提供する。
         * @return test専用の案件利用者store
         */
        @Bean
        UserDetailsService projectUsers() {
            final var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
            return new InMemoryUserDetailsManager(User.withUsername("project-user")
                .password(encoder.encode("project-password-12345")).authorities(FeatureAuthority.READ).build());
        }
    }
}
