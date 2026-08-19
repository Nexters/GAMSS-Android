package com.gamss.android.domain.card

import com.gamss.android.domain.usecase.NoParamUseCase
import javax.inject.Inject

/**
 * 기기에 남은 카드 캐시를 지운다. 서버 요청은 없다.
 * 로그아웃·탈퇴처럼 계정을 벗어나는 시점에 불러, 같은 기기에서 다음에 로그인하는 계정이
 * 이전 계정의 카드 요약·대사를 캐시로 보게 되는 일을 막는다.
 */
class ClearCardCacheUseCase @Inject constructor(
    private val cardRepository: CardRepository,
) : NoParamUseCase<Unit> {

    override suspend fun invoke() = cardRepository.clearCache()
}
