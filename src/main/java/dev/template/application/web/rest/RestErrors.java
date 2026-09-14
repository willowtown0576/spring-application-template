package dev.template.application.web.rest;

import dev.template.application.logging.TechnicalErrors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class RestErrors extends ResponseEntityExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(RestErrors.class);

  @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
  void propagateSecurityException(RuntimeException exception) {
    throw exception;
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail unexpected(Exception exception) {
    TechnicalErrors.log(LOG, "Unexpected REST error", exception);
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
  }
}
