# 득템시루 Backend

마감 임박 할인 상품의 지역 픽업 서비스를 제공하는 REST API 서버입니다. 구매자·판매자 Android 앱이 함께 사용합니다.

## 기술 스택

Kotlin · Spring Boot · Spring Data JPA · Spring Security · PostgreSQL · Flyway · JWT

의존성은 [build.gradle.kts](build.gradle.kts)에서 관리합니다.

## 시작하기

### 사전 요구사항

- JDK 21 (`JAVA_HOME`을 해당 JDK 경로로 설정)
- Docker 또는 실행 중인 PostgreSQL 16

### 설치 및 실행

저장소 루트에서 로컬 DB와 개발 서버를 실행합니다.

```bash
docker run --name deuktemsiru-postgres -p 5432:5432 -d \
  -e POSTGRES_DB=deuktemsiru -e POSTGRES_USER=deuktemsiru -e POSTGRES_PASSWORD=deuktemsiru \
  postgres:16-alpine
./gradlew bootRun --args='--spring.profiles.active=dev'
```

컨테이너가 이미 생성되어 있으면 `docker run` 대신 `docker start deuktemsiru-postgres`를 사용합니다.
Windows PowerShell에서는 Gradle 실행에 `.\gradlew`를 사용합니다.

### 환경 변수

로컬 설정은 [application.properties](src/main/resources/application.properties), 운영 시크릿·FCM·운영자 API 설정은 [배포 가이드](https://github.com/DeuktemSiru/.github/blob/main/reference/deploy.md#2-ec2-초기-셋업)를 참고합니다.

### Docker Compose로 실행

로컬에서 백엔드·PostgreSQL·Prometheus·Grafana를 함께 실행하려면 [docker-compose.yml](docker-compose.yml)을 사용합니다.

```bash
export POSTGRES_PASSWORD=deuktemsiru
export APP_JWT_SECRET="$(openssl rand -hex 32)"
export GRAFANA_PASSWORD=local-grafana
docker compose up -d
```

시작하기에서 띄운 서버·DB와 포트가 겹치므로 먼저 종료합니다. Prometheus는 `9090`, Grafana는 `3000` 포트를 사용합니다.
EC2·RDS 운영 배포는 [배포 가이드](https://github.com/DeuktemSiru/.github/blob/main/reference/deploy.md)를 따릅니다.

## 사용 방법

### API 접속

| 대상 | 주소 |
| --- | --- |
| 로컬 API | `http://localhost:8080` |
| Android 에뮬레이터에서 API | `http://10.0.2.2:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI | `http://localhost:8080/v3/api-docs` |

Swagger UI에서 엔드포인트를 확인하고 호출합니다. 인증·응답 형식은 [API 계약](https://github.com/DeuktemSiru/.github/blob/main/reference/contracts.md)를 참고합니다.

### 샘플 데이터

`prod`가 아니고 DB가 비어 있으면 구매자 1명과 시흥시 매장 5곳의 판매자·메뉴·마감 상품을 생성합니다.
매장은 오이도굽는집, 배곧 로스터리, 정왕시장 분식, 은행동 찬찬도시락, 목감 우리반찬입니다.
샘플 원본은 [DataInitializer.kt](src/main/kotlin/com/deuktemsiru/DataInitializer.kt)에서 관리합니다.

`dev` 프로파일에서는 `POST /api/v1/auth/debug/login`으로 샘플 계정에 로그인할 수 있습니다.

## 테스트

### Gradle

Docker를 준비한 뒤 실행합니다. 통합 테스트는 Testcontainers를 사용합니다.

```bash
./gradlew test
./gradlew check
```

`check`에는 Jacoco와 미완성 Stub 검사도 포함됩니다.

### API·부하 테스트

Node.js·npm과 실행 중인 `dev` 서버가 필요합니다. k6 테스트에는 별도로 k6를 설치합니다.

```bash
npm ci
npm run postman:test
npm run k6:smoke
BASE_URL=http://localhost:8080 VUS=20 RAMP_UP=1m HOLD=3m RAMP_DOWN=30s npm run k6:load
```

Postman GUI에서는 [tests/postman](tests/postman/)의 컬렉션·환경 파일을 가져와 `Deuktemsiru Local`을 선택하고 `Auth / Debug Login - Buyer|Seller`를 먼저 실행합니다.
전체 API를 가져오려면 `/v3/api-docs`를 사용합니다.

k6를 Docker로 실행하려면 다음을 사용합니다.

```bash
docker run --rm -i --network host -v "$PWD:/work" -w /work grafana/k6 run tests/k6/smoke.js
```

macOS에서는 `--network host`를 `-e BASE_URL=http://host.docker.internal:8080`으로 바꿉니다.
스모크 테스트는 주문 생성·취소를 수행하므로 로컬·테스트 환경을 대상으로 실행합니다. 부하 테스트는 조회 API를 반복합니다.

## 관련 문서

| 문서 | 내용 |
| --- | --- |
| [프로젝트 문서](https://github.com/DeuktemSiru/.github#관련-문서) | 요구사항·설계·작업 현황·배포 안내 |
| [구매자 앱](https://github.com/DeuktemSiru/BuyerApp) | 구매자 앱 설정·빌드·사용 |
| [판매자 앱](https://github.com/DeuktemSiru/SellerApp) | 판매자 앱 설정·빌드·사용 |
