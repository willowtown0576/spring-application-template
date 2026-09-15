package dev.template.application.feature.internal.query;

import java.util.Optional;
import java.util.UUID;

/** Queryが必要とするread modelを直接取得するPort。 */
public interface FeatureDataSource {
    /**
     * IDで参照結果を取得する。
     *
     * @param id 検索する識別子
     * @return 該当する結果。未登録の場合はempty
     */
    Optional<FeatureData> find(UUID id);

    /**
     * 永続化adapterから取得した内部参照データ。
     *
     * @param id 識別子
     * @param name 登録名
     */
    record FeatureData(UUID id, String name) {
    }
}
