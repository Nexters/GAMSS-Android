package com.gamss.android.domain.safety

import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

/**
 * 원문 → 위험도와 안내할 기관 목록.
 *
 * 검사는 전부 기기 안에서 한다. 사전만 원격에서 받고 원문은 기기 밖으로 내보내지 않는다.
 */
class DetectRiskInTextUseCase @Inject constructor(
    private val repository: RiskLexiconRepository,
    private val matcher: RiskTermMatcher,
) : UseCase<String, RiskDetection> {

    override suspend fun invoke(params: String): RiskDetection =
        matcher.match(params, repository.getLexicon())
}
