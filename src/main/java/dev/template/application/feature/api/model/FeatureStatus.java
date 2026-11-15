package dev.template.application.feature.api.model;

/** Featureの有効状態。無効状態も編集と参照の対象となる。 */
public enum FeatureStatus {
    /** 利用可能。 */
    ACTIVE,
    /** 利用を停止。 */
    INACTIVE
}
