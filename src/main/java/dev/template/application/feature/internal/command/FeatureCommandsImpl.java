package dev.template.application.feature.internal.command;

import dev.template.application.common.Result;
import dev.template.application.feature.api.authorization.FeatureAuthority;
import dev.template.application.feature.api.authorization.RequiresFeatureAuthority;
import dev.template.application.feature.api.command.CreateFeatureParam;
import dev.template.application.feature.api.command.CreateFeaturesParam;
import dev.template.application.feature.api.command.DeleteFeatureParam;
import dev.template.application.feature.api.command.DeleteFeaturesParam;
import dev.template.application.feature.api.command.FeatureCommandFailure;
import dev.template.application.feature.api.command.FeatureCommandFailure.Issue;
import dev.template.application.feature.api.command.FeatureCommandFailure.Reason;
import dev.template.application.feature.api.command.FeatureCommands;
import dev.template.application.feature.api.command.UpdateFeatureParam;
import dev.template.application.feature.api.command.UpdateFeaturesParam;
import dev.template.application.feature.api.model.FeatureStatus;
import dev.template.application.feature.internal.domain.FeatureName;
import dev.template.application.feature.internal.usecase.CreateFeatureUseCase;
import dev.template.application.feature.internal.usecase.CreateFeaturesUseCase;
import dev.template.application.feature.internal.usecase.DeleteFeaturesUseCase;
import dev.template.application.feature.internal.usecase.UpdateFeaturesUseCase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 公開Commandの認可・transactionと、全件書き込み前の入力検証を担当する。 */
@Service
class FeatureCommandsImpl implements FeatureCommands {
    /** 単件登録。 */
    private final CreateFeatureUseCase createFeature;
    /** 複数登録と再送制御。 */
    private final CreateFeaturesUseCase createFeatures;
    /** 履歴保存を伴う削除。 */
    private final DeleteFeaturesUseCase deleteFeatures;
    /** 全件検査を伴う更新。 */
    private final UpdateFeaturesUseCase updateFeatures;

    /**
     * 操作に対応するUseCaseを受け取る。
     * @param createFeature 単件登録
     * @param createFeatures 複数登録
     * @param updateFeatures 単件・複数更新
     * @param deleteFeatures 履歴保存・削除
     */
    FeatureCommandsImpl(final CreateFeatureUseCase createFeature, final CreateFeaturesUseCase createFeatures,
        final UpdateFeaturesUseCase updateFeatures, final DeleteFeaturesUseCase deleteFeatures) {
        this.deleteFeatures = deleteFeatures;
        this.createFeature = createFeature;
        this.createFeatures = createFeatures;
        this.updateFeatures = updateFeatures;
    }

