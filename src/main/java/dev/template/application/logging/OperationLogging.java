package dev.template.application.logging;

import dev.template.application.common.Result;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Command/Queryの境界で処理名・結果種別・所要時間を記録する。 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
class OperationLogging {
    /** Command／Queryの処理結果・所要時間と障害情報の出力先。 */
    private static final Logger LOG = LoggerFactory.getLogger(OperationLogging.class);

    /**
     * トランザクション完了まで観測し、技術例外はそのまま再送出する。
     *
     * @param call 呼び出される境界メソッド
     * @return 元の戻り値
     * @throws Throwable 元の処理が送出した例外
     */
    @Around("@annotation(org.springframework.transaction.annotation.Transactional) && (execution(* *..internal.command..*.*(..)) || execution(* *..internal.query..*.*(..)))")
    Object observe(final ProceedingJoinPoint call) throws Throwable {
        final String operation = call.getSignature().toShortString();
        final boolean command = call.getSignature().getDeclaringTypeName().contains(".internal.command.");
        (command ? LOG.atInfo() : LOG.atDebug()).log("Operation start operation={}", operation);
        final long start = System.nanoTime();
        try {
            final Object result = call.proceed();
            if (result instanceof Result.Failure<?, ?>) {
                LOG.info("Operation failure operation={} durationMs={}", operation,
                    (System.nanoTime() - start) / 1_000_000);
            } else {
                (command ? LOG.atInfo() : LOG.atDebug()).log("Operation success operation={} durationMs={}", operation,
                    (System.nanoTime() - start) / 1_000_000);
            }
            return result;
        } catch (final Throwable exception) {
            TechnicalErrors.log(LOG, operation, exception);
            throw exception;
        }
    }
}
