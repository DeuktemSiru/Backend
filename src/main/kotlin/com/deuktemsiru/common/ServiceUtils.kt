package com.deuktemsiru.common

import org.springframework.data.domain.PageRequest
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

fun <T> Optional<T>.orNotFound(message: String): T =
    orElseThrow { NoSuchElementException(message) }

fun safePage(page: Int, size: Int): PageRequest =
    PageRequest.of(page.coerceAtLeast(0), size.coerceAtLeast(1).coerceAtMost(100))

fun Clock.today(): LocalDate = LocalDate.now(this)

fun Clock.nowDateTime(): LocalDateTime = LocalDateTime.now(this)
