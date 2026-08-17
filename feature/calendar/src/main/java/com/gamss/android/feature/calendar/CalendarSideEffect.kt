package com.gamss.android.feature.calendar

sealed interface CalendarSideEffect {
    data class OpenChatRoom(val conversationId: Long) : CalendarSideEffect
    data object CardDiscardFailed : CalendarSideEffect
}
