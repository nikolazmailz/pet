package ru.ntdev.srhr.ms.sys.delegation.client;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.client.RestTemplate;
import ru.ntdev.srhr.common.constant.RealmConstant;
import ru.ntdev.srhr.common.dto.SapSystemParamsDto;
import ru.ntdev.srhr.common.epa.config.AuthEpaRestTemplateInterceptorConfig;
import ru.ntdev.srhr.common.util.LogUtil;
import ru.ntdev.srhr.ms.sys.delegation.config.AppRestProperties;
import ru.ntdev.srhr.ms.sys.delegation.config.MasterDataProperties;
import ru.ntdev.srhr.ms.sys.delegation.filter.SpoRequestInterceptor;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP-клиент master-data (СПО). Один пул соединений и один {@link RestTemplate},
 * создаются при старте и живут до остановки приложения.
 * <p>
 * Заголовки, зависящие от запроса, собираются через {@link #requestHeaders} и передаются в {@code RequestEntity}:
 * <pre>
 * RequestEntity&lt;Void&gt; req = RequestEntity.get(uri)
 *         .headers(client.requestHeaders(params, serviceId))
 *         .build();
 * client.restTemplate().exchange(req, Resp.class);
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysDelegationClient {

    public static final String HEADER_PERNR = "pernr";
    public static final String HEADER_SESSION_ID = "sessionId";
    public static final String HEADER_CSRF_TOKEN = "x-csrf-token";
    public static final String HEADER_ROLES = "Roles";
    public static final String HEADER_LOG_ID = "logId";
    public static final String HEADER_DELEGATION_SERVICE_ID = "X-Service-Id";
    public static final String CSRF_FETCH = "fetch";

    private static final String CONTENT_TYPE_JSON_UTF8 = "application/json;charset=UTF-8";

    private static final int MAX_CONNECTIONS_TOTAL = 50;
    private static final int MAX_CONNECTIONS_PER_ROUTE = 20;
    private static final TimeValue CONNECTION_TTL = TimeValue.ofMinutes(5);
    private static final TimeValue VALIDATE_AFTER_INACTIVITY = TimeValue.ofSeconds(10);
    private static final TimeValue IDLE_EVICTION = TimeValue.ofMinutes(1);

    private final SSLContext masterDataSslContext;
    private final LogUtil logUtil;
    private final AuthEpaRestTemplateInterceptorConfig authEpaInterceptor;
    private final MasterDataProperties masterDataProperties;
    private final AppRestProperties restProperties;

    private CloseableHttpClient httpClient;
    private RestTemplate restTemplate;

    @PostConstruct
    void init() {
        httpClient = createHttpClient();
        restTemplate = new RestTemplateBuilder()
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(httpClient))
                .additionalInterceptors(new SpoRequestInterceptor(logUtil), authEpaInterceptor)
                .build();
        log.info("Инициализирован клиент master-data");
    }

    // ------------------------------------------------------------------ API

    /** Общий RestTemplate. Потокобезопасен. */
    public RestTemplate restTemplate() {
        return restTemplate;
    }

    /** Заголовки запроса: контекст пользователя + X-Service-Id делегирования. */
    public HttpHeaders requestHeaders(SapSystemParamsDto params, String delegationServiceId) {
        MasterDataProperties.Headers headerProps = masterDataProperties.getHeaders();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON_UTF8);
        headers.add(HEADER_PERNR, params.getTabNumber());
        headers.add(HEADER_SESSION_ID, params.getSessionId());
        headers.add(HEADER_CSRF_TOKEN, CSRF_FETCH);
        headers.add(HEADER_ROLES, params.getRolesHeader());
        headers.add(HEADER_LOG_ID, params.getTraceId());
        headers.add(headerProps.getServiceIdHeaderName(), serviceIdFor(params.getRealm()));
        headers.add(HEADER_DELEGATION_SERVICE_ID, delegationServiceId);
        return headers;
    }

    // ------------------------------------------------------------------ legacy API

    /**
     * @deprecated заголовки запроса запекаются в RestTemplate. Используй
     * {@link #restTemplate()} + {@link #requestHeaders(SapSystemParamsDto, String)}.
     * Соединения при этом уже берутся из общего пула.
     */
    @Deprecated
    public RestTemplate buildRestTemplateWithRoles(SapSystemParamsDto params, String delegationServiceId) {
        RestTemplateBuilder builder = new RestTemplateBuilder()
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(httpClient))
                .additionalInterceptors(new SpoRequestInterceptor(logUtil), authEpaInterceptor);
        for (Map.Entry<String, List<String>> header : requestHeaders(params, delegationServiceId).entrySet()) {
            builder = builder.defaultHeader(header.getKey(), header.getValue().toArray(String[]::new));
        }
        return builder.build();
    }

    // ------------------------------------------------------------------ internals

    private CloseableHttpClient createHttpClient() {
        SSLConnectionSocketFactory sslSocketFactory =
                new SSLConnectionSocketFactory(masterDataSslContext, NoopHostnameVerifier.INSTANCE);

        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory)
                .setMaxConnTotal(MAX_CONNECTIONS_TOTAL)
                .setMaxConnPerRoute(MAX_CONNECTIONS_PER_ROUTE)
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(restProperties.getConnectTimeout(), TimeUnit.MILLISECONDS)
                        .setSocketTimeout(restProperties.getReadTimeout(), TimeUnit.MILLISECONDS)
                        .setTimeToLive(CONNECTION_TTL)
                        .setValidateAfterInactivity(VALIDATE_AFTER_INACTIVITY)
                        .build())
                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                // Пул общий на всех пользователей: куки одной сессии не должны утечь в другую.
                .disableCookieManagement()
                .evictExpiredConnections()
                .evictIdleConnections(IDLE_EVICTION)
                .build();
    }

    private String serviceIdFor(String realm) {
        MasterDataProperties.Headers headerProps = masterDataProperties.getHeaders();
        return RealmConstant.STAFF_MOBILE_REALM.equals(realm)
                ? headerProps.getServiceIdHeaderStaffMobileValue()
                : headerProps.getServiceIdHeaderStaffValue();
    }

    @PreDestroy
    void shutdown() {
        try {
            httpClient.close();
        } catch (IOException e) {
            log.warn("Не удалось закрыть HttpClient master-data: {}", e.getMessage());
        }
    }
}
