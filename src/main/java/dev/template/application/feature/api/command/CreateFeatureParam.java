package dev.template.application.feature.api.command;

/**
 * Feature作成の入力。HTTP / Vaadinの入力をこのrecordへ変換してCommandへ渡す。
 *
 * <p>nameのnull・文字数・Unicode整合性はCommandで判定し、INVALID_NAMEとして返す。
 * 各adapterからの呼出しに共通の入力規則を定義する。
 *
 * @param name 入力文字列をそのまま保存する名前
 */
public record CreateFeatureParam(String name) {
}
