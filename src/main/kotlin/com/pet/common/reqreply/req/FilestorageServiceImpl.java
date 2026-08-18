package com.pet.common.reqreply.req;

import ru.ntdev.srhr.common.reqreply.dto.MessagePostDto;
import ru.ntdev.srhr.common.reqreply.dto.MessagePostRequestDto;

public class FilestorageServiceImpl implements FilestorageService {

    private static final String URL_PUT_MESSAGE = "/filestorage/message";

    private final FilestorageRestClient restClient;

    public FilestorageServiceImpl(FilestorageRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public String putMessageToDb(String adLogin, String sessionId, String traceId,
                                 String requestUid, String messageName, String messageContent) {
        MessagePostDto result = restClient.post(
                URL_PUT_MESSAGE,
                new MessagePostRequestDto(requestUid, messageName, messageContent),
                MessagePostDto.class,
                adLogin, sessionId, traceId);

        if (result == null || result.getMessageUID() == null) {
            throw new IllegalStateException(
                    "Filestorage returned empty response for requestUid=" + requestUid);
        }
        return result.getMessageUID();
    }
}
