package com.gamss.android.domain.safety

import com.gamss.android.domain.usecase.NoParamUseCase
import javax.inject.Inject

/**
 * 원격 사전 갱신은 앱 시작 시 한 번만 시도한다. 전송할 때마다 네트워크를 타면
 * 감지가 느려지고 읽기 비용이 사용량에 비례해 늘어난다.
 */
class RefreshRiskLexiconUseCase @Inject constructor(
    private val repository: RiskLexiconRepository,
) : NoParamUseCase<Unit> {

    override suspend fun invoke() = repository.refresh()
}
