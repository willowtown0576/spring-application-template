package dev.template.application.logging;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import org.slf4j.Logger;

/** 機密情報が含まれ得る例外messageを出さず、型とstack traceだけを記録する。 */
public final class TechnicalErrors {
    /** static methodのみを公開するためのprivate constructor。 */
    private TechnicalErrors() {
    }

    /**
     * causeの循環を検出しながら障害箇所を記録する。
     *
     * @param logger 出力先logger
     * @param operation 固定の操作名。利用者入力を渡さない
     * @param exception 記録する障害
     */
    public static void log(final Logger logger, final String operation, final Throwable exception) {
        final var visited = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
        for (Throwable current = exception; current != null && visited.add(current); current = current.getCause()) {
            logger.error("{} exceptionType={} stack={}", operation, current.getClass().getName(),
                Arrays.toString(current.getStackTrace()));
        }
    }
}
