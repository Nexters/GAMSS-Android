package com.gamss.android.core.common.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val KOREAN_TIME_PATTERN = "a h:mm"

private const val CONVERSATION_DATE_PATTERN = "yy.MM.dd"

// 존을 덮지 않는다. 이미 벽시계로 변환된 값을 그대로 찍는 용도다.
private val KoreanWallClockFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern(KOREAN_TIME_PATTERN, Locale.KOREAN)

// 불교력이나 일본력 로케일에서 연도가 밀리지 않게 ROOT 로 고정한다.
private val ConversationDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern(CONVERSATION_DATE_PATTERN, Locale.ROOT)

/**
 * 이미 벽시계로 바뀐 시각을 `오전/오후 h:mm` 형식으로 변환한다.
 *
 * 존 변환이 끝난 값을 받으므로 여기서는 존을 다시 적용하지 않는다. 로케일만 한국어로 고정해,
 * 기기 로케일이 영어여도 `AM/PM` 으로 바뀌지 않는다.
 */
fun formatKoreanTime(dateTime: LocalDateTime): String = KoreanWallClockFormatter.format(dateTime)

/** 대화방 생성 시각을 `yy.MM.dd` 형식의 날짜로 표시한다. */
fun formatConversationDate(dateTime: LocalDateTime): String = ConversationDateFormatter.format(dateTime)
