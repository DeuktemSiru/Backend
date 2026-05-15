package com.deuktemsiru.common

class UnauthorizedException(message: String = "인증 실패") : RuntimeException(message)
