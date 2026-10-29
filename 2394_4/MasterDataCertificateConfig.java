package ru.ntdev.srhr.ms.sys.delegation.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

/**
 * SSLContext для master-data (СПО). Собирается один раз при старте приложения —
 * раньше кейсторы читались с диска на каждый запрос.
 */
@Slf4j
@Configuration
public class MasterDataCertificateConfig {

    @Bean
    public SSLContext masterDataSslContext(MasterDataProperties properties) {
        MasterDataProperties.Ssl ssl = properties.getSsl();
        try {
            KeyStore trustStore = loadKeyStore(ssl.getTrustStore(), ssl.getTrustStorePassword(), ssl.getTrustStoreType());
            KeyStore keyStore = loadKeyStore(ssl.getKeyStore(), ssl.getKeyStorePassword(), ssl.getKeyStoreType());

            // TODO: TrustAllStrategy делает trustStore бесполезным — проверка цепочки сервера отключена.
            return SSLContextBuilder.create()
                    .loadKeyMaterial(keyStore, ssl.getKeyStorePassword().toCharArray())
                    .loadTrustMaterial(trustStore, TrustAllStrategy.INSTANCE)
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Ошибка сборки SSLContext для master-data.ssl", e);
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
