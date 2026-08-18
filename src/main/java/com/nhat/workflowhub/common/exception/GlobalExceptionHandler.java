package com.nhat.workflowhub.common.exception;

import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Map<String, Object>> handleApiException(ApiException exception) {
    return ResponseEntity.status(exception.getStatus()).body(Map.of(
        "success", false,
        "message", exception.getMessage(),
        "timestamp", OffsetDateTime.now().toString()
    ));
  }
}
