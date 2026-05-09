package com.deuktemsiru.common

data class ApiResponse<T>(
    val code: Int,
    val message: String,
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
