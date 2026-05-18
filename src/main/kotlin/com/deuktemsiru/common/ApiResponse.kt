package com.deuktemsiru.common

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

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

        fun <T> createdEntity(data: T, message: String = "생성 성공"): ResponseEntity<ApiResponse<T>> =
            ResponseEntity.status(HttpStatus.CREATED).body(created(data, message))

        fun error(message: String, code: Int = 400) =
            ApiResponse<Nothing>(code = code, message = message, data = null)
    }
}

fun <T> ok(data: T, message: String = "성공"): ApiResponse<T> =
    ApiResponse.success(data, message)

fun <T> created(data: T, message: String = "생성 성공"): ResponseEntity<ApiResponse<T>> =
    ApiResponse.createdEntity(data, message)
