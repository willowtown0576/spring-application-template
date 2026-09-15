package dev.template.application.feature.api.query;

import java.util.Optional;

/**
 * Featureの参照契約を定義する公開API。
 *
 * <p>結果は読み取り専用のResult recordで返す。表示文言やUI部品の状態はweb adapterで付与する。
 */
public interface FeatureQueries {

    /**
     * IDに一致するFeatureを取得する。
     *
     * @param param 必須の検索条件
     * @return 検索結果。未検出はempty、技術障害は例外として伝播する
     * @throws NullPointerException paramがnullの場合
     */
    Optional<FeatureResult> find(FindFeatureParam param);
}
