# AWS 배포 가이드 (EC2 + RDS + GitHub Actions)

`deuktemsiru_backend` (Spring Boot 4.0 / Kotlin / Java 21 / Gradle) 프로젝트를 AWS EC2에 배포하고, GitHub Actions로 자동 배포하는 절차를 정리한 문서입니다.

---

## 1. 아키텍처 개요

```
[GitHub] --push--> [GitHub Actions]
                         |
                         | 1) ./gradlew bootJar
                         | 2) SCP로 JAR 업로드
                         | 3) SSH로 재시작
                         v
                   [EC2 (Ubuntu 22.04)]
                         |
                         | JDBC
                         v
                   [RDS PostgreSQL]
```

- **EC2**: Spring Boot JAR을 `systemd`로 상시 구동
- **RDS**: PostgreSQL (build.gradle.kts에 `postgresql` 런타임 의존성 존재)
- **S3 (선택)**: `uploads/` 디렉터리를 대체할 파일 스토리지
- **Route53 + ACM + ALB (선택)**: HTTPS 도메인 적용 시

---

## 2. AWS 리소스 사전 준비

### 2.1 VPC / 보안 그룹

| 보안 그룹 | 인바운드 규칙 |
|----------|--------------|
| `sg-app` (EC2)  | 22(SSH, 내 IP), 8080(0.0.0.0/0 또는 ALB), 443(ALB만) |
| `sg-db` (RDS)   | 5432, 소스 = `sg-app` 만 허용 |

### 2.2 RDS (PostgreSQL)

1. **콘솔 → RDS → 데이터베이스 생성**
2. 엔진: PostgreSQL 16, 템플릿: 프리 티어(개발) / Multi-AZ(운영)
3. 자격 증명: `master_user` / 강력한 패스워드
4. 퍼블릭 액세스: **No**
5. VPC 보안 그룹: `sg-db`
6. 초기 DB명: `deuktemsiru`
7. 생성 후 **엔드포인트** 메모 (예: `deuktemsiru.xxxx.ap-northeast-2.rds.amazonaws.com`)

### 2.3 EC2

1. AMI: Ubuntu Server 22.04 LTS, 타입: `t3.small` 이상 권장 (Spring Boot 4 + JDK 21)
2. 키 페어 생성 후 `.pem` 안전 보관
3. 보안 그룹: `sg-app`
4. 탄력적 IP 할당 후 인스턴스에 연결 (재부팅 시 IP 변경 방지)

---

## 3. EC2 초기 셋업

```bash
ssh -i deuktemsiru.pem ubuntu@<EC2_PUBLIC_IP>

# JDK 21
sudo apt update
sudo apt install -y openjdk-21-jre-headless

# 배포 전용 사용자 & 디렉터리
sudo useradd -r -m -s /bin/bash app
sudo mkdir -p /opt/deuktemsiru
sudo chown app:app /opt/deuktemsiru
```

### 3.1 환경 변수 파일

```bash
sudo tee /etc/deuktemsiru.env >/dev/null <<'EOF'
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://<RDS_ENDPOINT>:5432/deuktemsiru
SPRING_DATASOURCE_USERNAME=master_user
SPRING_DATASOURCE_PASSWORD=<DB_PASSWORD>
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
SERVER_PORT=8080
EOF
sudo chmod 600 /etc/deuktemsiru.env
sudo chown app:app /etc/deuktemsiru.env
```

> 시크릿은 가능한 한 **AWS Systems Manager Parameter Store** 또는 **Secrets Manager**로 옮기는 것을 권장합니다.

### 3.2 systemd 서비스

```bash
sudo tee /etc/systemd/system/deuktemsiru.service >/dev/null <<'EOF'
[Unit]
Description=Deuktemsiru Backend
After=network.target

[Service]
User=app
WorkingDirectory=/opt/deuktemsiru
EnvironmentFile=/etc/deuktemsiru.env
ExecStart=/usr/bin/java -jar /opt/deuktemsiru/app.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable deuktemsiru
```

---

## 4. 애플리케이션 설정 (`application-prod.yml`)

`src/main/resources/application-prod.yml` 신규 작성:

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: ${SPRING_JPA_HIBERNATE_DDL_AUTO:validate}
    properties:
      hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect
  h2:
    console:
      enabled: false

