package com.gamss.android.domain.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.model.AuthEvent
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authEvents: Flow<AuthEvent>

    suspend fun login(googleIdToken: String): AppResult<Unit>

    suspend fun reissueTokens(): AppResult<Unit>

    /**
     * 저장된 인증 세션을 복원한다.
     *
     * 성공 결과는 인증 가능한 세션의 존재 여부를 의미한다.
     * `true`이면 인증된 세션이 복원된 상태이고, `false`이면 저장된 토큰이 없는 정상적인 비인증 상태다.
     *
     * 실패(`Failure`)는 재발급 시도 중 발생한 예외(네트워크 오류, 서버의 refreshToken 거부 등)를 뜻하며,
     * 네트워크 오류가 아닌 실패는 내부적으로 세션을 무효화(토큰 삭제 + [AuthEvent.SessionExpired] 발행)한다.
     */
    suspend fun restoreSession(): AppResult<Boolean>

    suspend fun logout(): AppResult<Unit>
}
