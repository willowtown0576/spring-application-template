package dev.template.application.web.ui;

import dev.template.application.feature.api.command.FeatureCommandFailure;
import dev.template.application.feature.api.model.FeatureStatus;
import dev.template.application.feature.api.query.FeatureDetails;
import dev.template.application.feature.api.query.FeatureSortField;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.web.util.UriComponentsBuilder;

/** Feature画面の表示モデル。DB識別子はリンクとフォームの内部値に使う。 */
public final class FeatureViews {
    /** 日本時間の秒単位表示。 */
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss")
        .withZone(ZoneId.of("Asia/Tokyo"));

    /** 表示型の名前空間。 */
    private FeatureViews() {
    }

    /**
     * 状態の表示名。
     * @param value 状態
     * @return 表示名
     */
    public static String status(final FeatureStatus value) {
        return value == FeatureStatus.ACTIVE ? "有効" : "無効";
    }

    /**
     * 時刻を画面用に整形する。
     * @param value 時刻
     * @return 日本時間
     */
    public static String time(final Instant value) {
        return TIME.format(value);
    }

    /**
     * 登録・更新・削除に共通する行エラーを表示する。
     * @param issues 拒否理由
     * @param row 行番号。0は全体
     * @return 利用者向けの説明
     */
    static String error(final List<FeatureCommandFailure.Issue> issues, final int row) {
        return issues.stream().filter(issue -> issue.row() == row).map(issue -> switch (issue.reason()) {
            case INVALID_NAME -> "名前を1〜100文字で入力してください。";
            case INVALID_STATUS -> "状態を選択してください。";
            case CONFLICT -> "他の操作で更新されています。最新内容を確認してください。";
            case NOT_FOUND -> "対象が見つかりません。";
            case REPLAY_MISMATCH -> "保存済みの操作キーです。一覧から新しく登録を開始してください。";
            case DUPLICATE_ID -> "対象が重複しています。";
            case INVALID_VERSION -> "参照時の情報を確認してください。";
            case INVALID_REQUEST -> "対象と操作情報を確認してください（1〜100件）。";
        }).distinct().collect(Collectors.joining(" "));
    }

    /**
     * 登録・更新フォーム。
     * @param creating 登録か
     * @param requestId 登録操作キー
     * @param filter 戻り先条件
     * @param rows 編集行
     * @param issues 行エラー
     * @param reviewing 最新内容を確認中か
     * @param dirty 未保存内容があるか
     */
    public record Editor(boolean creating, UUID requestId, Filter filter, List<Row> rows,
        List<FeatureCommandFailure.Issue> issues, boolean reviewing, boolean dirty) {
        /**
         * 表示中の入力とエラーを不変にする。
         * @param creating 登録か
         * @param requestId 操作キー
         * @param filter 戻り先条件
         * @param rows 入力行
         * @param issues エラー
         * @param reviewing 比較中か
         * @param dirty 未保存か
         */
        public Editor {
            rows = List.copyOf(rows);
            issues = List.copyOf(issues);
        }

        /**
         * 行のエラーを日本語で取得する。
         * @param row 1始まりの行番号。0は全体
         * @return エラー文
         */
        public String error(final int row) {
            return FeatureViews.error(issues, row);
        }

        /**
         * 全行の最新内容を確認できたか。
         * @return 確認可能ならtrue
         */
        public boolean confirmable() {
            return !rows.isEmpty() && rows.stream().allMatch(row -> row.latest() != null);
        }

        /**
         * 競合の確認が必要か。
         * @return 競合があればtrue
         */
        public boolean conflicted() {
            return issues.stream().anyMatch(issue -> issue.reason() == FeatureCommandFailure.Reason.CONFLICT);
        }
    }

    /**
     * 検索条件。画面間で保存する。
     * @param name 名前
     * @param status 状態。空は全状態
     * @param sort ソート対象
     * @param descending 降順か
     * @param size ページサイズ
     */
    public record Filter(String name, String status, FeatureSortField sort, boolean descending, int size) {
        /**
         * 検索条件を含むURLを作る。
         * @param path 遷移先
         * @return 相対URL
         */
        public String url(final String path) {
            return UriComponentsBuilder.fromPath(path).queryParam("name", "{name}").queryParam("status", status)
                .queryParam("sort", sort).queryParam("descending", descending).queryParam("size", size).encode()
                .buildAndExpand(name).toUriString();
        }
    }

    /**
     * 一覧表示。
     * @param filter 条件
     * @param items 取得行
     * @param previous 前ページURL
     * @param next 次ページURL
     */
    public record Listing(Filter filter, List<FeatureDetails> items, String previous, String next) {
        /**
         * 取得行を不変にする。
         * @param filter 条件
         * @param items 取得行
         * @param previous 前URL
         * @param next 次URL
         */
        public Listing {
            items = List.copyOf(items);
        }
    }

    /**
     * 編集行。失敗時も入力値を保持する。
     * @param id 更新対象。登録時はnull
     * @param name 入力された名前
     * @param status 入力された状態
     * @param version 期待version
     * @param latest 競合確認時の最新内容
     */
    public record Row(UUID id, String name, String status, long version, FeatureDetails latest) {
    }
}
