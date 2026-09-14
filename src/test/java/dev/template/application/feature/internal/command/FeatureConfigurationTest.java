package dev.template.application.feature.internal.command;

import static org.assertj.core.api.Assertions.assertThat;

import dev.template.application.feature.internal.domain.FeatureName;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class FeatureConfigurationTest {
  @Test
  void generatesVersionSevenIdsUsingInjectedClock() {
    var instant = Instant.parse("2026-09-13T00:00:00Z");
    var useCase =
        new FeatureConfiguration()
            .createFeatureUseCase(feature -> {}, Clock.fixed(instant, ZoneOffset.UTC));
    var first = useCase.execute(new FeatureName("first"));
    var second = useCase.execute(new FeatureName("second"));
    assertThat(first.version()).isEqualTo(7);
    assertThat(first.variant()).isEqualTo(2);
    assertThat(first.getMostSignificantBits() >>> 16).isEqualTo(instant.toEpochMilli());
    assertThat(second).isNotEqualTo(first);
  }
}
