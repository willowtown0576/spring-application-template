package dev.template.application.feature.internal.domain;

import java.util.Objects;
import java.util.UUID;

public record Feature(UUID id, FeatureName name) {
  public Feature {
    Objects.requireNonNull(id);
    Objects.requireNonNull(name);
  }
}
