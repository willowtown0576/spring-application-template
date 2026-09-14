package dev.template.application.feature.internal.domain;

public record FeatureName(String value) {
  public FeatureName {
    if (!isValid(value)) {
      throw new IllegalArgumentException("Invalid feature name");
    }
  }

  public static boolean isValid(String value) {
    return value != null
        && !value.isEmpty()
        && value.codePointCount(0, value.length()) <= 100
        && value.codePoints().anyMatch(c -> c != ' ')
        && value.codePoints().noneMatch(c -> c == 0 || (c >= 0xD800 && c <= 0xDFFF));
  }
}
