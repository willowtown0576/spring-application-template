package dev.template.application.feature.api.command;

import java.util.List;

/**
 * Featureの登録・更新で共有する業務拒否。行番号・項目・理由を持ち、書き込み前に返す。
 * @param issues 全ての入力エラーまたは競合
 */
public record FeatureCommandFailure(List<Issue> issues) {
    /**
     * エラー一覧の外部変更を防ぐ。
     * @param issues エラー一覧
     */
    public FeatureCommandFailure {
        issues = List.copyOf(issues);
    }

    /** 業務拒否の分類。 */
    public enum Reason {
        /** 参照後に更新された。 */
        CONFLICT,
        /** 同一IDを複数指定。 */
        DUPLICATE_ID,
        /** 名前が不正。 */
        INVALID_NAME,
        /** 一括件数または操作情報が不正。 */
        INVALID_REQUEST,
        /** 状態が未指定。 */
        INVALID_STATUS,
        /** IDまたはversionが不正。 */
        INVALID_VERSION,
        /** 対象が存在しない。 */
        NOT_FOUND,
        /** 登録操作キーが異なる内容で使用済み。 */
        REPLAY_MISMATCH
    }

    /**
     * 一行の問題。row=0は操作全体を表す。
     * @param row 1始まりの入力行番号
     * @param field 項目名
     * @param reason 拒否理由
     */
    public record Issue(int row, String field, Reason reason) {
    }
}
