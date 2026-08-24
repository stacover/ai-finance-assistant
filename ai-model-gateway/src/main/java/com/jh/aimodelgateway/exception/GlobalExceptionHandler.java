package com.jh.aimodelgateway.exception;

import com.jh.aimodelgateway.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author jinhang
 * @since 2026/8/24 22:07
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AiModelException.class)
  public ResponseEntity<ErrorResponse> handleAiModelException(AiModelException exception) {

    AiErrorCode errorCode = exception.getErrorCode();

    HttpStatus status =
        switch (errorCode) {
          case MODEL_TIMEOUT, MODEL_UNAVAILABLE, MODEL_REQUEST_FAILED ->
              HttpStatus.SERVICE_UNAVAILABLE;

          case STRUCTURED_OUTPUT_ERROR -> HttpStatus.BAD_GATEWAY;

          default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

    return ResponseEntity.status(status)
        .body(new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), LocalDateTime.now()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {

    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getDefaultMessage())
            .orElse("请求参数不正确");

    return ResponseEntity.badRequest()
        .body(
            new ErrorResponse(AiErrorCode.INVALID_REQUEST.getCode(), message, LocalDateTime.now()));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(
      ConstraintViolationException exception) {

    return ResponseEntity.badRequest()
        .body(
            new ErrorResponse(
                AiErrorCode.INVALID_REQUEST.getCode(), "请求参数不正确", LocalDateTime.now()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnknownException(Exception exception) {

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse("SYSTEM_ERROR", "系统异常，请稍后重试", LocalDateTime.now()));
  }
}
