package dev.template.application.feature.api.command;

import java.util.List;

/**
 * 複数Featureの更新入力。全行を一つのtransactionで検証・保存する。
 * @param rows 入力順の更新行
 */
public record UpdateFeaturesParam(List<UpdateFeatureParam> rows) {
    /**
     * 入力の外部変更を防ぎ、欠落行は同じ行番号で検証できる形にする。
     * @param rows 更新行
     */
    public UpdateFeaturesParam {
        rows = rows == null
            ? List.of()
            : List.copyOf(
                rows.stream().map(row -> row == null ? new UpdateFeatureParam(null, null, null, 0) : row).toList());
    }
}
