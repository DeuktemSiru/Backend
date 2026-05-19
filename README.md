<h1 align="center">득템시루 Backend</h1>

<p align="center">
  <b>마감 임박 할인 상품을 발견하고, 픽업으로 득템하세요</b><br/>
  구매자 앱과 판매자 앱을 연결하는 Spring Boot 기반 REST API 서버
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"/>
  <img src="https://img.shields.io/badge/Java 21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"/>
  <img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white"/>
  <img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Flyway-CC0200?style=flat-square&logo=flyway&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white"/>
  <img src="https://img.shields.io/badge/JPA / Hibernate-59666C?style=flat-square&logo=hibernate&logoColor=white"/>
  <img src="https://img.shields.io/badge/Prometheus-E6522C?style=flat-square&logo=prometheus&logoColor=white"/>
  <img src="https://img.shields.io/badge/Grafana-F46800?style=flat-square&logo=grafana&logoColor=white"/>
  <img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white"/>
</p>

---

마감 임박 식품을 할인 판매하고, 사용자가 가까운 매장에서 픽업 주문할 수 있도록 연결하는 **득템시루** 서비스의 Spring Boot 백엔드입니다. 구매자 앱과 판매자 앱이 공통으로 사용하는 REST API, JWT 인증, 주문/장바구니/찜/리뷰/알림/정산 기능을 제공합니다.

## 프로젝트 소개

| 항목 | 내용 |
| --- | --- |
| 프로젝트명 | 득템시루 `deuktemsiru_backend` |
| 주제 | 마감 할인 상품 기반 지역 픽업 커머스 |
| 개발 목적 | 판매자는 폐기 예정 상품을 줄이고, 구매자는 합리적인 가격으로 지역 매장 상품을 구매할 수 있는 플랫폼 구현 |
| 주요 사용자 | 구매자 앱 사용자, 판매자 앱 사용자 |
| API 기본 주소 | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| 인증 방식 | JWT Bearer Token |

## 핵심 기능

### 구매자

- 카카오 로그인 및 JWT 기반 인증
- 마감 할인 상품 목록/상세 조회
- 매장 목록, 매장 상세, 지도 마커 조회
- 장바구니 담기, 수량 변경, 삭제, 전체 비우기
- 픽업 주문 생성, 주문 내역/상세 조회, 주문 취소
- 매장 찜 등록/해제 및 찜 목록 조회
- 리뷰 작성/삭제, 매장 리뷰 조회
- 알림 목록 조회, 읽음 처리, 삭제
- 내 프로필, 통계, 알림 설정 관리

### 판매자

- 사업자 정보 등록
- 내 매장 등록/조회/수정
- 메뉴 등록/수정/삭제
- 판매 상품 등록/수정/상태 변경/삭제
- 주문 목록/상세 조회, 주문 상태 변경, 픽업 확정
- 픽업 코드 검증
- 고객 대상 알림 발송 및 발송 내역 조회
- 매출 요약 조회
- 월별 정산 내역 조회 및 출금 신청

### 운영/품질

- Spring Security + JWT 인증/인가
- 역할 기반 접근 제어 `CONSUMER`, `SELLER`
- PostgreSQL 기반 데이터 저장
- Flyway 기반 운영 DB 마이그레이션
- Spring Boot Actuator, Micrometer, Prometheus, Grafana 모니터링
- Jacoco 테스트 리포트 및 미완성 Stub 검증 태스크

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| Language | Kotlin 2.2.21 |
| Runtime | Java 21 |
| Framework | Spring Boot 4.0.6 |
| Web | Spring Web MVC |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL 16 |
| Migration | Flyway |
| Security | Spring Security, JWT |
| API Docs | springdoc-openapi 3.0.3 |
| Monitoring | Spring Boot Actuator, Micrometer, Prometheus, Grafana |
| Test | JUnit 5, Spring Boot Test, Testcontainers, Jacoco |
| Build | Gradle Kotlin DSL |
| Infra | Docker, Docker Compose |

