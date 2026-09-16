package dev.template.application.common;

import java.util.Objects;

/**
 * feature共通の、期待される成功または業務上の失敗を表す型。
 *
 * <p>呼び出し側はsealed型のswitchで両方を処理する。技術障害はこの型に包まず例外として伝播し、 トランザクションをrollbackさせる。成功値と失敗理由は非nullとする。
 *
 * @param <S> 成功値の型
 * @param <F> featureごとに定義する失敗理由の型
 */
public sealed interface Result<S, F> {
    /**
     * 呼び出し側で処理すべき業務上の失敗を保持する。
     *
     * @param <S> 成功値の型
     * @param <F> 失敗理由の型
     * @param reason 型付きの失敗理由
     */
    record Failure<S, F>(F reason) implements Result<S, F> {
        /**
         * 失敗理由の非null契約を検証する。
         *
         * @param reason 失敗理由
         * @throws NullPointerException 失敗理由がnullの場合
         */
        public Failure {
            Objects.requireNonNull(reason);
        }
    }

    /**
     * 成功値を保持する。
     *
     * @param <S> 成功値の型
     * @param <F> 失敗理由の型
     * @param value 成功値
     */
    record Success<S, F>(S value) implements Result<S, F> {
        /**
         * 成功値の非null契約を検証する。
         *
         * @param value 成功値
         * @throws NullPointerException 成功値がnullの場合
         */
        public Success {
            Objects.requireNonNull(value);
        }
    }
}
