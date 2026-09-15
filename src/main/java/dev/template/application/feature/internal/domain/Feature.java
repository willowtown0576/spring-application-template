package dev.template.application.feature.internal.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Featureの不変Entity。永続化や画面の型を持たず、生成時に必須項目を保証する。
 *
 * @param id application側で生成した識別子
 * @param name 検証済みの名前
 */
public record Feature(UUID id, FeatureName name) {
    /**
     * 必須項目を検証する。
     *
     * @param id 識別子
     * @param name 名前
     */
    public Feature {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
    }
}
