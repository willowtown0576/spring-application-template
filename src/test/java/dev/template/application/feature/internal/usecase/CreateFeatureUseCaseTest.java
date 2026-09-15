package dev.template.application.feature.internal.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.template.application.feature.internal.domain.Feature;
import dev.template.application.feature.internal.domain.FeatureName;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** PortとID生成関数を注入したUseCaseを直接実行し、作成処理を検証する。 */
class CreateFeatureUseCaseTest {
    /** 固定ID生成関数と記録用Repositoryを注入し、生成IDと検証済み名前が一度だけ保存されることを検証する。 */
    @Test
    void assignsIdAndStoresDomainValue() {
        final var saved = new ArrayList<Feature>();
        final UUID id = UUID.fromString("0199417c-0000-7000-8000-000000000001");
        final var name = new FeatureName("name");
        final var useCase = new CreateFeatureUseCase(saved::add, () -> id);
        assertThat(useCase.execute(name)).isEqualTo(id);
        assertThat(saved).containsExactly(new Feature(id, name));
    }

    /** Repositoryが技術例外を送出したとき、UseCaseが同じ例外を呼び出し元へ伝播することを検証する。 */
    @Test
    void propagatesTechnicalFailureWithoutWrapping() {
        final var failure = new IllegalStateException("storage unavailable");
        final var useCase = new CreateFeatureUseCase(feature -> {
            throw failure;
        }, UUID::randomUUID);
        assertThatThrownBy(() -> useCase.execute(new FeatureName("name"))).isSameAs(failure);
    }
}
