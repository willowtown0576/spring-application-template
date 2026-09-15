package dev.template.application.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/** 初期利用者の設定、password秘匿、運用権限の明示付与を検証する。 */
class LocalAuthenticationConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(LocalAuthenticationConfiguration.class));

    /** credential不足と長さ制約違反を起動時に拒否する。 */
    @Test
    void rejectsMissingAndInvalidCredentials() {
        runner.run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("starter.security.user.name=system:batch",
            "starter.security.user.password=test-password-12345").run(context -> assertThat(context).hasFailed());
        for (final String password : new String[]{"short", "a".repeat(73), "あ".repeat(25)}) {
            runner
                .withPropertyValues("starter.security.user.name=operator", "starter.security.user.password=" + password)
                .run(context -> assertThat(context).hasFailed());
        }
    }

    /** 正しいpasswordのみ照合でき、運用権限は明示設定時だけ付与され設定値は秘匿される。 */
    @Test
    void encodesPasswordAndGrantsExplicitOperationsAccess() {
        runner
            .withPropertyValues("starter.security.user.name=operator",
                "starter.security.user.password=test-password-12345", "starter.security.user.operations-read=true")
            .run(context -> {
                assertThat(context).hasNotFailed();
                final var user = context.getBean(UserDetailsService.class).loadUserByUsername("operator");
                final var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
                assertThat(encoder.matches("test-password-12345", user.getPassword())).isTrue();
                assertThat(encoder.matches("wrong-password", user.getPassword())).isFalse();
                assertThat(user.getAuthorities()).extracting(GrantedAuthority::getAuthority).contains("feature:read",
                    "feature:write", "ops:read");
                assertThat(context.getBean(LocalAuthenticationConfiguration.UserSettings.class).toString())
                    .doesNotContain("test-password-12345", "operator");
            });
    }
    /** UserDetailsService定義済みの構成では、その実装を使用する。 */
    @Test
    void customUserStoreReplacesLocalAuthentication() {
        runner.withUserConfiguration(CustomAuthentication.class).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(UserDetailsService.class);
            assertThat(context).doesNotHaveBean(LocalAuthenticationConfiguration.UserSettings.class);
        });
    }

    /** 案件側の通常Configurationによる認証store。 */
    @Configuration(proxyBeanMethods = false)
    static class CustomAuthentication {
        /**
         * test専用の空のユーザーstoreを提供する。
         * @return 案件側の認証store
         */
        @Bean
        UserDetailsService customUsers() {
            return new InMemoryUserDetailsManager();
        }
    }
}
