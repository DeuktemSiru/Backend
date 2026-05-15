package com.deuktemsiru.config

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.common.UnauthorizedException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(NoSuchElementException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(e: NoSuchElementException) =
        ApiResponse.error(e.message ?: "리소스를 찾을 수 없습니다.", 404)

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBadRequest(e: IllegalArgumentException) =
        ApiResponse.error(e.message ?: "잘못된 요청입니다.", 400)

    @ExceptionHandler(IllegalStateException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleConflict(e: IllegalStateException) =
        ApiResponse.error(e.message ?: "요청을 처리할 수 없습니다.", 409)

    @ExceptionHandler(UnauthorizedException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleUnauthorized(e: UnauthorizedException) =
        ApiResponse.error(e.message ?: "인증 실패", 401)

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleServerError(e: Exception): ApiResponse<Nothing> {
        log.error("Unhandled exception", e)
        return ApiResponse.error("서버 오류가 발생했습니다.", 500)
    }
}
