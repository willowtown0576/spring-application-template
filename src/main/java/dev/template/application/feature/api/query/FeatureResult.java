package dev.template.application.feature.api.query;

import java.util.Objects;
import java.util.UUID;

/**
 * Featureの参照結果。画面専用モデルでもdomain Entityでもない。
 *
 * @param id Featureの識別子
 * @param name 登録済みの名前
 */
public record FeatureResult(UUID id, String name) {
    /**
     * 参照結果の非null契約を検証する。
     *
     * @param id 識別子
     * @param name 名前
     */
    public FeatureResult {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
    }
}
