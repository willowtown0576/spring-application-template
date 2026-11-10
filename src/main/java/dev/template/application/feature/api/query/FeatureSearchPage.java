package dev.template.application.feature.api.query;

import java.util.List;

/**
 * 総件数を取得しないcursorページ。
 * @param items 表示順の行
 * @param previous 前ページ境界。nullなら前ページなし
 * @param next 次ページ境界。nullなら次ページなし
 */
public record FeatureSearchPage(List<FeatureDetails> items, FeatureSearchCursor previous, FeatureSearchCursor next) {
    /**
     * 参照結果を不変にする。
     * @param items 参照行
     * @param previous 前境界
     * @param next 次境界
     */
    public FeatureSearchPage {
        items = List.copyOf(items);
    }
}
