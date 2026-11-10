package dev.template.application.web;

import dev.template.application.feature.api.model.FeatureStatus;
import dev.template.application.feature.api.query.FeatureSearchCursor;
import dev.template.application.feature.api.query.FeatureSortField;
import dev.template.application.feature.api.query.SearchFeaturesParam;
import java.util.Base64;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

/** 条件に結び付いたページ境界をHTTP用文字列へ変換する。認可は公開APIが担当する。 */
@Component
public final class FeatureSearchCursorCodec {
    /** Boot管理のJSON変換。 */
    private final JsonMapper mapper;

    /**
     * JSON変換を受け取る。
     * @param mapper JSON変換
     */
    public FeatureSearchCursorCodec(final JsonMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * ページ境界を符号化する。
     * @param position 境界。nullはページなし
     * @param search 検索条件
     * @return token。境界なしならnull
     */
    public String encode(final FeatureSearchCursor position, final SearchFeaturesParam search) {
        return position == null
            ? null
            : Base64.getUrlEncoder().withoutPadding().encodeToString(mapper.writeValueAsBytes(new Token(1,
                search.name(), search.status(), search.sort(), search.descending(), search.size(), position)));
    }

    /**
     * tokenを検証して検索条件を作る。
     * @param name 名前条件
     * @param status 状態条件
     * @param sort ソート対象
     * @param descending 降順か
     * @param size ページサイズ
     * @param token ページ境界
     * @param previous 前ページか
     * @return 検証済み検索条件
     */
    public SearchFeaturesParam search(final String name, final FeatureStatus status, final FeatureSortField sort,
        final boolean descending, final int size, final String token, final boolean previous) {
        try {
            final var base = new SearchFeaturesParam(name, status, sort, descending, size, null, false);
            if (token == null || token.isEmpty()) {
                if (previous) {
                    throw new IllegalArgumentException("前ページの境界が必要です。");
                }
                return base;
            }
            if (token.length() > 2048) {
                throw new IllegalArgumentException("ページ境界が長すぎます。");
            }
            final var decoded = mapper.readValue(Base64.getUrlDecoder().decode(token), Token.class);
            if (decoded.version() != 1 || !Objects.equals(base.name(), decoded.name())
                || base.status() != decoded.status() || base.sort() != decoded.sort()
                || descending != decoded.descending() || size != decoded.size() || decoded.position() == null) {
                throw new IllegalArgumentException("検索条件が一致しません。");
            }
            return new SearchFeaturesParam(base.name(), status, sort, descending, size, decoded.position(), previous);
        } catch (final RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid search cursor or conditions.",
                exception);
        }
    }

    /**
     * HTTP境界のversion付き表現。
     * @param version token形式
     * @param name 名前条件
     * @param status 状態条件
     * @param sort ソート対象
     * @param descending 降順か
     * @param size ページサイズ
     * @param position 境界
     */
    private record Token(int version, String name, FeatureStatus status, FeatureSortField sort, boolean descending,
        int size, FeatureSearchCursor position) {
    }
}
