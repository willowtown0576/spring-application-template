package dev.template.application.feature.internal.infrastructure;

import static dev.template.application.jooq.feature.tables.Feature.FEATURE_;

import dev.template.application.feature.internal.query.FeatureDataSource;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/** jOOQによる読み取りadapter。必要な列だけ選び内部参照データへ写す。 */
@Repository
class JooqFeatureDataSource implements FeatureDataSource {
    /** Springのtransactionに参加するSQL実行context。 */
    private final DSLContext sql;

    /**
     * 実装が必要とする依存を受け取る。
     *
     * @param sql Springのトランザクションに参加するDSLContext
     */
    JooqFeatureDataSource(final DSLContext sql) {
        this.sql = sql;
    }

    /**
     * SQL行を参照データへ写す。
     * @param row SQL行
     * @return 参照データ
     */
    private static FeatureData data(final Record row) {
        return new FeatureData(row.get(FEATURE_.ID), row.get(FEATURE_.NAME), "ACTIVE".equals(row.get(FEATURE_.STATUS)),
            row.get(FEATURE_.UPDATED_AT).toInstant(), row.get(FEATURE_.VERSION));
    }

    /** {@inheritDoc} */
    @Override
    public Optional<FeatureData> findById(final UUID id) {
        return sql.selectFrom(FEATURE_).where(FEATURE_.ID.eq(id)).fetchOptional(JooqFeatureDataSource::data);
    }

    /** {@inheritDoc} */
    @Override
    public List<FeatureData> search(final Search search) {
        final Condition condition = FEATURE_.NAME.containsIgnoreCase(search.name()).and(
            search.active() == null ? DSL.noCondition() : FEATURE_.STATUS.eq(search.active() ? "ACTIVE" : "INACTIVE"));
        final boolean descending = search.descending() != search.previous();
        final var idOrder = descending ? FEATURE_.ID.desc() : FEATURE_.ID.asc();
        if (search.byName()) {
            final var query = sql.selectFrom(FEATURE_).where(condition)
                .orderBy(descending ? FEATURE_.NAME.desc() : FEATURE_.NAME.asc(), idOrder);
            return search.position() == null
                ? query.limit(search.size() + 1).fetch(JooqFeatureDataSource::data)
                : query.seek(search.position().name(), search.position().id()).limit(search.size() + 1)
                    .fetch(JooqFeatureDataSource::data);
        }
        final var query = sql.selectFrom(FEATURE_).where(condition)
            .orderBy(descending ? FEATURE_.UPDATED_AT.desc() : FEATURE_.UPDATED_AT.asc(), idOrder);
        return search.position() == null
            ? query.limit(search.size() + 1).fetch(JooqFeatureDataSource::data)
            : query.seek(search.position().updatedAt().atOffset(ZoneOffset.UTC), search.position().id())
                .limit(search.size() + 1).fetch(JooqFeatureDataSource::data);
    }
}
