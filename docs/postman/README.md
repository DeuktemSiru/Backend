# Deuktemsiru Postman

## Files

- `deuktemsiru-core.postman_collection.json`: 로컬 개발에서 자주 쓰는 핵심 API 플로우
- `deuktemsiru-local.postman_environment.json`: 로컬 서버용 환경 변수

## Import

1. 백엔드를 `dev` 프로파일로 실행합니다.

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

2. Postman에서 두 JSON 파일을 import합니다.
3. Environment를 `Deuktemsiru Local`로 선택합니다.
4. `Auth / Debug Login - Buyer`와 `Auth / Debug Login - Seller`를 먼저 실행합니다.

## CLI / CI

Newman으로 저장된 컬렉션을 실행합니다.

```bash
npm ci
npm run postman:test
```

GitHub Actions CI는 PostgreSQL 서비스를 띄우고, 백엔드를 `dev` 프로파일로 실행한 뒤 위 Newman 테스트를 자동 실행합니다.

## Full API Import

전체 엔드포인트 컬렉션이 필요하면 백엔드 실행 후 아래 URL을 Postman에서 import합니다.

```text
http://localhost:8080/v3/api-docs
```

Swagger UI는 아래 주소에서 확인합니다.

```text
http://localhost:8080/swagger-ui/index.html
```

## Suggested Flow

1. `Auth / Debug Login - Buyer`
2. `Buyer - Browse / List Products`
3. `Buyer - Cart and Orders / Add To Cart`
4. `Buyer - Cart and Orders / Create Order`
5. `Auth / Debug Login - Seller`
6. `Seller - Orders / Get Store Orders`
7. `Seller - Orders / Confirm Order`
8. `Seller - Orders / Verify Pickup Code`
