package com.gamss.android.core.common.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private const val KOREAN_TIME_PATTERN = "a h:mm"

val KoreanTimeZone: ZoneId = ZoneId.of("Asia/Seoul")
private val KoreanTimeFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern(KOREAN_TIME_PATTERN, Locale.KOREAN)
    .withZone(KoreanTimeZone)

// 존을 덮지 않는다. 이미 벽시계로 변환된 값을 그대로 찍는 용도다.
private val KoreanWallClockFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern(KOREAN_TIME_PATTERN, Locale.KOREAN)

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

/**
 * 이미 벽시계로 바뀐 시각을 `오전/오후 h:mm` 형식으로 변환한다.
 *
 * 존 변환이 끝난 값을 받으므로 여기서는 존을 다시 적용하지 않는다. 로케일만 한국어로 고정해,
 * 기기 로케일이 영어여도 `AM/PM` 으로 바뀌지 않는다.
 */
fun formatKoreanTime(dateTime: LocalDateTime): String = KoreanWallClockFormatter.format(dateTime)
