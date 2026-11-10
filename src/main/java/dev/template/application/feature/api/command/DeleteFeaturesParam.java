package dev.template.application.feature.api.command;

import java.util.List;

/**
 * 同じ実行主体による、全件成功または全件拒否の複数削除。
 * @param actor 信頼されたadapterが認証から得た主体
 * @param targets 入力順の削除対象
 */
public record DeleteFeaturesParam(String actor, List<FeatureDeletionTarget> targets) {
    /**
     * 対象を不変にし、欠落行も同じ行番号で検証できる形にする。
     * @param actor 実行主体
     * @param targets 削除対象
     */
    public DeleteFeaturesParam {
        targets = targets == null
            ? List.of()
            : List.copyOf(
                targets.stream().map(target -> target == null ? new FeatureDeletionTarget(null, 0) : target).toList());
    }
}