## 시스템 구성

```text
[구매자 앱] ──┐
             ├── REST API ──> deuktemsiru_backend ──> PostgreSQL
[판매자 앱] ──┘                    │
                                  ├── Kakao API
                                  ├── Local Upload Storage
                                  └── Actuator / Prometheus / Grafana
```

## 프로젝트 구조

```text
src/main/kotlin/com/deuktemsiru
├── DeuktemsiruApplication.kt
├── DataInitializer.kt              # prod 외 프로파일 샘플 데이터 생성
├── auth/                           # 카카오 로그인, 토큰 재발급, 로그아웃
├── common/                         # 공통 응답, 예외, 유틸
├── config/                         # Security, OpenAPI, Jackson, Metrics 설정
├── controller/                     # REST API 컨트롤러
│   ├── cart/
│   ├── fcm/
│   ├── member/
│   ├── notification/
│   ├── order/
│   ├── product/
│   ├── review/
│   ├── seller/
│   ├── store/
│   └── wishlist/
├── dto/                            # 요청/응답 DTO
├── entity/                         # JPA 엔티티 및 Enum
├── repository/                     # Spring Data JPA Repository
├── security/                       # JWT 필터, 인증 컨텍스트
└── service/                        # 도메인 서비스
```

## 실행 방법

### 사전 준비

- Java 21
- Docker 또는 로컬 PostgreSQL
- Kakao Developers 앱 및 카카오 Access Token 테스트 환경

### 로컬 DB 실행

```bash
docker run --name deuktemsiru-postgres \
  -e POSTGRES_DB=deuktemsiru \
  -e POSTGRES_USER=deuktemsiru \
  -e POSTGRES_PASSWORD=deuktemsiru \
  -p 5432:5432 \
  -d postgres:16-alpine
```

기본 설정은 위 PostgreSQL 접속 정보를 사용합니다.

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/deuktemsiru
SPRING_DATASOURCE_USERNAME=deuktemsiru
SPRING_DATASOURCE_PASSWORD=deuktemsiru
```

### 애플리케이션 실행

```bash
./gradlew bootRun
```

Android Studio 내장 JDK를 사용할 경우:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew bootRun
```

개발 편의를 위해 `dev` 프로파일을 함께 사용할 수 있습니다.

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 테스트 및 검증

```bash
./gradlew test
./gradlew check
```

`check`는 테스트, Jacoco 리포트, 미완성 Stub 검사를 함께 실행합니다.

## Docker Compose 실행

백엔드, PostgreSQL, Prometheus, Grafana를 한 번에 실행합니다.

```bash
POSTGRES_PASSWORD=deuktemsiru \
APP_JWT_SECRET=replace-with-long-random-secret \
GRAFANA_PASSWORD=admin \
docker-compose up -d
```

| 서비스 | 주소 |
| --- | --- |
| Backend | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |

## 환경 변수

| 이름 | 기본값 | 설명 |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/deuktemsiru` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `deuktemsiru` | DB 사용자 |
| `SPRING_DATASOURCE_PASSWORD` | `deuktemsiru` | DB 비밀번호 |
| `APP_JWT_SECRET` | 개발용 기본값 | JWT 서명 키. 운영에서는 반드시 교체 |
| `APP_UPLOAD_MENU_IMAGE_DIR` | `uploads/menu-images` | 업로드 이미지 저장 경로 |
| `SPRING_PROFILES_ACTIVE` | 없음 | `dev`, `prod` 등 활성 프로파일 |

### 운영 프로파일

`prod` 프로파일은 운영 DB 연결값을 환경변수로 주입받고, Flyway 마이그레이션을 활성화하며, 개발용 엔드포인트를 비활성화합니다.

```bash
./gradlew bootJar

