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

data class PageSlice<T>(val items: List<T>, val hasNext: Boolean)

fun <T> List<T>.toPageSlice(page: Int, size: Int): PageSlice<T> {
    val safeSize = size.coerceAtLeast(1)
    val fromIndex = page.coerceAtLeast(0) * safeSize
    val items = drop(fromIndex).take(safeSize)
    return PageSlice(items, fromIndex + items.size < this.size)
}

fun Clock.today(): LocalDate = LocalDate.now(this)

fun Clock.nowDateTime(): LocalDateTime = LocalDateTime.now(this)
