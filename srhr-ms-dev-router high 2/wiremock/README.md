# EPA WireMock configuration

Copy `wiremock/mappings/*.json` into the mappings directory of
`srhr-ms-mock-service`.

The expected DEV validation chain is:

```text
SRHR service -> srhr-ms-mock-service /epa/open-keys
             -> proxy -> srhr-ms-dev-router /epa/open-keys

SRHR service -> srhr-ms-mock-service /epa/jti?<jti>
             -> {"status": true}
```

Configure the EPA/JWT SDK in each SRHR service with URLs equivalent to:

```yaml
sign-url: http://srhr-ms-mock-service:8080/epa/open-keys
jti-url: http://srhr-ms-mock-service:8080/epa/jti
```

The exact property names belong to the installed EPA SDK version. Keep the
existing property names from the service and replace only their URL values.

The open-keys mapping proxies to the router because the router creates an
ephemeral ES256 key at startup. This guarantees that WireMock always exposes
the public key matching newly issued tokens. With more than one router replica,
move the signing key to a Kubernetes Secret; the supplied Helm chart intentionally
uses one replica.

For an anti-replay failure test, replace `mappings/epa-jti-success.json` with
`examples/epa-jti-failure.json` and reset WireMock mappings.
