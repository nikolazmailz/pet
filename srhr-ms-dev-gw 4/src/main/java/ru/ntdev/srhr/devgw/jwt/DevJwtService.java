package ru.ntdev.srhr.devgw.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class DevJwtService {

    private final DevJwtProperties properties;
    private final ECKey signingKey;

    public DevJwtService(DevJwtProperties properties) throws JOSEException {
        this.properties = properties;
        validateConfiguration(properties);
        this.signingKey = new ECKeyGenerator(Curve.P_256)
                .algorithm(JWSAlgorithm.ES256)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(properties.getKeyId())
                .generate();
    }

    public String issue(String user, String sessionId, String clientIp) {
        DevJwtProperties.UserProfile profile = properties.getUsers().get(user);
        if (profile == null) {
            throw new UnknownDevUserException(user);
        }

        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(properties.getIssuer())
                .audience(properties.getAudience())
                .subject(profile.getSub())
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now.minusSeconds(5)))
                .expirationTime(Date.from(now.plus(properties.getTtl())))
                .jwtID(UUID.randomUUID().toString())
                .claim("ctxi", StringUtils.hasText(sessionId) ? sessionId : profile.getSessionId())
                .claim("channel", profile.getChannel())
                .claim("realm", profile.getRealm())
                .claim("ip", StringUtils.hasText(clientIp) ? clientIp : "127.0.0.1")
                .build();

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.ES256)
                        .keyID(properties.getKeyId())
                        .type(JOSEObjectType.JWT)
                        .build(),
                claims
        );

        try {
            jwt.sign(new ECDSASigner(signingKey));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Не удалось подписать DEV JWT", e);
        }
    }

    public Map<String, Object> jwks() {
        return new JWKSet(signingKey.toPublicJWK()).toJSONObject();
    }

    public Map<String, DevJwtProperties.UserProfile> users() {
        return Map.copyOf(properties.getUsers());
    }

    private static void validateConfiguration(DevJwtProperties properties) {
        if (!StringUtils.hasText(properties.getDefaultUser())
                || !properties.getUsers().containsKey(properties.getDefaultUser())) {
            throw new IllegalStateException("DEV-пользователь по умолчанию отсутствует в dev-gateway.jwt.users");
        }
        properties.getUsers().forEach((name, profile) -> {
            if (!StringUtils.hasText(profile.getSub()) || !profile.getSub().contains("@")) {
                throw new IllegalStateException("JWT sub пользователя '" + name + "' должен содержать домен после @");
            }
            if (!StringUtils.hasText(profile.getSessionId())
                    || !StringUtils.hasText(profile.getChannel())
                    || !StringUtils.hasText(profile.getRealm())) {
                throw new IllegalStateException("Для пользователя '" + name + "' обязательны ctxi, channel и realm");
            }
        });
    }
}
