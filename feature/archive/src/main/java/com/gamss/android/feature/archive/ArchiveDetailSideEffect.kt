package com.gamss.android.feature.archive

sealed interface ArchiveDetailSideEffect {
    data class OpenChatRoom(val conversationId: Long) : ArchiveDetailSideEffect
    data object CardLoadFailed : ArchiveDetailSideEffect
    data object CardDiscardFailed : ArchiveDetailSideEffect

    /** 실제 삭제는 파쇄 화면이 맡는다. 여기서는 그 화면으로 넘기기만 한다. */
    data object OpenCardDelete : ArchiveDetailSideEffect
}
