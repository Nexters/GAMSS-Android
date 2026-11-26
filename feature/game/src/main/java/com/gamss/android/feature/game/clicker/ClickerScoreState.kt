package com.gamss.android.feature.game.clicker

import com.gamss.android.domain.game.ClickerScoreRecord

data class ClickerScoreState(
    val topScores: List<ClickerScoreRecord> = emptyList(),
)
