package com.pet.requestreply.handler;

import com.pet.requestreply.model.*;

public interface RequestHandler<T, R> {

    String requestType();

    Class<T> requestClass();

    R handle(PreparedRequest<T> request) throws Exception;
}
