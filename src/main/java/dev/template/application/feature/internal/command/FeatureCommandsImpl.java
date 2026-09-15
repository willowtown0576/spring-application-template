package dev.template.application.feature.internal.command;

import dev.template.application.common.Result;
import dev.template.application.feature.api.authorization.FeatureAuthority;
import dev.template.application.feature.api.authorization.RequiresFeatureAuthority;
import dev.template.application.feature.api.command.CreateFailure;
import dev.template.application.feature.api.command.CreateFeatureParam;
import dev.template.application.feature.api.command.FeatureCommands;
import dev.template.application.feature.internal.domain.FeatureName;
import dev.template.application.feature.internal.usecase.CreateFeatureUseCase;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Commandの認可・入力検証・トランザクション境界。業務調整はUseCaseに委譲する。 */
@Service
class FeatureCommandsImpl implements FeatureCommands {
    private final CreateFeatureUseCase useCase;

    /**
     * 実装が必要とする依存を受け取る。
     *
     * @param useCase 作成処理の調整役
     */
    FeatureCommandsImpl(final CreateFeatureUseCase useCase) {
        this.useCase = useCase;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    @RequiresFeatureAuthority(FeatureAuthority.WRITE)
    public Result<UUID, CreateFailure> create(final CreateFeatureParam param) {
        final var name = Objects.requireNonNull(param).name();
        if (!FeatureName.isValid(name)) {
            return new Result.Failure<>(CreateFailure.INVALID_NAME);
        }
        return new Result.Success<>(useCase.execute(new FeatureName(name)));
    }
}
