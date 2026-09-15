package dev.template.application.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Clock差し替えとUUIDのversion / 時刻 / 一意性を保証する。 */
class UuidV7GeneratorTest {
    /** 固定UTC ClockでIDを二つ生成し、UUID v7のversion、variant、時刻、一意性を検証する。 */
    @Test
    void generatesVersionSevenIdsUsingInjectedClock() {
        final var instant = Instant.parse("2026-09-13T00:00:00Z");
        final var ids = new UuidV7Generator(Clock.fixed(instant, ZoneOffset.UTC));
        final var first = ids.get();
        final var second = ids.get();
        assertThat(first.version()).isEqualTo(7);
        assertThat(first.variant()).isEqualTo(2);
        assertThat(first.getMostSignificantBits() >>> 16).isEqualTo(instant.toEpochMilli());
        assertThat(second).isNotEqualTo(first);
    }
}
