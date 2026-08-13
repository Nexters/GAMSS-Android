package com.gamss.android.core.common.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val KoreanTimeZone: ZoneId = ZoneId.of("Asia/Seoul")
private val KoreanTimeFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("a h:mm", Locale.KOREAN)
    .withZone(KoreanTimeZone)

/**
 * UTC ISO-8601 날짜 문자열을 한국 시간의 `오전/오후 h:mm` 형식으로 변환한다.
 *
 * [isoDateTime]이 ISO-8601 형식이 아니면 `null`을 반환한다.
 */
fun formatKoreanTime(isoDateTime: String): String? =
    try {
        KoreanTimeFormatter.format(Instant.parse(isoDateTime))
    } catch (_: DateTimeParseException) {
        null
    }
