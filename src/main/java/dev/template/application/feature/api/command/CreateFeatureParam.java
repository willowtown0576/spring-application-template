package dev.template.application.feature.api.command;

import dev.template.application.feature.api.model.FeatureStatus;

/**
 * Featureの登録入力。名前と状態の業務検証はCommandで行う。
 * @param name 入力された名前
 * @param status 有効状態
 */
public record CreateFeatureParam(String name, FeatureStatus status) {
    /**
     * 有効状態で登録する入力を作る。
     * @param name 入力された名前
     */
    public CreateFeatureParam(final String name) {
        this(name, FeatureStatus.ACTIVE);
    }
}
