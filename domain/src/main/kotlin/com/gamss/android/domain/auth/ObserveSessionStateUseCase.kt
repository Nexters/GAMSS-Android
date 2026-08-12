package com.gamss.android.domain.auth

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSessionStateUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Flow<SessionState> = authRepository.sessionState
}
