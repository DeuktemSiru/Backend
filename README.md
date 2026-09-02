<h1 align="center">득템시루 Backend</h1>

<p align="center">
  <b>마감 임박 할인 상품을 발견하고, 픽업으로 득템하세요</b><br/>
  구매자 앱과 판매자 앱이 공유하는 Spring Boot REST API 서버
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"/>
  <img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white"/>
  <img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white"/>
  <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white"/>
</p>

---

## 한눈에 보기

**득템시루**는 마감 임박 식품을 할인 판매하는 지역 픽업 커머스입니다. 판매자는 폐기 예정 상품을 줄이고, 구매자는 가까운 매장 상품을 싸게 삽니다. 이 저장소는 두 앱이 공통으로 쓰는 백엔드로, **JWT 인증 · 상품/장바구니/주문 · 픽업 검증 · 리뷰/찜/알림 · 매출/정산** API를 제공합니다.

| 항목 | 내용 |
| --- | --- |
| 기술 스택 | Kotlin, Java 21, Spring Boot 4, Spring Data JPA, PostgreSQL 16, Flyway |
| 인증 | Spring Security + JWT (역할: `CONSUMER` / `SELLER`) |
| API 문서 | Swagger UI `http://localhost:8080/swagger-ui/index.html` |
| 알림 | DB 인앱 알림 + 선택적 Firebase Cloud Messaging |
| 모니터링 | Actuator · Micrometer · Prometheus · Grafana |
| 테스트 | JUnit 5, Testcontainers, Jacoco, k6, Postman/Newman |

## 핵심 기능

- **구매자** — 카카오 로그인, 매장/상품 탐색(지도 포함), 장바구니, 픽업 주문·취소, 찜, 리뷰, 알림, 프로필/통계
- **판매자** — 사업자·매장 등록, 메뉴/판매 상품 관리, 주문 상태 처리, 픽업 코드 검증, 고객 알림 발송, 매출 요약, 월별 정산·출금
- **운영** — 역할 기반 인가, Flyway 마이그레이션, Prometheus/Grafana 모니터링, 샘플 데이터 자동 생성(비-prod)

## 빠른 시작

### 1. PostgreSQL 준비 (둘 중 하나)

```bash
# Docker
docker run --name deuktemsiru-postgres \
  -e POSTGRES_DB=deuktemsiru -e POSTGRES_USER=deuktemsiru -e POSTGRES_PASSWORD=deuktemsiru \
  -p 5432:5432 -d postgres:16-alpine

# 또는 Homebrew (역할·DB 생성은 최초 1회)
brew services start postgresql@16
psql -d postgres -c "CREATE ROLE deuktemsiru LOGIN PASSWORD 'deuktemsiru';"
psql -d postgres -c "CREATE DATABASE deuktemsiru OWNER deuktemsiru;"
```

### 2. 백엔드 실행

```bash
# 시스템 java가 21이 아니면 Android Studio 내장 JDK 사용
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# dev 프로파일 = 샘플 데이터 생성 + 개발용 debug 로그인 활성화
./gradlew bootRun --args='--spring.profiles.active=dev'
```

- Android 에뮬레이터는 `http://10.0.2.2:8080`으로 무설정 접속됩니다.
- 개발용 로그인: `POST /api/v1/auth/debug/login` (dev 프로파일 전용)
- 종료: 터미널에서 `Ctrl+C`, DB까지 내리려면 `brew services stop postgresql@16`

### 3. 테스트

```bash
./gradlew test    # 단위/통합 테스트
./gradlew check   # + Jacoco 리포트, 미완성 Stub 검사
```

k6 부하 테스트는 dev 프로파일로 서버 실행 후 `npm run k6:smoke` / `npm run k6:load` (상세: `docs/k6/README.md`).

## API

전체 명세는 Swagger UI(`/swagger-ui/index.html`)와 OpenAPI JSON(`/v3/api-docs`)이 기준입니다. 인증이 필요한 요청은 `Authorization: Bearer {accessToken}` 헤더를 사용합니다.

