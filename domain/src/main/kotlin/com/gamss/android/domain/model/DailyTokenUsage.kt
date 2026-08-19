package com.gamss.android.domain.model

data class DailyTokenUsage(
    val usedTokens: Long,
    val dailyLimit: Long?,
    val exceeded: Boolean,
    val usagePercent: Int? = null,
)
