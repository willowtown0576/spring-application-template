package dev.template.application.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** システム主体の権限と、正常・例外・nested実行後のcontext復元を検証する。 */
class SystemExecutionTest {
    /**
     * 利用者contextを保持し、nestedなsystem呼び出しと例外の後に元のcontextを復元する。
     * @throws Exception 実行境界が予期しない例外を送出した場合
     */
    @Test
    void restoresCallerAfterNestedSuccessAndFailure() throws Exception {
        final var previous = SecurityContextHolder.getContext();
        final var caller = SecurityContextHolder.createEmptyContext();
        caller.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("human", null, List.of()));
        SecurityContextHolder.setContext(caller);

        // nestedな主体切替と、その内側での失敗を確認する。
        try {
            final var execution = new SystemExecution();
            execution.call(SystemActor.BATCH, () -> {
                final var batch = SecurityContextHolder.getContext();
                assertThat(batch.getAuthentication().getPrincipal()).isEqualTo(SystemActor.BATCH);
                assertThat(batch.getAuthentication().getName()).isEqualTo("system:batch");

                final var failure = new IllegalStateException("expected");
                assertThatThrownBy(() -> execution.call(SystemActor.SCHEDULER, () -> {
                    assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                        .isEqualTo(SystemActor.SCHEDULER);
                    throw failure;
                })).isSameAs(failure);
                assertThat(SecurityContextHolder.getContext()).isSameAs(batch);
                return null;
            });
            assertThat(SecurityContextHolder.getContext()).isSameAs(caller);

            // 最外側の呼び出しが失敗しても、利用者contextへ復元する。
            assertThatThrownBy(() -> execution.call(SystemActor.BATCH, () -> {
                throw new IllegalStateException();
            })).isInstanceOf(IllegalStateException.class);
            assertThat(SecurityContextHolder.getContext()).isSameAs(caller);
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }
}
