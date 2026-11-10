package dev.template.application.feature.internal.usecase;

import dev.template.application.common.Result;
import dev.template.application.feature.api.command.FeatureCommandFailure;
import dev.template.application.feature.api.command.FeatureCommandFailure.Issue;
import dev.template.application.feature.api.command.FeatureCommandFailure.Reason;
import dev.template.application.feature.api.command.UpdateFeatureParam;
import dev.template.application.feature.api.model.FeatureStatus;
import dev.template.application.feature.internal.domain.Feature;
import dev.template.application.feature.internal.domain.FeatureName;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** 単件・複数のFeatureを全件検査し、変更行を一つのtransactionで更新する。 */
@Service
public final class UpdateFeaturesUseCase {
    /** 全変更行に共通の時刻。 */
    private final Clock clock;
    /** 書き込みPort。 */
    private final FeatureRepository repository;

    /**
     * 書き込みと時刻の供給元を受け取る。
     * @param repository 書き込みPort
     * @param clock 時刻
     */
    public UpdateFeaturesUseCase(final FeatureRepository repository, final Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * 対象をlockし、全件検査後に変更行だけ更新する。
     * @param rows 検証済み更新入力
     * @return 変更したIDまたは書き込み前の業務拒否
     */
    public Result<List<UUID>, FeatureCommandFailure> execute(final List<UpdateFeatureParam> rows) {
        final var locked = repository.lock(rows.stream().map(UpdateFeatureParam::id).toList()).stream()
            .collect(Collectors.toMap(Feature::id, feature -> feature));
        final List<Issue> issues = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            final var row = rows.get(index);
            final var existing = locked.get(row.id());
            if (existing == null) {
                issues.add(new Issue(index + 1, "id", Reason.NOT_FOUND));
            } else if (existing.version() != row.version()) {
                issues.add(new Issue(index + 1, "version", Reason.CONFLICT));
            }
        }
        if (!issues.isEmpty()) {
            return new Result.Failure<>(new FeatureCommandFailure(issues));
        }
        // 全ての変更行で同じ時刻を使い、clockの巻き戻りでも日時制約を守る。
        final var now = locked.values().stream().map(Feature::updatedAt).reduce(
            clock.instant().truncatedTo(ChronoUnit.MICROS), (left, right) -> left.isAfter(right) ? left : right);
        final List<UUID> updated = new ArrayList<>();
        for (final var row : rows) {
            final var existing = locked.get(row.id());
            final var revised = existing.revise(new FeatureName(row.name()), row.status() == FeatureStatus.ACTIVE, now);
            if (revised != existing) {
                repository.update(revised, existing.version());
                updated.add(row.id());
            }
        }
        return new Result.Success<>(List.copyOf(updated));
    }
}
