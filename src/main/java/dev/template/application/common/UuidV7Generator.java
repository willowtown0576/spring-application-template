package dev.template.application.common;

import com.github.f4b6a3.uuid.factory.standard.TimeOrderedEpochFactory;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * 全featureで使用するapplication側UUID v7生成器。
 *
 * <p>時刻は注入されたClockから取得する。UseCaseへSupplier&lt;UUID&gt;として注入する。
 */
@Component
public final class UuidV7Generator implements Supplier<UUID> {
    private final TimeOrderedEpochFactory factory;

    /**
     * 本番では共通UTC Clock、testではClock.fixedを指定する。
     *
     * @param clock UUIDへ埋め込む時刻の供給元
     */
    public UuidV7Generator(final Clock clock) {
        factory = new TimeOrderedEpochFactory(Objects.requireNonNull(clock));
    }

    /**
     * {@inheritDoc}
     *
     * <p>新しいUUID v7を生成する。業務上の連番や発生順序を必要とする場合は、その契約を別途設計する。
     *
     * @return 新しい非nullのUUID v7
     */
    @Override
    public UUID get() {
        return factory.create();
    }
}
