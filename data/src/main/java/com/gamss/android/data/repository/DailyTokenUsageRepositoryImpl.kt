package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.data.remote.token.TokenUsageService
import com.gamss.android.domain.model.DailyTokenUsage
import com.gamss.android.domain.repository.DailyTokenUsageRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DailyTokenUsageRepositoryImpl @Inject constructor(
    private val tokenUsageService: TokenUsageService,
) : DailyTokenUsageRepository {

    override suspend fun getDailyTokenUsage(): AppResult<DailyTokenUsage> = runCatchingApiCall {
        tokenUsageService.getDailyTokenUsage()
            .data
            ?.toDomain()
            ?: error("No available daily token usage data")
    }
}
