package com.gamss.android.data.local.card

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.gamss.android.data.local.card.model.CardEntity

@Dao
internal interface CardDao {

    /** indexInDate 순서를 그대로 복원해야 selectCard 가 고르는 순번이 서버 응답과 어긋나지 않는다. */
    @Query("SELECT * FROM cards WHERE date = :date ORDER BY index_in_date ASC")
    suspend fun findByDate(date: String): List<CardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(cards: List<CardEntity>)

    @Query("DELETE FROM cards")
    suspend fun deleteAll()
}
