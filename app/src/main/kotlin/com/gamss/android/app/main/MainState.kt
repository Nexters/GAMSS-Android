package com.gamss.android.app.main

import com.gamss.android.domain.auth.SessionState

data class MainState(
    val sessionState: SessionState = SessionState.Loading,
    /** 카드 기능은 현재 원격 설정과 무관하게 항상 노출한다. */
    val useCardFeature: Boolean = true,
)
