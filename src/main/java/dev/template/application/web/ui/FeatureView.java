package dev.template.application.web.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import dev.template.application.feature.api.command.FeatureCommands;
import dev.template.application.feature.api.command.Result;
import dev.template.application.feature.api.query.FeatureQueries;
import jakarta.annotation.security.PermitAll;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;

@Route("features")
@PageTitle("Features")
@PermitAll
public class FeatureView extends VerticalLayout {
  public FeatureView(FeatureCommands commands, FeatureQueries queries) {
    TextField name = new TextField("名前");
    name.setId("feature-name");
    name.setWidthFull();
    name.setRequiredIndicatorVisible(true);
    name.setErrorMessage("名前を1〜100文字で入力してください。");
    TextField id = new TextField("ID");
    id.setId("feature-id");
    id.setWidthFull();
    id.setErrorMessage("正しいUUIDを入力してください。");
    Paragraph result = new Paragraph();
    result.setId("feature-result");
    result.getElement().setAttribute("role", "status");
    Button create =
        new Button(
            "作成",
            event -> {
              result.setText("");
              try {
                switch (commands.create(name.getValue())) {
                  case Result.Success(var created) -> {
                    name.setInvalid(false);
                    id.setValue(created.toString());
                    id.setInvalid(false);
                    result.setText("作成しました。");
                  }
                  case Result.Failure(var failure) -> {
                    switch (failure) {
                      case INVALID_NAME -> name.setInvalid(true);
                    }
                  }
                }
              } catch (AccessDeniedException exception) {
                result.setText("作成する権限がありません。");
              }
            });
    create.setId("feature-create");
    Button find =
        new Button(
            "取得",
            event -> {
              result.setText("");
              UUID requested;
              try {
                requested = UUID.fromString(id.getValue());
                if (!requested.toString().equalsIgnoreCase(id.getValue())) {
                  id.setInvalid(true);
                  return;
                }
              } catch (IllegalArgumentException exception) {
                id.setInvalid(true);
                return;
              }
              id.setInvalid(false);
              try {
                result.setText(
                    queries
                        .find(requested)
                        .map(feature -> "名前: " + feature.name())
                        .orElse("見つかりませんでした。"));
              } catch (AccessDeniedException exception) {
                result.setText("取得する権限がありません。");
              }
            });
    find.setId("feature-find");
    setMaxWidth("40rem");
    getElement().setAttribute("lang", "ja");
    add(new H1("Features"), name, create, id, find, result);
  }
}
