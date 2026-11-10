package dev.template.application.feature.internal.usecase;

import dev.template.application.common.Result;
import dev.template.application.feature.api.command.CreateFeatureParam;
import dev.template.application.feature.api.command.CreateFeaturesParam;
import dev.template.application.feature.api.command.FeatureCommandFailure;
import dev.template.application.feature.api.command.FeatureCommandFailure.Issue;
import dev.template.application.feature.api.command.FeatureCommandFailure.Reason;
import dev.template.application.feature.api.model.FeatureStatus;
import dev.template.application.feature.internal.domain.Feature;
import dev.template.application.feature.internal.domain.FeatureName;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/** 複数Featureの登録と再送結果の一貫性を保持する。 */
@Service
public final class CreateFeaturesUseCase {
    /** 更新・登録時刻の供給元。 */
    private final Clock clock;
    /** UUIDの供給元。 */
    private final Supplier<UUID> ids;
    /** 書き込みPort。 */
    private final FeatureRepository repository;

    /**
     * 必要なPortと時刻・IDの供給元を受け取る。
     * @param repository 書き込みPort
     * @param clock 時刻
     * @param ids UUID
     */
    public CreateFeaturesUseCase(final FeatureRepository repository, final Clock clock, final Supplier<UUID> ids) {
        this.repository = repository;
        this.clock = clock;
        this.ids = ids;
    }

    /**
     * 長さ付きUTF-8値と状態から、入力順を含めた再送比較値を生成する。
     * @param rows 登録行
     * @return SHA-256の16進表現
     */
    private static String fingerprint(final List<CreateFeatureParam> rows) {
        try {
            final var digest = MessageDigest.getInstance("SHA-256");
            for (final var row : rows) {
                final byte[] name = row.name().getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(name.length).array());
                digest.update(name);
                digest.update((byte) (row.status() == FeatureStatus.ACTIVE ? 1 : 0));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the JDK", exception);
        }
    }

    /**
     * 内容を検証済みの登録を実行し、再送には以前の結果を返す。
     * @param param 登録操作
     * @return 登録IDまたはキーの内容不一致
     */
    public Result<List<UUID>, FeatureCommandFailure> execute(final CreateFeaturesParam param) {
        final String hash = fingerprint(param.rows());
        final var reservation = repository.reserveCreation(param.actor(), param.requestId(), hash);
        if (!reservation.hash().equals(hash)) {
            return new Result.Failure<>(
                new FeatureCommandFailure(List.of(new Issue(0, "requestId", Reason.REPLAY_MISMATCH))));
        }
        if (!reservation.fresh()) {
            return new Result.Success<>(reservation.ids());
        }
        final var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        final List<UUID> created = new ArrayList<>();
        for (final var row : param.rows()) {
            final var feature = new Feature(ids.get(), new FeatureName(row.name()),
                row.status() == FeatureStatus.ACTIVE, now, now, 1);
            repository.insert(feature);
            created.add(feature.id());
        }
        repository.completeCreation(param.actor(), param.requestId(), created);
        return new Result.Success<>(List.copyOf(created));
    }

}
