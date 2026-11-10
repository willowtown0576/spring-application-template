package dev.template.application.feature.internal.query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 公開Queryの結果型に依存しない、参照SQL用Port。 */
public interface FeatureDataSource {
    /**
     * IDで取得する。
     * @param id ID
     * @return 未検出ならempty
     */
    Optional<FeatureData> findById(UUID id);

    /**
     * 境界の後ろからsize＋1件を取得する。前ページは逆順で返す。
     * @param search 内部検索条件
     * @return 取得順の結果
     */
    List<FeatureData> search(Search search);

    /**
     * 内部参照行。
     * @param id ID
     * @param name 名前
     * @param active 有効か
     * @param updatedAt 更新日時
     * @param version version
     */
    record FeatureData(UUID id, String name, boolean active, Instant updatedAt, long version) {
    }

    /**
     * SQLに渡す境界値。
     * @param id ID
     * @param name 名前
     * @param updatedAt 更新日時
     */
    record Position(UUID id, String name, Instant updatedAt) {
    }

    /**
     * SQLに必要な検索条件。
     * @param name 名前条件
     * @param active 状態条件。nullは全状態
     * @param byName 名前順か
     * @param descending 降順か
     * @param size ページサイズ
     * @param position 境界
     * @param previous 前ページか
     */
    record Search(String name, Boolean active, boolean byName, boolean descending, int size, Position position,
        boolean previous) {
    }
}
