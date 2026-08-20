package com.gamss.android.feature.archive

import com.gamss.android.domain.card.Card
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.emotion.EmotionCharacter
import java.time.YearMonth

data class ArchiveDetailState(
    val emotion: EmotionCharacter? = null,
    val yearMonth: YearMonth,
    val cards: ArchiveCards = ArchiveCards.Loading,
    val isMonthPickerVisible: Boolean = false,
    val isClearDialogVisible: Boolean = false,
    val selectedCard: Card? = null,
    val conversationCard: ConversationCard? = null,
    val isConversationLoading: Boolean = false,
)

/** 종이 더미 자리가 가질 수 있는 상태. 셋이 겹칠 수 없어 플래그 조합 대신 하나로 든다. */
sealed interface ArchiveCards {
    data object Loading : ArchiveCards
    data object LoadFailed : ArchiveCards
    data class Loaded(val cards: List<Card>) : ArchiveCards
}

/**
 * 대화보기로 뒤집은 카드. 닫을 때 종이 더미가 아니라 원래 보던 감정 카드로 되돌리려면 대화를 열어 준
 * [card] 를 그대로 들고 있어야 한다.
 */
data class ConversationCard(
    val card: Card,
    val messages: List<Message>,
)
