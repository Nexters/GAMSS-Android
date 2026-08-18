package com.gamss.android.feature.archive

sealed interface ArchiveDetailSideEffect {
    data class OpenChatRoom(val conversationId: Long) : ArchiveDetailSideEffect
    data object CardLoadFailed : ArchiveDetailSideEffect
    data object CardDiscardFailed : ArchiveDetailSideEffect
}
