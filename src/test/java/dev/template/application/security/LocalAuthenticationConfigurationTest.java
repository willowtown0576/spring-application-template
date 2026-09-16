package dev.template.application.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/** 開発用認証の有効化、credential検証と案件認証への置換を検証する。 */
class LocalAuthenticationConfigurationTest {
    /** 開発用認証を明示的に有効にしてcredential契約を検証するrunner。 */
    private final ApplicationContextRunner localRunner;

    /** 認証設定の有効化と案件Beanへの置換を検証する共通runner。 */
    private final ApplicationContextRunner runner = new ApplicationContextRunner().withConfiguration(
        AutoConfigurations.of(LocalAuthenticationConfiguration.class, AuthenticationConfiguration.class));

    /** 共通設定を構築した後に、開発用認証を有効化した設定を派生させる。 */
    LocalAuthenticationConfigurationTest() {
        localRunner = runner.withPropertyValues("starter.security.local.enabled=true");
    }

    /** 無効時は利用者storeも平文credentialを持つ設定Beanも作成しない。 */
    @Test
    void disabledLocalAuthenticationDoesNotBindCredentials() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(LocalAuthenticationConfiguration.class))
            .withPropertyValues("starter.security.user.name=operator",
                "starter.security.user.password=test-password-12345")
            .run(context -> {
                assertThat(context).hasNotFailed().doesNotHaveBean(UserDetailsService.class);
                assertThat(context).doesNotHaveBean(LocalAuthenticationConfiguration.UserSettings.class);
            });
    }

    /** 正しいpasswordのみ照合でき、運用権限は明示設定時だけ付与され設定値は秘匿される。 */
    @Test
    void encodesPasswordAndGrantsExplicitOperationsAccess() {
        localRunner
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

    /** 案件の認証Beanを優先し、開発用認証が有効でもcredentialを要求しない。 */
    @Test
    void projectAuthenticationReplacesLocalAuthentication() {
        for (final boolean enabled : new boolean[]{false, true}) {
            final var configured = runner.withPropertyValues("starter.security.local.enabled=" + enabled);
            configured.withUserConfiguration(CustomAuthentication.class).run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(UserDetailsService.class);
                assertThat(context).doesNotHaveBean(LocalAuthenticationConfiguration.UserSettings.class);
                assertThat(context).doesNotHaveBean("localUsers");
            });
            configured.withBean(AuthenticationProvider.class,
                () -> new DaoAuthenticationProvider(new InMemoryUserDetailsManager())).run(context -> {
                    assertThat(context).hasNotFailed().doesNotHaveBean(UserDetailsService.class);
                    assertThat(context).doesNotHaveBean(LocalAuthenticationConfiguration.UserSettings.class);
                });
            configured
                .withBean(AuthenticationManager.class,
                    () -> new ProviderManager(new DaoAuthenticationProvider(new InMemoryUserDetailsManager())))
                .run(context -> {
                    assertThat(context).hasNotFailed().doesNotHaveBean(UserDetailsService.class);
                    assertThat(context).doesNotHaveBean(LocalAuthenticationConfiguration.UserSettings.class);
                });
        }
    }

    /** credential不足と長さ制約違反を起動時に拒否する。 */
    @Test
    void rejectsMissingAndInvalidCredentials() {
        localRunner.run(context -> assertThat(context).hasFailed());
        localRunner.withPropertyValues("starter.security.user.name=system:batch",
            "starter.security.user.password=test-password-12345").run(context -> assertThat(context).hasFailed());
        for (final String password : new String[]{"short", "a".repeat(73), "あ".repeat(25)}) {
            localRunner
                .withPropertyValues("starter.security.user.name=operator", "starter.security.user.password=" + password)
                .run(context -> assertThat(context).hasFailed());
        }
    }
    /** credentialが残っていても、明示有効化と案件認証がなければ起動を拒否する。 */
    @Test
    void rejectsMissingAuthenticationEvenWithLocalCredentials() {
        for (final String[] properties : new String[][]{{}, {"starter.security.local.enabled=false"}}) {
            runner.withPropertyValues(properties).run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).hasMessageContaining("Authentication is not configured");
            });
            runner.withPropertyValues(properties).withPropertyValues("starter.security.user.name=operator",
                "starter.security.user.password=test-password-12345").run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasMessageContaining("Authentication is not configured");
                });
        }
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
