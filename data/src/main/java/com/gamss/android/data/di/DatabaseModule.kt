package com.gamss.android.data.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.gamss.android.data.local.card.GamssDatabase
import com.gamss.android.data.local.card.CardDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun provideGamssDatabase(
        @ApplicationContext context: Context,
    ): GamssDatabase =
        Room.databaseBuilder(
            context,
            GamssDatabase::class.java,
            DATABASE_NAME,
        )
            .setDriver(BundledSQLiteDriver())
            .build()

    @Provides
    fun provideCardDao(database: GamssDatabase): CardDao =
        database.cardDao()

    private const val DATABASE_NAME = "gamss.db"
}
