package com.gamss.android.feature.chat

import androidx.compose.runtime.Immutable
import com.gamss.android.domain.card.Card

/**
 * 대화 종료 흐름의 단계.
 *
 * 불리언 여러 개로 두면 "종료 전인데 카드가 있음" 같은 불가능한 조합이 타입상 표현되고,
 * 화면과 ViewModel 이 매번 조합을 다시 계산해야 한다. 한 축으로 모아 합법한 상태만 남긴다.
 */
@Immutable
sealed interface EndFlow {

    /** 종료 API 가 성공한 뒤. 되돌릴 수 없고 메시지를 더 보낼 수 없다. */
    sealed interface Ended : EndFlow

    data object NotStarted : EndFlow

    data object Confirming : EndFlow

    data object Ending : EndFlow

    data object CreatingCard : Ended

    data class CardReady(val card: Card) : Ended

    /** 재시도하면 결과가 달라질 수 있는 실패. */
    data object CardFailedRetryable : Ended

    /** 재시도해도 같은 결과인 실패. 재시도 경로를 열어두면 영구히 같은 오류다. */
    data object CardFailedFinal : Ended
}

/** 서버 왕복이 진행 중이라 사용자 입력을 받지 않는 단계. */
val EndFlow.isBusy: Boolean
    get() = this == EndFlow.Ending || this == EndFlow.CreatingCard

/** 종료 버튼을 누를 수 있는 단계. */
val EndFlow.acceptsEndRequest: Boolean
    get() = this == EndFlow.NotStarted || this == EndFlow.CardFailedRetryable
