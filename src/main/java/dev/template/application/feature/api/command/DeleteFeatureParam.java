package dev.template.application.feature.api.command;

/**
 * 一件の削除操作。実行主体と対象を分離する。
 * @param actor 信頼されたadapterが認証から得た主体
 * @param target 確認済みの削除対象
 */
public record DeleteFeatureParam(String actor, FeatureDeletionTarget target) {
}
