package dev.template.application.feature.internal.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * versionと日時の整合性を保持するFeature。
 * @param id 識別子
 * @param name 名前
 * @param active 有効状態
 * @param createdAt 登録日時
 * @param updatedAt 更新日時
 * @param version 1以上のversion
 */
public record Feature(UUID id, FeatureName name, boolean active, Instant createdAt, Instant updatedAt, long version) {
    /**
     * Entityの必須値と日時・versionの整合性を検証する。
     * @param id 識別子
     * @param name 名前
     * @param active 有効状態
     * @param createdAt 登録日時
     * @param updatedAt 更新日時
     * @param version version
     */
    public Feature {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(updatedAt);
        if (version < 1 || updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Invalid feature revision");
        }
    }

    /**
     * 値が変わる場合だけversionと更新日時を進める。
     * @param newName 検証済みの名前
     * @param newActive 有効状態
     * @param now 同じ一括処理で共通の時刻
     * @return 更新後のEntity。変更なしでは自身
     */
    public Feature revise(final FeatureName newName, final boolean newActive, final Instant now) {
        if (name.equals(newName) && active == newActive) {
            return this;
        }
        return new Feature(id, newName, newActive, createdAt, now.isBefore(updatedAt) ? updatedAt : now,
            Math.incrementExact(version));
    }
}
