package ru.ntdev.srhr.common.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;
import ru.ntdev.srhr.common.jwt.dto.OpenKeyDto;
import ru.ntdev.srhr.common.jwt.dto.OpenKeyDtoList;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

@Slf4j
public class JWTUtil {

    public static String getFullSub(String token) {
        return JWT.decode(token).getClaim("sub").asString();
    }

    public static String getSessionId(String token) {
        return JWT.decode(token).getClaim("ctxi").asString();
    }

    public static String getChannel(String token) {
        return JWT.decode(token).getClaim("channel").asString();
    }

    public static String getClientIp(String token) {
        return JWT.decode(token).getClaim("ip").asString();
    }


    private static String decode(String encodedString) {
        return new String(Base64.getUrlDecoder().decode(encodedString));
    }

    private static final String WRONG_JWT_TOKEN = "Некорректный JWT токен";
    private static final String WRONG_JTI_REQUEST_STATUS = "Возникла ошибка при anti-replay проверке";

    /**
     * Получение jti из токена JWT
     *
     * @param token Токен JWT
     * @return jti
     */
    public static String getJti(String token) {
        return JWT.decode(token).getClaim("jti").asString();
    }

    /**
     * Получение sub из токена до символа '@'
     *
     * @param token Токен
     * @return sub до символа '@'
     * @throws Exception Ошибка получения sub
     */
    public static String getSub(String token) throws Exception {
        String _subject = JWT.decode(token).getSubject();
        return _subject.split("@")[0];
    }

    /**
     * Получение sub из токена с доменным именем
     *
     * @param token Токен
     * @return sub до символа '@'
     * @throws Exception Ошибка получения sub
     */
    public static String getSubWithDomain(String token) throws Exception {
        return JWT.decode(token).getSubject();
    }

    /**
     * Получение realm из токена
     *
     * @param token Токен
     * @return realm
     * @throws Exception Ошибка получения realm
     */
        public static String getRealm(String token) {
            return JWT.decode(token).getClaim("realm").asString();
        }

    /**
     * Порверка JTI
     *
     * @param jtiUrl        Адрес
     * @param jwt           Токен JWT
     * @param certPath      Пусть к keystore с сертификатом для запроса по адресу
     * @param certPassword  Пароль к keystore по указаному пути
     * @param needException Выкинуть ошибку или вернуть флаг (флаг)
     * @return true - проверка успешна, false - needException=false и проверка не успешна, иначе ошибка
     * @throws IllegalStateException Ошибка проверки JTI
     */
    public static Boolean checkJTI(
            String jtiUrl,
            String jwt,
            String certPath,
            String certPassword,
            boolean needException
    ) {
        boolean _result;
        try {
            String _jti = getJti(jwt);
            SSLContext sc = SSLContextBuilder
                    .create()
                    .loadKeyMaterial(keyStore(certPath, certPassword), certPassword.toCharArray())
                    .loadTrustMaterial(keyStore(certPath, certPassword), new TrustAllStrategy())
                    .build();

            URL url = new URL(jtiUrl + "?" + _jti);
            log.info("JTI url: [{}]", jtiUrl + "?***");
            HttpURLConnection _connection = (HttpURLConnection) url.openConnection();
            if (_connection instanceof HttpsURLConnection) {
                ((HttpsURLConnection) _connection)
                        .setSSLSocketFactory(sc.getSocketFactory());
            }
            _connection.setRequestMethod("GET");
            int status = _connection.getResponseCode();
            log.info("JTI response status: [{}]", status);
            Reader streamReader;
            if (status > 299) {
                streamReader = new InputStreamReader(_connection.getErrorStream());

            } else {
                streamReader = new InputStreamReader(_connection.getInputStream());
            }
            BufferedReader in = new BufferedReader(streamReader);
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            _connection.disconnect();
            JsonObject _jsonObject = JsonParser.parseString(content.toString()).getAsJsonObject();
            JsonElement _jsonValue = _jsonObject.get("status");
            _result = _jsonValue.getAsBoolean();
            if (status > 299) {
                log.error("JTI response status value: [{}]", _result);
            } else {
                log.info("JTI response status value: [{}]", _result);
            }
        } catch (Exception e) {
            if (needException) {
                throw new IllegalStateException("Ошибка проверки jti: " + e.getMessage());
            }
            log.error("Ошибка проверки jti", e);
            _result = false;
        }

        return _result;
    }

