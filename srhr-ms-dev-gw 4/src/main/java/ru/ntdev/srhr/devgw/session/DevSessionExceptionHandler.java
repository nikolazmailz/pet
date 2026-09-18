package ru.ntdev.srhr.devgw.session;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.ntdev.srhr.devgw.jwt.UnknownDevUserException;

import java.util.Map;

@RestControllerAdvice
public class DevSessionExceptionHandler {

    @ExceptionHandler(UnknownDevUserException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> unknownUser(UnknownDevUserException exception) {
        return Map.of("error", exception.getMessage());
    }
}
