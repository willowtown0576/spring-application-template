package dev.template.application.feature.api.command;

import dev.template.application.common.Result;
import java.util.List;
import java.util.UUID;

/**
 * Featureを変更する公開API。REST、MVC画面、他featureはこの契約を通して更新する。
 *
 * <p>認可とトランザクションは実装側の境界で適用する。認証方式とHTTPの制御は技術adapterの責務とする。
 */
public interface FeatureCommands {

    /**
     * 名前を検証してFeatureを作成する。
     *
     * @param param 作成入力。nameのnullや不正値はINVALID_NAMEとして返す
     * @return 成功時はUUID v7、業務検証に失敗した場合は型付き理由
     * @throws NullPointerException paramがnullの場合
     */
    Result<UUID, FeatureCommandFailure> create(CreateFeatureParam param);

    /**
     * 入力行を一つのtransactionで登録し、同一操作の再送に同じ結果を返す。
     * @param param 登録操作
     * @return 入力順の登録IDまたは業務拒否
     */
    Result<List<UUID>, FeatureCommandFailure> createMany(CreateFeaturesParam param);

    /**
     * 期待versionが一致する一件を履歴へ退避して削除する。
     * @param param 実行主体と確認済み対象
     * @return 削除したIDまたは業務拒否
     */
    Result<UUID, FeatureCommandFailure> delete(DeleteFeatureParam param);

    /**
     * 全件を検査し、一つのtransactionで履歴へ退避して削除する。
     * @param param 実行主体と入力順の対象
     * @return 入力順の削除IDまたは業務拒否
     */
    Result<List<UUID>, FeatureCommandFailure> deleteMany(DeleteFeaturesParam param);

    /**
     * 一行をversion付きで更新する。変更なしの場合はversionを維持する。
     * @param param 更新入力
     * @return 対象IDまたは業務拒否
     */
    Result<UUID, FeatureCommandFailure> update(UpdateFeatureParam param);

    /**
     * 一括入力を全件検査して更新する。拒否時は一件も更新しない。
     * @param param 入力順の更新行
     * @return 実際に変更したIDまたは業務拒否
     */
    Result<List<UUID>, FeatureCommandFailure> updateMany(UpdateFeaturesParam param);
}
