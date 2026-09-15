package dev.template.application.feature.internal.domain;

/**
 * DB制約と整合するFeature名。文字数はUTF-16長でなくUnicode code pointで数える。
 *
 * @param value 名前。非空、100 code point以下、半角空白だけの値とNUL、不正サロゲートを拒否する
 */
public record FeatureName(String value) {
    /**
     * 名前の不変条件を検証する。
     *
     * @param value 検証する名前
     * @throws IllegalArgumentException 名前が制約に違反する場合
     */
    public FeatureName {
        if (!isValid(value)) {
            throw new IllegalArgumentException("Invalid feature name");
        }
    }

    /**
     * Commandが例外を使わず業務Failureを返すための事前検証。
     *
     * @param value 未検証の名前。nullも検証対象
     * @return DBへ保存可能な名前ならtrue
     */
    public static boolean isValid(final String value) {
        return value != null && !value.isEmpty() && value.codePointCount(0, value.length()) <= 100
            && value.codePoints().anyMatch(c -> c != ' ')
            && value.codePoints().noneMatch(c -> c == 0 || (c >= 0xD800 && c <= 0xDFFF));
    }
}
