package com.pet.requisition.application.exception;
import ru.ntdev.srhr.ms.requisition.application.model.*;
public class PreparationException extends ProcessingException {
    private final SystemParams systemParams; private final RequestRecord requestRecord; private final long startedAt;
    public PreparationException(String code,String message,Throwable cause,SystemParams p,RequestRecord r,long startedAt){super(code,message,cause);this.systemParams=p;this.requestRecord=r;this.startedAt=startedAt;}
    public SystemParams getSystemParams(){return systemParams;} public RequestRecord getRequestRecord(){return requestRecord;} public long getStartedAt(){return startedAt;}
}
