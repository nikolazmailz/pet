package com.pet.requisition.application.handler;
import java.util.*; import java.util.function.Function; import java.util.stream.Collectors;
public class RequestHandlerResolver {
    private final Map<String,RequestHandler<?,?>> handlers;
    public RequestHandlerResolver(List<RequestHandler<?,?>> handlers){this.handlers=handlers.stream().collect(Collectors.toMap(RequestHandler::requestType,Function.identity()));}
    public RequestHandler<?,?> resolve(String requestType){RequestHandler<?,?> h=handlers.get(requestType); if(h==null) throw new IllegalArgumentException("Handler not found: "+requestType); return h;}
}
