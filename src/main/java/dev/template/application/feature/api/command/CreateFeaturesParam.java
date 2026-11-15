package dev.template.application.feature.api.command;

import java.util.List;
import java.util.UUID;

/**
 * 同じ操作キーで再送可能な登録入力。
 * @param actor 信頼されたadapterが認証情報から得た主体
 * @param requestId 登録操作キー
 * @param rows 入力順の登録行
 */
public record CreateFeaturesParam(String actor, UUID requestId, List<CreateFeatureParam> rows) {
    /**
     * 入力の外部変更を防ぐ。行の業務検証はCommandで行う。
     * @param actor 実行主体
     * @param requestId 操作キー
     * @param rows 登録行
     */
    public CreateFeaturesParam {
        rows = rows == null
            ? List.of()
            : List.copyOf(rows.stream().map(row -> row == null ? new CreateFeatureParam(null, null) : row).toList());
    }
}
