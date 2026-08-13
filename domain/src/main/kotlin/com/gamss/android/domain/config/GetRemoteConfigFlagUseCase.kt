package com.gamss.android.domain.config

import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

class GetRemoteConfigFlagUseCase @Inject constructor(
    private val repository: RemoteConfigRepository,
) : UseCase<RemoteConfigKey, Boolean> {

    override suspend fun invoke(params: RemoteConfigKey): Boolean = repository.getBoolean(params)
}
