package dev.template.application.feature.internal.usecase;

import dev.template.application.feature.internal.domain.Feature;
import dev.template.application.feature.internal.domain.FeatureName;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/** 作成処理の調整役。呼び出し元のトランザクション内でRepositoryを使う。 */
@Service
public final class CreateFeatureUseCase {
    /** 新規Featureへ割り当てるUUID v7の供給元。 */
    private final Supplier<UUID> ids;
    /** Featureを永続化する書き込みPort。 */
    private final FeatureRepository repository;

    /**
     * 必要なPortとID生成関数を受け取る。テストではどちらも差し替えられる。
     *
     * @param repository 書き込みPort
     * @param ids UUID v7を生成する関数
     */
    public CreateFeatureUseCase(final FeatureRepository repository, final Supplier<UUID> ids) {
        this.repository = Objects.requireNonNull(repository);
        this.ids = Objects.requireNonNull(ids);
    }

    /**
     * 検証済みの名前からEntityを作り、一度だけ保存する。
     *
     * @param name 検証済みの名前
     * @return 保存したEntityのID
     */
    public UUID execute(final FeatureName name) {
        final var feature = new Feature(ids.get(), name);
        repository.insert(feature);
        return feature.id();
    }
}
