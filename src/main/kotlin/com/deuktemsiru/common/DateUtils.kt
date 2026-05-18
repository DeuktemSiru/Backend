package com.deuktemsiru.common

import java.time.LocalDate
import java.time.LocalTime

fun String.toLocalDateOrThrow(fieldName: String = "날짜"): LocalDate =
    runCatching { LocalDate.parse(this) }
        .getOrElse { throw IllegalArgumentException("$fieldName 형식은 yyyy-MM-dd 이어야 합니다.") }

fun String.toLocalTimeOrThrow(fieldName: String): LocalTime =
    runCatching { LocalTime.parse(this) }
        .getOrElse { throw IllegalArgumentException("$fieldName 형식은 HH:mm 이어야 합니다.") }
