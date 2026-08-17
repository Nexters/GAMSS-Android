package com.gamss.android.data.remote.card.model.response

import com.gamss.android.data.remote.emotion.toEmotionCharacter
import com.gamss.android.domain.card.CardEntry
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
internal data class CardCalendarResponse(
    val date: String,
    /** 그날 카드들의 대표 감정. 카드 생성순이고 같은 감정이 여러 번 올 수 있다. */
    val emotions: List<String> = emptyList(),
)

/**
 * 하루치 대표 감정 목록을 카드 한 건씩으로 펼친다.
 *
 * 순번은 알 수 없는 감정을 걸러낸 뒤를 기준으로 센다. 날짜별 조회도 같은 기준으로 걸러내므로,
 * 나중에 [CardEntry.indexInDate] 로 그 응답의 카드를 그대로 찾을 수 있다.
 */
internal fun CardCalendarResponse.toDomain(): List<CardEntry> {
    val createdDate = LocalDate.parse(date)
    return emotions.mapNotNull { it.toEmotionCharacter() }
        .mapIndexed { index, character ->
            CardEntry(date = createdDate, indexInDate = index, character = character)
        }
}
