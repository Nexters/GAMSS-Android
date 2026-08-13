package com.gamss.android.feature.chat

import com.gamss.android.core.common.util.formatKoreanTime
import com.gamss.android.domain.conversation.Conversation
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 도메인 목록을 화면이 그대로 그릴 수 있는 묶음으로 바꾼다.
 *
 * 날짜와 시각을 여기서 문자열로 만드는 이유는 "오전/오후" 가 로케일에 따라 달라지기 때문이다.
 * Composable 에서 기기 로케일로 포맷하면 영어 기기에서 "AM 4:20" 이 되어 디자인이 깨진다.
 *
 * 서버가 최신순으로 주므로 다시 정렬하지 않는다. 여기서 정렬하면 정렬 정책이 두 곳에 생긴다.
 * 시각을 모르는 방은 임의 날짜에 끼워 넣지 않고 헤더 없는 마지막 묶음으로 모은다.
 */
internal fun List<Conversation>.toConversationGroups(): List<ConversationGroup> {
    // 화면이 id 를 LazyColumn 키로 쓴다. 응답에 같은 id 가 두 번 오면 키 중복으로 죽으므로 여기서 막는다.
    val (dated, undated) = distinctBy { it.id }.partition { it.createdAt != null }

    val datedGroups = dated
        .groupBy { requireNotNull(it.createdAt).toLocalDate() }
        .map { (date, conversations) ->
            ConversationGroup(
                dateLabel = date.format(DateHeaderFormatter),
                rows = conversations.map(Conversation::toConversationRow),
            )
        }

    if (undated.isEmpty()) return datedGroups

    return datedGroups + ConversationGroup(
        dateLabel = null,
        rows = undated.map(Conversation::toConversationRow),
    )
}

private fun Conversation.toConversationRow(): ConversationRow = ConversationRow(
    id = id,
    title = title,
    timeLabel = createdAt?.let(::formatKoreanTime),
)

// 불교력이나 일본력 로케일에서 연도가 밀리지 않게 ROOT 로 고정한다.
private val DateHeaderFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yy.MM.dd", Locale.ROOT)
