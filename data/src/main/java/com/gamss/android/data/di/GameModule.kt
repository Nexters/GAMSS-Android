package com.gamss.android.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.gamss.android.data.game.ClickerScoreDataStore
import com.gamss.android.data.game.DataStoreClickerScoreHistory
import com.gamss.android.domain.game.ClickerScoreHistory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class GameModule {

    @Binds
    abstract fun bindClickerScoreHistory(
        dataStoreClickerScoreHistory: DataStoreClickerScoreHistory,
    ): ClickerScoreHistory

    companion object {
        @Provides
        @Singleton
        @ClickerScoreDataStore
        fun provideClickerScoreDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(CLICKER_SCORE_DATA_STORE_NAME) },
        )

        private const val CLICKER_SCORE_DATA_STORE_NAME = "clicker_score"
    }
}