server:
  port: ${SERVER_PORT:8080}
  forward-headers-strategy: framework

management:
  endpoints.web.exposure.include: health,info
  endpoint.health.probes.enabled: true
```

> `ddl-auto`는 운영에서 `validate`로 두고, 스키마 변경은 Flyway/Liquibase로 관리하는 것을 강력히 권장합니다.

---

## 5. GitHub Actions 자동 배포

### 5.1 GitHub Secrets 등록

레포 → Settings → Secrets and variables → Actions

| 이름 | 값 |
|------|----|
| `EC2_HOST` | EC2 탄력적 IP 또는 도메인 |
| `EC2_USER` | `ubuntu` |
| `EC2_SSH_KEY` | `.pem` 파일 전체 내용 |

### 5.2 워크플로 파일

저장소의 `.github/workflows/deploy.yml`이 `main` 브랜치 push 또는 수동 실행(`workflow_dispatch`) 시 다음 순서로 배포합니다.

1. `./gradlew clean check bootJar --stacktrace`로 테스트/검증/실행 JAR 빌드
2. `*-plain.jar`를 제외한 실행 JAR을 `app.jar`로 패키징
3. GitHub Secrets의 EC2 접속 정보로 `/tmp/deuktemsiru-release/app.jar` 업로드
4. `/opt/deuktemsiru/app.jar` 교체 후 `deuktemsiru` systemd 서비스 재시작
5. `http://127.0.0.1:8080/actuator/health` 확인, 실패 시 서비스 로그 출력

```yaml
name: Deploy to EC2

on:
  push:
    branches: [main]
  workflow_dispatch:

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Validate Gradle wrapper
        uses: gradle/actions/wrapper-validation@v4

      - name: Build executable JAR
        run: ./gradlew clean check bootJar --stacktrace

      - name: Prepare deployment artifact
        run: |
          set -euo pipefail
          jar_path="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | head -n 1)"
          if [ -z "$jar_path" ]; then
            echo "No executable JAR found in build/libs" >&2
            exit 1
          fi
          mkdir -p deploy-package
          cp "$jar_path" deploy-package/app.jar

      - name: Upload JAR to EC2
        uses: appleboy/scp-action@v0.1.7
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USER }}
          key: ${{ secrets.EC2_SSH_KEY }}
          source: deploy-package/app.jar
          target: /tmp/deuktemsiru-release
          strip_components: 1

      - name: Restart service
        uses: appleboy/ssh-action@v1.2.0
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USER }}
          key: ${{ secrets.EC2_SSH_KEY }}
          script: |
            set -euo pipefail
            sudo install -o app -g app -m 0644 /tmp/deuktemsiru-release/app.jar /opt/deuktemsiru/app.jar
            sudo systemctl restart deuktemsiru

            for attempt in $(seq 1 12); do
              if curl -fsS http://127.0.0.1:8080/actuator/health; then
                exit 0
              fi
              echo "Waiting for backend health check... (${attempt}/12)"
              sleep 5
            done

            sudo systemctl status deuktemsiru --no-pager
            sudo journalctl -u deuktemsiru -n 100 --no-pager
            exit 1
```

---

## 6. 첫 배포 검증

```bash
# 로컬에서
gh workflow run deploy.yml

# EC2에서
sudo journalctl -u deuktemsiru -f
curl http://localhost:8080/actuator/health
# 외부에서
curl http://<EC2_PUBLIC_IP>:8080/actuator/health
```

기대 응답: `{"status":"UP"}`

---

## 7. HTTPS / 도메인 (선택)

1. Route53에서 도메인 호스팅 영역 생성
2. ACM에서 인증서 발급 (ap-northeast-2 리전, ALB용)
3. ALB 생성 → 타깃 그룹에 EC2 등록 → 리스너 443(HTTPS) → 80은 443으로 리다이렉트
4. EC2 보안 그룹은 ALB 보안 그룹에서 들어오는 8080만 허용하도록 축소

---

## 8. S3 이미지 스토리지 전환

