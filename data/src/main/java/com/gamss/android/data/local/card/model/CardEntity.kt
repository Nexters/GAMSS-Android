package com.gamss.android.data.local.card.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.gamss.android.data.remote.emotion.toEmotionCharacter
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.emotion.EmotionCharacter
import java.time.LocalDate

@Entity(
    tableName = "cards",
    indices = [
        Index(
            value = ["conversation_id"],
            unique = true,
        ),
    ],
)
internal data class CardEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "conversation_id")
    val conversationId: Long,
    @ColumnInfo(name = "emotion")
    val emotion: String,
    @ColumnInfo(name = "emotion_label")
    val emotionLabel: String,
    @ColumnInfo(name = "summary")
    val summary: String,
    @ColumnInfo(name = "message")
    val message: String,
    @ColumnInfo(name = "date")
    val date: String,
)

/**
 * 번들 drawable 자체나 빌드마다 달라질 수 있는 resource id 대신 서버 감정 키를 캐시한다.
 * 캐시 복원 시 이 값이 UI가 표시할 감정별 이미지의 안정적인 선택 키가 된다.
 */
internal val CardEntity.imageCharacter: EmotionCharacter
    get() = checkNotNull(emotion.toEmotionCharacter()) { "Unknown card emotion=$emotion" }

internal fun CardEntity.toDomain(): Card =
    Card(
        id = id,
        conversationId = conversationId,
        character = imageCharacter,
        emotionLabel = emotionLabel,
        summary = summary,
        message = message,
        date = LocalDate.parse(date),
    )
