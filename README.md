# 득템시루 백엔드

마감 임박 할인 상품을 실시간으로 탐색하고 픽업 주문을 연결하는 **득템시루** 플랫폼의 Spring Boot 백엔드입니다. 구매자 앱과 판매자 앱이 공통으로 사용하는 REST API를 제공합니다.

## 시스템 개요

```
[구매자 앱]  ──┐
               ├──▶  deuktemsiru_backend (Spring Boot)  ──▶  PostgreSQL / H2
[판매자 앱]  ──┘          │
                          ├──▶  Kakao API (사용자 인증)
                          ├──▶  로컬 파일 스토리지 (메뉴 이미지)
                          └──▶  Prometheus / Grafana (모니터링)
```

## 프로젝트 정보

| 항목 | 내용 |
| --- | --- |
| 프로젝트 | `deuktemsiru_backend` |
| 언어 / 런타임 | Kotlin 2.2.21 / Java 21 |
| 프레임워크 | Spring Boot 4.0.6 |
| 기본 주소 | `http://localhost:8080` |
| API 문서 (Swagger) | `http://localhost:8080/swagger-ui/index.html` |
| H2 콘솔 (개발) | `http://localhost:8080/h2-console` |
| 인증 방식 | JWT Bearer Token |
| 개발 DB | H2 인메모리 |
| 운영 DB | PostgreSQL |

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| 언어 | Kotlin 2.2.21 |
| 런타임 | Java 21 |
| 프레임워크 | Spring Boot 4.0.6 |
| 데이터 | Spring Data JPA, Hibernate, H2, PostgreSQL |
| 보안 | Spring Security 6, JWT |
| API 문서 | springdoc-openapi 3.0.3 |
| 모니터링 | Spring Boot Actuator, Micrometer, Prometheus, Grafana |
| 테스트 | JUnit 5, Spring Boot Test, Jacoco |
| 빌드 | Gradle Kotlin DSL |
| 컨테이너 | Docker, Docker Compose |

## 시작하기

### 사전 준비

- **Java 21** (Android Studio 내장 JDK 사용 가능)
- Kakao Developers 앱 등록 및 네이티브 앱 키 발급

### 1. Java 경로 설정

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

### 2. 서버 실행

```bash
./gradlew bootRun
```

Gradle이 다른 JDK를 자동 감지해 충돌이 생기는 경우:

```bash
./gradlew bootRun \
  -Dorg.gradle.java.installations.auto-detect=false \
  -Dorg.gradle.java.installations.paths="$JAVA_HOME"
```

### 3. 테스트 실행

```bash
./gradlew test
```

## 환경 설정

### 개발 환경 (기본값)

별도 설정 없이 `./gradlew bootRun`으로 실행하면 H2 인메모리 DB와 개발용 기본값이 적용됩니다.

| 설정 키 | 기본값 | 설명 |
| --- | --- | --- |
| `server.port` | `8080` | 서버 포트 |
| `spring.datasource.url` | `jdbc:h2:mem:deuktemsiru;DB_CLOSE_DELAY=-1` | 개발용 H2 DB |
| `app.jwt.access-token-expiration-seconds` | `1800` | Access Token 만료 (30분) |
| `app.jwt.refresh-token-expiration-seconds` | `1209600` | Refresh Token 만료 (14일) |
| `app.upload.menu-image-dir` | `uploads/menu-images` | 메뉴 이미지 저장 경로 |
| `spring.servlet.multipart.max-file-size` | `5MB` | 파일당 최대 크기 |
| `spring.servlet.multipart.max-request-size` | `6MB` | 요청당 최대 크기 |
| `app.security.dev-endpoints-enabled` | `true` | H2 콘솔 공개 여부 |

### 운영 환경 (prod 프로파일)

`prod` 프로파일은 H2 콘솔을 비활성화하고 JPA DDL 전략을 `validate`로 전환합니다. 아래 환경변수를 반드시 설정해야 합니다.