SPRING_PROFILES_ACTIVE=prod \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/deuktemsiru \
SPRING_DATASOURCE_USERNAME=deuktemsiru \
SPRING_DATASOURCE_PASSWORD=deuktemsiru \
APP_JWT_SECRET=replace-with-long-random-secret \
java -jar build/libs/deuktemsiru-0.0.1-SNAPSHOT.jar
```

## 샘플 데이터

`DataInitializer`는 `prod`가 아닌 환경에서 DB가 비어 있을 때 시흥시 생활권 기반 샘플 데이터를 생성합니다.

| 역할 | 이름 | 닉네임 | 매장 |
| --- | --- | --- | --- |
| 구매자 | 홍길동 | 시흥득템러 | - |
| 판매자 | 김영희 | 오이도굽는집 | 오이도굽는집 |
| 판매자 | 박민준 | 배곧로스터리 | 배곧 로스터리 |
| 판매자 | 이수진 | 정왕시장분식 | 정왕시장 분식 |
| 판매자 | 최하늘 | 은행동찬찬도시락 | 은행동 찬찬도시락 |
| 판매자 | 정다은 | 목감우리반찬 | 목감 우리반찬 |

각 매장에는 주소, 좌표, 전화번호, 평점, 메뉴, 당일 판매 가능한 마감 할인 상품이 함께 생성됩니다.

개발용 로그인은 `dev` 프로파일 또는 `app.security.dev-endpoints-enabled=true`일 때 사용할 수 있습니다.

```http
POST /api/v1/auth/debug/login
```

## API 요약

인증이 필요한 요청은 아래 헤더를 포함해야 합니다.

```http
Authorization: Bearer {accessToken}
```

### Auth

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `POST` | `/api/v1/auth/kakao/login` | 카카오 로그인 및 자동 회원가입 |
| `POST` | `/api/v1/auth/debug/login` | 개발용 샘플 사용자 로그인 |
| `POST` | `/api/v1/auth/refresh` | Access Token 재발급 |
| `POST` | `/api/v1/auth/logout` | 로그아웃 및 FCM 토큰 비활성화 |
| `POST` | `/api/v1/auth/siru/link` | 시루 계정 연동 |
| `DELETE` | `/api/v1/auth/siru/link` | 시루 계정 연동 해제 |

### Buyer

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `GET` | `/api/v1/products` | 판매 상품 목록 조회 |
| `GET` | `/api/v1/products/{productId}` | 판매 상품 상세 조회 |
| `GET` | `/api/v1/stores` | 매장 목록 조회 |
| `GET` | `/api/v1/stores/{storeId}` | 매장 상세 조회 |
| `GET` | `/api/v1/stores/map` | 지도 마커 조회 |
| `GET` | `/api/v1/stores/{storeId}/reviews` | 매장 리뷰 조회 |
| `POST` | `/api/v1/cart` | 장바구니 상품 추가 |
| `GET` | `/api/v1/cart` | 장바구니 조회 |
| `PATCH` | `/api/v1/cart/{cartItemId}` | 장바구니 수량 변경 |
| `DELETE` | `/api/v1/cart/{cartItemId}` | 장바구니 상품 삭제 |
| `DELETE` | `/api/v1/cart` | 장바구니 전체 비우기 |
| `POST` | `/api/v1/orders` | 주문 생성 |
| `GET` | `/api/v1/orders/my` | 내 주문 목록 조회 |
| `GET` | `/api/v1/orders/{orderId}` | 주문 상세 조회 |
| `PATCH` | `/api/v1/orders/{orderId}/cancel` | 주문 취소 |
| `POST` | `/api/v1/wishlist/{storeId}` | 찜 토글 |
| `DELETE` | `/api/v1/wishlist/{storeId}` | 찜 삭제 |
| `GET` | `/api/v1/wishlist` | 찜 목록 조회 |
| `POST` | `/api/v1/reviews` | 리뷰 작성 |
| `DELETE` | `/api/v1/reviews/{reviewId}` | 리뷰 삭제 |

### Member / Notification / FCM

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `GET` | `/api/v1/members/me` | 내 프로필 조회 |
| `PUT` | `/api/v1/members/me` | 내 프로필 수정 |
| `DELETE` | `/api/v1/members/me` | 회원 탈퇴 |
| `GET` | `/api/v1/members/me/stats` | 내 통계 조회 |
| `GET` | `/api/v1/members/me/notification-settings` | 알림 설정 조회 |
| `PUT` | `/api/v1/members/me/notification-settings` | 알림 설정 수정 |
| `GET` | `/api/v1/notifications` | 내 알림 목록 조회 |
| `PATCH` | `/api/v1/notifications/{notificationId}/read` | 알림 읽음 처리 |
| `DELETE` | `/api/v1/notifications/{notificationId}` | 알림 삭제 |
| `POST` | `/api/v1/fcm/token` | FCM 토큰 등록 |

### Seller

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `POST` | `/api/v1/sellers/business-info` | 사업자 정보 등록 |
| `GET` | `/api/v1/sellers/stores/my` | 내 매장 조회 |
| `PUT` | `/api/v1/sellers/stores/my` | 내 매장 수정 |
| `POST` | `/api/v1/sellers/stores` | 매장 등록 |
| `GET` | `/api/v1/sellers/menu-items` | 메뉴 목록 조회 |
| `POST` | `/api/v1/sellers/menu-items` | 메뉴 등록 |
| `PATCH` | `/api/v1/sellers/menu-items/{menuItemId}` | 메뉴 수정 |
| `DELETE` | `/api/v1/sellers/menu-items/{menuItemId}` | 메뉴 삭제 |
| `GET` | `/api/v1/sellers/products` | 판매 상품 목록 조회 |
| `POST` | `/api/v1/sellers/products` | 판매 상품 등록 |
| `PATCH` | `/api/v1/sellers/products/{productId}` | 판매 상품 수정 |
| `PATCH` | `/api/v1/sellers/products/{productId}/status` | 판매 상품 상태 변경 |
| `DELETE` | `/api/v1/sellers/products/{productId}` | 판매 상품 삭제 |
| `GET` | `/api/v1/sellers/orders` | 매장 주문 목록 조회 |
| `GET` | `/api/v1/sellers/orders/{orderId}` | 매장 주문 상세 조회 |
| `PATCH` | `/api/v1/sellers/orders/{orderId}/status` | 주문 상태 변경 |
| `PATCH` | `/api/v1/sellers/orders/{orderId}/confirm` | 픽업 확정 |
| `GET` | `/api/v1/sellers/pickup/verify` | 픽업 코드 검증 |
| `POST` | `/api/v1/sellers/notifications` | 고객 대상 알림 발송 |
| `GET` | `/api/v1/sellers/notifications` | 알림 발송 내역 조회 |
| `GET` | `/api/v1/sellers/sales/summary` | 매출 요약 조회 |
| `GET` | `/api/v1/sellers/settlements` | 월별 정산 조회 |
| `POST` | `/api/v1/sellers/settlements/withdrawals` | 출금 신청 |

## 도메인 규칙

### 회원 역할

```text
CONSUMER: 구매자
SELLER: 판매자
```

### 상품 상태

```text
AVAILABLE -> SOLD_OUT
AVAILABLE -> EXPIRED
AVAILABLE -> DELETED
```

### 주문 상태

```text
PENDING -> CONFIRMED -> PICKED_UP
PENDING -> CANCELLED
CONFIRMED -> CANCELLED
```

### 매장 카테고리

```text
BAKERY / RESTAURANT / CAFE / GROCERY / OTHER
```

## 모니터링

| 항목 | 주소 |
| --- | --- |
| Health Check | `http://localhost:8080/actuator/health` |
| Prometheus Metrics | `http://localhost:8080/actuator/prometheus` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |

수집 대상:

- HTTP 요청 수, 응답 시간, 상태 코드
- JVM 메모리, 스레드, GC, CPU
- DB 커넥션 풀
- 주문 수, 매장 수, 회원 수 등 서비스 지표