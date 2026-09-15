package dev.template.application.web.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import dev.template.application.common.Result;
import dev.template.application.feature.api.command.CreateFeatureParam;
import dev.template.application.feature.api.command.FeatureCommands;
import dev.template.application.feature.api.query.FeatureQueries;
import dev.template.application.feature.api.query.FindFeatureParam;
import jakarta.annotation.security.PermitAll;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;

/**
 * CommandとQueryを呼ぶ最小Vaadin adapter。業務Failureと認可拒否を画面表示へ変換する。
 *
 * <p>部品自体の使い方はComponentGalleryViewを参照する。DB操作は公開APIだけを通す。
 */
@Route(value = "features", layout = StarterLayout.class)
@PageTitle("Features")
@PermitAll
public class FeatureView extends VerticalLayout {
    /**
     * 作成・ID検索の入力欄と操作を構築する。
     *
     * @param commands 更新API
     * @param queries 参照API
     */
    public FeatureView(final FeatureCommands commands, final FeatureQueries queries) {
        final TextField name = new TextField("名前");
        name.setId("feature-name");
        name.setWidthFull();
        name.setRequiredIndicatorVisible(true);
        name.setErrorMessage("名前を1〜100文字で入力してください。");
        final TextField id = new TextField("ID");
        id.setId("feature-id");
        id.setWidthFull();
        id.setErrorMessage("正しいUUIDを入力してください。");
        final Paragraph result = new Paragraph();
        result.setId("feature-result");
        result.getElement().setAttribute("role", "status");

        // 作成結果と業務Failureを入力欄・結果表示へ反映する。
        final Button create = new Button("作成", event -> {
            result.setText("");
            try {
                switch (commands.create(new CreateFeatureParam(name.getValue()))) {
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
            } catch (final AccessDeniedException exception) {
                result.setText("作成する権限がありません。");
            }
        });
        create.setId("feature-create");

        // UUIDの形式を確認してから公開Queryへ渡す。
        final Button find = new Button("取得", event -> {
            result.setText("");
            final UUID requested;
            try {
                requested = UUID.fromString(id.getValue());
                if (!requested.toString().equalsIgnoreCase(id.getValue())) {
                    id.setInvalid(true);
                    return;
                }
            } catch (final IllegalArgumentException exception) {
                id.setInvalid(true);
                return;
            }
            id.setInvalid(false);
            try {
                result.setText(queries.find(new FindFeatureParam(requested)).map(feature -> "名前: " + feature.name())
                    .orElse("見つかりませんでした。"));
            } catch (final AccessDeniedException exception) {
                result.setText("取得する権限がありません。");
            }
        });
        find.setId("feature-find");

        // 操作部品を画面へ配置する。
        setMaxWidth("40rem");
        getElement().setAttribute("lang", "ja");
        add(new H1("Features"), new RouterLink("画面コンポーネント集へ", ComponentGalleryView.class), name, create, id, find,
            result);
    }
}
