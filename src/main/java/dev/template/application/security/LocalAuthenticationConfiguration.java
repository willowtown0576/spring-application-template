package dev.template.application.security;

import dev.template.application.feature.api.authorization.FeatureAuthority;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.validation.annotation.Validated;

/** 明示的に有効化された開発環境の利用者をフォーム認証へ提供する。案件の認証Beanを優先する。 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "starter.security.local", name = "enabled", havingValue = "true")
@ConditionalOnMissingBean({UserDetailsService.class, AuthenticationProvider.class, AuthenticationManager.class})
@EnableConfigurationProperties(LocalAuthenticationConfiguration.UserSettings.class)
class LocalAuthenticationConfiguration {
    /**
     * passwordをencodeし、明示的な操作権限を持つ利用者を登録する。
     * @param settings 環境から受け取る利用者設定
     * @return 設定された利用者だけを持つ認証用store
     */
    @Bean
    UserDetailsService localUsers(final UserSettings settings) {
        final List<GrantedAuthority> authorities = new ArrayList<>(
            List.of(FeatureAuthority.READ, FeatureAuthority.WRITE));
        if (settings.operationsRead()) {
            authorities.add(OperationsAuthority.READ);
        }
        final String encoded = new BCryptPasswordEncoder().encode(settings.password());
        return new InMemoryUserDetailsManager(
            User.withUsername(settings.name()).password("{bcrypt}" + encoded).authorities(authorities).build());
    }

    /**
     * 開発用認証を使用する場合の必須設定。ログ表現ではpasswordを秘匿する。
     * @param name ログインID
     * @param password 起動時にencodeするpassword
     * @param operationsRead 運用情報の参照許可
     */
    @Validated
    @ConfigurationProperties("starter.security.user")
    record UserSettings(
        @NotBlank @Pattern(regexp = "(?!system:).*", message = "User name must not use the reserved system: prefix") String name,
        String password, boolean operationsRead) {
        /**
         * BCryptで切り捨てられない長さと最低文字数を検証する。
         * @return 12文字以上かつ72 UTF-8 bytes以下ならtrue
         */
        @AssertTrue(message = "Password must contain at least 12 characters and at most 72 UTF-8 bytes")
        public boolean isPasswordValid() {
            return password != null && password.codePointCount(0, password.length()) >= 12
                && password.getBytes(StandardCharsets.UTF_8).length <= 72;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "UserSettings[credentials=REDACTED, operationsRead=" + operationsRead + "]";
        }
    }
}
