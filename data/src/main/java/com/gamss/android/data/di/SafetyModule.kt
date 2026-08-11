package com.gamss.android.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.gamss.android.data.safety.RiskLexiconDataStore
import com.gamss.android.data.safety.RiskLexiconRepositoryImpl
import com.gamss.android.domain.safety.RiskLexiconRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SafetyModule {

    @Binds
    abstract fun bindRiskLexiconRepository(
        riskLexiconRepositoryImpl: RiskLexiconRepositoryImpl,
    ): RiskLexiconRepository

    companion object {
        @Provides
        @Singleton
        @RiskLexiconDataStore
        fun provideRiskLexiconDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(RISK_LEXICON_DATA_STORE_NAME) },
        )

        private const val RISK_LEXICON_DATA_STORE_NAME = "risk_lexicon"
    }
}
