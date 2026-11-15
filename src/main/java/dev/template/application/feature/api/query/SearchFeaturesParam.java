package dev.template.application.feature.api.query;

import dev.template.application.feature.api.model.FeatureStatus;
import java.util.Objects;

/**
 * 条件付きcursor検索。件数制限はUIとRESTに共通とする。
 * @param name 名前の部分一致。空なら条件なし
 * @param status 状態。nullなら条件なし
 * @param sort 単一の並び項目
 * @param descending 降順か
 * @param size 1〜100件
 * @param cursor 境界。先頭はnull
 * @param previous 境界の前ページを取得するか
 */
public record SearchFeaturesParam(String name, FeatureStatus status, FeatureSortField sort, boolean descending,
    int size, FeatureSearchCursor cursor, boolean previous) {
    /**
     * 共通検索条件を正規化し、取得件数と名前の長さを制限する。
     * @param name 名前条件
     * @param status 状態条件
     * @param sort 並び項目
     * @param descending 降順か
     * @param size 件数
     * @param cursor 境界
     * @param previous 前ページか
     */
    public SearchFeaturesParam {
        name = name == null ? "" : name.strip();
        Objects.requireNonNull(sort);
        if (name.codePointCount(0, name.length()) > 100
            || name.codePoints().anyMatch(c -> c == 0 || (c >= 0xD800 && c <= 0xDFFF)) || size < 1 || size > 100
            || (previous && cursor == null)) {
            throw new IllegalArgumentException("Invalid search conditions");
        }
    }
}
