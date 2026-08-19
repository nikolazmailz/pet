package com.pet.requisition.application.handler;
import ru.ntdev.srhr.ms.requisition.application.model.PreparedRequest;
public interface RequestHandler<T,R> {
    String requestType();
    Class<T> requestClass();
    R handle(PreparedRequest<T> request) throws Exception;
}
