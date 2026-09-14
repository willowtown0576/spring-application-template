package dev.template.application.feature.internal.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.template.application.feature.internal.domain.Feature;
import dev.template.application.feature.internal.domain.FeatureName;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateFeatureUseCaseTest {
  @Test
  void assignsIdAndStoresDomainValue() {
    var saved = new ArrayList<Feature>();
    UUID id = UUID.fromString("0199417c-0000-7000-8000-000000000001");
    var name = new FeatureName("name");
    var useCase = new CreateFeatureUseCase(saved::add, () -> id);
    assertThat(useCase.execute(name)).isEqualTo(id);
    assertThat(saved).containsExactly(new Feature(id, name));
  }

  @Test
  void propagatesTechnicalFailureWithoutWrapping() {
    var failure = new IllegalStateException("storage unavailable");
    var useCase =
        new CreateFeatureUseCase(
            feature -> {
              throw failure;
            },
            UUID::randomUUID);
    assertThatThrownBy(() -> useCase.execute(new FeatureName("name"))).isSameAs(failure);
  }
}
