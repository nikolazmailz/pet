package com.pet.requisition.application.handler;


import com.pet.requisition.application.model.*;

public interface RequestHandler<T, R> {
    String requestType();

    Class<T> requestClass();

    R handle(PreparedRequest<T> request) throws Exception;
}
