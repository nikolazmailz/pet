package ru.ntdev.srhr.ms.easup.integration.client;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.util.TimeValue;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import ru.ntdev.srhr.common.constant.RealmConstant;
import ru.ntdev.srhr.common.dto.SapSystemParamsDto;
import ru.ntdev.srhr.common.util.LogUtil;
import ru.ntdev.srhr.ms.easup.integration.config.EasupCertificateConfig;
import ru.ntdev.srhr.ms.easup.integration.config.EasupProperties;
import ru.ntdev.srhr.ms.easup.integration.filter.EasupClientSideMetricsInterceptor;
import ru.ntdev.srhr.ms.easup.integration.filter.EasupRequestInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Вариант с eager-инициализацией: все пулы и RestTemplate создаются на старте приложения
 * в {@link #init()}, по одному на каждую роль из {@link EasupCertificateConfig#supportedRoles()}.
 * Битый кейстор роняет приложение при старте, а не на первом запросе.
 *
 * <pre>
 * RequestEntity&lt;Body&gt; req = RequestEntity.post(url)
 *         .headers(easupClient.requestHeaders(params, csrfResponse.getHeaders()))
 *         .body(body);
 * easupClient.forRole(params.getRole()).exchange(req, Resp.class);
 * </pre>
 */
@Slf4j
@Component
public class EasupClientV2 {

    public static final String HEADER_PERNR = "pernr";
    public static final String HEADER_SESSION_ID = "sessionId";
    public static final String HEADER_CSRF_TOKEN = "x-csrf-token";
    public static final String HEADER_ROLES = "Roles";
    public static final String HEADER_LOG_ID = "logId";
    public static final String CSRF_FETCH = "fetch";

    private static final String CONTENT_TYPE_JSON_UTF8 = "application/json;charset=UTF-8";

    private static final int MAX_CONNECTIONS_TOTAL = 50;
    private static final int MAX_CONNECTIONS_PER_ROUTE = 20;
    private static final TimeValue CONNECTION_TTL = TimeValue.ofMinutes(5);
    private static final TimeValue VALIDATE_AFTER_INACTIVITY = TimeValue.ofSeconds(10);
    private static final TimeValue IDLE_EVICTION = TimeValue.ofMinutes(1);

    private final EasupCertificateConfig certificateConfig;
    private final LogUtil logUtil;
    private final EasupClientSideMetricsInterceptor metricsInterceptor;
    private final EasupProperties.Headers headerProps;
    /** Готовое значение Authorization или null, если basic auth выключен. */
    private final String basicAuthHeaderValue;

    /** Заполняются один раз в {@link #init()}, дальше только чтение — синхронизация не нужна. */
    private final Map<String, CloseableHttpClient> httpClientsByRole = new HashMap<>();
    private final Map<String, RestTemplate> restTemplatesByRole = new HashMap<>();

    public EasupClientV2(EasupCertificateConfig certificateConfig,
                         LogUtil logUtil,
                         EasupClientSideMetricsInterceptor metricsInterceptor,
                         EasupProperties properties) {
        this.certificateConfig = certificateConfig;
        this.logUtil = logUtil;
        this.metricsInterceptor = metricsInterceptor;
        this.headerProps = properties.getHeaders();
        this.basicAuthHeaderValue = buildBasicAuth(properties.getBasicAuth());
    }

    @PostConstruct
    void init() {
        for (String role : certificateConfig.supportedRoles()) {
            CloseableHttpClient httpClient = createHttpClient(role);
            httpClientsByRole.put(role, httpClient);
            restTemplatesByRole.put(role, new RestTemplateBuilder()
                    .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(httpClient))
                    .additionalInterceptors(new EasupRequestInterceptor(logUtil), metricsInterceptor)
                    .build());
        }
        log.info("Инициализированы клиенты ЕАСУП для ролей {}", restTemplatesByRole.keySet());
    }

    // ------------------------------------------------------------------ API

    public RestTemplate forRole(String role) {
        RestTemplate restTemplate = restTemplatesByRole.get(role);
        if (restTemplate == null) {
            throw new IllegalArgumentException("Нет клиента ЕАСУП для роли: " + role);
        }
        return restTemplate;
    }

    /** Заголовки первого запроса — получение CSRF-токена. */
    public HttpHeaders requestHeaders(SapSystemParamsDto params) {
        return requestHeaders(params, CSRF_FETCH, List.of());
    }

    /** Заголовки последующих запросов — с токеном и куками из ответа ЕАСУП. */
    public HttpHeaders requestHeaders(SapSystemParamsDto params, Map<String, List<String>> responseHeaders) {
        List<String> csrf = responseHeaders.get(HEADER_CSRF_TOKEN);
        if (csrf == null || csrf.isEmpty()) {
            throw new IllegalStateException("В ответе ЕАСУП отсутствует заголовок " + HEADER_CSRF_TOKEN);
        }
        return requestHeaders(params, csrf.get(0), responseHeaders.getOrDefault("cookie", List.of()));
    }

    private HttpHeaders requestHeaders(SapSystemParamsDto params, String csrfToken, List<String> cookies) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON_UTF8);
        if (basicAuthHeaderValue != null) {
            headers.add(HttpHeaders.AUTHORIZATION, basicAuthHeaderValue);
        }
        headers.add(HEADER_PERNR, params.getTabNumber());
        headers.add(HEADER_SESSION_ID, params.getSessionId());
        headers.add(HEADER_CSRF_TOKEN, csrfToken);
        headers.add(HEADER_ROLES, params.getRolesHeader());
        headers.add(HEADER_LOG_ID, params.getTraceId());
        headers.add(headerProps.getServiceIdHeaderName(), serviceIdFor(params.getRealm()));
        cookies.forEach(cookie -> headers.add(HttpHeaders.COOKIE, cookie));
        return headers;
    }

    // ------------------------------------------------------------------ internals

    private CloseableHttpClient createHttpClient(String role) {
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                certificateConfig.configureHttpClientSsl(role), NoopHostnameVerifier.INSTANCE);

        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory)
                .setMaxConnTotal(MAX_CONNECTIONS_TOTAL)
                .setMaxConnPerRoute(MAX_CONNECTIONS_PER_ROUTE)
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setTimeToLive(CONNECTION_TTL)
                        .setValidateAfterInactivity(VALIDATE_AFTER_INACTIVITY)
                        .build())
                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .evictExpiredConnections()
                .evictIdleConnections(IDLE_EVICTION)
                .build();
    }

    private String serviceIdFor(String realm) {
        return RealmConstant.STAFF_MOBILE_REALM.equals(realm)
                ? headerProps.getServiceIdHeaderStaffMobileValue()
                : headerProps.getServiceIdHeaderStaffValue();
    }

    private static String buildBasicAuth(EasupProperties.BasicAuth auth) {
        if (!auth.isEnable() || !StringUtils.hasText(auth.getLogin()) || !StringUtils.hasText(auth.getPassword())) {
            return null;
        }
        String credentials = auth.getLogin() + ":" + auth.getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.ISO_8859_1));
    }

    @PreDestroy
    void shutdown() {
        httpClientsByRole.forEach((role, client) -> {
            try {
                client.close();
            } catch (IOException e) {
                log.warn("Не удалось закрыть HttpClient ЕАСУП для роли [{}]", role, e);
            }
        });
    }
}
