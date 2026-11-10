package dev.template.application.feature.internal.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import dev.template.application.feature.internal.domain.Feature;
import dev.template.application.feature.internal.domain.FeatureName;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** PortとID生成関数を注入したUseCaseを直接実行し、作成処理を検証する。 */
class CreateFeatureUseCaseTest {
    /** 固定ID生成関数と記録用Repositoryを注入し、生成IDと検証済み名前が一度だけ保存されることを検証する。 */
    @Test
    void assignsIdAndStoresDomainValue() {
        final var repository = mock(FeatureRepository.class);
        final var saved = new ArrayList<Feature>();
        doAnswer(invocation -> saved.add(invocation.getArgument(0))).when(repository).insert(any());
        final var clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        final UUID id = UUID.fromString("0199417c-0000-7000-8000-000000000001");
        final var name = new FeatureName("name");
        final var useCase = new CreateFeatureUseCase(repository, () -> id, clock);
        assertThat(useCase.execute(name, true)).isEqualTo(id);
        assertThat(saved).containsExactly(new Feature(id, name, true, clock.instant(), clock.instant(), 1));
    }

    /** Repositoryが技術例外を送出したとき、UseCaseが同じ例外を呼び出し元へ伝播することを検証する。 */
    @Test
    void propagatesTechnicalFailureWithoutWrapping() {
        final var failure = new IllegalStateException("storage unavailable");
        final var repository = mock(FeatureRepository.class);
        doThrow(failure).when(repository).insert(any());
        final var useCase = new CreateFeatureUseCase(repository, UUID::randomUUID, Clock.systemUTC());
        assertThatThrownBy(() -> useCase.execute(new FeatureName("name"), true)).isSameAs(failure);
    }
}
