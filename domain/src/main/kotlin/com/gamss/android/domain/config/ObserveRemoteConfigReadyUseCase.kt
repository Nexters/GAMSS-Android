package com.gamss.android.domain.config

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRemoteConfigReadyUseCase @Inject constructor(
    private val repository: RemoteConfigRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.isReady
}
