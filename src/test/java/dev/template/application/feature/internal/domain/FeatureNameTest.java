package dev.template.application.feature.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** DBと一致する名前の文字数・Unicode・空値の境界条件を検証する。 */
class FeatureNameTest {
    /** 補助文字を含む100 code pointの名前を作り、文字数の判定と入力値の保持を検証する。 */
    @Test
    void countsUnicodeCodePointsAndPreservesInput() {
        final String maximum = "😀".repeat(100);
        assertThat(new FeatureName(maximum).value()).isEqualTo(maximum);
        assertThat(new FeatureName(" name ").value()).isEqualTo(" name ");
        assertThat(FeatureName.isValid(maximum + "a")).isFalse();
    }

    /**
     * null、空白、上限超過、NUL、不正surrogateを名前へ指定し、domainが拒否することを検証する。
     *
     * @param name 検証する名前
     */
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\u0000", "\uD800", "\uDC00"})
    void rejectsInvalidNames(final String name) {
        assertThat(FeatureName.isValid(name)).isFalse();
        assertThatThrownBy(() -> new FeatureName(name)).isInstanceOf(IllegalArgumentException.class);
    }
}
