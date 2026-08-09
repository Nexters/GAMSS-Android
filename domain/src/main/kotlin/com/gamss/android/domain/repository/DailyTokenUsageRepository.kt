package com.gamss.android.domain.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.model.DailyTokenUsage

interface DailyTokenUsageRepository {
    suspend fun getDailyTokenUsage(): AppResult<DailyTokenUsage>
}