    /**
     * 登録と更新に共通の項目を検証する。
     * @param row 行番号
     * @param name 名前
     * @param status 状態
     * @param issues エラー追加先
     */
    private static void validate(final int row, final String name, final FeatureStatus status,
        final List<Issue> issues) {
        if (!FeatureName.isValid(name)) {
            issues.add(new Issue(row, "name", Reason.INVALID_NAME));
        }
        if (status == null) {
            issues.add(new Issue(row, "status", Reason.INVALID_STATUS));
        }
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    @RequiresFeatureAuthority(FeatureAuthority.WRITE)
    public Result<UUID, FeatureCommandFailure> create(final CreateFeatureParam param) {
        Objects.requireNonNull(param);
        final List<Issue> issues = new ArrayList<>();
        validate(1, param.name(), param.status(), issues);
        return issues.isEmpty()
            ? new Result.Success<>(
                createFeature.execute(new FeatureName(param.name()), param.status() == FeatureStatus.ACTIVE))
            : new Result.Failure<>(new FeatureCommandFailure(issues));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    @RequiresFeatureAuthority(FeatureAuthority.WRITE)
    public Result<List<UUID>, FeatureCommandFailure> createMany(final CreateFeaturesParam param) {
        Objects.requireNonNull(param);
        final List<Issue> issues = new ArrayList<>();
        if (param.actor() == null || param.actor().isBlank() || param.requestId() == null) {
            issues.add(new Issue(0, "requestId", Reason.INVALID_REQUEST));
        }
        if (param.rows().isEmpty() || param.rows().size() > 100) {
            issues.add(new Issue(0, "rows", Reason.INVALID_REQUEST));
        } else {
            for (int index = 0; index < param.rows().size(); index++) {
                final var row = param.rows().get(index);
                validate(index + 1, row == null ? null : row.name(), row == null ? null : row.status(), issues);
            }
        }
        return issues.isEmpty()
            ? createFeatures.execute(param)
            : new Result.Failure<>(new FeatureCommandFailure(issues));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    @RequiresFeatureAuthority(FeatureAuthority.WRITE)
    public Result<UUID, FeatureCommandFailure> delete(final DeleteFeatureParam param) {
        Objects.requireNonNull(param);
        return switch (deleteRows(new DeleteFeaturesParam(param.actor(), Collections.singletonList(param.target())))) {
            case Result.Success(var ids) -> new Result.Success<>(ids.getFirst());
            case Result.Failure(var failure) -> new Result.Failure<>(failure);
        };
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    @RequiresFeatureAuthority(FeatureAuthority.WRITE)
    public Result<List<UUID>, FeatureCommandFailure> deleteMany(final DeleteFeaturesParam param) {
        return deleteRows(Objects.requireNonNull(param));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    @RequiresFeatureAuthority(FeatureAuthority.WRITE)
    public Result<UUID, FeatureCommandFailure> update(final UpdateFeatureParam param) {
        Objects.requireNonNull(param);
        return switch (updateRows(List.of(param))) {
            case Result.Success<?, ?> _ -> new Result.Success<>(param.id());
            case Result.Failure(var failure) -> new Result.Failure<>(failure);
        };
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    @RequiresFeatureAuthority(FeatureAuthority.WRITE)
    public Result<List<UUID>, FeatureCommandFailure> updateMany(final UpdateFeaturesParam param) {
        return updateRows(Objects.requireNonNull(param).rows());
    }

    /**
     * 実行主体と全対象を、書き込み前に検証する。
     * @param param 削除操作
     * @return 削除結果または入力拒否
     */
    private Result<List<UUID>, FeatureCommandFailure> deleteRows(final DeleteFeaturesParam param) {
        final List<Issue> issues = new ArrayList<>();
        final var seen = new HashSet<UUID>();
        if (param.actor() == null || param.actor().isBlank()) {
            issues.add(new Issue(0, "actor", Reason.INVALID_REQUEST));
        }
        if (param.targets().isEmpty() || param.targets().size() > 100) {
            issues.add(new Issue(0, "targets", Reason.INVALID_REQUEST));
        } else {
            for (int index = 0; index < param.targets().size(); index++) {
                final var target = param.targets().get(index);
                if (target.id() == null) {
                    issues.add(new Issue(index + 1, "id", Reason.INVALID_REQUEST));
                } else if (!seen.add(target.id())) {
                    issues.add(new Issue(index + 1, "id", Reason.DUPLICATE_ID));
                }
                if (target.version() < 1) {
                    issues.add(new Issue(index + 1, "version", Reason.INVALID_VERSION));
                }
            }
        }
        return issues.isEmpty()
            ? deleteFeatures.execute(param)
            : new Result.Failure<>(new FeatureCommandFailure(issues));
    }

    /**
     * 共通の更新入力を検証する。呼び出し元の公開境界が認可とtransactionを持つ。
     * @param rows 更新行
     * @return 更新結果
     */
    private Result<List<UUID>, FeatureCommandFailure> updateRows(final List<UpdateFeatureParam> rows) {
        final List<Issue> issues = new ArrayList<>();
        final var seen = new HashSet<UUID>();
        if (rows.isEmpty() || rows.size() > 100) {
            issues.add(new Issue(0, "rows", Reason.INVALID_REQUEST));
        } else {
            for (int index = 0; index < rows.size(); index++) {
                final var row = rows.get(index);
                validate(index + 1, row == null ? null : row.name(), row == null ? null : row.status(), issues);
                if (row == null || row.id() == null || row.version() < 1) {
                    issues.add(new Issue(index + 1, "version", Reason.INVALID_VERSION));
                } else if (!seen.add(row.id())) {
                    issues.add(new Issue(index + 1, "id", Reason.DUPLICATE_ID));
                }
            }
        }
        return issues.isEmpty()
            ? updateFeatures.execute(List.copyOf(rows))
            : new Result.Failure<>(new FeatureCommandFailure(issues));
    }
}
