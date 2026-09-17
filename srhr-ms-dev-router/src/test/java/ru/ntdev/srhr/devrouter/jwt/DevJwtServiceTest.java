package ru.ntdev.srhr.devrouter.jwt;

import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DevJwtServiceTest {

    @Test
    void createsSignedTokenWithClaimsExpectedByJwtUtil() throws Exception {
        DevJwtProperties properties = properties();
        DevJwtService service = new DevJwtService(properties);

        IssuedDevToken issued = service.issue("developer", "10.20.30.40");
        SignedJWT parsed = SignedJWT.parse(issued.value());

        Map<String, Object> jwks = service.jwks();
        @SuppressWarnings("unchecked")
        Map<String, Object> firstKey = ((List<Map<String, Object>>) jwks.get("keys")).get(0);
        JWK jwk = JWK.parse(firstKey);

        assertThat(parsed.verify(new ECDSAVerifier((ECKey) jwk))).isTrue();
        assertThat(parsed.getHeader().getKeyID()).isEqualTo("test-key");
        assertThat(parsed.getJWTClaimsSet().getSubject()).isEqualTo("developer@VTB.RU");
        assertThat(parsed.getJWTClaimsSet().getStringClaim("ctxi")).isEqualTo("test-session");
        assertThat(parsed.getJWTClaimsSet().getStringClaim("channel")).isEqualTo("WEB");
        assertThat(parsed.getJWTClaimsSet().getStringClaim("realm")).isEqualTo("SRHR");
        assertThat(parsed.getJWTClaimsSet().getStringClaim("ip")).isEqualTo("10.20.30.40");
        assertThat(parsed.getJWTClaimsSet().getJWTID()).isNotBlank();
        assertThat(parsed.getJWTClaimsSet().getExpirationTime()).isAfter(parsed.getJWTClaimsSet().getIssueTime());
    }

    private static DevJwtProperties properties() {
        DevJwtProperties properties = new DevJwtProperties();
        properties.setIssuer("test-issuer");
        properties.setAudience("test-audience");
        properties.setKeyId("test-key");
        properties.setTtl(Duration.ofMinutes(10));
        properties.setDefaultUser("developer");

        DevJwtProperties.UserProfile developer = new DevJwtProperties.UserProfile();
        developer.setSub("developer@VTB.RU");
        developer.setSessionId("test-session");
        developer.setChannel("WEB");
        developer.setRealm("SRHR");
        properties.setUsers(Map.of("developer", developer));
        return properties;
    }
}