현재 `MenuImageStorageService`는 EC2 로컬 디스크(`uploads/menu-images/`)에 저장하고 `/uploads/menu-images/{uuid}.ext` 경로를 반환합니다. EC2는 휘발성이고 스케일아웃 시 파일이 공유되지 않으므로 **S3로 옮기는 것을 권장**합니다.

### 8.1 S3 버킷 생성

1. **S3 → 버킷 만들기**
   - 이름: `deuktemsiru-images-prod` (전역 유일)
   - 리전: `ap-northeast-2`
   - **퍼블릭 액세스 차단: 모두 켜둠** (CloudFront 또는 Presigned URL로 노출)
2. **버전 관리**: 활성화 권장 (실수 삭제 복구)
3. **수명 주기 규칙(선택)**: 30일 후 `STANDARD_IA` 전환으로 비용 절감
4. **CORS 설정** (이미지 직접 업로드/조회 시):

```json
[
  {
    "AllowedOrigins": ["https://your-domain.com"],
    "AllowedMethods": ["GET", "PUT"],
    "AllowedHeaders": ["*"],
    "MaxAgeSeconds": 3000
  }
]
```

### 8.2 IAM 정책 & 자격 증명

EC2에 **IAM Role**을 부여하는 방식이 가장 안전합니다 (액세스 키 노출 X).

