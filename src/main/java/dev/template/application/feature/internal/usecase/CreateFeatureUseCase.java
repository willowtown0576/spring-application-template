package dev.template.application.feature.internal.usecase;

import dev.template.application.feature.internal.domain.Feature;
import dev.template.application.feature.internal.domain.FeatureName;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class CreateFeatureUseCase {
  private final FeatureRepository repository;
  private final Supplier<UUID> ids;

  public CreateFeatureUseCase(FeatureRepository repository, Supplier<UUID> ids) {
    this.repository = Objects.requireNonNull(repository);
    this.ids = Objects.requireNonNull(ids);
  }

  public UUID execute(FeatureName name) {
    var feature = new Feature(ids.get(), name);
    repository.insert(feature);
    return feature.id();
  }
}
