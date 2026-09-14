package dev.template.application.logging;

import dev.template.application.feature.api.command.Result;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
class OperationLogging {
  private static final Logger LOG = LoggerFactory.getLogger(OperationLogging.class);

  @Around(
      "@annotation(org.springframework.transaction.annotation.Transactional) && (execution(* dev.template.application..internal.command.*.*(..)) || execution(* dev.template.application..internal.query.*.*(..)))")
  Object observe(ProceedingJoinPoint call) throws Throwable {
    String operation = call.getSignature().toShortString();
    boolean command = call.getSignature().getDeclaringTypeName().contains(".internal.command.");
    (command ? LOG.atInfo() : LOG.atDebug()).log("Operation start operation={}", operation);
    long start = System.nanoTime();
    try {
      Object result = call.proceed();
      if (result instanceof Result.Failure<?, ?>) {
        LOG.info(
            "Operation failure operation={} durationMs={}",
            operation,
            (System.nanoTime() - start) / 1_000_000);
      } else {
        (command ? LOG.atInfo() : LOG.atDebug())
            .log(
                "Operation success operation={} durationMs={}",
                operation,
                (System.nanoTime() - start) / 1_000_000);
      }
      return result;
    } catch (Throwable exception) {
      TechnicalErrors.log(LOG, operation, exception);
      throw exception;
    }
  }
}
