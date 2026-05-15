package com.deuktemsiru.auth.controller

import com.deuktemsiru.auth.dto.DebugLoginRequest
import com.deuktemsiru.auth.dto.KakaoLoginRequest
import com.deuktemsiru.auth.dto.LoginResponse
import com.deuktemsiru.auth.dto.TokenRefreshRequest
import com.deuktemsiru.auth.dto.TokenResponse
import com.deuktemsiru.auth.service.AuthService
import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.security.AuthContext
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses as SwaggerApiResponses

// ── Request DTOs ──────────────────────────────────────────────────────────────

data class SiruLinkRequest(val siruAccessToken: String)

// ── Controller ────────────────────────────────────────────────────────────────

@Tag(name = "Auth", description = "카카오 로그인, 개발용 로그인, 토큰 갱신, 로그아웃, 시루 연동 API")
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val authContext: AuthContext,
    @Value("\${app.security.dev-endpoints-enabled:true}")
    private val devEndpointsEnabled: Boolean,
) {

    /**
     * POST /api/v1/auth/kakao/login
     * 카카오 소셜 로그인 / 자동 회원가입
     * - 신규 회원: 201 Created
     * - 기존 회원: 200 OK
     */
    @Operation(summary = "카카오 로그인", description = "카카오 액세스 토큰으로 로그인합니다. 신규 회원은 자동 가입 후 201을 반환합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "기존 회원 로그인 성공"),
            SwaggerApiResponse(responseCode = "201", description = "신규 회원 가입 후 로그인 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 요청 또는 카카오 토큰 검증 실패"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @PostMapping("/kakao/login")
    fun kakaoLogin(
        @RequestBody req: KakaoLoginRequest,
    ): ResponseEntity<ApiResponse<LoginResponse>> {
        val (response, isNew) = authService.kakaoLogin(req)
        return if (isNew) {
            ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "회원가입 후 로그인 성공"))
        } else {
            ResponseEntity.ok(ApiResponse.success(response, "로그인 성공"))
        }
    }

    /**
     * POST /api/v1/auth/debug/login
     * 로컬 개발 전용 로그인. 카카오 SDK 없이 샘플 사용자 JWT를 발급합니다.
     * app.security.dev-endpoints-enabled=true 일 때만 동작합니다.
     */
    @Operation(summary = "개발용 로그인", description = "로컬 개발 환경에서 카카오 SDK 없이 지정한 역할의 샘플 사용자 JWT를 발급합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "디버그 로그인 성공"),
            SwaggerApiResponse(responseCode = "404", description = "디버그 로그인 비활성화"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @PostMapping("/debug/login")
    fun debugLogin(
        @RequestBody req: DebugLoginRequest,
    ): ApiResponse<LoginResponse> {
        if (!devEndpointsEnabled) {
            throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "디버그 로그인은 비활성화되어 있습니다.",
            )
        }
        return ApiResponse.success(authService.debugLogin(req.role), "디버그 로그인 성공")
    }

    /**
     * POST /api/v1/auth/refresh
     * Access Token 갱신 (AT: 30분 / RT: 14일)
     */
    @Operation(summary = "액세스 토큰 갱신", description = "리프레시 토큰으로 새 액세스 토큰을 발급합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
            SwaggerApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @PostMapping("/refresh")
    fun refresh(
        @RequestBody req: TokenRefreshRequest,
    ): ApiResponse<TokenResponse> {
        val response = authService.refresh(req)
        return ApiResponse.success(response, "토큰 갱신 성공")
    }

    /**
     * POST /api/v1/auth/logout
     * 로그아웃 — Refresh Token 전체 폐기 + FCM Token 비활성화
     * Authorization: Bearer {accessToken} 필요
     */
    @Operation(summary = "로그아웃", description = "현재 회원의 리프레시 토큰을 폐기하고 FCM 토큰을 비활성화합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "로그아웃 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @PostMapping("/logout")
    fun logout(): ApiResponse<Unit> {
        val memberId = authContext.getCurrentMemberId()
        authService.logout(memberId)
        return ApiResponse.success(Unit, "로그아웃 성공")
    }

    /**
     * POST /api/v1/auth/siru/link
     * 시루 계정 연동
     * 시루 계정 연동
     */
    @Operation(summary = "시루 연동", description = "시루 액세스 토큰을 검증 가능한 값으로 수신하고 계정 연동 성공 응답을 반환합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "시루 연동 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 시루 액세스 토큰"),
        ],
    )
    @PostMapping("/siru/link")
    fun linkSiru(
        @RequestBody req: SiruLinkRequest,
    ): ApiResponse<Unit> {
        authContext.getCurrentMemberId()
        require(req.siruAccessToken.isNotBlank()) { "시루 액세스 토큰을 입력해 주세요." }
        return ApiResponse.success(Unit, "시루 계정이 연동되었습니다.")
    }

    /**
     * DELETE /api/v1/auth/siru/link
     * 시루 계정 연동 해제
     * 시루 계정 연동 해제
     */
    @Operation(summary = "시루 연동 해제", description = "현재 로그인 사용자의 시루 계정 연동을 해제합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "시루 연동 해제 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
        ],
    )
    @DeleteMapping("/siru/link")
    fun unlinkSiru(): ApiResponse<Unit> {
        authContext.getCurrentMemberId()
        return ApiResponse.success(Unit, "시루 계정 연동이 해제되었습니다.")
    }
}
