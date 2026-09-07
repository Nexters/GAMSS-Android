package com.gamss.android.feature.archive

sealed interface ArchiveDetailSideEffect {
    data object ConversationLoadFailed : ArchiveDetailSideEffect

    /**
     * 실제 삭제는 파쇄 화면이 맡는다. 여기서는 그 화면으로 넘기기만 한다.
     *
     * @param cardId 파쇄할 카드. null 이면 이 감정 칸을 통째로 비운다.
     */
    data class OpenCardDelete(val cardId: Long?) : ArchiveDetailSideEffect
}
