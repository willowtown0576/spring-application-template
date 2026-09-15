package dev.template.application.security;

import dev.template.application.feature.api.authorization.FeatureAuthority;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/**
 * Method Security統合testでenum権限を付与するtest専用annotation。 フォーム認証から独立して、Spring Testのcontext
 * lifecycleで隔離する。
 */
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithTestUser.Factory.class)
public @interface WithTestUser {
    /**
     * testに必要な権限だけを指定する。
     *
     * @return 認証主体へ付与するFeature権限
     */
    FeatureAuthority[] value();

    /** Spring Security Testから呼ばれ、testごとに新しいcontextを作る。 */
    final class Factory implements WithSecurityContextFactory<WithTestUser> {
        /** {@inheritDoc} */
        @Override
        public SecurityContext createSecurityContext(final WithTestUser user) {
            final var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("test-user", null, List.of(user.value())));
            return context;
        }
    }
}