    /**
     * Получение sub из JWT
     *
     * @param token Токен JWT
     * @return sub
     */
    public static String getSubject(String token) {
        return JWT.decode(token).getSubject();
    }


    /**
     * Проверка, что токен JWT протух
     *
     * @param token Токен JWT
     * @return true - токен протух, иначе false
     */
    public static Boolean isTokenExpired(String token) {
        try {
            return JWT.decode(token).getExpiresAt().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Проверка, что передан iss
     *
     * @param token Токен JWT
     * @return true - передан, иначе false
     */
    public static Boolean isCorrectIss(String token) {
        try {
            return !JWT.decode(token).getIssuer().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Проверка, что передан sub
     *
     * @param token Токен JWT
     * @return true - передан, иначе false
     */
    public static Boolean isCorrectSub(String token) {
        try {
            return !JWT.decode(token).getSubject().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Проверка, что передан iss
     *
     * @param token Токен JWT
     * @throws IllegalStateException iss не передан или не удалось извлечь
     */
    public static void isCorrectIssWithEx(String token) {
        boolean isPresent;
        try {
            isPresent = !JWT.decode(token).getIssuer().isEmpty();
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось извлечь Issuer. Ошибка: " + e.getMessage());
        }

        if (!isPresent) {
            throw new IllegalStateException("Issuer не передан");
        }
    }

    /**
     * Проверка, что передан sub
     *
     * @param token Токен JWT
     * @throws IllegalStateException sub не передан или не удалось извлечь
     */
    public static void isCorrectSubWithEx(String token) {
        boolean isPresent;
        try {
            isPresent = !JWT.decode(token).getSubject().isEmpty();
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось извлечь Subject. Ошибка: " + e.getMessage());
        }

        if (!isPresent) {
            throw new IllegalStateException("Subject не передан");
        }
    }

    /**
     * Проверка, что токен протух
     *
     * @param token Токен JWT
     * @throws IllegalStateException протух или ошибка извлечения
     */
    public static void isTokenExpiredWithEx(String token) {
        boolean isValid;
        try {
            isValid = JWT.decode(token).getExpiresAt().after(new Date());
        } catch (Exception e) {
            isValid = false;
        }

        if (!isValid) throw new IllegalStateException("Некорректный ExpiresAt");
    }

    /**
     * Проверка, IssuedAt до текущей даты
     *
     * @param token Токен JWT
     * @throws IllegalStateException IssuedAt не валиден или не удалось извлечь
     */
    public static void isTokenIssuedProperlyWithEx(String token) {
        boolean isValid;
        try {
            isValid = JWT.decode(token).getIssuedAt().before(new Date());
        } catch (Exception e) {
            isValid = false;
        }

        if (!isValid) throw new IllegalStateException("Некорректный IssuedAt");
    }

    /**
     * Проверка, IssuedAt до текущей даты
     *
     * @param token Токен JWT
     * @return true - IssuedAt валиден, иначе false
     */
    public static Boolean isTokenIssuedProperly(String token) {
        try {
            return JWT.decode(token).getIssuedAt().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Валидация токена
     * - текущая дата между IssuedAt и ExpiresAt
     * - iss и sub обязательны
     *
     * @param token Токен JWT
     * @return true - валиднвй токен, иначе false
     */
    public static Boolean validateToken(String token) {
        Boolean _isValid = !isTokenExpired(token)
                && isTokenIssuedProperly(token)
                && isCorrectIss(token)
                && isCorrectSub(token);
        if (_isValid) {
            log.info("Проверка JWT токена успешна");
        } else {
            log.error("Проверка JWT токена не успешна");
        }
        return _isValid;
    }

    /**
     * Получение хранилища ключей
     *
     * @param storePath     путь
     * @param storePassword пароль
     * @return Хранилище клчей
     * @throws KeyStoreException Ошибка получения
     * @throws IOException       Ошибка получения
     */
    private static KeyStore keyStore(String storePath, String storePassword) throws KeyStoreException, IOException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        File key = ResourceUtils.getFile(storePath);
        try (InputStream in = new FileInputStream(key)) {
            keyStore.load(in, storePassword.toCharArray());
        } catch (CertificateException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return keyStore;
    }

    /**
     * Валидация подписи
     *
     * @param token         Токен JWT
     * @param url           Адрес, для получения ключей
     * @param certPath      Путь к кейстору
     * @param certPassword  Пароль к кейстору
     * @param needException Выкидывать ошибку (флаг)
     * @return true - валиднвй токен, false - needException=false и проверка не успешна, иначе ошибка
     */
    public static boolean isValidSign(
            String token,
            String url,
            String certPath,
            String certPassword,
            boolean needException
    ) {
        try {
            String kid = JWT.decode(token).getKeyId();
            String algName = JWT.decode(token).getAlgorithm();
            if (kid == null
                    || algName == null
                    || !algName.startsWith("ES")) {

                if (needException) {
                    throw new IllegalStateException("Не переданы/валидны идентификатор/алгоритм");
                }
                log.error("Не переданы/валидны идентификатор/алгоритм");
                return false;
            }

            // Получаем ключи
            KeyStore keyStore = keyStore(certPath, certPassword);
            SSLContext sc = SSLContextBuilder
                    .create()
                    .loadKeyMaterial(keyStore, certPassword.toCharArray())
                    .loadTrustMaterial(keyStore, new TrustAllStrategy())
                    .build();
            final SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sc, NoopHostnameVerifier.INSTANCE);
            final Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
                    .register("https", sslsf)
                    .register("http", new PlainConnectionSocketFactory())
                    .build();

            final BasicHttpClientConnectionManager connectionManager =
                    new BasicHttpClientConnectionManager(socketFactoryRegistry);

            HttpClient client = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .build();
            RestTemplate rest = new RestTemplate();
            rest.setRequestFactory(new HttpComponentsClientHttpRequestFactory(client));

            ResponseEntity<OpenKeyDtoList> keysResponse = rest.getForEntity(url, OpenKeyDtoList.class);
            HttpStatusCode status = keysResponse.getStatusCode();
            if (!status.is2xxSuccessful()
                    || keysResponse.getBody() == null
                    || keysResponse.getBody().getKeys() == null
                    || keysResponse.getBody().getKeys().isEmpty()
            ) {
                if (needException) {
                    throw new IllegalStateException("Не получены публичные ключи");
                }
                log.error("Не получены публичные ключи");
                return false;
            }

            Optional<OpenKeyDto> keyOpt = keysResponse.getBody().getKeys()
                    .stream()
                    .filter(it -> algName.equals(it.getAlg()) && kid.equals(it.getKid()))
                    .findFirst();

            if (keyOpt.isEmpty()) {
                if (needException) {
                    throw new IllegalStateException("Среди полученных ключей нет совпадений по алгоритму и id");
                }
                log.error("Среди полученных ключей нет совпадений по алгоритму и id");
                return false;
            }

            OpenKeyDto key = keyOpt.get();
            String kty = key.getKty() != null ? key.getKty() : "EC";
            String crv = key.getCrv() != null ? key.getCrv() : "P-256";

            // Генерируем параметры
            ECParameterSpec parameterSpec = generateParams(kty, crv);

            // Генерируем публичный ключ
            PublicKey publicKey = generateEcPublicKey(key.getX(), key.getY(), kty, parameterSpec);

            // Валидируем токен
            Algorithm alg = getAlg(publicKey);
            JWTVerifier verifier = JWT.require(alg).build();
            verifier.verify(token);
            log.info("Успешная проверка подписи");
            return true;
        } catch (Exception e) {
            if (needException) {
                throw new IllegalStateException("Ошибка проверки подписи: " + e.getMessage());
            }
            log.error("Ошибка проверки подписи", e);
            return false;
        }
    }

    /**
     * Валидация JWT с выбросом ошибки
     *
     * @param token        Токен JWT
     * @param signUrl      Адрес, для получения публичных ключей
     * @param jtiUrl       Адрес, для проверки jti
     * @param certPath     Пусть к кейстору
     * @param certPassword Пароль к кейстору
     */
    public static void validateTokenWithEx(
            String token,
            String signUrl,
            String jtiUrl,
            String certPath,
            String certPassword
    ) {
        isTokenExpiredWithEx(token);
        isTokenIssuedProperlyWithEx(token);
        isCorrectIssWithEx(token);
        isCorrectSubWithEx(token);
        isValidSign(token, signUrl, certPath, certPassword, true);
        if (jtiUrl != null) {
            checkJTI(jtiUrl, token, certPath, certPassword, true);
        }
    }

    /**
     * Создание публичного ключа
     *
     * @param x             The x member contains the x coordinate for the elliptic curve point
     * @param y             The y member contains the y coordinate for the elliptic curve point
     * @param kty           Key Type
     * @param parameterSpec Параметры
     * @return Публичный ключ
     * @throws Exception Ошибка создания ключа
     */
    private static PublicKey generateEcPublicKey(
            String x,
            String y,
            String kty,
            ECParameterSpec parameterSpec
    ) throws Exception {
        if (x == null || y == null) {
            throw new IllegalArgumentException("Не переданы координаты");
        }
        BigInteger bigX = decodeToBigInteger(x);
        BigInteger bigY = decodeToBigInteger(y);
        ECPoint p = new ECPoint(bigX, bigY);
        KeyFactory keyFactory = KeyFactory.getInstance(kty, "SunEC");
        ECPublicKeySpec keySpec = new ECPublicKeySpec(p, parameterSpec);
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * Получение параметров
     *
     * @param kty Key Type
     * @param crv The crv member identifies the cryptographic curve used with the key
     * @return ECParameterSpec Параметры
     * @throws Exception Ошибка получения
     */
    private static ECParameterSpec generateParams(String kty, String crv) throws Exception {
        AlgorithmParameters algoParameters = AlgorithmParameters.getInstance(kty);
        algoParameters.init(getECParamGen(crv));
        return algoParameters.getParameterSpec(ECParameterSpec.class);
    }

    /**
     * Получение генератора параметров по crv
     *
     * @param crv The crv member identifies the cryptographic curve used with the key
     * @return elliptic curve (EC) domain parameters
     */
    private static ECGenParameterSpec getECParamGen(String crv) {
        switch (crv) {
            case "P-384":
                return new ECGenParameterSpec("secp384r1");
            case "P-521":
                return new ECGenParameterSpec("secp521r1");
            default:
                return new ECGenParameterSpec("secp256r1");
        }
    }

    /**
     * Получение алгоритма по публичному ключу
     *
     * @param publicKey публичный ключ
     * @return Алгоритм
     */
    private static Algorithm getAlg(PublicKey publicKey) {
        ECPublicKey publicKeyEC = (ECPublicKey) publicKey;
        EllipticCurve curve = publicKeyEC.getParams().getCurve();
        switch (curve.getField().getFieldSize()) {
            case 384:
                return Algorithm.ECDSA384(publicKeyEC, null);
            case 521:
                return Algorithm.ECDSA512(publicKeyEC, null);
            default:
                return Algorithm.ECDSA256(publicKeyEC, null);
        }
    }

    /**
     * Преобразование координаты к BigInteger
     *
     * @param coordinate координата
     * @return BigInteger
     */
    private static BigInteger decodeToBigInteger(String coordinate) {
        return new BigInteger(
                Base64.getDecoder().decode(
                        coordinate.replaceAll("_", "/").replaceAll("-", "+")
                )
        );
    }
}
