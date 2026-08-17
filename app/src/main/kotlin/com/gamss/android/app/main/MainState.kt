package com.gamss.android.app.main

import com.gamss.android.domain.auth.SessionState

data class MainState(
    val sessionState: SessionState = SessionState.Loading,
)