```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://RDS_ENDPOINT:5432/deuktemsiru"
export SPRING_DATASOURCE_USERNAME="postgres"
export SPRING_DATASOURCE_PASSWORD="your-rds-password"
export APP_JWT_SECRET="replace-with-a-long-random-secret"

java -jar build/libs/deuktemsiru_backend-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod
```

## Docker Compose로 전체 스택 실행

백엔드, PostgreSQL, Prometheus, Grafana를 한 번에 실행합니다.

```bash
POSTGRES_PASSWORD=비밀번호 \
APP_JWT_SECRET=시크릿 \
GRAFANA_PASSWORD=그라파나비밀번호 \
docker-compose up -d
```

Grafana 초기 계정은 `admin` / `GRAFANA_PASSWORD` 환경변수 값(기본값 `admin`)입니다.

## 샘플 데이터

`DataInitializer`는 DB가 비어 있을 때 시흥시 생활권을 기반으로 한 샘플 데이터를 자동 생성합니다.

| 역할 | 이름 (닉네임) | 매장 | 카테고리 |
| --- | --- | --- | --- |
| 구매자 | 홍길동 (시흥득템러) | — | — |
| 판매자 | 김영희 (오이도굽는집) | 오이도 굽는집 | BAKERY |
| 판매자 | 박민준 (배곧로스터리) | 배곧 로스터리 | CAFE |
| 판매자 | 이수진 (정왕시장분식) | 정왕시장 분식 | RESTAURANT |
| 판매자 | 최하늘 (은행동찬찬도시락) | 은행동 찬찬도시락 | RESTAURANT |
| 판매자 | 정다은 (목감우리반찬) | 목감 우리반찬 | GROCERY |

각 매장에는 시흥시 생활권 주소·좌표, 전화번호, 평점, 메뉴 1개, 오늘 판매 가능한 마감할인 상품 1개가 함께 생성됩니다.

## 모니터링

Spring Boot Actuator와 Micrometer로 Prometheus 메트릭을 수집하고 Grafana로 시각화합니다.

| 항목 | 주소 |
| --- | --- |
| 메트릭 엔드포인트 | `http://localhost:8080/actuator/prometheus` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |

### 수집 메트릭

| 구분 | 메트릭 |
| --- | --- |
| 비즈니스 | 총 주문 수, 총 매장 수, 총 회원 수 (`deuktemsiru.*`) |
| HTTP | 엔드포인트별 RPS, p50 / p95 / p99 응답 시간, 상태 코드 분포 |
| DB | HikariCP 커넥션 풀 활성 / 대기 / 최대 수 |
| JVM | 힙 메모리, 스레드 수, GC 일시정지 시간, CPU 사용률 |

### 모니터링 파일 구조

```
monitoring/
├── prometheus.yml
└── grafana/
    ├── provisioning/
    │   ├── datasources/prometheus.yml
    │   └── dashboards/dashboard.yml
    └── dashboards/
        └── deuktemsiru.json
```

> **운영 보안**: Prometheus(9090)와 Grafana(3000) 포트는 방화벽으로 내부망에서만 접근하도록 제한하세요.

## 프로젝트 구조

```
src/main/kotlin/com/deuktemsiru/
├── DeuktemsiruApplication.kt
├── DataInitializer.kt          # 개발용 샘플 데이터 초기화
├── auth/                       # 카카오 로그인, 토큰 재발급, 로그아웃
├── common/                     # 공통 응답 형식, 공통 예외
├── config/                     # Security, OpenAPI, WebConfig, 전역 예외 처리
├── controller/                 # REST 컨트롤러
├── dto/                        # 요청 / 응답 DTO
├── entity/                     # JPA 엔티티
├── repository/                 # Spring Data JPA 리포지토리
├── security/                   # JWT 서비스, 인증 필터, 인증 컨텍스트
└── service/                    # 도메인 서비스
```

## API 목록

인증이 필요한 API는 요청 헤더에 `Authorization: Bearer {accessToken}`을 포함해야 합니다.

### 인증

