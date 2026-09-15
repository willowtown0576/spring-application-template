package dev.template.application.feature.api.authorization;

import org.springframework.security.core.GrantedAuthority;

/**
 * Feature操作の権限。認証providerはログイン時に該当enumをGrantedAuthorityとして付与する。
 *
 * <p>wire値は外部認証との接続契約であり、enum名の変更から独立させる。Roleは認証adapterが割り当てるAuthorityの集合として扱う。
 */
public enum FeatureAuthority implements GrantedAuthority {
    /** Featureを参照できる。 */
    READ("feature:read"),
    /** Featureを作成できる。 */
    WRITE("feature:write");

    private final String authority;

    /**
     * Securityと交換する安定した権限名を保持する。
     *
     * @param authority wire値
     */
    FeatureAuthority(final String authority) {
        this.authority = authority;
    }

    /** {@inheritDoc} */
    @Override
    public String getAuthority() {
        return authority;
    }
}
