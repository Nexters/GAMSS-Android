package com.gamss.android.domain.usecase

import com.gamss.android.domain.model.SessionState
import com.gamss.android.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSessionStateUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Flow<SessionState> = authRepository.sessionState
}
