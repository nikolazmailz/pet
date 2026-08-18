package com.pet.requestreply.handler;

import com.pet.requestreply.exception.HandlerNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RequestHandlerResolver {
    private final Map<String, RequestHandler<?, ?>> handlers;

    public RequestHandlerResolver(List<RequestHandler<?, ?>> handlers) {
        this.handlers = handlers.stream().collect(Collectors.toMap(
                RequestHandler::requestType,
                Function.identity()
        ));
    }

    public RequestHandler<?, ?> resolve(String requestType) {
        var handler = handlers.get(requestType);
        if (handler == null) {
            throw new HandlerNotFoundException(requestType);
        }
        return handler;
    }
}
