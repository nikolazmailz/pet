package com.pet.requisition.application.service;
import ru.ntdev.srhr.ms.requisition.application.model.*;
import ru.ntdev.srhr.ms.requisition.application.port.out.*;
public class ResponseProcessingService {
    public static final String STATUS_REPLY="REPLY"; public static final String STATUS_ERROR_REPLY="ERROR_REPLY";
    private final MessageCodecPort codec; private final FileStoragePort fileStorage; private final RequestPersistencePort persistence; private final int maxInnerMessageSizeKb;
    public ResponseProcessingService(MessageCodecPort codec,FileStoragePort fileStorage,RequestPersistencePort persistence,int maxInnerMessageSizeKb){this.codec=codec;this.fileStorage=fileStorage;this.persistence=persistence;this.maxInnerMessageSizeKb=maxInnerMessageSizeKb;}

    public String success(PreparedRequest<?> prepared,Object payload) throws Exception {
        persistence.updateRequestStatus(prepared.correlationId(),STATUS_REPLY);
        ResponseEnvelope<Object> response=ResponseEnvelope.success(prepared.systemParams(),payload);
        return finish(prepared.systemParams(),prepared.requestRecord(),prepared.startedAt(),response,"200");
    }

    public String error(SystemParams params,RequestRecord requestRecord,long startedAt,String code,String message) throws Exception {
        if(params==null) {
            params=new SystemParams(); params.setCorrelationId("unknown"); params.setRequestType("unknown");
        }
        if(requestRecord!=null && params.getCorrelationId()!=null) persistence.updateRequestStatus(params.getCorrelationId(),STATUS_ERROR_REPLY);
        ResponseEnvelope<Void> response=ResponseEnvelope.error(params,code,message);
        return finish(params,requestRecord,startedAt,response,code);
    }

    private String finish(SystemParams params,RequestRecord requestRecord,long startedAt,ResponseEnvelope<?> response,String code) throws Exception {
        long processingTime=System.currentTimeMillis()-startedAt;
        String fullMessage=codec.serializeResponse(response);
        ResponseRecord responseRecord=null;
        if(requestRecord!=null) responseRecord=persistence.saveResponse(params.getCorrelationId(),requestRecord,fullMessage,processingTime,code);

        if("200".equals(code) && requestRecord!=null && responseRecord!=null) {
            persistence.deleteResponse(params.getCorrelationId(),responseRecord.id());
            persistence.deleteRequest(params.getCorrelationId(),requestRecord.id());
        }

        return checkMessageSize(params,response,fullMessage);
    }

    private String checkMessageSize(SystemParams params,ResponseEnvelope<?> response,String message) throws Exception {
        int messageSizeKb=((2*message.length())+32)/1000;
        if(messageSizeKb<maxInnerMessageSizeKb) return message;
        String messageUuid=fileStorage.storeMessage(params.getAdLogin(),params.getSessionId(),params.getTraceId(),params.getAdLogin()+"_"+params.getRequestType(),message,params.getCorrelationId());
        return codec.serializeResponse(response.asStoredReference(messageUuid));
    }
}
