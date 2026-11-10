package dev.template.application.web.ui;

import dev.template.application.feature.api.command.FeatureCommandFailure;
import dev.template.application.web.ui.FeatureViews.Filter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 削除確認画面。拒否時は確認した名前と期待versionを保持する。
 * @param filter 一覧へ戻る検索条件
 * @param rows 確認対象
 * @param issues 削除を拒否した理由
 */
public record FeatureDeletionPage(Filter filter, List<Row> rows, List<FeatureCommandFailure.Issue> issues) {
    /**
     * 確認対象と拒否理由を不変にする。
     * @param filter 戻り先条件
     * @param rows 確認対象
     * @param issues 拒否理由
     */
    public FeatureDeletionPage {
        rows = List.copyOf(rows);
        issues = List.copyOf(issues);
    }

    /**
     * 行の拒否理由を表示する。
     * @param row 行番号。0は全体
     * @return エラー文
     */
    public String error(final int row) {
        return FeatureViews.error(issues, row);
    }

    /**
     * 同じ対象の最新内容を確認し直すURL。
     * @param path 配置先を含む確認画面path
     * @return 検索条件と対象を含むURL
     */
    public String refreshUrl(final String path) {
        return filter.url(path) + rows.stream().map(row -> "&ids=" + row.id()).collect(Collectors.joining());
    }

    /**
     * 利用者が確認した表示名と更新情報。
     * @param id 対象ID
     * @param name 確認した名前。削除に使用する値はサーバーで再取得する
     * @param version 確認時のversion
     */
    public record Row(UUID id, String name, long version) {
    }
}
