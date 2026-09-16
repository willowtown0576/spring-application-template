package dev.template.application.feature.internal.query;

import dev.template.application.feature.api.authorization.FeatureAuthority;
import dev.template.application.feature.api.authorization.RequiresFeatureAuthority;
import dev.template.application.feature.api.query.FeatureQueries;
import dev.template.application.feature.api.query.FeatureResult;
import dev.template.application.feature.api.query.FindFeatureParam;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Queryの認可とread-onlyトランザクション境界。SQLに適した参照結果を返す。 */
@Service
class FeatureQueriesImpl implements FeatureQueries {
    /** Featureの内部参照データを取得する読み取りPort。 */
    private final FeatureDataSource source;

    /**
     * 実装が必要とする依存を受け取る。
     *
     * @param source 読み取りPort
     */
    FeatureQueriesImpl(final FeatureDataSource source) {
        this.source = source;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    @RequiresFeatureAuthority(FeatureAuthority.READ)
    public Optional<FeatureResult> find(final FindFeatureParam param) {
        return source.find(Objects.requireNonNull(param).id()).map(data -> new FeatureResult(data.id(), data.name()));
    }
}
