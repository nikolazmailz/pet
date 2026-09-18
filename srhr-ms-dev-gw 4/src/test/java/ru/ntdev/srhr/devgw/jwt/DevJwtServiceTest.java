package ru.ntdev.srhr.devgw.jwt;

import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DevJwtServiceTest {

    @Test
    void createsSignedTokenWithRequiredClaims() throws Exception {
        DevJwtProperties properties = new DevJwtProperties();
        properties.setDefaultUser("employee");
        properties.setUsers(Map.of("employee", profile()));
        DevJwtService service = new DevJwtService(properties);

        SignedJWT jwt = SignedJWT.parse(service.issue("employee", "browser-session", "10.20.30.40"));
        @SuppressWarnings("unchecked")
        Map<String, Object> publicKey = ((List<Map<String, Object>>) service.jwks().get("keys")).get(0);

        assertThat(jwt.verify(new ECDSAVerifier((ECKey) JWK.parse(publicKey)))).isTrue();
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo("employee@VTB.RU");
        assertThat(jwt.getJWTClaimsSet().getStringClaim("ctxi")).isEqualTo("browser-session");
        assertThat(jwt.getJWTClaimsSet().getStringClaim("channel")).isEqualTo("WEB");
        assertThat(jwt.getJWTClaimsSet().getStringClaim("realm")).isEqualTo("SRHR");
        assertThat(jwt.getJWTClaimsSet().getStringClaim("ip")).isEqualTo("10.20.30.40");
        assertThat(jwt.getJWTClaimsSet().getJWTID()).isNotBlank();
    }

    private static DevJwtProperties.UserProfile profile() {
        DevJwtProperties.UserProfile profile = new DevJwtProperties.UserProfile();
        profile.setSub("employee@VTB.RU");
        profile.setSessionId("employee-session");
        profile.setChannel("WEB");
        profile.setRealm("SRHR");
        return profile;
    }
}
