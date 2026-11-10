package dev.template.application.feature.api.command;

import dev.template.application.feature.api.model.FeatureStatus;
import java.util.UUID;

/**
 * 一行の更新入力。versionは参照した時点の値とする。
 * @param id 更新対象
 * @param name 入力された名前
 * @param status 有効状態
 * @param version 期待するversion
 */
public record UpdateFeatureParam(UUID id, String name, FeatureStatus status, long version) {
}
