# 득템시루 백엔드

식품 마감 할인 픽업 예약 서비스 득템시루의 Spring Boot 백엔드 서버입니다. 구매자 앱과 판매자 앱이 사용하는 인증, 매장, 메뉴, 주문, 찜, 매출, 알림 API를 제공합니다.

## 개요

| 항목 | 내용 |
| --- | --- |
| 프로젝트 | `deuktemsiru_backend` |
| 서버 | `http://localhost:8080` |
| API 문서 | `http://localhost:8080/swagger-ui/index.html` |
| H2 콘솔 | `http://localhost:8080/h2-console` |
| 인증 | JWT Bearer Token |
| 업로드 경로 | `/uploads/menu-images/**` |

## 주요 기능

| 기능 | 설명 |
| --- | --- |
| 인증 | 회원가입, 로그인, JWT 발급을 처리합니다. |
| 사용자 | 로그인 사용자의 프로필 정보를 조회합니다. |
| 매장/메뉴 | 카테고리별 매장 목록, 매장 상세, 메뉴 정보를 제공합니다. |
| 주문 | 구매자 주문 생성과 주문 상세/목록 조회를 처리합니다. |
| 찜 | 구매자의 관심 매장 토글과 목록 조회를 제공합니다. |
| 판매자 | 매장 정보, 메뉴 등록/수정/삭제, 주문 상태 변경을 처리합니다. |
| 메뉴 이미지 | multipart/form-data 메뉴 이미지 업로드와 정적 파일 제공을 처리합니다. |
| 매출 | 판매자별 기간별 매출 통계를 제공합니다. |
| 알림 | 판매자 알림 발송, 발송 이력, 구매자 알림 조회를 제공합니다. |
| 샘플 데이터 | 앱 실행 테스트용 사용자, 매장, 메뉴 데이터를 시작 시 생성합니다. |

## 기술 스택

| 구분 | 내용 |
| --- | --- |
| 언어 | Kotlin 2.2.21 |
| 프레임워크 | Spring Boot 4.0.6 |
| 데이터 | Spring Data JPA, Hibernate, H2, MySQL Connector |
| 보안 | Spring Security, JWT |
| API 문서 | springdoc-openapi 3.0.3 |
| 테스트 | JUnit 5, Spring Boot Test |
| 런타임 | Java 21 |
| 빌드 | Gradle Kotlin DSL |

## 실행 방법

### 1. Java 21 확인

Android Studio 내장 JDK를 사용하는 경우 다음처럼 `JAVA_HOME`을 설정할 수 있습니다.

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

### 2. 서버 실행

```bash
./gradlew bootRun
```

Gradle이 다른 JDK를 자동 감지해 문제가 생기면 Java 경로를 명시해 실행합니다.

```bash
./gradlew bootRun \
  -Dorg.gradle.java.installations.auto-detect=false \
  -Dorg.gradle.java.installations.paths="$JAVA_HOME"
```

### 3. 접속 정보

| 항목 | 값 |
| --- | --- |
| 서버 | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| H2 콘솔 | `http://localhost:8080/h2-console` |
| JDBC URL | `jdbc:h2:mem:deuktemsiru;DB_CLOSE_DELAY=-1` |
| 사용자명 | `sa` |
| 비밀번호 | 빈 값 |

## 샘플 계정

앱 시작 시 `DataInitializer`가 샘플 계정과 매장/메뉴 데이터를 생성합니다.

| 역할 | 이메일 | 비밀번호 |
| --- | --- | --- |
| 구매자 | `buyer@test.com` | `1234` |
| 판매자 - 영희네 베이커리 | `bakery@test.com` | `1234` |
| 판매자 - 맛있는 도시락 | `lunchbox@test.com` | `1234` |
| 판매자 - 그린 샐러드 | `salad@test.com` | `1234` |
| 판매자 - 커피향기 | `cafe1@test.com` | `1234` |
| 판매자 - 달콤카페 | `cafe2@test.com` | `1234` |
| 판매자 - 파리크라상 | `bakery2@test.com` | `1234` |

## 프로젝트 구조

```text
src/main/kotlin/com/deuktemsiru/
├── DeuktemsiruApplication.kt
├── DataInitializer.kt
├── config/       # 보안, OpenAPI, 웹 리소스, 전역 예외 처리
├── controller/   # REST API 컨트롤러
├── dto/          # 요청/응답 DTO
├── entity/       # JPA 엔티티
├── repository/   # Spring Data JPA 리포지토리
├── security/     # 인증 컨텍스트, JWT, 필터
└── service/      # 도메인 서비스, 이미지 저장
```

## API 목록

### 인증/사용자

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `POST` | `/api/auth/register` | 회원가입 |
| `POST` | `/api/auth/login` | 로그인 |
| `GET` | `/api/users/{userId}` | 사용자 정보 조회 |

### 구매자

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `GET` | `/api/stores?category={category}&userId={userId}` | 매장 목록 조회 |
| `GET` | `/api/stores/{storeId}?userId={userId}` | 매장 상세 조회 |
| `POST` | `/api/orders?buyerId={buyerId}` | 주문 생성 |
| `GET` | `/api/orders?buyerId={buyerId}` | 내 주문 목록 조회 |
| `GET` | `/api/orders/{orderId}` | 주문 상세 조회 |
| `POST` | `/api/wishlist/{storeId}?userId={userId}` | 찜 토글 |
| `GET` | `/api/wishlist?userId={userId}` | 찜 목록 조회 |
| `GET` | `/api/notifications?userId={userId}` | 구매자 알림 조회 |

### 판매자

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `GET` | `/api/seller/store?sellerId={sellerId}` | 내 가게 정보 조회 |
| `PATCH` | `/api/seller/store?sellerId={sellerId}` | 내 가게 정보 수정 |
| `POST` | `/api/seller/menus?sellerId={sellerId}` | 메뉴 등록(JSON 또는 multipart/form-data) |
| `PATCH` | `/api/seller/menus/{menuItemId}?sellerId={sellerId}` | 메뉴 수정 |
| `DELETE` | `/api/seller/menus/{menuItemId}?sellerId={sellerId}` | 메뉴 삭제 |
| `GET` | `/api/seller/orders?sellerId={sellerId}` | 주문 목록 조회 |
| `PATCH` | `/api/seller/orders/{orderId}?sellerId={sellerId}` | 주문 상태 변경 |
| `GET` | `/api/seller/sales?sellerId={sellerId}&period={period}&offset={offset}` | 매출 통계 조회 |
| `POST` | `/api/seller/notifications?sellerId={sellerId}` | 알림 발송 |
| `GET` | `/api/seller/notifications?sellerId={sellerId}` | 알림 내역 조회 |

## 도메인 참고

### 주문 상태

```text
NEW -> PREPARING -> READY -> COMPLETED
              └-> REJECTED
```

### 매장 카테고리

```text
BAKERY / LUNCHBOX / SALAD / CAFE
```

### 메뉴 이미지 업로드

| 항목 | 값 |
| --- | --- |
| 요청 형식 | `multipart/form-data` |
| 파일 필드 | `image` |
| 저장 위치 | `uploads/menu-images` |
| 최대 파일 크기 | `5MB` |
| 최대 요청 크기 | `6MB` |
