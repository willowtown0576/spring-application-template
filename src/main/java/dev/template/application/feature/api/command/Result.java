package dev.template.application.feature.api.command;

import java.util.Objects;

public sealed interface Result<S, F> {
  record Success<S, F>(S value) implements Result<S, F> {
    public Success {
      Objects.requireNonNull(value);
    }
  }

  record Failure<S, F>(F reason) implements Result<S, F> {
    public Failure {
      Objects.requireNonNull(reason);
    }
  }
}
