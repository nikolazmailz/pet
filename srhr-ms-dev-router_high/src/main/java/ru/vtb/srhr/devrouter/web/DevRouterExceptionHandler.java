package ru.vtb.srhr.devrouter.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.vtb.srhr.devrouter.impersonation.UnknownDevUserException;

import java.util.Collection;

@RestControllerAdvice
@Profile("dev")
@ConditionalOnProperty(name = "dev-router.impersonation.enabled", havingValue = "true")
public class DevRouterExceptionHandler {

    @ExceptionHandler(UnknownDevUserException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorView unknownUser(UnknownDevUserException exception) {
        return new ErrorView(
                "UNKNOWN_DEV_USER",
                exception.getMessage(),
                exception.getAvailableUsers()
        );
    }

    public record ErrorView(String code, String message, Collection<String> availableUsers) {
    }
}
