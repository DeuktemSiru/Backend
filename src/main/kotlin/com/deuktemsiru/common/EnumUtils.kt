package com.deuktemsiru.common

/** S1: 공통 enum 파싱 헬퍼. 잘못된 값이면 IllegalArgumentException 을 던집니다. */
inline fun <reified T : Enum<T>> String.toEnumOrThrow(fieldName: String = "값"): T =
    runCatching { enumValueOf<T>(this.uppercase()) }
        .getOrElse { throw IllegalArgumentException("지원하지 않는 $fieldName: $this") }
