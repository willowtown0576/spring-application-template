package dev.template.application.feature.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Entityの更新日時・versionと変更なしの契約を検証する。 */
class FeatureTest {
    /** 不正version・日時とversionのoverflowを拒否する。 */
    @Test
    void rejectsInvalidMetadataAndVersionOverflow() {
        final Instant now = Instant.parse("2026-01-01T00:00:00Z");
        final UUID id = UUID.randomUUID();
        final var name = new FeatureName("name");
        assertThatThrownBy(() -> new Feature(id, name, true, now, now, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Feature(id, name, true, now, now.minusSeconds(1), 1))
            .isInstanceOf(IllegalArgumentException.class);
        final var feature = new Feature(id, name, true, now, now, Long.MAX_VALUE);
        assertThatThrownBy(() -> assertThat(feature.revise(name, false, now)).isNotNull())
            .isInstanceOf(ArithmeticException.class);
    }

    /** 時刻が巻き戻っても登録日時と更新日時を守り、変更なしは同じEntityを返す。 */
    @Test
    void revisesOnlyEffectiveChangesAndKeepsTimeMonotonic() {
        final Instant created = Instant.parse("2026-01-01T00:00:00Z");
        final Instant updated = created.plusSeconds(60);
        final var feature = new Feature(UUID.randomUUID(), new FeatureName("original"), true, created, updated, 2);
        assertThat(feature.revise(new FeatureName("original"), true, updated.plusSeconds(1))).isSameAs(feature);
        final var revised = feature.revise(new FeatureName("changed"), false, created);
        assertThat(revised.version()).isEqualTo(3);
        assertThat(revised.createdAt()).isEqualTo(created);
        assertThat(revised.updatedAt()).isEqualTo(updated);
        assertThat(revised.active()).isFalse();
        assertThat(revised.name()).isEqualTo(new FeatureName("changed"));
    }
}
