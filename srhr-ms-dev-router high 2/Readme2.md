# srhr-ms-dev-router

DEV-замена маршрутизирующей части корпоративного Tyk для изолированного стенда SRHR.

Фронтенду не нужно получать или передавать токен: Router выбирает настроенного DEV-пользователя, создаёт подписанный JWT, добавляет его в заголовок `Authorization` и перенаправляет запрос в реальный SRHR-сервис.

> Приложение нельзя использовать в production или публиковать за пределами защищённого DEV-контура. Оно позволяет переключать пользователя без аутентификации вызывающей стороны.

## Как проходит запрос

```text
Frontend без JWT
  -> srhr-ms-dev-router
       1. Выбор пользователя: X-Dev-User -> cookie DEV_USER -> default-user
       2. Удаление входящего Authorization
       3. Создание ES256 JWT
       4. Установка Authorization: Bearer <DEV JWT>
  -> реальный SRHR-сервис
       5. JwtTokenFilter извлекает claims
       6. EPA SDK проверяет ключ и JTI через srhr-ms-mock-service (WireMock)
```

Router не мокирует бизнес-ответы. Вызовы из SRHR-сервисов в EASUP, TSRM, EPA и другие внешние системы должны мокироваться в `srhr-ms-mock-service`.

## Что входит в проект

- маршруты Spring Cloud Gateway для `paystub`, `userinfo` и `userphoto`;
- автоматическая установка `Authorization: Bearer <DEV JWT>`;
- примеры пользователей `employee`, `approver` и `admin`;
- выбор пользователя через HttpOnly-cookie;
- выбор через `X-Dev-User` для Postman и автотестов;
- страница переключения пользователя `/dev-users.html`;
- постоянный `ctxi` в рамках выбранной браузерной сессии;
- CORS, Actuator-пробы, Dockerfile и Helm chart;
- WireMock-маппинги публичных ключей EPA и проверки JTI;
- unit-тесты подписи, claims, фильтра и выбора пользователя.

## Состав JWT

Токен содержит значения, которые читает предоставленный `JWTUtil` и `JwtTokenFilter`:

| Claim или header | Пример | Где используется |
|---|---|---|
| `sub` | `ivanov_ii@VTB.RU` | `getSub`, `getSubWithDomain`, `getFullSub` |
| `ctxi` | UUID браузерной сессии | `getSessionId` |
| `channel` | `WEB` | `getChannel`, `AllowedChannels` |
| `realm` | `SRHR` | `getRealm` |
| `ip` | IP запроса | `getClientIp` |
| `jti` | случайный UUID | anti-replay проверка |
| `iss`, `iat`, `nbf`, `exp`, `aud` | стандартные claims | валидация |
| `kid`, `alg=ES256` | заголовок JWT | поиск публичного ключа |

Значение `channel` должно совпадать с одним из значений `AllowedChannels.channelNames()`. Если в вашем enum нет `WEB`, замените его в настройках профилей.

`sessionCreatedAt` создаётся самим сервисом, а `traceId` берётся из активного OpenTelemetry span. Добавлять их в JWT не нужно.

## Выбор пользователя

Приоритет выбора:

1. Заголовок `X-Dev-User` — для Postman и тестов.
2. HttpOnly-cookie `DEV_USER` — для браузера.
3. `dev-router.jwt.default-user` — если пользователь не задан.

Страница выбора пользователя:

```text
http://localhost:8080/dev-users.html
```

Она вызывает следующие endpoints:

| Метод | Путь | Назначение |
|---|---|---|
| `GET` | `/dev/session/users` | получить доступные профили |
| `GET` | `/dev/session/current` | получить текущий профиль и `ctxi` |
| `POST` | `/dev/session/user/{user}` | выбрать профиль и создать cookies |
| `DELETE` | `/dev/session` | сбросить выбор |

Фронтенду не нужно читать cookies. Для запросов в тот же origin браузер отправляет их автоматически.

Если frontend и Router находятся на разных origin, HTTP-клиент должен включать передачу credentials: `credentials: 'include'` или `withCredentials: true`. Без этого Router использует `default-user`. Передавать Bearer-токен с frontend не требуется.

Пример запроса из Postman или curl:

```bash
curl -H 'X-Dev-User: approver' \
  http://localhost:8080/ui-api-web/cross/srhr/paystub/v1/paystub/period
```

Любой входящий `Authorization` удаляется и заменяется новым DEV-токеном. Так корпоративный токен не попадёт через Router в целевой сервис.

## Маршруты

Маршрутизация по умолчанию:

```text
/ui-api-web/cross/srhr/paystub/**  -> PAYSTUB_ROUTE_URI/**
/ui-api-web/cross/srhr/userinfo/** -> USERINFO_ROUTE_URI/**
/ui-api-web/cross/srhr/userphoto/** -> USERPHOTO_ROUTE_URI/**
```

Фильтр `StripPrefix=4` преобразует путь:

```text
/ui-api-web/cross/srhr/paystub/v1/paystub/period
-> /v1/paystub/period
```

