package dev.template.application.feature.internal.infrastructure;

import static dev.template.application.jooq.feature.tables.Feature.FEATURE_;
import static dev.template.application.jooq.feature.tables.FeatureHistory.FEATURE_HISTORY;
import static dev.template.application.jooq.feature.tables.RegistrationRequest.REGISTRATION_REQUEST;

import dev.template.application.feature.internal.domain.Feature;
import dev.template.application.feature.internal.domain.FeatureName;
import dev.template.application.feature.internal.usecase.FeatureRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
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
    public void archiveAndDelete(final Feature feature, final String actor, final Instant deletedAt) {
        sql.insertInto(FEATURE_HISTORY).set(FEATURE_HISTORY.ID, feature.id())
            .set(FEATURE_HISTORY.NAME, feature.name().value())
            .set(FEATURE_HISTORY.STATUS, feature.active() ? "ACTIVE" : "INACTIVE")
            .set(FEATURE_HISTORY.CREATED_AT, feature.createdAt().atOffset(ZoneOffset.UTC))
            .set(FEATURE_HISTORY.UPDATED_AT, feature.updatedAt().atOffset(ZoneOffset.UTC))
            .set(FEATURE_HISTORY.VERSION, feature.version())
            .set(FEATURE_HISTORY.DELETED_AT, deletedAt.atOffset(ZoneOffset.UTC)).set(FEATURE_HISTORY.DELETED_BY, actor)
            .execute();
        final int count = sql.deleteFrom(FEATURE_)
            .where(FEATURE_.ID.eq(feature.id()).and(FEATURE_.VERSION.eq(feature.version()))).execute();
        if (count != 1) {
            throw new IllegalStateException("削除対象のversionが一致しません。");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void completeCreation(final String actor, final UUID requestId, final List<UUID> ids) {
        final int count = sql.update(REGISTRATION_REQUEST)
            .set(REGISTRATION_REQUEST.RESULT_IDS, ids.toArray(UUID[]::new))
            .where(REGISTRATION_REQUEST.ACTOR.eq(actor).and(REGISTRATION_REQUEST.REQUEST_ID.eq(requestId))).execute();
        if (count != 1) {
            throw new IllegalStateException("登録操作キーが見つかりません。");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void insert(final Feature feature) {
        sql.insertInto(FEATURE_).set(FEATURE_.ID, feature.id()).set(FEATURE_.NAME, feature.name().value())
            .set(FEATURE_.STATUS, feature.active() ? "ACTIVE" : "INACTIVE")
            .set(FEATURE_.CREATED_AT, feature.createdAt().atOffset(ZoneOffset.UTC))
            .set(FEATURE_.UPDATED_AT, feature.updatedAt().atOffset(ZoneOffset.UTC))
            .set(FEATURE_.VERSION, feature.version()).execute();
    }

    /** {@inheritDoc} */
    @Override
    public List<Feature> lock(final List<UUID> ids) {
        return sql.selectFrom(FEATURE_).where(FEATURE_.ID.in(ids)).orderBy(FEATURE_.ID).forUpdate()
            .fetch(row -> new Feature(row.getId(), new FeatureName(row.getName()), "ACTIVE".equals(row.getStatus()),
                row.getCreatedAt().toInstant(), row.getUpdatedAt().toInstant(), row.getVersion()));
    }

    /** {@inheritDoc} */
    @Override
    public CreationReservation reserveCreation(final String actor, final UUID requestId, final String hash) {
        final int count = sql.insertInto(REGISTRATION_REQUEST).set(REGISTRATION_REQUEST.ACTOR, actor)
            .set(REGISTRATION_REQUEST.REQUEST_ID, requestId).set(REGISTRATION_REQUEST.PAYLOAD_HASH, hash)
            .onConflict(REGISTRATION_REQUEST.ACTOR, REGISTRATION_REQUEST.REQUEST_ID).doNothing().execute();
        if (count == 1) {
            return new CreationReservation(true, hash, List.of());
        }
        return sql.selectFrom(REGISTRATION_REQUEST)
            .where(REGISTRATION_REQUEST.ACTOR.eq(actor).and(REGISTRATION_REQUEST.REQUEST_ID.eq(requestId)))
            .fetchSingle(row -> new CreationReservation(false, row.getPayloadHash(),
                List.copyOf(Arrays.asList(row.getResultIds()))));
    }

    /** {@inheritDoc} */
    @Override
    public void update(final Feature feature, final long expectedVersion) {
        final int count = sql.update(FEATURE_).set(FEATURE_.NAME, feature.name().value())
            .set(FEATURE_.STATUS, feature.active() ? "ACTIVE" : "INACTIVE")
            .set(FEATURE_.UPDATED_AT, feature.updatedAt().atOffset(ZoneOffset.UTC))
            .set(FEATURE_.VERSION, feature.version())
            .where(FEATURE_.ID.eq(feature.id()).and(FEATURE_.VERSION.eq(expectedVersion))).execute();
        if (count != 1) {
            throw new IllegalStateException("更新対象のversionが一致しません。");
        }
    }
}
