package com.gamss.android.data.local

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.gamss.android.data.local.card.CardDao
import com.gamss.android.data.local.card.model.CardEntity

@Database(
    entities = [CardEntity::class],
    version = 1,
    exportSchema = true,
)
internal abstract class GamssDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
}
