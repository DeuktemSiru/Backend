# k6 System Tests

These scripts exercise the local Spring Boot REST API with the same dev seed data used by the Postman collection.

## Prerequisites

- Run the backend with the `dev` profile or enable debug auth endpoints.
- Install the `k6` CLI, or run through Docker.

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## Smoke Test

Runs one end-to-end pass through health, debug login, buyer browse, cart, order creation/cancel, and seller order lookup.

```bash
npm run k6:smoke
```

## Read Load Test

Runs repeatable read-heavy traffic against store, product, buyer order list, and seller order list APIs. It avoids order creation so repeated runs do not consume stock or skew state.

```bash
npm run k6:load
```

The load profile can be tuned with environment variables:

```bash
BASE_URL=http://localhost:8080 VUS=20 RAMP_UP=1m HOLD=3m RAMP_DOWN=30s npm run k6:load
```

## Docker Alternative

When k6 is not installed locally:

```bash
docker run --rm -i --network host -v "$PWD:/work" -w /work grafana/k6 run tests/k6/smoke.js
docker run --rm -i --network host -v "$PWD:/work" -w /work grafana/k6 run tests/k6/load.js
```

On Docker Desktop for macOS, replace the base URL because `--network host` does not behave like Linux:

```bash
docker run --rm -i -v "$PWD:/work" -w /work -e BASE_URL=http://host.docker.internal:8080 grafana/k6 run tests/k6/smoke.js
```
