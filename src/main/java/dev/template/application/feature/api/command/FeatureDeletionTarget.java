package dev.template.application.feature.api.command;

import java.util.UUID;

/**
 * 削除対象と、確認画面または参照APIで取得した期待version。
 * @param id 対象ID
 * @param version 期待version
 */
public record FeatureDeletionTarget(UUID id, long version) {
}
