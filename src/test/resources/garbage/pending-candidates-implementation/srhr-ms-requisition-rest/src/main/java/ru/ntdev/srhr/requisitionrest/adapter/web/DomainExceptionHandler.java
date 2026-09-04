package ru.ntdev.srhr.requisitionrest.adapter.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.ntdev.srhr.requisitionrest.domain.DomainException;
import ru.ntdev.srhr.requisitionrest.domain.PendingCandidatesTimeoutException;

@RestControllerAdvice
public class DomainExceptionHandler {
    @ExceptionHandler(PendingCandidatesTimeoutException.class)
    ResponseEntity<ApiError> handleTimeout(PendingCandidatesTimeoutException ex) {
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(new ApiError(ex.getCode(), ex.getMessage(), ex.getTraceId()));
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiError> handleDomain(DomainException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiError(ex.getCode(), ex.getMessage(), ex.getTraceId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Некорректный запрос");
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", message, null));
    }
}
