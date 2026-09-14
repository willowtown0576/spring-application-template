package dev.template.application.feature.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class FeatureNameTest {
  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "   ", "\u0000", "\uD800", "\uDC00"})
  void rejectsInvalidNames(String name) {
    assertThat(FeatureName.isValid(name)).isFalse();
    assertThatThrownBy(() -> new FeatureName(name)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void countsUnicodeCodePointsAndPreservesInput() {
    String maximum = "😀".repeat(100);
    assertThat(new FeatureName(maximum).value()).isEqualTo(maximum);
    assertThat(new FeatureName(" name ").value()).isEqualTo(" name ");
    assertThat(FeatureName.isValid(maximum + "a")).isFalse();
  }
}
