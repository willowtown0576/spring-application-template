package dev.template.application.feature.internal.usecase;

import dev.template.application.feature.internal.domain.Feature;

/** Commandが必要とする書き込みPort。読取用SQLはFeatureDataSourceへ分離する。 */
public interface FeatureRepository {
    /**
     * Entityを新規保存する。DB例外は呼び出し元へ伝播させrollbackする。
     *
     * @param feature 保存するEntity
     */
    void insert(Feature feature);
}
