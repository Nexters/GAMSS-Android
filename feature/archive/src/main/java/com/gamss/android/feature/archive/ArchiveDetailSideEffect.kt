package com.gamss.android.feature.archive

sealed interface ArchiveDetailSideEffect {
    data class OpenChatRoom(val conversationId: Long) : ArchiveDetailSideEffect
    data object CardLoadFailed : ArchiveDetailSideEffect

    /**
     * 실제 삭제는 파쇄 화면이 맡는다. 보관함은 무엇을 지울지만 정해서 넘긴다.
     *
     * [cardId] 가 있으면 그 카드 한 장, null 이면 가진 카드 전부다.
     */
    data class OpenCardShred(val cardId: Long?) : ArchiveDetailSideEffect
}
