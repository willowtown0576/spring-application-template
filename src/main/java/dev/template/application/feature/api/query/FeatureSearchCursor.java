package dev.template.application.feature.api.query;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 順序を一意にする境界値。IDは画面表示しない。
 * @param id 補助順序キー
 * @param name 名前順の境界値
 * @param updatedAt 更新日時順の境界値
 */
public record FeatureSearchCursor(UUID id, String name, Instant updatedAt) {
    /**
     * 境界値の必須項目を検証する。
     * @param id ID
     * @param name 名前
     * @param updatedAt 更新日時
     */
    public FeatureSearchCursor {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        Objects.requireNonNull(updatedAt);
        if (name.codePointCount(0, name.length()) > 100
            || name.codePoints().anyMatch(c -> c == 0 || (c >= 0xD800 && c <= 0xDFFF))) {
            throw new IllegalArgumentException("Invalid cursor");
        }
    }
}