| 도메인 | 대표 엔드포인트 |
| --- | --- |
| 인증 | `POST /api/v1/auth/kakao/login`, `/auth/refresh`, `/auth/logout`, `/auth/siru/link` |
| 상품/매장 | `GET /api/v1/products`, `GET /api/v1/stores`, `GET /api/v1/stores/map` |
| 장바구니/주문 | `POST /api/v1/cart`, `POST /api/v1/orders`, `PATCH /api/v1/orders/{id}/cancel` |
| 찜/리뷰/알림 | `POST /api/v1/wishlist/{storeId}`, `POST /api/v1/reviews`, `GET /api/v1/notifications` |
| 회원 | `GET/PUT/DELETE /api/v1/members/me`, 통계, 알림 설정 |
| 판매자 | `/api/v1/sellers/**` — 매장, 메뉴, 상품, 주문, 픽업 검증, 알림 발송, 매출, 정산 |

자주 쓰는 플로우는 Postman 컬렉션(`docs/postman/`)으로도 제공합니다.

## 도메인 규칙

```text
상품 상태:  AVAILABLE <-> PAUSED
           AVAILABLE -> SOLD_OUT (잔여 수량 0 시 자동) | EXPIRED | DELETED (삭제 API 전용)

주문 상태:  PENDING -> CONFIRMED -> PICKED_UP
           PENDING | CONFIRMED -> CANCELLED
```

매장 카테고리: `BAKERY / RESTAURANT / CAFE / GROCERY / OTHER`

## 프로젝트 구조

```text
src/main/kotlin/com/deuktemsiru
├── auth/          # 카카오 로그인, 토큰 재발급, 로그아웃
├── config/        # Security, OpenAPI, Jackson, Metrics
├── controller/    # REST API (cart, order, product, seller, store, ...)
├── dto/ entity/ repository/ service/
├── security/      # JWT 필터, 인증 컨텍스트
└── DataInitializer.kt  # 비-prod 프로파일 샘플 데이터 (시흥시 매장 5곳 + 구매자 1명)
```

## 샘플 데이터

`prod`가 아닌 프로파일에서 DB가 비어 있으면 아래 계정과 매장이 자동 생성됩니다. 각 매장에는 주소·좌표·전화번호·평점·메뉴·당일 마감 할인 상품이 함께 채워집니다.

| 역할 | 이름 | 닉네임 | 매장 |
| --- | --- | --- | --- |
| 구매자 | 홍길동 | 시흥득템러 | - |
| 판매자 | 김영희 | 오이도굽는집 | 오이도굽는집 |
| 판매자 | 박민준 | 배곧로스터리 | 배곧 로스터리 |
| 판매자 | 이수진 | 정왕시장분식 | 정왕시장 분식 |
| 판매자 | 최하늘 | 은행동찬찬도시락 | 은행동 찬찬도시락 |
| 판매자 | 정다은 | 목감우리반찬 | 목감 우리반찬 |

## Docker Compose / 운영

백엔드 + PostgreSQL + Prometheus + Grafana 일괄 실행:

```bash
POSTGRES_PASSWORD=deuktemsiru \
APP_JWT_SECRET=replace-with-long-random-secret \
GRAFANA_PASSWORD=admin \
docker-compose up -d
```

| 서비스 | 주소 |
| --- | --- |
| Backend / Swagger | `http://localhost:8080` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |

주요 환경 변수:

| 이름 | 기본값 | 설명 |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/deuktemsiru` | DB 접속 (USERNAME/PASSWORD 동일 계열) |
| `APP_JWT_SECRET` | 개발용 기본값 | JWT 서명 키 — **운영에서 반드시 교체** |
| `APP_FCM_ENABLED` | `false` | `true`이면 Firebase 푸시 발송 활성화 |
| `GOOGLE_APPLICATION_CREDENTIALS` | 없음 | Firebase 서비스 계정 JSON 경로 |
| `SPRING_PROFILES_ACTIVE` | 없음 | `dev`(샘플 데이터·debug 로그인) / `prod`(Flyway 활성, 개발 엔드포인트 비활성) |

운영 배포는 `./gradlew bootJar` 후 `SPRING_PROFILES_ACTIVE=prod`와 위 변수를 주입해 `java -jar build/libs/deuktemsiru-0.0.1-SNAPSHOT.jar`로 실행합니다.
