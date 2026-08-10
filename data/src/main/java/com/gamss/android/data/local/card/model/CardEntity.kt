package com.gamss.android.data.local.card.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.gamss.android.domain.card.Card

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

internal fun CardEntity.toDomain(): Card =
    Card(
        id = id,
        conversationId = conversationId,
        emotion = emotion,
        emotionLabel = emotionLabel,
        summary = summary,
        message = message,
        date = date,
    )
