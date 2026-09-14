package dev.template.application.feature.internal.query;

import dev.template.application.feature.api.query.FeatureView;
import java.util.Optional;
import java.util.UUID;

public interface FeatureDataSource {
  Optional<FeatureView> find(UUID id);
}
