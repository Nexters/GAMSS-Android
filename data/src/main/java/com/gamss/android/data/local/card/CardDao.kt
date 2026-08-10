package com.gamss.android.data.local.card

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.gamss.android.data.local.card.model.CardEntity

@Dao
internal interface CardDao {

    @Query("SELECT * FROM cards WHERE id = :cardId LIMIT 1")
    suspend fun findById(cardId: Long): CardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(card: CardEntity)
}
