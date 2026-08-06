package com.gamss.android.domain.conversation

/** @param title 첫 발화로 정해지는 방 제목. 이 기능 이전에 만들어진 방은 null 이다. */
data class Conversation(
    val id: Long,
    val title: String?,
)
