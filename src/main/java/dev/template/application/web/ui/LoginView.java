package dev.template.application.web.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** Spring Securityのフォーム認証へcredentialを送信する標準ログイン画面。 */
@Route("login")
@PageTitle("ログイン | Application Starter")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {
    private final LoginForm login = new LoginForm();

    /** 標準LoginFormを構成し、passwordをapplicationのイベント処理へ渡さない。 */
    public LoginView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        final var labels = LoginI18n.createDefault();
        labels.getForm().setTitle("ログイン");
        labels.getForm().setUsername("ユーザー名");
        labels.getForm().setPassword("パスワード");
        labels.getForm().setSubmit("ログイン");
        labels.getErrorMessage().setTitle("ログインできませんでした");
        labels.getErrorMessage().setMessage("ユーザー名とパスワードを確認してください。");
        login.setI18n(labels);
        login.setAction("login");
        login.setForgotPasswordButtonVisible(false);
        add(new H1("Application Starter"), login);
    }

    /** {@inheritDoc} */
    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        login.setError(event.getLocation().getQueryParameters().getParameters().containsKey("error"));
    }
}
