package com.gamss.android.domain.safety

import com.gamss.android.domain.usecase.NoParamUseCase
import javax.inject.Inject

class RefreshRiskLexiconUseCase @Inject constructor(
    private val repository: RiskLexiconRepository,
) : NoParamUseCase<Unit> {

    override suspend fun invoke() = repository.refresh()
}