| 메서드 | 경로 | 설명 | 인증 필요 |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/kakao/login` | 카카오 로그인 / 자동 회원가입 | — |
| `POST` | `/api/v1/auth/refresh` | Access Token 재발급 | — |
| `POST` | `/api/v1/auth/logout` | 로그아웃, Refresh Token 폐기 | ✓ |
| `POST` | `/api/v1/auth/siru/link` | 시루 계정 연동 | ✓ |
| `DELETE` | `/api/v1/auth/siru/link` | 시루 계정 연동 해제 | ✓ |

### 매장 / 찜

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `GET` | `/api/v1/stores?category={category}` | 카테고리별 매장 목록 조회 |
| `GET` | `/api/v1/stores/{storeId}` | 매장 상세 조회 |
| `POST` | `/api/v1/wishlist/{storeId}` | 찜 등록 / 해제 토글 |
| `GET` | `/api/v1/wishlist` | 내 찜 목록 조회 |

### 장바구니

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `POST` | `/api/v1/cart` | 장바구니 상품 추가 |
| `GET` | `/api/v1/cart` | 내 장바구니 조회 |
| `DELETE` | `/api/v1/cart/{cartItemId}` | 장바구니 상품 삭제 |
| `DELETE` | `/api/v1/cart` | 장바구니 전체 비우기 |

### 주문

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `POST` | `/api/v1/orders` | 주문 생성 |
| `GET` | `/api/v1/orders` | 내 주문 목록 조회 |
| `GET` | `/api/v1/orders/{orderId}` | 주문 상세 조회 |
| `PATCH` | `/api/v1/orders/{orderId}/cancel` | 주문 취소 |

### 판매자

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `GET` | `/api/v1/sellers/products` | 판매 상품 목록 조회 |
| `POST` | `/api/v1/sellers/products` | 판매 상품 등록 |
| `PATCH` | `/api/v1/sellers/products/{productId}/status` | 판매 상품 상태 변경 |
| `DELETE` | `/api/v1/sellers/products/{productId}` | 판매 상품 삭제 |
| `GET` | `/api/v1/sellers/menu-items` | 메뉴 목록 조회 |
| `POST` | `/api/v1/sellers/menu-items` | 메뉴 등록 (JSON 또는 multipart) |
| `PATCH` | `/api/v1/sellers/menu-items/{menuItemId}` | 메뉴 수정 |
| `DELETE` | `/api/v1/sellers/menu-items/{menuItemId}` | 메뉴 삭제 |
| `GET` | `/api/v1/sellers/orders` | 매장 주문 목록 조회 |
| `PATCH` | `/api/v1/sellers/orders/{orderId}/status` | 주문 상태 변경 |
| `GET` | `/api/v1/sellers/pickup/verify?code={code}` | 픽업 코드 검증 |
| `GET` | `/api/v1/sellers/stores/my` | 내 매장 조회 |
| `PUT` | `/api/v1/sellers/stores/my` | 내 매장 정보 수정 |
| `POST` | `/api/v1/sellers/notifications` | 고객 대상 알림 발송 |
| `GET` | `/api/v1/sellers/notifications` | 알림 발송 내역 조회 |
| `GET` | `/api/v1/sellers/sales/summary?period={period}&date={date}` | 매출 통계 조회 |
| `GET` | `/api/v1/sellers/settlements?year={year}&month={month}` | 월별 정산 내역 조회 |
| `POST` | `/api/v1/sellers/settlements/withdrawals` | 출금 신청 |

### 사용자 / 알림

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `GET` | `/api/v1/users/me` | 내 회원 정보 조회 |
| `GET` | `/api/v1/notifications` | 내 알림 목록 조회 |
| `PATCH` | `/api/v1/notifications/{notificationId}/read` | 알림 읽음 처리 |

## 도메인 참고

### 주문 상태 흐름

```
PENDING ──▶ PREPARING ──▶ READY ──▶ COMPLETED
   │              │          │
   └──────────────┴──────────┴──▶ CANCELLED
```

### 매장 카테고리

```
BAKERY  /  CAFE  /  RESTAURANT  /  GROCERY  /  OTHER
```

### 주문 생성 요청 예시

```json
{
  "storeId": 1,
  "items": [
    { "menuItemId": 1, "quantity": 2 }
  ]
}
```
