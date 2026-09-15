package dev.template.application.security;

import java.util.Objects;
import java.util.concurrent.Callable;
import org.springframework.security.concurrent.DelegatingSecurityContextCallable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

/** 信頼されたBatch／Scheduler worker内でシステム主体を適用し、終了時に元のcontextを復元する。 */
@Component
public final class SystemExecution {
    /**
     * 指定主体で同期実行する。非同期処理では実行worker内でこの境界を適用する。
     * @param <T> 戻り値の型
     * @param actor 信頼されたadapterのコードで選択したシステム主体
     * @param action worker内で実行する処理
     * @return 処理結果
     * @throws Exception 処理が送出した例外。そのまま伝播する
     */
    public <T> T call(final SystemActor actor, final Callable<T> action) throws Exception {
        Objects.requireNonNull(actor);
        Objects.requireNonNull(action);
        final var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new PreAuthenticatedAuthenticationToken(actor, null, actor.authorities()));
        return new DelegatingSecurityContextCallable<>(action, context).call();
    }
}
