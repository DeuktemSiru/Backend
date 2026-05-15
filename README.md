# 득템시루 백엔드

마감 할인 상품을 탐색하고 픽업 주문을 관리하는 득템시루 서비스의 Spring Boot 백엔드입니다. 구매자 앱과 판매자 앱에서 사용하는 카카오 로그인, JWT 인증, 매장, 찜, 주문, 판매자 매장 관리, 매출 통계 API를 제공합니다.

## 프로젝트 정보

| 항목 | 내용 |
| --- | --- |
| 프로젝트 | `deuktemsiru_backend` |
| 기본 주소 | `http://localhost:8080` |
| API 문서 | `http://localhost:8080/swagger-ui/index.html` |
| H2 콘솔 | `http://localhost:8080/h2-console` |
| 인증 방식 | JWT Bearer Token |
| 개발 DB | H2 인메모리 DB |
| 업로드 경로 | `/uploads/menu-images/**` |

## 주요 기능

| 기능 | 설명 |
| --- | --- |
| 카카오 인증 | 카카오 Access Token으로 로그인하거나 자동 회원가입합니다. |
| 토큰 관리 | Access Token 재발급, 로그아웃, Refresh Token 폐기를 처리합니다. |
| 매장 조회 | 카테고리별 매장 목록과 매장 상세 정보를 제공합니다. |
| 찜 | 로그인 사용자의 관심 매장 등록/해제와 목록 조회를 처리합니다. |
| 주문 | 구매자 주문 생성, 주문 목록, 주문 상세 조회를 제공합니다. |
| 판매자 주문 | 판매자 매장의 주문 목록 조회와 주문 상태 변경을 처리합니다. |
| 판매자 매장 | 판매자 매장 정보 조회와 설명/전화번호 수정을 지원합니다. |
| 매출 통계 | 주간, 월간, 연간 매출 데이터와 인기 상품을 제공합니다. |
| 알림 | 구매자 알림 조회와 읽음 처리를 제공합니다. |
| 샘플 데이터 | 개발 실행 시 판매자, 구매자, 매장, 메뉴, 상품 샘플 데이터를 생성합니다. |

## 기술 스택

| 구분 | 내용 |
| --- | --- |
| 언어 | Kotlin 2.2.21 |
| 런타임 | Java 21 |
| 프레임워크 | Spring Boot 4.0.6 |
| 데이터 | Spring Data JPA, Hibernate, H2, PostgreSQL Connector |
| 보안 | Spring Security, JWT |
| API 문서 | springdoc-openapi 3.0.3 |
| 테스트 | JUnit 5, Spring Boot Test |
| 빌드 | Gradle Kotlin DSL |
| CI | GitHub Actions, `./gradlew test` |

## 실행 방법

### 1. Java 21 설정

Android Studio 내장 JDK를 사용할 수 있습니다.

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

### 2. 서버 실행

```bash
./gradlew bootRun
```

Gradle이 다른 JDK를 자동 감지해 문제가 생기면 Java 경로를 명시합니다.

```bash
./gradlew bootRun \
  -Dorg.gradle.java.installations.auto-detect=false \
  -Dorg.gradle.java.installations.paths="$JAVA_HOME"
```

### 3. 테스트

```bash
./gradlew test
```

## 설정

| 설정 | 기본값 | 설명 |
| --- | --- | --- |
| `server.port` | `8080` | 서버 포트 |
| `spring.datasource.url` | `jdbc:h2:mem:deuktemsiru;DB_CLOSE_DELAY=-1` | 개발용 H2 DB |
| `spring.datasource.username` | `sa` | H2 사용자명 |
| `spring.datasource.password` | 빈 값 | H2 비밀번호 |
| `app.jwt.secret` | 개발용 기본값 | 운영 환경에서는 `APP_JWT_SECRET` 환경변수로 교체해야 합니다. |
| `app.jwt.access-token-expiration-seconds` | `1800` | Access Token 만료 시간, 30분 |
| `app.jwt.refresh-token-expiration-seconds` | `1209600` | Refresh Token 만료 시간, 14일 |
| `app.upload.menu-image-dir` | `uploads/menu-images` | 메뉴 이미지 저장 디렉터리 |
| `spring.servlet.multipart.max-file-size` | `5MB` | 단일 업로드 파일 제한 |
| `spring.servlet.multipart.max-request-size` | `6MB` | multipart 요청 제한 |
| `kakao.api.user-info-url` | `https://kapi.kakao.com/v2/user/me` | 카카오 사용자 정보 API |
| `app.security.dev-endpoints-enabled` | `true` | 개발용 H2 콘솔 공개 여부. 운영 profile에서는 `false`입니다. |

운영 실행 시에는 `prod` profile을 사용하고 `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `APP_JWT_SECRET`을 환경변수로 설정합니다. `prod` profile은 H2 콘솔을 비활성화하고 JPA DDL 자동 생성을 `validate`로 둡니다.

AWS RDS PostgreSQL 예시:

```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://RDS_ENDPOINT:5432/deuktemsiru"
export SPRING_DATASOURCE_USERNAME="postgres"
export SPRING_DATASOURCE_PASSWORD="your-rds-password"
export APP_JWT_SECRET="replace-with-a-long-random-secret"
java -jar build/libs/deuktemsiru_backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## 샘플 데이터

