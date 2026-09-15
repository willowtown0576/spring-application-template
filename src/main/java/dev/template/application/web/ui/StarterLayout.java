package dev.template.application.web.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;

/** Aura標準のヘッダー・レスポンシブdrawer・画面ナビゲーション。 */
@AnonymousAllowed
public class StarterLayout extends AppLayout {
    /**
     * 共通メニュー、明暗切替、認証状態に応じたログイン／ログアウトを構成する。
     * @param authentication Vaadinと統合された認証context
     */
    public StarterLayout(final AuthenticationContext authentication) {
        final DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("ナビゲーションを開閉");
        final Checkbox dark = new Checkbox("ダークモード");
        dark.setId("starter-dark-mode");
        dark.addValueChangeListener(event -> getUI().ifPresent(
            ui -> ui.getPage().setColorScheme(event.getValue() ? ColorScheme.Value.DARK : ColorScheme.Value.LIGHT)));
        addAttachListener(event -> event.getUI().getPage()
            .setColorScheme(dark.getValue() ? ColorScheme.Value.DARK : ColorScheme.Value.LIGHT));

        // ヘッダーは狭い幅で折り返し、認証状態に応じた操作を表示する。
        final H3 title = new H3("Application Starter");
        final HorizontalLayout header = new HorizontalLayout(toggle, title, dark);
        header.setWidthFull();
        header.setPadding(true);
        header.setWrap(true);
        header.setAlignItems(HorizontalLayout.Alignment.CENTER);
        header.expand(title);
        final Button account = authentication.isAuthenticated()
            ? new Button("ログアウト", event -> authentication.logout())
            : new Button("ログイン", event -> getUI().ifPresent(ui -> ui.navigate(LoginView.class)));
        account.setId("starter-account");
        header.add(account);
        addToNavbar(header);

        final SideNav navigation = new SideNav();
        navigation.setLabel("メインナビゲーション");
        navigation.addItem(new SideNavItem("ホーム", WelcomeView.class, VaadinIcon.HOME.create()),
            new SideNavItem("コンポーネント", ComponentGalleryView.class, VaadinIcon.GRID_SMALL.create()),
            new SideNavItem("テーマ比較", ThemeComparisonView.class, VaadinIcon.ADJUST.create()),
            new SideNavItem("DB連携", FeatureView.class, VaadinIcon.DATABASE.create()));
        final VerticalLayout drawer = new VerticalLayout(new H3("Explore"), navigation,
            new Paragraph("画面を動かしながら、次のアプリケーションを組み立てましょう。"));
        drawer.setWidth("16rem");
        navigation.setWidthFull();
        addToDrawer(new Scroller(drawer));
    }
}
