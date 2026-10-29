package ru.ntdev.srhr.ms.easup.integration.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import ru.ntdev.srhr.common.constant.RolesConstant;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class EasupCertificateConfig {

    /** Роль из запроса → ключ в easup.ssl.* */
    private static final Map<String, String> ROLE_TO_STORE_KEY = Map.of(
            RolesConstant.MSS_FULL, "mss-full",
            RolesConstant.MSS_LIMITED, "mss-limited",
            RolesConstant.MSS_STANDARD, "mss-standard",
            RolesConstant.ESS, "ess",
            RolesConstant.TABEL, "tabel"
    );

    private final EasupProperties properties;
    private final Map<String, SSLContext> contextsByRole = new ConcurrentHashMap<>();

    /** Роли, под которые есть SSL-конфигурация. */
    public Set<String> supportedRoles() {
        return ROLE_TO_STORE_KEY.keySet();
    }

    public SSLContext configureHttpClientSsl(String role) {
        String storeKey = ROLE_TO_STORE_KEY.get(role);
        if (storeKey == null) {
            log.warn("При выборе сертификата не найдена указанная в запросе роль: [{}]", role);
            throw new IllegalArgumentException(
                    "Указанная роль не числится среди вариантов при выборе сертификата: " + role);
        }
        return contextsByRole.computeIfAbsent(role, r -> buildContext(storeKey));
    }

    private SSLContext buildContext(String storeKey) {
        EasupProperties.SslStores stores = properties.getSsl().get(storeKey);
        if (stores == null) {
            throw new IllegalStateException("Не задана конфигурация easup.ssl." + storeKey);
        }
        try {
            KeyStore trustStore = loadKeyStore(stores.getTrustStore(), stores.getTrustStorePassword(), stores.getTrustStoreType());
            KeyStore keyStore = loadKeyStore(stores.getKeyStore(), stores.getKeyStorePassword(), stores.getKeyStoreType());

            // TODO: TrustAllStrategy делает trustStore бесполезным — проверка цепочки сервера отключена.
            return SSLContextBuilder.create()
                    .loadKeyMaterial(keyStore, stores.getKeyStorePassword().toCharArray())
                    .loadTrustMaterial(trustStore, TrustAllStrategy.INSTANCE)
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Ошибка сборки SSLContext для easup.ssl." + storeKey, e);
        }
    }

    private static KeyStore loadKeyStore(String path, String password, String type)
            throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(type);
        try (InputStream in = new FileInputStream(ResourceUtils.getFile(path))) {
            keyStore.load(in, password.toCharArray());
        }
        return keyStore;
    }
}