Проверьте преобразованный путь относительно реального mapping контроллера. Если backend ожидает другой путь, измените или удалите `StripPrefix`.

## Проверка EPA через WireMock

После чтения JWT `JwtTokenFilter` вызывает:

```java
epaSdkService.checkJWT(jwt)
```

Предоставленный `JWTUtil` использует два внешних endpoint:

1. получение публичных ключей JWKS;
2. anti-replay проверку JTI с ответом `{"status": true}`.

Готовые маппинги находятся здесь:

```text
wiremock/mappings/epa-open-keys-proxy.json
wiremock/mappings/epa-jti-success.json
```

Скопируйте их в каталог mappings сервиса `srhr-ms-mock-service`. В настройках EPA/JWT SDK каждого SRHR-сервиса укажите адреса:

```text
http://srhr-ms-mock-service:<port>/epa/open-keys
http://srhr-ms-mock-service:<port>/epa/jti
```

Названия properties зависят от версии EPA SDK. Сохраните существующие названия, изменив только URL.

WireMock самостоятельно возвращает успешный результат JTI. Запрос публичного ключа он проксирует в:

```text
http://srhr-ms-dev-router:8180/epa/open-keys
```

Проксирование необходимо, потому что Router создаёт новый ES256-ключ при старте. Поэтому WireMock всегда возвращает публичный ключ, соответствующий подписи созданного токена.

Если Kubernetes Service имеет другое имя или порт, измените `proxyBaseUrl` в маппинге.

Для проверки отказа используйте:

```text
wiremock/examples/epa-jti-failure.json
```

Замените им успешный маппинг и перезагрузите mappings WireMock.

Если конкретная версия `EpaSdkService` вызывает единый endpoint валидации, а не endpoint ключей и JTI из `JWTUtil`, зафиксируйте реальный HTTP-запрос и добавьте маппинг с его путём и форматом ответа. Состав JWT в Router при этом не меняется.

## Диагностические endpoints

```bash
curl http://localhost:8080/dev/users
curl http://localhost:8080/dev/token
curl 'http://localhost:8080/dev/token?user=approver'
curl http://localhost:8080/.well-known/jwks.json
curl http://localhost:8080/epa/open-keys
curl 'http://localhost:8080/dev/jti?any-jti-value'
```

Не записывайте в логи и не коммитьте токены из `/dev/token`.

## Локальная сборка и запуск

Требуются JDK 17 и Maven 3.9 или новее.

```bash
./mvnw clean verify
java -jar target/srhr-ms-dev-router-1.0.0-SNAPSHOT.jar
```

Если зависимости загружаются из корпоративного Nexus:

```bash
./mvnw clean verify -s /path/to/settings.xml
```

`settings.xml` не включён в проект, потому что он может содержать адреса корпоративных репозиториев и credentials.

Пример локальных настроек:

```bash
export PAYSTUB_ROUTE_URI=http://localhost:9081
export USERINFO_ROUTE_URI=http://localhost:9082
export USERPHOTO_ROUTE_URI=http://localhost:9083
export FRONTEND_ORIGIN=http://localhost:3000
export DEV_COOKIE_SECURE=false
java -jar target/srhr-ms-dev-router-1.0.0-SNAPSHOT.jar
```

Для корпоративной сборки замените `RUNTIME_IMAGE` на разрешённый образ JRE 17:

```bash
docker build \
  --build-arg RUNTIME_IMAGE=<corporate-jre17-image> \
  -t srhr-ms-dev-router:1.0.0 .
```

## Настройка frontend

После настройки HTTPS для Router Ingress:

```json
{
  "HOST": "https://srhr-dev-api.srhr.innodev.local",
  "ENV_USERINFO_URI": "/ui-api-web/cross/srhr/userinfo/v1",
  "ENV_PAYSTUB_URI": "/ui-api-web/cross/srhr/paystub/v1",
  "FGW_HOST": "https://srhr-dev-api.srhr.innodev.local",
  "ENV_USERPHOTO_URI": "/ui-api-web/cross/srhr/userphoto/v1"
}
```

Если frontend работает по HTTPS, Router также должен быть доступен по HTTPS. Соединение от Ingress до Router может оставаться HTTP.

## Helm

Chart находится в `Deployment/helm-srhr-ms-dev-router`.

Перед развёртыванием настройте:

- корпоративный image repository и tag;
- реальные имена и порты Kubernetes Service;
- значения `sub` для `employee`, `approver` и `admin`;
- разрешённое значение `channel`;
- Ingress host, class и TLS;
- `wiremock/mappings/epa-open-keys-proxy.json`, если имя Router Service отличается.

Проверка chart:

```bash
helm lint Deployment/helm-srhr-ms-dev-router
helm template srhr-ms-dev-router Deployment/helm-srhr-ms-dev-router
```

По умолчанию запускается одна реплика Router, поскольку ключ подписи хранится в памяти. Для нескольких реплик необходимо хранить единый EC private key в Kubernetes Secret и загружать его в `DevJwtService`.