package com.gamss.android.feature.archive

sealed interface ArchiveDetailSideEffect {
    data object CardLoadFailed : ArchiveDetailSideEffect
    data object ConversationLoadFailed : ArchiveDetailSideEffect

    /**
     * 실제 삭제는 파쇄 화면이 맡는다. 여기서는 그 화면으로 넘기기만 한다.
     *
     * @param cardId 파쇄할 카드. null 이면 이 칸이 아니라 보관함 전체를 비운다.
     */
    data class OpenCardDelete(val cardId: Long?) : ArchiveDetailSideEffect
}
