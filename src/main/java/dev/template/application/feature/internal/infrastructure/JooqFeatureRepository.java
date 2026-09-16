package dev.template.application.feature.internal.infrastructure;

import static dev.template.application.jooq.feature.tables.Feature.FEATURE_;

import dev.template.application.feature.internal.domain.Feature;
import dev.template.application.feature.internal.usecase.FeatureRepository;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/** jOOQによる書き込みadapter。所有するfeature schemaだけを更新する。 */
@Repository
class JooqFeatureRepository implements FeatureRepository {
    /** Springのtransactionに参加するSQL実行context。 */
    private final DSLContext sql;

    /**
     * 実装が必要とする依存を受け取る。
     *
     * @param sql Springのトランザクションに参加するDSLContext
     */
    JooqFeatureRepository(final DSLContext sql) {
        this.sql = sql;
    }

    /** {@inheritDoc} */
    @Override
    public void insert(final Feature feature) {
        sql.insertInto(FEATURE_).set(FEATURE_.ID, feature.id()).set(FEATURE_.NAME, feature.name().value()).execute();
    }
}
