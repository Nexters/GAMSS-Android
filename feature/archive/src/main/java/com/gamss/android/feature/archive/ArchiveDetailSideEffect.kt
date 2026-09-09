package com.gamss.android.feature.archive

import com.gamss.android.core.ui.share.StoryShareResult

sealed interface ArchiveDetailSideEffect {
    data object ConversationLoadFailed : ArchiveDetailSideEffect

    /**
     * 실제 삭제는 파쇄 화면이 맡는다. 여기서는 그 화면으로 넘기기만 한다.
     *
     * @param cardId 파쇄할 카드. null 이면 이 감정 칸을 통째로 비운다.
     */
    data class OpenCardDelete(val cardId: Long?) : ArchiveDetailSideEffect

    /** 카드를 인스타그램 스토리로 공유하지 못했다. [result] 로 실패 이유를 가른다. */
    data class CardShareFailed(val result: StoryShareResult) : ArchiveDetailSideEffect

    /** 카카오톡이 없어 카드를 공유하지 못했다. */
    data object KakaoTalkShareFailed : ArchiveDetailSideEffect
}
