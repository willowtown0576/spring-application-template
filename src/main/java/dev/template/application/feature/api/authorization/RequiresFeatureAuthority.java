package dev.template.application.feature.api.authorization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * transaction境界でFeatureの権限を要求する、Spring標準meta-annotation。
 *
 * <p>例: {@code @RequiresFeatureAuthority(FeatureAuthority.WRITE)}。
 * Springのtemplate展開後にenumのwire値を取得するため、呼出し箇所は文字列を組み立てない。
 * 有効化はSecurityConfigurationのAnnotationTemplateExpressionDefaults Beanが担当する。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAuthority(T(dev.template.application.feature.api.authorization.FeatureAuthority).{value}.authority)")
public @interface RequiresFeatureAuthority {
    /**
     * 操作に必要な権限。
     *
     * @return コンパイル時に制約されるFeature権限
     */
    FeatureAuthority value();
}
