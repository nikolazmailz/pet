# srhr-ms-dev-router

DEV-only router that replaces the routing part of corporate Tyk for an isolated
SRHR stand. It forwards browser calls to real Kubernetes services and injects a
fresh, signed test JWT into every proxied request.

> **Never deploy this application to production or expose it outside the
> protected DEV network.** It intentionally issues tokens without authenticating
> the caller.

## What the router reproduces

- accepts the existing frontend paths under `/ui-api-web/cross/srhr/...`;
- routes `paystub`, `userinfo` and `userphoto` to real services;
- removes the four-segment Tyk prefix before proxying;
- removes any incoming `Authorization` header;
- adds `Authorization: Bearer <DEV JWT>`;
- adds `X-Dev-Router`, `X-Dev-User` and `X-Request-ID`;
- handles browser CORS;
- publishes JWKS and JTI endpoints for DEV validation.

It does not reproduce Tyk quotas, analytics, production authentication or
corporate signing keys.

## JWT contract

The token contains every value read by the provided `JWTUtil`:

| Claim/header | Example | Consumer |
|---|---|---|
| `sub` | `developer@VTB.RU` | `getSub`, `getSubWithDomain`, `getFullSub` |
| `ctxi` | `dev-session-developer` | `getSessionId` |
| `channel` | `WEB` | `getChannel` and `AllowedChannels` |
| `realm` | `SRHR` | `getRealm` |
| `ip` | request IP | `getClientIp` |
| `jti` | random UUID | `getJti` |
| `iss`, `iat`, `nbf`, `exp`, `aud` | standard claims | validation |
| `kid`, `alg=ES256` | JWT header | signature lookup |

`channel` must exactly match one of your `AllowedChannels.channelNames()`.
Change it in `application.yml` or Helm values if `WEB` is not allowed.

## Routes

Default mapping:

```text
/ui-api-web/cross/srhr/paystub/**  -> PAYSTUB_ROUTE_URI/**
/ui-api-web/cross/srhr/userinfo/** -> USERINFO_ROUTE_URI/**
/ui-api-web/cross/srhr/userphoto/** -> USERPHOTO_ROUTE_URI/**
```

`StripPrefix=4` transforms, for example:

```text
/ui-api-web/cross/srhr/paystub/v1/paystub/period
-> /v1/paystub/period
```

Verify that this is the URI expected by the backend controller. If the real
Tyk keeps a different prefix, adjust `StripPrefix` in `application.yml`.

## Build and run

Requirements: JDK 17 and Maven 3.9+.

```bash
./mvnw clean verify
java -jar target/srhr-ms-dev-router-1.0.0-SNAPSHOT.jar
```

If your corporate network blocks Maven Central, either run the corporate
`mvn` command or set `MAVEN_DISTRIBUTION_URL` to the approved Nexus URL before
starting `./mvnw`.

Useful overrides:

```bash
export PAYSTUB_ROUTE_URI=http://localhost:9081
export USERINFO_ROUTE_URI=http://localhost:9082
export USERPHOTO_ROUTE_URI=http://localhost:9083
export FRONTEND_ORIGIN=https://information-about-salary.srhr.innodev.local
java -jar target/srhr-ms-dev-router-1.0.0-SNAPSHOT.jar
```

For a corporate build, replace `RUNTIME_IMAGE` with an approved base image:

```bash
docker build --build-arg RUNTIME_IMAGE=<corporate-jre17-image> -t srhr-ms-dev-router:1.0.0 .
```

## Check JWT and JWKS

```bash
curl http://localhost:8080/dev/users
curl http://localhost:8080/dev/token
curl http://localhost:8080/dev/token?user=reviewer
curl http://localhost:8080/.well-known/jwks.json
curl http://localhost:8080/dev/jti?any-jti-value
```

The router uses `developer` unless the request contains:

```http
X-Dev-User: reviewer
```

Do not log or commit tokens obtained from `/dev/token`.

## Backend JWT validation

The provided `JwtTokenFilter` decodes the claims first and then calls:

```java
epaSdkService.checkJWT(jwt)
```

Therefore claim compatibility alone is not enough when EPA validation is
enabled. Choose one DEV-only integration mode:

1. Configure the DEV validator to use:

   ```text
   JWKS: http://srhr-ms-dev-router:8180/.well-known/jwks.json
   JTI:  http://srhr-ms-dev-router:8180/dev/jti
   ```

2. Stub the EPA validation call so that it returns a successful
   `EpaValidationResult`.

3. Use the existing `jwt.disable`/local-principal mode. Note that this mode does
   not read the injected JWT because `JwtTokenFilter` applies its configured
   local principal immediately.

The corporate Tyk private signing key cannot and must not be copied into this
router.

## Frontend config

After HTTPS is configured for the router ingress:

```json
{
  "HOST": "https://srhr-dev-api.srhr.innodev.local",
  "ENV_USERINFO_URI": "/ui-api-web/cross/srhr/userinfo/v1",
  "ENV_PAYSTUB_URI": "/ui-api-web/cross/srhr/paystub/v1",
  "FGW_HOST": "https://srhr-dev-api.srhr.innodev.local",
  "ENV_USERPHOTO_URI": "/ui-api-web/cross/srhr/userphoto/v1"
}
```

The frontend is HTTPS, so the router must also be available through HTTPS.
`nginx.ingress.kubernetes.io/backend-protocol: HTTP` is still correct: it
describes only the Ingress-to-router connection.

## Helm

The chart is located in `Deployment/helm-srhr-ms-dev-router`.

Before deploying, set:

- the corporate image repository and tag;
- the actual internal Service URLs and ports;
- an allowed value for `jwt.*.channel`;
- the Ingress host/class;
- TLS secret or the platform's default wildcard certificate.

Render locally:

```bash
helm lint Deployment/helm-srhr-ms-dev-router
helm template srhr-ms-dev-router Deployment/helm-srhr-ms-dev-router
```
