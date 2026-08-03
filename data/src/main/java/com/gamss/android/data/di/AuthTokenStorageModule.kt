package com.gamss.android.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.gamss.android.data.local.auth.AuthTokenAead
import com.gamss.android.data.local.auth.AuthTokenDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.integration.android.AndroidKeystore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AuthTokenStorageModule {

    @Provides
    @Singleton
    @AuthTokenDataStore
    fun provideAuthTokenDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile(AUTH_TOKEN_DATA_STORE_NAME) },
    )

    @Provides
    @Singleton
    @AuthTokenAead
    fun provideAuthTokenAead(): Aead {
        if (!AndroidKeystore.hasKey(AUTH_TOKEN_KEY_ALIAS)) {
            AndroidKeystore.generateNewAes256GcmKey(AUTH_TOKEN_KEY_ALIAS)
        }
        return AndroidKeystore.getAead(AUTH_TOKEN_KEY_ALIAS)
    }

    private const val AUTH_TOKEN_DATA_STORE_NAME = "auth_tokens"
    private const val AUTH_TOKEN_KEY_ALIAS = "gamss_auth_token_key"
}
