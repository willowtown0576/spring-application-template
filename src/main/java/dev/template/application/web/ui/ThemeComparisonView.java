package dev.template.application.web.ui;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** 同じ部品集を独立したdocumentで表示する標準テーマの比較画面。 */
@Route(value = "theme-comparison", layout = StarterLayout.class)
@PageTitle("Aura / Lumo")
@AnonymousAllowed
public class ThemeComparisonView extends VerticalLayout {
    /** テーマごとの操作例と共通の明暗切替を配置する。 */
    public ThemeComparisonView() {
        setWidthFull();
        final IFrame aura = preview("aura");
        final IFrame lumo = preview("lumo");
        final Checkbox dark = new Checkbox("比較をダークモードにする");
        dark.setId("comparison-dark-mode");
        dark.addValueChangeListener(event -> {
            final String scheme = event.getValue() ? "dark" : "light";
            aura.setSrc("theme-preview?theme=aura&scheme=" + scheme);
            lumo.setSrc("theme-preview?theme=lumo&scheme=" + scheme);
        });

        // 独立したdocumentを並べ、狭い幅では縦に配置する。
        final FormLayout comparison = new FormLayout();
        comparison.setWidthFull();
        comparison.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("64rem", 2));
        comparison.add(new VerticalLayout(new H2("Aura"), aura), new VerticalLayout(new H2("Lumo"), lumo));
        add(new H1("AuraとLumoを比較"), new Paragraph("同じ7つの部品タブを、それぞれの標準テーマで操作できます。"),
            new Paragraph("各画面の編集内容は独立しています。明暗を切り替えると、両方の入力内容が初期化されます。"), dark, comparison);
    }

    /**
     * 共通部品集を指定テーマで読み込む。
     * @param theme 標準テーマ名
     * @return 操作可能な部品集
     */
    private static IFrame preview(final String theme) {
        final IFrame frame = new IFrame("theme-preview?theme=" + theme + "&scheme=light");
        frame.setId("preview-" + theme);
        frame.setTitle(theme + " コンポーネントプレビュー");
        frame.setWidthFull();
        frame.setHeight("64rem");
        return frame;
    }
}
