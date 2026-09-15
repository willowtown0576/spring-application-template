package dev.template.application.feature.internal.infrastructure;

import static dev.template.application.jooq.feature.tables.Feature.FEATURE_;

import dev.template.application.feature.internal.query.FeatureDataSource;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/** jOOQによる読み取りadapter。必要な列だけ選び内部参照データへ写す。 */
@Repository
class JooqFeatureDataSource implements FeatureDataSource {
    private final DSLContext sql;

    /**
     * 実装が必要とする依存を受け取る。
     *
     * @param sql Springのトランザクションに参加するDSLContext
     */
    JooqFeatureDataSource(final DSLContext sql) {
        this.sql = sql;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<FeatureData> find(final UUID id) {
        return sql.select(FEATURE_.ID, FEATURE_.NAME).from(FEATURE_).where(FEATURE_.ID.eq(id))
            .fetchOptional(row -> new FeatureData(row.value1(), row.value2()));
    }
}
