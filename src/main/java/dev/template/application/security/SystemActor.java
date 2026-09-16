package dev.template.application.security;

import dev.template.application.feature.api.authorization.FeatureAuthority;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.GrantedAuthority;

/** process内の信頼されたadapterが使用するシステム主体と、その最小権限。 */
public enum SystemActor implements Principal {
    /** 作成と参照を行うBatch主体。 */
    BATCH(List.of(FeatureAuthority.READ, FeatureAuthority.WRITE)),
    /** 参照専用のScheduler主体。 */
    SCHEDULER(List.of(FeatureAuthority.READ));

    /** このシステム主体に許可された不変の権限一覧。 */
    private final List<GrantedAuthority> authorities;

    /**
     * 主体に許可された操作を固定する。
     * @param authorities feature権限
     */
    SystemActor(final List<GrantedAuthority> authorities) {
        this.authorities = List.copyOf(authorities);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "system:" + name().toLowerCase(Locale.ROOT);
    }

    /**
     * 不変の権限一覧を取得する。
     * @return この主体に許可された操作
     */
    List<GrantedAuthority> authorities() {
        return authorities;
    }
}
