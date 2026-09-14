package dev.template.application.feature.api.query;

import java.util.Optional;
import java.util.UUID;

public interface FeatureQueries {
  String READ = "feature:read";

  Optional<FeatureView> find(UUID id);
}
