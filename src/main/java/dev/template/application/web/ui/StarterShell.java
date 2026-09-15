package dev.template.application.web.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.theme.aura.Aura;
import com.vaadin.flow.theme.lumo.Lumo;

/** documentごとにVaadin標準テーマを一つだけ読み込む。 */
public class StarterShell implements AppShellConfigurator {
    /** {@inheritDoc} */
    @Override
    public void configurePage(final AppShellSettings settings) {
        final var request = settings.getRequest();
        final boolean lumo = "/theme-preview".equals(request.getPathInfo())
            && "lumo".equals(request.getParameter("theme"));
        settings.addLink("stylesheet", lumo ? Lumo.STYLESHEET : Aura.STYLESHEET);
    }
}
