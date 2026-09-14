package dev.template.application.feature.internal.usecase;

import dev.template.application.feature.internal.domain.Feature;

public interface FeatureRepository {
  void insert(Feature feature);
}
