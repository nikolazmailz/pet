# srhr-ms-dev-router

DEV-only replacement for the routing part of corporate Tyk on an isolated SRHR
stand. The frontend does not need to obtain or send a token: the router selects
a configured DEV user, generates a signed JWT, places it into `Authorization`,
and proxies the request to a real SRHR service.

> Never deploy this application to production or expose it outside the
> protected DEV network. It intentionally allows switching identity without
> authenticating the caller.

## Request flow

```text
Frontend without JWT
  -> srhr-ms-dev-router
       1. select user: X-Dev-User -> DEV_USER cookie -> default-user
       2. remove incoming Authorization
       3. generate ES256 JWT
       4. set Authorization: Bearer <DEV JWT>
  -> real SRHR service
       5. JwtTokenFilter decodes claims
       6. EPA SDK validates key/JTI through srhr-ms-mock-service (WireMock)
```

The router does not mock business responses. Calls from SRHR services to EASUP,
TSRM, EPA, and other external systems belong in `srhr-ms-mock-service`.

## What is included

- Spring Cloud Gateway routes for `paystub`, `userinfo`, and `userphoto`;
- automatic `Authorization: Bearer <DEV JWT>` injection;
- three example users: `employee`, `approver`, and `admin`;
- user selection through an HttpOnly cookie;
- optional `X-Dev-User` selector for Postman and automated tests;
- built-in browser page `/dev-users.html`;
- stable `ctxi` for the selected browser session;
- CORS, Actuator probes, Dockerfile, and Helm chart;
- WireMock mappings for EPA public keys and JTI validation;
- unit tests for claims, signature, routing filter, and user selection.

## JWT contract

The token contains every value read by the supplied `JWTUtil` and
`JwtTokenFilter`:

| Claim/header | Example | Consumer |
|---|---|---|
| `sub` | `ivanov_ii@VTB.RU` | `getSub`, `getSubWithDomain`, `getFullSub` |
| `ctxi` | browser session UUID | `getSessionId` |
| `channel` | `WEB` | `getChannel`, `AllowedChannels` |
| `realm` | `SRHR` | `getRealm` |
| `ip` | request IP | `getClientIp` |
| `jti` | random UUID | anti-replay check |
| `iss`, `iat`, `nbf`, `exp`, `aud` | standard claims | validation |
| `kid`, `alg=ES256` | JWT header | public-key lookup |

`channel` must exactly match an element returned by
`AllowedChannels.channelNames()`. Replace the example `WEB` value when your
enum uses another name.

The service itself still creates `sessionCreatedAt` and gets `traceId` from the
active OpenTelemetry span; these values do not need to be added to the JWT.

## User selection

Priority:

1. `X-Dev-User` request header - convenient for Postman and tests;
2. `DEV_USER` HttpOnly cookie - used by a browser;
3. `dev-router.jwt.default-user` - no frontend changes required.

Open the selector page:

```text
http://localhost:8080/dev-users.html
```

The page calls:

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/dev/session/users` | available profiles |
| `GET` | `/dev/session/current` | current profile and `ctxi` |
| `POST` | `/dev/session/user/{user}` | select profile and create cookies |
| `DELETE` | `/dev/session` | clear selection |

The frontend does not have to read the cookies. For same-origin calls the
browser sends them automatically. If the frontend and Router have different
origins, its HTTP client must enable credentialed requests (`credentials:
'include'` / `withCredentials: true`); without that, Router safely falls back
to `default-user`. No bearer-token code is required in the frontend.

For a single Postman request:

```bash
curl -H 'X-Dev-User: approver' \
  http://localhost:8080/ui-api-web/cross/srhr/paystub/v1/paystub/period
