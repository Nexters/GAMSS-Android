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

    /**
     * 접기 단계를 카드와 함께 든다. 단계는 카드가 있을 때만 뜻이 있으므로 밖에 두면
     * "카드 없는데 두 번 접힘" 같은 조합이 타입상 표현된다.
     *
     * 연출 값을 컴포지션이 아니라 여기 두는 것은, 프로세스가 죽었다 살아나도 접다 만 자리에서
     * 이어져야 하기 때문이다. rememberSaveable 로는 복원한 카드와 단계가 따로 놀 수 있다.
     */
    data class CardReady(
        val card: Card,
        val foldStage: CardFoldStage = CardFoldStage.Unfolded,
    ) : Ended

    /** 재시도하면 결과가 달라질 수 있는 실패. */
    data object CardFailedRetryable : Ended

    /** 재시도해도 같은 결과인 실패. 재시도 경로를 열어두면 영구히 같은 오류다. */
    data object CardFailedFinal : Ended
}

/**
 * 카드를 접은 정도. 마지막 단계에서 통으로 내리는 건 연속적인 드래그라 단계로 두지 않고
 * 화면 쪽 제스처가 맡는다.
 */
enum class CardFoldStage {
    Unfolded,
    FoldedOnce,
    FoldedTwice,
    ;

    /** 더 접을 수 없으면 null. 호출부가 마지막 단계를 따로 비교하지 않게 한다. */
    val next: CardFoldStage?
        get() = when (this) {
            Unfolded -> FoldedOnce
            FoldedOnce -> FoldedTwice
            FoldedTwice -> null
        }
}

/** 서버 왕복이 진행 중이라 사용자 입력을 받지 않는 단계. */
val EndFlow.isBusy: Boolean
    get() = this == EndFlow.Ending || this == EndFlow.CreatingCard

/** 종료 버튼을 누를 수 있는 단계. */
val EndFlow.acceptsEndRequest: Boolean
    get() = this == EndFlow.NotStarted || this == EndFlow.CardFailedRetryable
