package com.gamss.android.domain.safety

import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

class DetectRiskInTextUseCase @Inject constructor(
    private val repository: RiskLexiconRepository,
    private val matcher: RiskTermMatcher,
) : UseCase<String, RiskDetection> {

    override suspend fun invoke(params: String): RiskDetection =
        matcher.match(params, repository.getLexicon())
}
