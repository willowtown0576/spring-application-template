package dev.template.application.feature.api.query;

import java.util.Objects;
import java.util.UUID;

public record FeatureView(UUID id, String name) {
  public FeatureView {
    Objects.requireNonNull(id);
    Objects.requireNonNull(name);
  }
}
