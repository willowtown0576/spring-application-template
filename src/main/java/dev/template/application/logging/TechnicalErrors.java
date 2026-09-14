package dev.template.application.logging;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import org.slf4j.Logger;

public final class TechnicalErrors {
  private TechnicalErrors() {}

  public static void log(Logger logger, String operation, Throwable exception) {
    var visited = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
    for (Throwable current = exception;
        current != null && visited.add(current);
        current = current.getCause()) {
      logger.error(
          "{} exceptionType={} stack={}",
          operation,
          current.getClass().getName(),
          Arrays.toString(current.getStackTrace()));
    }
  }
}