`DataInitializer`는 DB가 비어 있을 때 다음 데이터를 생성합니다.

| 역할 | 이메일 | 이름/닉네임 | 비고 |
| --- | --- | --- | --- |
| 구매자 | `buyer@test.com` | 홍길동 / 시흥득템러 | 카카오 샘플 사용자 |
| 판매자 | `bakery@test.com` | 김영희 / 오이도굽는집 | 오이도 굽는집, BAKERY |
| 판매자 | `cafe@siheung.test` | 박민준 / 배곧로스터리 | 배곧 로스터리, CAFE |
| 판매자 | `bunsik@siheung.test` | 이수진 / 정왕시장분식 | 정왕시장 분식, RESTAURANT |
| 판매자 | `dosirak@siheung.test` | 최하늘 / 은행동찬찬도시락 | 은행동 찬찬도시락, RESTAURANT |
| 판매자 | `mart@siheung.test` | 정다은 / 목감우리반찬 | 목감 우리반찬, GROCERY |

각 판매자 매장에는 시흥시 생활권 주소, 좌표, 전화번호, 평점, 리뷰 수와 함께 메뉴 1개와 오늘 판매 가능한 마감할인 상품 1개가 생성됩니다. 샘플 매장은 오이도, 배곧, 정왕시장, 은행동, 목감 권역을 기준으로 구성했습니다.

## 프로젝트 구조

```text
src/main/kotlin/com/deuktemsiru/
├── DeuktemsiruApplication.kt
├── DataInitializer.kt
├── auth/         # 카카오 로그인, 토큰 재발급, 로그아웃
├── common/       # 공통 응답, 공통 예외
├── config/       # Security, OpenAPI, WebConfig, 전역 예외 처리
├── controller/   # REST API 컨트롤러
├── dto/          # 요청/응답 DTO
├── entity/       # JPA 엔티티
├── repository/   # Spring Data JPA 리포지토리
├── security/     # JWT 서비스, 인증 필터, 인증 컨텍스트
└── service/      # 도메인 서비스
```

## API 목록

인증이 필요한 API는 `Authorization: Bearer {accessToken}` 헤더를 전송해야 합니다.

### 인증

| 메서드 | 경로 | 설명 | 인증 |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/kakao/login` | 카카오 로그인/자동 회원가입 | 불필요 |
| `POST` | `/api/v1/auth/refresh` | Access Token 재발급 | 불필요 |
| `POST` | `/api/v1/auth/logout` | 로그아웃, Refresh Token 폐기 | 필요 |

### 매장/찜

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `GET` | `/api/v1/stores?category={category}` | 매장 목록 조회 |
| `GET` | `/api/v1/stores/{storeId}` | 매장 상세 조회 |
| `POST` | `/api/v1/wishlist/{storeId}` | 찜 등록/해제 |
| `GET` | `/api/v1/wishlist` | 내 찜 목록 조회 |

### 주문

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `POST` | `/api/v1/orders` | 주문 생성 |
| `GET` | `/api/v1/orders` | 내 주문 목록 조회 |
| `GET` | `/api/v1/orders/{orderId}` | 주문 상세 조회 |

### 판매자

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `GET` | `/api/v1/seller/products` | 판매 상품 목록 조회 |
| `POST` | `/api/v1/seller/products` | 판매 상품 등록 |
| `PATCH` | `/api/v1/seller/products/{id}` | 판매 상품 상태 변경 |
| `DELETE` | `/api/v1/seller/products/{id}` | 판매 상품 취소 |
| `GET` | `/api/v1/seller/menus` | 메뉴 목록 조회 |
| `POST` | `/api/v1/seller/menus` | 메뉴 등록, JSON 또는 multipart |
| `PATCH` | `/api/v1/seller/menus/{menuItemId}` | 메뉴 수정 |
| `DELETE` | `/api/v1/seller/menus/{menuItemId}` | 메뉴 삭제 |
| `GET` | `/api/v1/seller/orders` | 매장 주문 목록 조회 |
| `PATCH` | `/api/v1/seller/orders/{orderId}` | 주문 상태 변경 |
| `GET` | `/api/v1/seller/pickup/verify?code={code}` | 픽업 코드 검증 |
| `GET` | `/api/v1/seller/store` | 내 매장 조회 |
| `PATCH` | `/api/v1/seller/store` | 내 매장 수정 |
| `POST` | `/api/v1/seller/notifications` | 알림 발송 |
| `GET` | `/api/v1/seller/notifications` | 알림 내역 조회 |
| `GET` | `/api/v1/seller/sales?period={period}&offset={offset}` | 매출 통계 조회 |

### 사용자/알림

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `GET` | `/api/v1/users/me` | 내 회원 정보 조회 |
| `GET` | `/api/v1/notifications` | 내 알림 목록 조회 |
| `PATCH` | `/api/v1/notifications/{notificationId}/read` | 알림 읽음 처리 |

## 도메인 참고

### 주문 상태

```text
PENDING -> PREPARING -> READY -> COMPLETED
       └-> CANCELLED

PREPARING -> CANCELLED
READY -> CANCELLED
```

### 매장 카테고리

```text
BAKERY / CAFE / RESTAURANT / GROCERY / OTHER
```

### 주문 생성 요청 예시

```json
{
  "storeId": 1,
  "items": [
    {
      "menuItemId": 1,
      "quantity": 2
    }
  ]
}
```
