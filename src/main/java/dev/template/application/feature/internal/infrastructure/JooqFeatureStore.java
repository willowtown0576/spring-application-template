package dev.template.application.feature.internal.infrastructure;

import static dev.template.application.jooq.feature.tables.Feature.FEATURE_;

import dev.template.application.feature.api.query.FeatureView;
import dev.template.application.feature.internal.domain.Feature;
import dev.template.application.feature.internal.query.FeatureDataSource;
import dev.template.application.feature.internal.usecase.FeatureRepository;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
class JooqFeatureStore implements FeatureRepository, FeatureDataSource {
  private final DSLContext sql;

  JooqFeatureStore(DSLContext sql) {
    this.sql = sql;
  }

  @Override
  public void insert(Feature feature) {
    sql.insertInto(FEATURE_)
        .set(FEATURE_.ID, feature.id())
        .set(FEATURE_.NAME, feature.name().value())
        .execute();
  }

  @Override
  public Optional<FeatureView> find(UUID id) {
    return sql.select(FEATURE_.ID, FEATURE_.NAME)
        .from(FEATURE_)
        .where(FEATURE_.ID.eq(id))
        .fetchOptional(row -> new FeatureView(row.value1(), row.value2()));
  }
}
