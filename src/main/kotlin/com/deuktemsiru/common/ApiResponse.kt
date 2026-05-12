package com.deuktemsiru.common

import org.springframework.http.HttpStatus

data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?,
) {
    companion object {
        fun <T> success(data: T, message: String = "성공") =
            ApiResponse(HttpStatus.OK.value(), message, data)

        fun <T> created(data: T, message: String = "생성 성공") =
            ApiResponse(HttpStatus.CREATED.value(), message, data)

        fun error(code: Int, message: String) =
            ApiResponse<Nothing>(code, message, null)
    }
}