```

Any incoming `Authorization` is removed and replaced by a newly generated DEV
token. A corporate token can therefore never leak through this router.

## Routes

Default mapping:

```text
/ui-api-web/cross/srhr/paystub/**  -> PAYSTUB_ROUTE_URI/**
/ui-api-web/cross/srhr/userinfo/** -> USERINFO_ROUTE_URI/**
/ui-api-web/cross/srhr/userphoto/** -> USERPHOTO_ROUTE_URI/**
```

`StripPrefix=4` transforms:

```text
/ui-api-web/cross/srhr/paystub/v1/paystub/period
-> /v1/paystub/period
```

Verify this path against the actual controller mapping. Change or remove
`StripPrefix` if the backend expects another prefix.

## EPA validation through WireMock

`JwtTokenFilter` first decodes the JWT and then invokes:

```java
epaSdkService.checkJWT(jwt)
```

The supplied `JWTUtil` shows two external contracts used during validation:

1. a public-key/JWKS endpoint;
2. a JTI anti-replay endpoint returning `{"status": true}`.

Mappings are in:

```text
wiremock/mappings/epa-open-keys-proxy.json
wiremock/mappings/epa-jti-success.json
```

Copy them into the mappings directory of `srhr-ms-mock-service`. Configure the
existing EPA SDK URL properties in each SRHR service to point to:

```text
http://srhr-ms-mock-service:<port>/epa/open-keys
http://srhr-ms-mock-service:<port>/epa/jti
```

Keep the property names already used by your EPA SDK version; replace only the
URL values.

WireMock returns JTI success directly. The open-key mapping proxies to:

```text
http://srhr-ms-dev-router:8180/epa/open-keys
```

The proxy is intentional: the router creates an ES256 key at startup, so
WireMock always returns the public key matching the token that Router generated.
If Kubernetes uses another Service name or port, edit `proxyBaseUrl` in the
mapping.

To test rejection, use:

```text
wiremock/examples/epa-jti-failure.json
```

instead of the success mapping and reset WireMock mappings.

If your concrete `EpaSdkService` calls a single validation endpoint rather than
the open-key and JTI URLs from `JWTUtil`, record that HTTP request once and add a
mapping with the actual path and response DTO. The Router-side JWT contract does
not change.

## Diagnostic endpoints

```bash
curl http://localhost:8080/dev/users
curl http://localhost:8080/dev/token
curl 'http://localhost:8080/dev/token?user=approver'
curl http://localhost:8080/.well-known/jwks.json
curl http://localhost:8080/epa/open-keys
curl 'http://localhost:8080/dev/jti?any-jti-value'
```

Do not log or commit tokens returned by `/dev/token`.

## Local build and run

Requirements: JDK 17 and Maven 3.9+.

```bash
./mvnw clean verify
java -jar target/srhr-ms-dev-router-1.0.0-SNAPSHOT.jar
```

Useful overrides:

```bash
export PAYSTUB_ROUTE_URI=http://localhost:9081
export USERINFO_ROUTE_URI=http://localhost:9082
export USERPHOTO_ROUTE_URI=http://localhost:9083
export FRONTEND_ORIGIN=http://localhost:3000
export DEV_COOKIE_SECURE=false
java -jar target/srhr-ms-dev-router-1.0.0-SNAPSHOT.jar
```

For a corporate build, replace `RUNTIME_IMAGE` with an approved JRE 17 image:

```bash
docker build \
  --build-arg RUNTIME_IMAGE=<corporate-jre17-image> \
  -t srhr-ms-dev-router:1.0.0 .
```

## Frontend configuration

After HTTPS is configured for the Router Ingress:

```json
{
  "HOST": "https://srhr-dev-api.srhr.innodev.local",
  "ENV_USERINFO_URI": "/ui-api-web/cross/srhr/userinfo/v1",
  "ENV_PAYSTUB_URI": "/ui-api-web/cross/srhr/paystub/v1",
  "FGW_HOST": "https://srhr-dev-api.srhr.innodev.local",
  "ENV_USERPHOTO_URI": "/ui-api-web/cross/srhr/userphoto/v1"
}
```

The frontend is HTTPS, so Router must also be exposed through HTTPS. The
Ingress-to-Router connection can remain HTTP.

## Helm

The chart is located in `Deployment/helm-srhr-ms-dev-router`.

Before deployment, configure:

- corporate image repository and tag;
- actual Kubernetes Service names and ports;
- real subjects for `employee`, `approver`, and `admin`;
- an allowed `channel` value;
- Ingress host/class and TLS;
- `wiremock/mappings/epa-open-keys-proxy.json` if the Router Service name differs.

Render and validate:

```bash
helm lint Deployment/helm-srhr-ms-dev-router
helm template srhr-ms-dev-router Deployment/helm-srhr-ms-dev-router
```

The chart intentionally starts one Router replica because its signing key is
generated in memory. For multiple replicas, store one shared EC private key in
a Kubernetes Secret and load it in `DevJwtService`.
