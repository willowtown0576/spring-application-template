package dev.template.application.feature.internal.command;

import dev.template.application.feature.api.command.CreateFailure;
import dev.template.application.feature.api.command.FeatureCommands;
import dev.template.application.feature.api.command.Result;
import dev.template.application.feature.internal.domain.FeatureName;
import dev.template.application.feature.internal.usecase.CreateFeatureUseCase;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DefaultFeatureCommands implements FeatureCommands {
  private final CreateFeatureUseCase useCase;

  DefaultFeatureCommands(CreateFeatureUseCase useCase) {
    this.useCase = useCase;
  }

  @Override
  @Transactional
  @PreAuthorize("hasAuthority('" + FeatureCommands.WRITE + "')")
  public Result<UUID, CreateFailure> create(String name) {
    if (!FeatureName.isValid(name)) {
      return new Result.Failure<>(CreateFailure.INVALID_NAME);
    }
    return new Result.Success<>(useCase.execute(new FeatureName(name)));
  }
}
