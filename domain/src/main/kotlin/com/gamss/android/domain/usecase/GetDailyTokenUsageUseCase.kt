package com.gamss.android.domain.usecase

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.model.DailyTokenUsage
import com.gamss.android.domain.user.UserRepository
import javax.inject.Inject

class GetDailyTokenUsageUseCase @Inject constructor(
    private val userRepository: UserRepository,
) : NoParamUseCase<AppResult<DailyTokenUsage>> {

    override suspend fun invoke(): AppResult<DailyTokenUsage> =
        userRepository.getDailyTokenUsage()
}
