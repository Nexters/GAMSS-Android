package com.gamss.android.feature.chat

import androidx.compose.runtime.Immutable

/**
 * 채팅 목록 화면의 단계.
 *
 * 로딩 여부와 선택 상태를 따로 두면 "로딩 중인데 삭제 중", "선택 모드가 아닌데 선택된 방이 있음" 같은
 * 불가능한 조합이 타입상 표현되고, 화면과 ViewModel 이 매번 조합을 다시 계산해야 한다.
 * 한 축으로 모아 합법한 상태만 남긴다.
 */
@Immutable
sealed interface ChattingListPhase {
    data object Loading : ChattingListPhase

    data object Browsing : ChattingListPhase

    /**
     * 선택 모드. [selectedIds] 는 비어 있을 수 있다. 헤더의 삭제 액션으로 진입하면 아무것도 고르지 않은
     * 상태로 시작하므로, 마지막 선택을 해제해도 자동으로 나가지 않는다. 진입 경로와 어긋나기 때문이다.
     */
    data class Selecting(val selectedIds: Set<Long>) : ChattingListPhase

    /** 확인 다이얼로그가 떠 있는 단계. 되돌릴 수 없는 삭제라 실행 전에 한 번 끊는다. */
    data class Confirming(val targetIds: Set<Long>) : ChattingListPhase {
        init {
            require(targetIds.isNotEmpty()) { "Confirming requires at least one target" }
        }
    }

    /** 삭제 요청이 도는 중. 선택 변경, 카드 탭, 뒤로가기 취소를 모두 막는다. */
    data class Deleting(val targetIds: Set<Long>) : ChattingListPhase {
        init {
            // 빈 삭제 요청은 서버를 부르지 않고도 성공이라 단계로 남을 이유가 없다.
            require(targetIds.isNotEmpty()) { "Deleting requires at least one target" }
        }
    }
}
