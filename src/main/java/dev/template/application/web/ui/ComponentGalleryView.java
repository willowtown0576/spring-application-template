package dev.template.application.web.ui;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 標準Vaadin部品の組み合わせを学ぶ、View内の一時dataを使う画面サンプル。
 *
 * <p>一覧の絞り込み、recordへのフォーム入力、モーダル、削除確認、通知をこのViewだけで試せる。
 * データはViewインスタンスごとに持ち、再読み込みすると初期化される。案件へ転用する際は保存処理を featureのCommand
 * API呼び出しへ置き換え、認可と業務検証をCommand側でも行う。
 */
@Route(value = "components", layout = StarterLayout.class)
@RouteAlias("theme-preview")
@PageTitle("UI component gallery")
@AnonymousAllowed
public class ComponentGalleryView extends VerticalLayout implements BeforeEnterObserver {
    // View内の行識別子。Viewの再作成時に初期化する。
    private long nextId;
    private final Grid<SampleItem> grid = new Grid<>(SampleItem.class, false);
    private final GridListDataView<SampleItem> data;
    private final TextField search = new TextField("名前で検索");
    private final ComboBox<Status> statusFilter = new ComboBox<>("状態");

    /** {@inheritDoc} */
    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if ("theme-preview".equals(event.getLocation().getPath())) {
            final boolean dark = event.getLocation().getQueryParameters().getSingleParameter("scheme")
                .filter("dark"::equals).isPresent();
            event.getUI().getPage().setColorScheme(dark ? ColorScheme.Value.DARK : ColorScheme.Value.LIGHT);
        }
    }

    /** 標準部品だけで、一覧と入力例を組み立てる。 */
    public ComponentGalleryView() {
        setWidthFull();
        getElement().setAttribute("lang", "ja");
        final Span label = new Span("APPLICATION STARTER / UI EXAMPLES");
        final H1 title = new H1("画面コンポーネント");
        final Paragraph introduction = new Paragraph("一覧・入力・確認の基本操作を、動くサンプルで確認できます。データはこの画面内だけで保持します。");
        final Card introductionCard = new Card();
        introductionCard.setWidthFull();
        introductionCard.addThemeVariants(CardVariant.ELEVATED);
        introductionCard.setTitle("部品を選ぶ、組み合わせる、操作する");
        introductionCard.setSubtitle(label);
        introductionCard.setHeaderPrefix(VaadinIcon.GRID_SMALL.create());
        introductionCard.add(introduction);
        introductionCard.addToFooter(new RouterLink("ウェルカムページへ", WelcomeView.class));
        add(title, introductionCard);

        // 一覧の列と行操作を定義する。表示用データはこのView内だけで保持する。
        grid.setId("sample-grid");
        grid.addColumn(SampleItem::name).setHeader("名前").setSortable(true).setAutoWidth(true);
        grid.addComponentColumn(item -> {
            final VaadinIcon icon = switch (item.status()) {
                case DRAFT -> VaadinIcon.CIRCLE_THIN;
                case ACTIVE -> VaadinIcon.CLOCK;
                case DONE -> VaadinIcon.CHECK_CIRCLE;
            };
            final HorizontalLayout badge = new HorizontalLayout(icon.create(), new Span(item.status().label));
            badge.setAlignItems(Alignment.CENTER);
            return badge;
        }).setHeader("状態").setAutoWidth(true);
        grid.addColumn(SampleItem::dueDate).setHeader("予定日").setAutoWidth(true);
        grid.addComponentColumn(item -> {
            final Button edit = new Button("編集", event -> edit(item));
            edit.setAriaLabel(item.name() + "を編集");
            edit.addThemeVariants(ButtonVariant.TERTIARY);
            final Button delete = new Button("削除", event -> confirmDelete(item));
            delete.setAriaLabel(item.name() + "を削除");
            delete.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.ERROR);
            return new HorizontalLayout(edit, delete);
        }).setHeader("操作").setAutoWidth(true);
        grid.setAllRowsVisible(true);
        grid.addThemeVariants(GridVariant.ROW_STRIPES, GridVariant.WRAP_CELL_CONTENT);

        // 画面ごとに独立したsampleを用意し、絞り込み後の件数を表示する。
        data = grid.setItems(
            new ArrayList<>(List.of(new SampleItem(++nextId, "画面レイアウトの確認", Status.DRAFT, LocalDate.of(2026, 9, 18)),
                new SampleItem(++nextId, "入力チェックの確認", Status.ACTIVE, LocalDate.of(2026, 9, 21)),
                new SampleItem(++nextId, "権限設定の確認", Status.DONE, LocalDate.of(2026, 9, 25)))));
        final Span count = new Span(data.getItemCount() + "件");
        count.setId("sample-count");
        data.addItemCountChangeListener(event -> count.setText(event.getItemCount() + "件"));

        // 検索と新規作成の操作を一覧へ接続する。
        search.setId("sample-search");
        search.setClearButtonVisible(true);
        search.setValueChangeMode(ValueChangeMode.EAGER);
        search.addValueChangeListener(event -> filter());
        statusFilter.setItems(Status.values());
        statusFilter.setItemLabelGenerator(status -> status.label);
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(event -> filter());
        final Button create = new Button("新規作成", event -> edit(null));
        create.setId("sample-create");
        create.addThemeVariants(ButtonVariant.PRIMARY);
        final HorizontalLayout toolbar = new HorizontalLayout(search, statusFilter, create);
        toolbar.setAlignItems(Alignment.END);
        toolbar.setWidthFull();
        toolbar.setWrap(true);
        final VerticalLayout list = new VerticalLayout(new H2("作業一覧"), toolbar, count, grid);
        list.setPadding(false);
        final Checkbox stripes = new Checkbox("行のストライプ", true);
        stripes.addValueChangeListener(event -> {
            if (event.getValue())
                grid.addThemeVariants(GridVariant.ROW_STRIPES);
            else
                grid.removeThemeVariants(GridVariant.ROW_STRIPES);
        });
        list.addComponentAtIndex(1, stripes);

        // 部品例を用途別のタブへ配置する。
        final TabSheet tabs = new TabSheet();
        tabs.setWidthFull();
        tabs.add("一覧とモーダル", list);
        tabs.add("入力部品", inputExamples());
        tabs.add("数値・時刻", typedInputExamples());
        tabs.add("選択部品", selectionExamples());
        tabs.add("表示・メニュー", displayExamples());
        tabs.add("一覧＋詳細", masterDetailExample());
        tabs.add("カード・操作", cardExamples());

        // 概要カードは一覧データとは独立した表示例とする。
        final ProgressBar progress = new ProgressBar(0, 1, 0.65);
        progress.getElement().setAttribute("aria-label", "進捗表示のサンプル 65パーセント");
        progress.setWidth("16rem");
        final Card progressCard = new Card();
        progressCard.setTitle("進捗表示の例", 2);
        progressCard.setSubtitle("サンプルの確認状況 · 65%");
        progressCard.add(progress);
        final Card navigationCard = new Card();
        navigationCard.setTitle("7つのカテゴリー", 2);
        navigationCard.setSubtitle("基本部品から画面の組み合わせまで");
        navigationCard.add(new Paragraph("タブを選んで操作できます。狭い画面ではタブを横にスクロールできます。"));
        final FormLayout overview = new FormLayout(progressCard, navigationCard);
        overview.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("48rem", 2));
        add(overview);
        add(tabs, new Details("開発者向け：このサンプルの使い方", new Paragraph(
            "Gridは表示用record、Binderは入力検証、Dialogは編集、ConfirmDialogは破壊的操作の確認に使っています。画面内データを業務データに置き換える際は、QueryのResultから表示モデルへ変換し、保存時にParamを作ってCommandへ渡します。")));
    }

    /** 検索条件を一覧のデータビューへ適用する。 */
    private void filter() {
        final String term = search.getValue().strip().toLowerCase(Locale.ROOT);
        data.setFilter(item -> item.name().toLowerCase(Locale.ROOT).contains(term)
            && (statusFilter.isEmpty() || item.status() == statusFilter.getValue()));
    }

    /**
     * 編集前のrecordを保持し、検証に成功したときだけ一覧を更新する。
     *
     * @param original 編集対象。新規作成時のみnull
     */
    private void edit(final SampleItem original) {
        final Dialog dialog = new Dialog();
        dialog.setHeaderTitle(original == null ? "サンプルを作成" : "サンプルを編集");
        dialog.setWidth("36rem");
        final TextField name = new TextField("名前");
        name.setId("sample-name");
        name.setMaxLength(100);
        final ComboBox<Status> status = new ComboBox<>("状態");
        status.setItems(Status.values());
        status.setItemLabelGenerator(value -> value.label);
        final DatePicker dueDate = new DatePicker("予定日");

        // 入力を検証してからrecordへ変換し、編集途中の値を一覧に漏らさない。
        final Binder<SampleParam> binder = new Binder<>(SampleParam.class);
        binder.forField(name).asRequired("名前を入力してください。")
            .withValidator(value -> !value.isBlank() && value.length() <= 100, "名前は1〜100文字です。").bind("name");
        binder.forField(status).asRequired("状態を選択してください。").bind("status");
        binder.forField(dueDate).asRequired("予定日を選択してください。").bind("dueDate");
        binder.readRecord(original == null
            ? new SampleParam("", Status.DRAFT, LocalDate.of(2026, 9, 30))
            : new SampleParam(original.name(), original.status(), original.dueDate()));
        final FormLayout form = new FormLayout(name, status, dueDate);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        dialog.add(form);

        // 検証成功時だけ行を置き換える。既存行の識別子は維持する。
        final Button save = new Button("保存", event -> {
            try {
                final SampleParam param = binder.writeRecord();
                final SampleItem replacement = new SampleItem(original == null ? ++nextId : original.id(), param.name(),
                    param.status(), param.dueDate());
                if (original != null)
                    data.removeItem(original);
                data.addItem(replacement);
                dialog.close();
                Notification.show("保存しました。", 3000, Notification.Position.BOTTOM_START);
            } catch (final ValidationException exception) {
                // Binderが各入力欄へエラーを表示する。一覧への反映は検証成功時とする。
            }
        });
        save.setId("sample-save");
        save.addThemeVariants(ButtonVariant.PRIMARY);
        dialog.getFooter().add(new Button("キャンセル", event -> dialog.close()), save);
        dialog.open();
        name.focus();
    }

    /**
     * 確認後にのみ画面内の行を削除する。
     *
     * @param item 削除対象
     */
    private void confirmDelete(final SampleItem item) {
        final ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("サンプルを削除");
        dialog.setText("「" + item.name() + "」を削除しますか？");
        dialog.setCancelable(true);
        dialog.setCancelText("キャンセル");
        dialog.setConfirmText("削除する");
        dialog.setConfirmButtonTheme("danger primary");
        dialog.addConfirmListener(event -> data.removeItem(item));
        dialog.open();
    }

    /**
     * 画面内で値を保持する入力部品と、現在値を読む操作の例を作る。
     *
     * @return レスポンシブなフォームを含むレイアウト
     */
    private VerticalLayout inputExamples() {
        final RadioButtonGroup<String> delivery = new RadioButtonGroup<>("通知方法");
        delivery.setItems("画面内", "通知しない");
        delivery.setValue("画面内");
        final Checkbox enabled = new Checkbox("通知を有効にする", true);
        final TextArea notes = new TextArea("メモ");
        notes.setPlaceholder("自由記述欄のサンプルです");
        notes.setMaxLength(500);
        notes.setHelperText("500文字以内。この内容は送信・保存されません。");
        final FormLayout form = new FormLayout(delivery, enabled, notes);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("40rem", 2));
        form.setColspan(notes, 2);
        final Button preview = new Button("入力内容を確認",
            event -> Notification.show(enabled.getValue() ? "通知方法: " + delivery.getValue() : "通知は無効です。"));
        final VerticalLayout layout = new VerticalLayout(new H2("入力部品の組み合わせ"), form, preview);
        layout.setPadding(false);
        return layout;
    }

    /**
     * 入力用途に合う標準fieldの制約と補助表示を試す。
     *
     * @return 数値・メール・パスワード・時刻の入力例
     */
    private VerticalLayout typedInputExamples() {
        final EmailField email = new EmailField("メールアドレス（EmailField）");
        email.setId("example-email");
        email.setPlaceholder("sample@example.com");
        email.setErrorMessage("メールアドレスの形式で入力してください。");
        final PasswordField password = new PasswordField("パスワード（PasswordField）");
        password.setHelperText("表示切替の確認用です。架空のパスワードを入力してください。");

        // 整数・小数・時刻は、それぞれの標準fieldで入力を制約する。
        final IntegerField quantity = new IntegerField("数量（IntegerField）");
        quantity.setId("example-quantity");
        quantity.setMin(1);
        quantity.setMax(10);
        quantity.setValue(1);
        quantity.setStepButtonsVisible(true);
        quantity.setHelperText("1〜10の整数");
        quantity.setErrorMessage("1〜10の整数を入力してください。");
        final NumberField temperature = new NumberField("温度（NumberField）");
        temperature.setValue(22.5);
        temperature.setStep(0.5);
        temperature.setStepButtonsVisible(true);
        temperature.setSuffixComponent(new Span("℃"));
        final TimePicker time = new TimePicker("開始時刻（TimePicker）");
        time.setStep(Duration.ofMinutes(30));
        time.setValue(LocalTime.of(9, 0));

        // 入力部品をレスポンシブに配置する。
        final FormLayout form = new FormLayout(email, password, quantity, temperature, time);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("40rem", 2));
        final VerticalLayout layout = new VerticalLayout(new H2("用途に合った入力欄"),
            new Paragraph("入力形式、増減ボタン、接尾辞、パスワードの表示切替を試せます。"), form);
        layout.setPadding(false);
        return layout;
    }

    /**
     * 単一選択と複数選択の操作・選択結果表示を試す。
     *
     * @return 標準の選択部品を並べたフォーム
     */
    private VerticalLayout selectionExamples() {
        final Select<String> priority = new Select<>();
        priority.setLabel("優先度（Select）");
        priority.setItems("低", "通常", "高");
        priority.setValue("通常");
        final MultiSelectComboBox<String> tags = new MultiSelectComboBox<>("タグ（MultiSelectComboBox）");
        tags.setItems("画面", "入力", "検索", "通知");
        tags.setClearButtonVisible(true);
        tags.select("画面");
        final CheckboxGroup<String> channels = new CheckboxGroup<>();
        channels.setId("example-channels");
        channels.setLabel("確認対象（CheckboxGroup）");
        channels.setItems("デスクトップ", "モバイル", "キーボード");
        final ListBox<String> density = new ListBox<>();
        density.setItems("ゆったり", "標準", "コンパクト");
        density.setValue("標準");
        density.getElement().setAttribute("aria-label", "表示密度の選択例");

        // 選択結果は読み上げ対象とし、表示順を固定する。
        final Span result = new Span("確認対象: 未選択");
        result.setId("example-selection-result");
        result.getElement().setAttribute("aria-live", "polite");
        channels.addValueChangeListener(event -> result.setText("確認対象: "
            + (event.getValue().isEmpty() ? "未選択" : String.join("・", event.getValue().stream().sorted().toList()))));
        final FormLayout form = new FormLayout(priority, tags, channels);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("40rem", 2));
        final VerticalLayout layout = new VerticalLayout(new H2("単一選択と複数選択"), form, result,
            new Paragraph("表示密度（ListBox） — 選択操作のサンプルです。"), density);
        layout.setPadding(false);
        return layout;
    }

    /**
     * メニュー操作、折りたたみ、アバター、アイコンとツールチップを試す。
     *
     * @return 標準の表示・操作部品の例
     */
    private VerticalLayout displayExamples() {
        final Span result = new Span("メニューから操作を選択してください。");
        result.setId("example-menu-result");
        result.getElement().setAttribute("aria-live", "polite");
        final MenuBar menu = new MenuBar();
        menu.addItem("操作").getSubMenu().addItem("プレビュー", event -> result.setText("プレビューを選択しました。データは変更されません。"));
        menu.addItem("ヘルプ", event -> result.setText("メニュー項目はクリックやキーボードで選べます。"));

        // 人物表示と補助操作の例を組み合わせる。
        final Avatar avatar = new Avatar("サンプル利用者");
        final Button info = new Button("ヒント", VaadinIcon.INFO_CIRCLE.create(),
            event -> Notification.show("標準アイコン付きボタンのサンプルです。"));
        Tooltip.forComponent(info).setText("クリックすると通知を表示します");

        // 補足情報は折りたたんで表示する。
        final Accordion accordion = new Accordion();
        accordion.setWidthFull();
        accordion.add("この部品集について", new Paragraph("Vaadin標準コンポーネントの操作例です。"));
        accordion.add("データの保存について", new Paragraph("入力内容はDBに保存されません。画面を再読み込みすると初期化されます。"));
        final VerticalLayout layout = new VerticalLayout(new H2("表示と操作の部品"), new Paragraph("MenuBar"), menu, result,
            new Paragraph("Avatar・Icon・Tooltip"), new HorizontalLayout(avatar, info), new Paragraph("Accordion"),
            accordion);
        layout.setPadding(false);
        return layout;
    }

    /**
     * 選択したサンプルの詳細を表示する。狭い幅では標準overlayへ切り替わる。
     *
     * @return 一覧と詳細を組み合わせたレイアウト
     */
    private VerticalLayout masterDetailExample() {
        final MasterDetailLayout split = new MasterDetailLayout();
        split.setWidthFull();
        split.setHeight("28rem");
        split.setMasterSize("16rem");
        split.setDetailSize("20rem");

        // 一覧を選択元とし、詳細へ表示するsampleを登録する。
        final Grid<SampleItem> items = new Grid<>(SampleItem.class, false);
        items.setId("example-master-grid");
        items.addColumn(SampleItem::name).setHeader("サンプル").setAutoWidth(true);
        items.addColumn(item -> item.status().label).setHeader("状態").setAutoWidth(true);
        items.setItems(List.of(new SampleItem(1, "一覧画面の確認", Status.ACTIVE, LocalDate.of(2026, 9, 18)),
            new SampleItem(2, "フォームの確認", Status.DRAFT, LocalDate.of(2026, 9, 21)),
            new SampleItem(3, "通知の確認", Status.DONE, LocalDate.of(2026, 9, 25))));
        items.setSizeFull();
        items.addThemeVariants(GridVariant.WRAP_CELL_CONTENT);
        split.setMaster(items);
        split.setDetailPlaceholder(new Paragraph("一覧から行を選択すると詳細を表示します。"));

        // 選択変更時に詳細だけを更新し、選択解除時は閉じる。
        items.asSingleSelect().addValueChangeListener(event -> {
            final SampleItem item = event.getValue();
            if (item == null) {
                split.setDetail(null);
                return;
            }
            final Card detail = new Card();
            detail.setId("example-detail-card");
            detail.setTitle(item.name());
            detail.setSubtitle("サンプルの詳細");
            detail.setHeaderPrefix(VaadinIcon.CLIPBOARD_TEXT.create());
            detail.add(new Paragraph("状態: " + item.status().label), new Paragraph("予定日: " + item.dueDate()),
                new Paragraph("行を選び直すと、この領域だけが切り替わります。"));
            final Button close = new Button("詳細を閉じる", eventClose -> items.deselectAll());
            close.setId("example-detail-close");
            detail.addToFooter(close);
            split.setDetail(detail);
        });

        // 背景クリックとEscapeでも選択解除へ戻す。
        split.addBackdropClickListener(event -> items.deselectAll());
        split.addDetailEscapePressListener(event -> items.deselectAll());
        final VerticalLayout layout = new VerticalLayout(new H2("一覧と詳細をひとつの画面に"),
            new Paragraph("行をクリックしてください。デスクトップは横並び、狭い画面は詳細を重ねて表示します。"), split);
        layout.setPadding(false);
        return layout;
    }

    /**
     * Auraのカード・ボタンvariantを比較し、通知と確認操作を試す。
     *
     * @return 標準スタイルを比較するカード群
     */
    private FormLayout cardExamples() {
        final Card actions = new Card();
        actions.setTitle("操作の優先度");
        actions.setSubtitle("標準・Primary・Tertiary・Error");
        actions.addThemeVariants(CardVariant.ELEVATED);
        final Button normal = new Button("標準", event -> Notification.show("標準ボタンを選びました。"));
        final Button primary = new Button("確認する", VaadinIcon.CHECK.create(),
            event -> Notification.show("確認しました。", 3000, Notification.Position.BOTTOM_START));
        primary.setId("example-primary-action");
        primary.addThemeVariants(ButtonVariant.PRIMARY);
        final Button tertiary = new Button("補助操作", event -> Notification.show("補助操作を選びました。"));
        tertiary.addThemeVariants(ButtonVariant.TERTIARY);
        final Button danger = new Button("リセット", event -> {
            final ConfirmDialog confirmation = new ConfirmDialog();
            confirmation.setHeader("操作の確認サンプル");
            confirmation.setText("この例では確認ダイアログだけを表示します。データは変更しません。");
            confirmation.setCancelable(true);
            confirmation.setCancelText("キャンセル");
            confirmation.setConfirmText("確認しました");
            confirmation.setConfirmButtonTheme("danger primary");
            confirmation.open();
        });
        danger.addThemeVariants(ButtonVariant.ERROR);
        final HorizontalLayout buttons = new HorizontalLayout(normal, primary, tertiary, danger);
        buttons.setWrap(true);
        actions.add(new Paragraph("重要な操作をPrimary、補助操作をTertiaryで表現します。"), buttons);

        // 複数担当者の表示は標準AvatarGroupへ委ねる。
        final Card team = new Card();
        team.setTitle("レビューのサンプル");
        team.setSubtitle("Outlined Card / AvatarGroup");
        team.addThemeVariants(CardVariant.OUTLINED);
        final AvatarGroup reviewers = new AvatarGroup(new AvatarGroup.AvatarGroupItem("サンプル A"),
            new AvatarGroup.AvatarGroupItem("サンプル B"), new AvatarGroup.AvatarGroupItem("サンプル C"));
        reviewers.setMaxItemsVisible(2);
        team.add(new Paragraph("担当者のまとまりを表示。末尾の人数を開くと全員を確認できます。"), reviewers);

        // 標準variantのカードと外観を比較する。
        final Card plain = new Card();
        plain.setTitle("シンプルなカード");
        plain.setSubtitle("Default Card");
        plain.add(new Paragraph("見出し・補足・本文・フッターを標準のslotで構成します。影や枠線はvariantで選択できます。"));
        plain.addToFooter(new Span("右上のダークモードで明暗を比較できます。"));
        final FormLayout cards = new FormLayout(actions, team, plain);
        cards.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("48rem", 2));
        return cards;
    }

    /**
     * この画面だけで使用する表示・入力モデル。featureのQuery結果とは独立している。
     *
     * @param id 行の識別子。編集前後で維持する
     * @param name 表示名
     * @param status 表示する状態
     * @param dueDate 予定日
     */
    public record SampleItem(long id, String name, Status status, LocalDate dueDate) implements Serializable {
    }

    /**
     * 編集フォームの入力。編集項目を業務属性に限定するため一覧モデルと分ける。
     *
     * @param name 名前
     * @param status 状態
     * @param dueDate 予定日
     */
    public record SampleParam(String name, Status status, LocalDate dueDate) implements Serializable {
    }

    /** 画面操作を試すための選択肢。 */
    public enum Status {
        /** まだ確認していない状態。 */
        DRAFT("未着手"),
        /** 確認を進めている状態。 */
        ACTIVE("確認中"),
        /** 確認が完了した状態。 */
        DONE("完了");
        private final String label;

        /**
         * 選択肢の表示名を保持する。
         *
         * @param label 日本語表示名
         */
        Status(final String label) {
            this.label = label;
        }
    }
}
