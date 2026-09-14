package dev.template.application.feature.internal.query;

import dev.template.application.feature.api.query.FeatureQueries;
import dev.template.application.feature.api.query.FeatureView;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DefaultFeatureQueries implements FeatureQueries {
  private final FeatureDataSource source;

  DefaultFeatureQueries(FeatureDataSource source) {
    this.source = source;
  }

  @Override
  @Transactional(readOnly = true)
  @PreAuthorize("hasAuthority('" + FeatureQueries.READ + "')")
  public Optional<FeatureView> find(UUID id) {
    return source.find(Objects.requireNonNull(id));
  }
}
