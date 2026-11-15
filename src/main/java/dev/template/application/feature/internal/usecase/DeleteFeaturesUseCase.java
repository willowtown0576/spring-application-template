package dev.template.application.feature.internal.usecase;

import dev.template.application.common.Result;
import dev.template.application.feature.api.command.DeleteFeaturesParam;
import dev.template.application.feature.api.command.FeatureCommandFailure;
import dev.template.application.feature.api.command.FeatureCommandFailure.Issue;
import dev.template.application.feature.api.command.FeatureCommandFailure.Reason;
import dev.template.application.feature.api.command.FeatureDeletionTarget;
import dev.template.application.feature.internal.domain.Feature;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** 全対象の存在とversionを確認し、同じtransactionで履歴保存・削除を行う。 */
@Service
public final class DeleteFeaturesUseCase {
    /** 削除時刻の供給元。 */
    private final Clock clock;
    /** 履歴保存と削除のPort。 */
    private final FeatureRepository repository;

    /**
     * 履歴保存・削除と時刻の供給元を受け取る。
     * @param repository 書き込みPort
     * @param clock 時刻
     */
    public DeleteFeaturesUseCase(final FeatureRepository repository, final Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * 全件検査後にだけ履歴を保存し、元データから削除する。
     * @param param 検証済み削除操作
     * @return 削除IDまたは未検出・競合
     */
    public Result<List<UUID>, FeatureCommandFailure> execute(final DeleteFeaturesParam param) {
        final var locked = repository.lock(param.targets().stream().map(FeatureDeletionTarget::id).toList()).stream()
            .collect(Collectors.toMap(Feature::id, feature -> feature));
        final List<Issue> issues = new ArrayList<>();
        for (int index = 0; index < param.targets().size(); index++) {
            final var target = param.targets().get(index);
            final var existing = locked.get(target.id());
            if (existing == null) {
                issues.add(new Issue(index + 1, "id", Reason.NOT_FOUND));
            } else if (existing.version() != target.version()) {
                issues.add(new Issue(index + 1, "version", Reason.CONFLICT));
            }
        }
        if (!issues.isEmpty()) {
            return new Result.Failure<>(new FeatureCommandFailure(issues));
        }
        final var deletedAt = locked.values().stream().map(Feature::updatedAt).reduce(
            clock.instant().truncatedTo(ChronoUnit.MICROS), (left, right) -> left.isAfter(right) ? left : right);
        for (final var target : param.targets()) {
            repository.archiveAndDelete(locked.get(target.id()), param.actor(), deletedAt);
        }
        return new Result.Success<>(param.targets().stream().map(FeatureDeletionTarget::id).toList());
    }
}
