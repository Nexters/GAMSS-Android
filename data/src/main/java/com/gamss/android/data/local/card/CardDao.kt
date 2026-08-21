package com.gamss.android.data.local.card

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.gamss.android.data.local.card.model.CardEntity

@Dao
internal interface CardDao {

    /** 날짜를 `yyyy-MM-dd` 문자열로 담으므로 `yyyy-MM` 접두사로 그 달을 고른다. */
    @Query(
        "SELECT * FROM cards WHERE emotion = :emotion AND date LIKE :yearMonthPrefix || '%' " +
            "ORDER BY date ASC, id ASC",
    )
    suspend fun findByEmotionAndMonth(emotion: String, yearMonthPrefix: String): List<CardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(cards: List<CardEntity>)

    @Query("DELETE FROM cards")
    suspend fun deleteAll()
}