1. IAM → 역할 → AWS 서비스(EC2) 선택 → 정책 생성:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::deuktemsiru-images-prod/*"
    },
    {
      "Effect": "Allow",
      "Action": ["s3:ListBucket"],
      "Resource": "arn:aws:s3:::deuktemsiru-images-prod"
    }
  ]
}
```

2. 역할명: `deuktemsiru-ec2-role`
3. EC2 인스턴스 → 작업 → 보안 → IAM 역할 수정 → 위 역할 연결

> 로컬 개발 환경에서는 `~/.aws/credentials`나 환경변수(`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`)를 사용. AWS SDK가 자동으로 자격 증명 체인을 탐색합니다.

### 8.3 의존성 추가

`build.gradle.kts`:

```kotlin
dependencies {
    // ...
    implementation(platform("software.amazon.awssdk:bom:2.28.16"))
    implementation("software.amazon.awssdk:s3")
}
```

### 8.4 설정 추가

`application.properties` (또는 `application-prod.yml`):

```properties
app.upload.storage=s3
app.upload.s3.bucket=deuktemsiru-images-prod
app.upload.s3.region=ap-northeast-2
app.upload.s3.public-base-url=https://deuktemsiru-images-prod.s3.ap-northeast-2.amazonaws.com
# CloudFront 사용 시: https://cdn.your-domain.com
```

로컬 개발에서는 `app.upload.storage=local`로 두면 기존 동작 유지.

### 8.5 S3 구현체

`MenuImageStorageService`를 인터페이스로 추출하고 S3 구현을 추가합니다.

```kotlin
// service/MenuImageStorageService.kt
interface MenuImageStorageService {
    fun save(image: MultipartFile?): String?
}
```

```kotlin
// service/S3MenuImageStorageService.kt
package com.deuktemsiru.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.util.UUID

@Service
@ConditionalOnProperty(name = ["app.upload.storage"], havingValue = "s3")
class S3MenuImageStorageService(
    private val s3: S3Client,
    @Value("\${app.upload.s3.bucket}") private val bucket: String,
    @Value("\${app.upload.s3.public-base-url}") private val publicBaseUrl: String,
) : MenuImageStorageService {

    override fun save(image: MultipartFile?): String? {
        if (image == null || image.isEmpty) return null

        val contentType = image.contentType.orEmpty()
        require(contentType.startsWith("image/")) { "이미지 파일만 업로드할 수 있습니다." }

        val extension = image.originalFilename
            ?.substringAfterLast('.', "")
            ?.takeIf { it.isNotBlank() }
            ?.lowercase()
            ?: contentType.substringAfter("image/", "jpg")

        val key = "menu-images/${UUID.randomUUID()}.$extension"

        val request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .contentLength(image.size)
            .build()

        s3.putObject(request, RequestBody.fromInputStream(image.inputStream, image.size))
        return "$publicBaseUrl/$key"
    }
}
```

기존 로컬 구현에는 `@ConditionalOnProperty(name = ["app.upload.storage"], havingValue = "local", matchIfMissing = true)`를 붙여 둘 중 하나만 빈으로 등록되도록 합니다.

### 8.6 S3Client 빈

```kotlin
// config/S3Config.kt
package com.deuktemsiru.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

@Configuration
@ConditionalOnProperty(name = ["app.upload.storage"], havingValue = "s3")
class S3Config(
    @Value("\${app.upload.s3.region}") private val region: String,
) {
    @Bean
    fun s3Client(): S3Client = S3Client.builder()
        .region(Region.of(region))
        // 자격 증명은 DefaultCredentialsProvider 자동 탐색 (EC2 IAM Role / 환경변수 / ~/.aws)
        .build()
}
```

### 8.7 공개 방식 선택

| 방식 | 특징 | 권장 시나리오 |
|------|------|-------------|
| **CloudFront + OAC** | 버킷은 비공개 유지, CDN으로 캐싱·HTTPS·도메인 적용 | **운영 환경 기본값** |
| **Presigned URL (GET)** | 요청 시 서버가 짧은 만료의 서명 URL 발급 | 권한 통제가 필요한 비공개 이미지 |
| **퍼블릭 버킷** | 단순하지만 비용·보안 위험 | 비권장 |

**CloudFront 추천 흐름**: 버킷 비공개 → CloudFront 배포 → OAC(Origin Access Control)로 버킷 정책 자동 구성 → ACM 인증서로 `cdn.your-domain.com` 연결 → `app.upload.s3.public-base-url`을 CloudFront 도메인으로 설정.

### 8.8 정적 리소스 핸들러 정리

S3로 전환했다면 `WebConfig`의 `/uploads/menu-images/**` 핸들러와 `SecurityConfig`의 동일 경로 permitAll은 더 이상 필요 없습니다. 단, **DB에 저장된 기존 `/uploads/...` 경로 데이터를 마이그레이션**한 뒤 제거하세요.

### 8.9 마이그레이션 절차 (기존 파일이 있다면)

```bash
# EC2에서 실행
aws s3 sync /opt/deuktemsiru/uploads/menu-images/ \
  s3://deuktemsiru-images-prod/menu-images/ \
  --content-type-by-extension
```

DB의 `image_url` 컬럼을 일괄 업데이트:

```sql
UPDATE menu
SET image_url = REPLACE(image_url, '/uploads/menu-images/', 'https://cdn.your-domain.com/menu-images/')
WHERE image_url LIKE '/uploads/menu-images/%';
```

---

## 9. 운영 체크리스트

- [ ] `application-prod.yml` 추가 및 `prod` 프로파일 활성화
- [ ] H2 콘솔 비활성화 확인 (`spring.h2.console.enabled=false`)
- [ ] Spring Security 설정에서 Actuator `/health`만 익명 허용
- [ ] RDS 자동 백업 보관 기간 7일 이상
- [ ] CloudWatch Logs Agent 설치 (`/var/log/syslog`, journald → CloudWatch)
- [ ] CloudWatch 알람: CPU > 80%, RDS 연결 수 임계, 5xx 비율
- [ ] `uploads/` → S3 마이그레이션 (EC2 디스크는 휘발성으로 가정)
- [ ] DB 마이그레이션 도구(Flyway) 도입
- [ ] 시크릿을 SSM Parameter Store / Secrets Manager로 이전

---

## 10. 트러블슈팅

| 증상 | 점검 포인트 |
|------|------------|
| `systemctl status` 에 `Active: failed` | `journalctl -u deuktemsiru -n 200` 확인, 환경변수 누락/JDK 버전 |
| `Connection refused` (DB) | RDS 보안 그룹이 `sg-app`을 허용하는지, 엔드포인트/포트 정확한지 |
| `403 Forbidden` (Actuator) | `SecurityConfig`에서 `/actuator/health` permitAll 추가 |
| 배포 후 변경사항 반영 안 됨 | `/opt/deuktemsiru/app.jar` 타임스탬프 확인, 캐시된 구 JAR 여부 |
| OOM Killed | 인스턴스 타입 상향 또는 `JAVA_TOOL_OPTIONS=-Xmx512m` 등으로 힙 제한 |
