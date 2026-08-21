package com.gamss.android.feature.archive

sealed interface ArchiveDetailSideEffect {
    data object CardLoadFailed : ArchiveDetailSideEffect
    data object CardDiscardFailed : ArchiveDetailSideEffect
    data object ConversationLoadFailed : ArchiveDetailSideEffect

    /** 실제 삭제는 파쇄 화면이 맡는다. 여기서는 그 화면으로 넘기기만 한다. */
    data object OpenCardDelete : ArchiveDetailSideEffect
}
