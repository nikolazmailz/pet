package ru.ntdev.srhr.devrouter.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
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

    public IssuedDevToken issue(String requestedUser, String clientIp) {
        return issue(requestedUser, null, clientIp);
    }

    public IssuedDevToken issue(String requestedUser, String sessionIdOverride, String clientIp) {
        String user = StringUtils.hasText(requestedUser)
                ? requestedUser
                : properties.getDefaultUser();

        DevJwtProperties.UserProfile profile = properties.getUsers().get(user);
        if (profile == null) {
            throw new UnknownDevUserException(user);
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getTtl());
        String jti = UUID.randomUUID().toString();
        String sessionId = StringUtils.hasText(sessionIdOverride)
                ? sessionIdOverride
                : profile.getSessionId();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(properties.getIssuer())
                .audience(properties.getAudience())
                .subject(profile.getSub())
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now.minusSeconds(5)))
                .expirationTime(Date.from(expiresAt))
                .jwtID(jti)
                .claim("ctxi", sessionId)
                .claim("channel", profile.getChannel())
                .claim("realm", profile.getRealm())
                .claim("ip", StringUtils.hasText(clientIp) ? clientIp : "127.0.0.1")
                .build();

        SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.ES256)
                        .keyID(properties.getKeyId())
                        .type(com.nimbusds.jose.JOSEObjectType.JWT)
                        .build(),
                claims
        );

        try {
            signedJwt.sign(new ECDSASigner(signingKey));
        } catch (JOSEException e) {
            throw new IllegalStateException("Cannot sign DEV JWT", e);
        }

        return new IssuedDevToken(
                user,
                signedJwt.serialize(),
                expiresAt,
                new LinkedHashMap<>(claims.getClaims())
        );
    }

    public Map<String, Object> jwks() {
        return new JWKSet(signingKey.toPublicJWK()).toJSONObject();
    }

    public Map<String, DevJwtProperties.UserProfile> users() {
        return Map.copyOf(properties.getUsers());
    }

    private static void validateConfiguration(DevJwtProperties properties) {
        if (!StringUtils.hasText(properties.getDefaultUser())) {
            throw new IllegalStateException("dev-router.jwt.default-user is required");
        }
        if (!properties.getUsers().containsKey(properties.getDefaultUser())) {
            throw new IllegalStateException("Default DEV user is absent from dev-router.jwt.users");
        }
        properties.getUsers().forEach((name, profile) -> {
            if (!StringUtils.hasText(profile.getSub()) || !profile.getSub().contains("@")) {
                throw new IllegalStateException("JWT sub for user '" + name + "' must contain a domain after @");
            }
            if (!StringUtils.hasText(profile.getSessionId())
                    || !StringUtils.hasText(profile.getChannel())
                    || !StringUtils.hasText(profile.getRealm())) {
                throw new IllegalStateException("JWT ctxi, channel and realm are required for user '" + name + "'");
            }
        });
    }
}
