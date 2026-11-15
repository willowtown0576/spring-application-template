package dev.template.application.feature.api.query;

/** 検索で同時に一つだけ選択する並び項目。 */
public enum FeatureSortField {
    /** DBの文字列照合順序。 */
    NAME,
    /** 更新日時順。 */
    UPDATED_AT
}
