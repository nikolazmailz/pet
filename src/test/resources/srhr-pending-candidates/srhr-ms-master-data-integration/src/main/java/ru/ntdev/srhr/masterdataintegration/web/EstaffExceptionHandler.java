package ru.ntdev.srhr.masterdataintegration.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.ntdev.srhr.common.dto.pendingcandidates.PendingCandidatesError;
import ru.ntdev.srhr.masterdataintegration.exception.EstaffIntegrationException;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class EstaffExceptionHandler {

    @ExceptionHandler(EstaffIntegrationException.class)
    public ResponseEntity<Map<String, PendingCandidatesError>> handleEstaffError(
            EstaffIntegrationException e) {
        log.error("Ошибка интеграции с Е-стафф", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", new PendingCandidatesError(
                        PendingCandidatesError.ESTAFF_UNAVAILABLE, e.getMessage())));
    }
}
