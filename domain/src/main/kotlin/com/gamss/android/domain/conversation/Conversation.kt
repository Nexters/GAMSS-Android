package com.gamss.android.domain.conversation

import java.time.LocalDateTime

/**
 * @param createdAt 방이 만들어진 시각. 서버가 주지 않거나 형식이 어긋나면 null 이고, 그때 화면은
 *   날짜 헤더와 시각 텍스트를 감춘다. 존이 이미 벗겨진 벽시계 값이라, 날짜 그룹핑은 존을 몰라도 된다.
 *   존 변환은 data 레이어가 파싱할 때 한 번만 한다.
 */
data class Conversation(
    val id: Long,
    val title: String?,
    val createdAt: LocalDateTime? = null,
)
