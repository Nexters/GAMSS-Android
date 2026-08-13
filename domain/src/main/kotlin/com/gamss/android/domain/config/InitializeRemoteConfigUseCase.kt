package com.gamss.android.domain.config

import com.gamss.android.domain.usecase.NoParamUseCase
import javax.inject.Inject

class InitializeRemoteConfigUseCase @Inject constructor(
    private val repository: RemoteConfigRepository,
) : NoParamUseCase<Unit> {

    override suspend fun invoke() = repository.initialize()
}
