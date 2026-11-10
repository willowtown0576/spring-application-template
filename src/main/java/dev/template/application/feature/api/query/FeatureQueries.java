package dev.template.application.feature.api.query;

import java.util.Optional;

/**
 * Featureの参照契約を定義する公開API。
 *
 * <p>一件の参照内容と検索ページを用途別のread modelで返す。表示文言やUI部品の状態はweb adapterで付与する。
 */
public interface FeatureQueries {

    /**
     * IDに一致するFeatureを取得する。
     *
     * @param param 必須の検索条件
     * @return 検索結果。未検出はempty、技術障害は例外として伝播する
     * @throws NullPointerException paramがnullの場合
     */
    Optional<FeatureDetails> findById(FindFeatureByIdParam param);

    /**
     * 条件とcursorで参照し、COUNTなしの前後ページ情報を返す。
     * @param param 検索条件
     * @return cursorページ
     */
    FeatureSearchPage search(SearchFeaturesParam param);
}
