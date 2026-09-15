package dev.template.application.feature.api.command;

import dev.template.application.common.Result;
import java.util.UUID;

/**
 * Featureを変更する公開API。REST、Vaadin、他featureはこの契約を通して更新する。
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
    Result<UUID, CreateFailure> create(CreateFeatureParam param);
}
