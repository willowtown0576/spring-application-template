package dev.template.application.feature.api.command;

/** Feature作成時に呼び出し側が処理する業務上の失敗理由。 */
public enum CreateFailure {
    /** 名前がdomainの文字数・空値・Unicode制約に違反した。 */
    INVALID_NAME
}
