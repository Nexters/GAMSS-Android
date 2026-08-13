package com.gamss.android.app.main

import com.gamss.android.domain.auth.SessionState

data class MainState(
    val sessionState: SessionState = SessionState.Loading,
    /** 원격 설정 `use_chat_end_feature`. 꺼져 있으면 보관함 탭을 아예 만들지 않는다. */
    val useCardFeature: Boolean = false,
)
