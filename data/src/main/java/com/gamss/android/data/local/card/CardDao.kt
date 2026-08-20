package com.gamss.android.data.local.card

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.gamss.android.data.local.card.model.CardEntity

@Dao
internal interface CardDao {

    @Query("SELECT * FROM cards WHERE date = :date ORDER BY id ASC")
    suspend fun findByDate(date: String): List<CardEntity>

    /**
     * 날짜를 `yyyy-MM-dd` 문자열로 담으므로 `yyyy-MM` 접두사로 그 달을 고른다.
     *
     * 서버 감정 키(`emotion`)를 그대로 비교한다. 오래된 순으로 돌려주는 것은 보관함 더미가 목록
     * 순서대로 쌓기 때문이다.
     */
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
