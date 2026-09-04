package ru.ntdev.srhr.requisitionrest.application;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import ru.ntdev.srhr.requisitionrest.domain.PendingCandidatesRemoteException;

@Component
public class CurrentUserPernrResolver {
    /**
     * В боевом проекте замените чтение заголовка на существующий JWT/SystemParams-контекст.
     * Клиент не должен иметь возможность подменить pernr.
     */
    public String resolve(HttpServletRequest request, String traceId) {
        Object attribute = request.getAttribute("pernr");
        String value = attribute == null ? request.getHeader("X-Pernr") : attribute.toString();
        if (value == null || value.isBlank()) {
            throw new PendingCandidatesRemoteException(
                    "CURRENT_USER_PERNR_NOT_FOUND", "Не найден табельный номер текущего пользователя", traceId);
        }
        return value;
    }
}
