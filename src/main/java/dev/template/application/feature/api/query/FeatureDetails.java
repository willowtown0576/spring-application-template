package dev.template.application.feature.api.query;

import dev.template.application.feature.api.model.FeatureStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 画面とRESTで共有する参照結果。登録日時は公開しない。
 * @param id 内部識別子
 * @param name 名前
 * @param status 有効状態
 * @param updatedAt 更新日時
 * @param version 期待version
 */
public record FeatureDetails(UUID id, String name, FeatureStatus status, Instant updatedAt, long version) {
    /**
     * 非nullの参照契約を検証する。
     * @param id ID
     * @param name 名前
     * @param status 状態
     * @param updatedAt 更新日時
     * @param version version
     */
    public FeatureDetails {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        Objects.requireNonNull(status);
        Objects.requireNonNull(updatedAt);
    }
}
