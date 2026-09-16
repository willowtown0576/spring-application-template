package dev.template.application.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.Assert;

/** 認証の提供元が未設定のapplicationの起動を拒否する。 */
@AutoConfiguration(after = LocalAuthenticationConfiguration.class)
class AuthenticationConfiguration {
    /**
     * singleton初期化後に認証Beanの存在を確認する。
     * @param users 利用者store
     * @param providers 認証provider
     * @param managers 案件の認証manager
     * @return 認証未設定なら起動を失敗させる検査
     */
    @Bean
    SmartInitializingSingleton requireAuthentication(final ObjectProvider<UserDetailsService> users,
        final ObjectProvider<AuthenticationProvider> providers, final ObjectProvider<AuthenticationManager> managers) {
        return () -> Assert.state(
            users.stream().findAny().isPresent() || providers.stream().findAny().isPresent()
                || managers.stream().findAny().isPresent(),
            "Authentication is not configured. Define a UserDetailsService, AuthenticationProvider or "
                + "AuthenticationManager bean for deployment. For local development, use bootRun or explicitly set "
                + "starter.security.local.enabled=true with APP_USER_NAME and APP_USER_PASSWORD.");
    }
}
