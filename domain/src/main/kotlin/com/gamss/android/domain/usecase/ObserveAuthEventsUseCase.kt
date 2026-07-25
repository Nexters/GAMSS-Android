package com.gamss.android.domain.usecase

import com.gamss.android.domain.model.AuthEvent
import com.gamss.android.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAuthEventsUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {

    operator fun invoke(): Flow<AuthEvent> = authRepository.authEvents
}
