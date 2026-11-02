package dev.template.application.web.ui;

/**
 * Feature作成・検索の入力と表示結果。
 * @param name 作成する名前
 * @param id 検索ID
 * @param message 利用者向け結果
 * @param nameError 名前の入力エラー
 * @param idError IDの入力エラー
 */
public record FeaturePage(String name, String id, String message, String nameError, String idError) {
}
