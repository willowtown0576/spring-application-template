package dev.template.application.security;

import org.springframework.security.core.GrantedAuthority;

/** 運用endpointとAPI資料の参照権限。業務権限は各featureが所有する。 */
public enum OperationsAuthority implements GrantedAuthority {
    /** health以外の公開Actuator endpointとAPI資料を参照できる。 */
    READ;

    /** {@inheritDoc} */
    @Override
    public String getAuthority() {
        return "ops:read";
    }
}
