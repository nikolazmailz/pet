package ru.ntdev.srhr.mdi.adapter.in.web;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.ntdev.srhr.mdi.domain.PendingCandidatesIntegrationException;

@RestControllerAdvice
public class DomainExceptionHandler {
    @ExceptionHandler(PendingCandidatesIntegrationException.class)
    ResponseEntity<ApiError> handleIntegration(PendingCandidatesIntegrationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiError("PENDING_CANDIDATES_INTEGRATION_ERROR", ex.getMessage(), MDC.get("traceId")));
    }
}
