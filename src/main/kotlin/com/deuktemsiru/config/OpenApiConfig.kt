package com.deuktemsiru.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI {
        val bearerAuth = "bearerAuth"

        return OpenAPI()
            .info(
                Info()
                    .title("Deuktemsiru API")
                    .description(
                        """
                        득템시루 구매자 앱과 판매자 앱이 사용하는 REST API입니다.

                        인증이 필요한 API는 `Authorization: Bearer {accessToken}` 헤더를 사용합니다.
                        로컬 개발에서는 `dev` 프로파일 또는 `app.security.dev-endpoints-enabled=true` 설정 후
                        `/api/v1/auth/debug/login`으로 테스트용 JWT를 발급받을 수 있습니다.
                        """.trimIndent(),
                    )
                    .version("v1"),
            )
            .externalDocs(
                ExternalDocumentation()
                    .description("Postman collection and usage guide")
                    .url("/docs/postman"),
            )
            .servers(
                listOf(
                    Server().url("http://localhost:8080").description("Local server"),
                ),
            )
            .tags(
                listOf(
                    Tag().name("Auth").description("로그인, 토큰 갱신, 로그아웃, 계정 연동"),
                    Tag().name("Buyer Stores").description("구매자 매장 조회, 지도, 리뷰 조회"),
                    Tag().name("Buyer Products").description("구매자 상품 목록 및 상세 조회"),
                    Tag().name("Cart").description("구매자 장바구니"),
                    Tag().name("Orders").description("구매자 주문"),
                    Tag().name("Wishlist").description("구매자 찜"),
                    Tag().name("Reviews").description("구매자 리뷰 작성/삭제"),
                    Tag().name("Members").description("회원 프로필, 통계, 알림 설정"),
                    Tag().name("Notifications").description("구매자 알림 목록 및 읽음 처리"),
                    Tag().name("FCM").description("FCM 토큰 등록"),
                    Tag().name("Seller Auth").description("판매자 사업자 정보"),
                    Tag().name("Seller Stores").description("판매자 매장 관리"),
                    Tag().name("Seller Menu").description("판매자 메뉴 관리"),
                    Tag().name("Seller Products").description("판매자 상품 관리"),
                    Tag().name("Seller Orders").description("판매자 주문 및 픽업 관리"),
                    Tag().name("Seller Notifications").description("판매자 고객 알림 발송"),
                    Tag().name("Seller Sales").description("판매자 매출 요약"),
                    Tag().name("Seller Settlements").description("판매자 정산"),
                ),
            )
            .components(
                Components().addSecuritySchemes(
                    bearerAuth,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"),
                ),
            )
            .addSecurityItem(SecurityRequirement().addList(bearerAuth))
    }
}
