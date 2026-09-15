package dev.template.application.feature.api.query;

import java.util.Objects;
import java.util.UUID;

/**
 * IDによる検索条件。画面やSQLの型を公開契約へ持ち込まない。
 *
 * @param id 検索するID。null不可
 */
public record FindFeatureParam(UUID id) {
    /**
     * 必須検索条件の欠落を呼出し側の誤りとして拒否する。
     *
     * @param id 検索するID
     * @throws NullPointerException idがnullの場合
     */
    public FindFeatureParam {
        Objects.requireNonNull(id);
    }
}
