package dev.template.application.web.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** 初回起動で表示する匿名公開のウェルカムページ。案内と画面例へのリンクを提供する。 */
@Route(value = "", layout = StarterLayout.class)
@PageTitle("Welcome | Application Starter")
@AnonymousAllowed
public class WelcomeView extends VerticalLayout {
    /** Aura標準のカードとレスポンシブレイアウトで起動確認と画面例への入口を作る。 */
    public WelcomeView() {
        setWidthFull();
        getElement().setAttribute("lang", "ja");

        // 最初に試せる部品集への入口を示す。
        final Card hero = new Card();
        hero.setWidthFull();
        hero.addThemeVariants(CardVariant.ELEVATED);
        hero.setHeaderPrefix(VaadinIcon.ROCKET.create());
        hero.setTitle("動くサンプルで、できあがりをイメージする");
        hero.setSubtitle("画面から始める、業務アプリケーション開発");
        hero.add(new Paragraph("起動できました。一覧、フォーム、詳細画面の動くサンプルで、アプリケーションの使い心地を確かめてください。"));
        final Button gallery = new Button("部品集を開く", VaadinIcon.ARROW_RIGHT.create(),
            event -> getUI().ifPresent(ui -> ui.navigate(ComponentGalleryView.class)));
        gallery.setId("welcome-gallery");
        gallery.addThemeVariants(ButtonVariant.PRIMARY);
        hero.addToFooter(gallery, new Span("ログイン不要 · サンプルデータで体験"));
        add(new H1("ようこそ、Application Starterへ。"), hero, new H2("画面づくりの出発点"));

        final FormLayout examples = new FormLayout();
        examples.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("48rem", 3));
        examples.add(
            featureCard("一覧から詳細へ", "探す・選ぶ・確認する", VaadinIcon.TABLE, "検索や並び替え、行の編集、選択した項目の詳細表示。業務画面の定番を一通り試せます。"),
            featureCard("迷わない入力", "入力する・検証する", VaadinIcon.EDIT, "必須入力、数値の範囲、日付や時刻、複数選択。用途に合った入力部品を比較できます。"),
            featureCard("伝わるフィードバック", "確認する・次へ進む", VaadinIcon.COMMENT, "ダイアログ、通知、進捗、操作メニュー。ボタンの強弱や明暗テーマも切り替えて確認できます。"));
        add(examples);

        final Card steps = new Card();
        steps.setTitle("はじめの3ステップ");
        steps.setSubtitle("見る → 試す → 自分の画面へ");
        steps.addThemeVariants(CardVariant.OUTLINED);
        steps.add(new Details("01  部品を触ってみる", new Paragraph("左のコンポーネントメニューから部品集へ。タブごとに操作例を切り替えられます。")),
            new Details("02  画面の組み合わせを確認する", new Paragraph("一覧＋詳細や編集ダイアログを操作してください。ダークモードと狭い画面での表示も確認できます。")),
            new Details("03  業務機能につなぐ", new Paragraph("READMEと開発者向けガイドを参考に、画面内のサンプルを案件のCommand / Queryへ接続します。")));

        // 永続化を伴う操作はDB連携sampleへのリンクで案内する。
        final Card database = new Card();
        database.setTitle("DB連携のサンプル");
        database.setSubtitle("認証・操作権限の設定後に利用できます");
        database.setHeaderPrefix(VaadinIcon.DATABASE.create());
        database.addThemeVariants(CardVariant.OUTLINED);
        database.add(new Paragraph("Featureの作成とUUIDによる取得を体験できます。実際にDBへ保存する画面です。"));
        database.addToFooter(new RouterLink("Featureサンプル（認証が必要）", FeatureView.class));
        final FormLayout next = new FormLayout(steps, database);
        next.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("48rem", 2));
        add(next, new Paragraph("部品集の入力はDBに保存されません。再読み込みで初期状態に戻ります。"));
    }

    /**
     * 画面例を紹介する標準カードを作る。
     *
     * @param title 見出し
     * @param subtitle 操作の要約
     * @param icon 標準アイコン
     * @param description 体験できる内容
     * @return レスポンシブ配置するカード
     */
    private Card featureCard(final String title, final String subtitle, final VaadinIcon icon,
        final String description) {
        final Card card = new Card();
        card.setTitle(title, 3);
        card.setSubtitle(subtitle);
        card.setHeaderPrefix(icon.create());
        card.addThemeVariants(CardVariant.ELEVATED);
        card.add(new Paragraph(description));
        return card;
    }
}
