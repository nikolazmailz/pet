package com.pet.common.reqreply.req;

public interface FilestorageService {

    /**
     * Сохраняет содержимое сообщения в filestorage.
     *
     * @param adLogin        логин пользователя (заголовок pernr)
     * @param sessionId      идентификатор сессии
     * @param traceId        идентификатор трассировки (заголовок logId)
     * @param requestUid     uuid исходного запроса
     * @param messageName    имя сообщения (например, adLogin_requestType)
     * @param messageContent содержимое сообщения (json)
     * @return messageUID сохранённого сообщения
     */
    String putMessageToDb(String adLogin, String sessionId, String traceId,
                          String requestUid, String messageName, String messageContent);
}
