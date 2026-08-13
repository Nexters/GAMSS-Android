package com.gamss.android.feature.chat

import androidx.compose.runtime.Immutable

/**
 * 목록이 올려보내는 사용자 동작. 화면이 한 번 만들어 하위 컴포저블에 그대로 내려보냅니다.
 *
 * 람다를 낱개로 넘기지 않고 묶는 이유는 재구성 때문입니다. `@Immutable` 한 덩어리로 넘기면 하위
 * 컴포저블이 건너뛸 수 있고, 파라미터 수도 늘지 않습니다.
 */
@Immutable
internal data class ChattingListActions(
    val onCardClick: (Long) -> Unit,
    val onCardLongClick: (Long) -> Unit,
    val onDeleteActionClick: () -> Unit,
)
