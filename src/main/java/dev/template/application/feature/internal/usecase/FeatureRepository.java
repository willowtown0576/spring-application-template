package dev.template.application.feature.internal.usecase;

import dev.template.application.feature.internal.domain.Feature;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Commandが必要とする書き込みPort。読取用SQLはFeatureDataSourceへ分離する。 */
public interface FeatureRepository {
    /**
     * 削除前の値を履歴へ保存し、id・version条件で元データを削除する。
     * 呼び出し元のtransaction内で行い、件数不一致は例外を伝播してrollbackする。
     * @param feature lock済みの削除前Entity
     * @param actor 認証済み実行主体
     * @param deletedAt 共通削除時刻
     */
    void archiveAndDelete(Feature feature, String actor, Instant deletedAt);

    /**
     * 登録結果を同じtransactionに保存する。
     * @param actor 実行主体
     * @param requestId 操作キー
     * @param ids 入力順の登録ID
     */
    void completeCreation(String actor, UUID requestId, List<UUID> ids);

    /**
     * Entityを新規保存する。DB例外は呼び出し元へ伝播させrollbackする。
     *
     * @param feature 保存するEntity
     */
    void insert(Feature feature);

    /**
     * ID順で対象をlockして読み取る。lockは呼び出し元transactionで保持する。
     * @param ids 対象ID
     * @return 存在するEntity
     */
    List<Feature> lock(List<UUID> ids);

    /**
     * 操作キーを予約する。同時予約はDBの一意制約で直列化する。
     * @param actor 実行主体
     * @param requestId 操作キー
     * @param hash 入力内容のhash
     * @return 新規予約か既存の登録結果
     */
    CreationReservation reserveCreation(String actor, UUID requestId, String hash);

    /**
     * 期待versionが一致するEntityを保存する。件数不一致は例外でrollbackする。
     * @param feature 変更後のEntity
     * @param expectedVersion 変更前のversion
     */
    void update(Feature feature, long expectedVersion);

    /**
     * 操作キーの予約結果。
     * @param fresh 今回初めて予約したか
     * @param hash 登録した内容のhash
     * @param ids 保存済みの結果ID
     */
    record CreationReservation(boolean fresh, String hash, List<UUID> ids) {
        /**
         * 保存結果を不変にする。
         * @param fresh 新規予約か
         * @param hash 内容hash
         * @param ids 結果ID
         */
        public CreationReservation {
            ids = List.copyOf(ids);
        }
    }
}
