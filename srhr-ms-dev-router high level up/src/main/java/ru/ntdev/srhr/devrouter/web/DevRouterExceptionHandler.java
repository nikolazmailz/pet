package ru.ntdev.srhr.devrouter.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.ntdev.srhr.devrouter.jwt.UnknownDevUserException;

import java.util.Map;

@RestControllerAdvice
public class DevRouterExceptionHandler {

    @ExceptionHandler(UnknownDevUserException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> unknownUser(UnknownDevUserException exception) {
        return Map.of("error", exception.getMessage());
    }
}
