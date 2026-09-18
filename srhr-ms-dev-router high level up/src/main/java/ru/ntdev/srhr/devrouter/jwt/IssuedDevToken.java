package ru.ntdev.srhr.devrouter.jwt;

import java.time.Instant;
import java.util.Map;

public record IssuedDevToken(
        String user,
        String value,
        Instant expiresAt,
        Map<String, Object> claims
) {
}
