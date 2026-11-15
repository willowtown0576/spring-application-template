package dev.template.application.feature.internal.query;

import dev.template.application.feature.api.authorization.FeatureAuthority;
import dev.template.application.feature.api.authorization.RequiresFeatureAuthority;
import dev.template.application.feature.api.model.FeatureStatus;
import dev.template.application.feature.api.query.FeatureDetails;
import dev.template.application.feature.api.query.FeatureQueries;
import dev.template.application.feature.api.query.FeatureSearchCursor;
import dev.template.application.feature.api.query.FeatureSearchPage;
import dev.template.application.feature.api.query.FeatureSortField;
import dev.template.application.feature.api.query.FindFeatureByIdParam;
import dev.template.application.feature.api.query.SearchFeaturesParam;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 認可・read-only transaction内でSQL参照結果を公開契約へ変換する。 */
@Service
class FeatureQueriesImpl implements FeatureQueries {
    /** 参照SQL用Port。 */
    private final FeatureDataSource source;

    /**
     * 参照用Portを受け取る。
     * @param source 参照Port
     */
    FeatureQueriesImpl(final FeatureDataSource source) {
        this.source = source;
    }

    /**
     * 公開行を次の境界値へ変換する。
     * @param item 参照行
     * @return 境界値
     */
    private static FeatureSearchCursor boundary(final FeatureDetails item) {
        return new FeatureSearchCursor(item.id(), item.name(), item.updatedAt());
    }

    /**
     * 内部データを公開read modelへ変換する。
     * @param data 内部参照行
     * @return 公開結果
     */
    private static FeatureDetails result(final FeatureDataSource.FeatureData data) {
        return new FeatureDetails(data.id(), data.name(), data.active() ? FeatureStatus.ACTIVE : FeatureStatus.INACTIVE,
            data.updatedAt(), data.version());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    @RequiresFeatureAuthority(FeatureAuthority.READ)
    public Optional<FeatureDetails> findById(final FindFeatureByIdParam param) {
        return source.findById(Objects.requireNonNull(param).id()).map(FeatureQueriesImpl::result);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    @RequiresFeatureAuthority(FeatureAuthority.READ)
    public FeatureSearchPage search(final SearchFeaturesParam param) {
        Objects.requireNonNull(param);
        final var cursor = param.cursor();
        final var position = cursor == null
            ? null
            : new FeatureDataSource.Position(cursor.id(), cursor.name(), cursor.updatedAt());
        final Boolean active = param.status() == null ? null : param.status() == FeatureStatus.ACTIVE;
        final var rows = source.search(new FeatureDataSource.Search(param.name(), active,
            param.sort() == FeatureSortField.NAME, param.descending(), param.size(), position, param.previous()));
        final boolean more = rows.size() > param.size();
        final var items = new ArrayList<>(
            rows.subList(0, Math.min(rows.size(), param.size())).stream().map(FeatureQueriesImpl::result).toList());
        if (param.previous()) {
            Collections.reverse(items);
        }
        final boolean hasPrevious = param.previous() ? more : cursor != null;
        final boolean hasNext = param.previous() ? cursor != null : more;
        return new FeatureSearchPage(items, hasPrevious && !items.isEmpty() ? boundary(items.getFirst()) : null,
            hasNext && !items.isEmpty() ? boundary(items.getLast()) : null);
    }
}
