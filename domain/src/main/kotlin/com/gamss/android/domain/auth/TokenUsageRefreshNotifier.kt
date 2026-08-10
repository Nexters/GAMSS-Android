package com.gamss.android.domain.repository

import kotlinx.coroutines.flow.Flow

interface TokenUsageRefreshNotifier {
    val refreshEvents: Flow<Unit>

    fun requestRefresh()
}
