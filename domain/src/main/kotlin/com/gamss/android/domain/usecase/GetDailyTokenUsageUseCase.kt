package com.gamss.android.domain.usecase

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.model.DailyTokenUsage
import com.gamss.android.domain.repository.DailyTokenUsageRepository
import javax.inject.Inject

class GetDailyTokenUsageUseCase @Inject constructor(
    private val dailyTokenUsageRepository: DailyTokenUsageRepository,
) : NoParamUseCase<AppResult<DailyTokenUsage>> {

    override suspend fun invoke(): AppResult<DailyTokenUsage> =
        dailyTokenUsageRepository.getDailyTokenUsage()
}
