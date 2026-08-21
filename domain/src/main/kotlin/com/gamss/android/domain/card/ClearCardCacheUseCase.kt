package com.gamss.android.domain.card

import com.gamss.android.domain.usecase.NoParamUseCase
import javax.inject.Inject

/**
 * 기기에 남은 카드 캐시를 지운다. 서버 요청은 없다.
 *
 * 로그아웃·탈퇴처럼 계정을 벗어나는 시점, 그리고 캐시가 서버 상태와 어긋난 것으로 확인된
 * 시점(예: ArchiveDetailViewModel.selectCard 가 월별 응답의 인덱스를 캐시에서 못 찾을 때)에 쓴다.
 */
class ClearCardCacheUseCase @Inject constructor(
    private val cardRepository: CardRepository,
) : NoParamUseCase<Unit> {

    override suspend fun invoke() = cardRepository.clearCache()
}
