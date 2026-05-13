package com.deuktemsiru.common

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공통 API 응답 형식")
data class ApiResponse<T>(
    @field:Schema(description = "HTTP 상태 코드와 동일한 애플리케이션 응답 코드", example = "200")
    val code: Int,
    @field:Schema(description = "응답 메시지", example = "성공")
    val message: String,
    @field:Schema(description = "응답 데이터. 오류 응답에서는 null입니다.", nullable = true)
    val data: T? = null,
) {
    companion object {
        fun <T> success(data: T, message: String = "성공", code: Int = 200) =
            ApiResponse(code = code, message = message, data = data)

        fun <T> created(data: T, message: String = "생성 성공") =
            ApiResponse(code = 201, message = message, data = data)

        fun error(message: String, code: Int = 400) =
            ApiResponse<Nothing>(code = code, message = message, data = null)
    }
}
