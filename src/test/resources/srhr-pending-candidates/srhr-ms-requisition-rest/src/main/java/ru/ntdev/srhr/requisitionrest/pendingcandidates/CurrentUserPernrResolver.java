package ru.ntdev.srhr.requisitionrest.pendingcandidates;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Извлечение табельного номера текущего пользователя из JWT
 * (аутентификация — общий JwtTokenFilter из srhr-common, @EnableSrhrJwtSecurity).
 *
 * ДОПУЩЕНИЕ: claim называется "pernr". Если в токене иное имя claim'а —
 * поправить здесь, это единственная точка.
 */
@Component
public class CurrentUserPernrResolver {

    private static final String PERNR_CLAIM = "pernr";

    public String resolve(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String pernr = jwt.getClaimAsString(PERNR_CLAIM);
            if (pernr != null && !pernr.isBlank()) {
                return pernr;
            }
        }
        throw new IllegalStateException(
                "Не удалось определить табельный номер текущего пользователя из JWT");
    }
}
