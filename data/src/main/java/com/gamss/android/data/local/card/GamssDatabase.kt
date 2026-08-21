package com.gamss.android.data.local.card

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.gamss.android.data.local.card.model.CardEntity

/**
 * 엔티티를 고치면 version 을 올려야 한다. identityHash 만 바뀌고 version 이 그대로면 파괴적
 * 마이그레이션도 타지 않아, 이전 스키마가 깔린 기기는 DB 첫 접근에서 예외로 죽는다.
 */
@Database(
    entities = [CardEntity::class],
    version = 2,
    exportSchema = true,
)
internal abstract class GamssDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
}
