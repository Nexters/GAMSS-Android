package com.gamss.android.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.gamss.android.data.push.AndroidNotificationPermissionChecker
import com.gamss.android.data.push.DataStoreNotificationPermissionPromptHistory
import com.gamss.android.data.push.FirebaseMessagingTokenProvider
import com.gamss.android.data.push.NotificationPermissionPromptDataStore
import com.gamss.android.domain.push.NotificationPermissionChecker
import com.gamss.android.domain.push.NotificationPermissionPromptHistory
import com.gamss.android.domain.push.PushTokenProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PushModule {

    @Binds
    abstract fun bindPushTokenProvider(
        firebaseMessagingTokenProvider: FirebaseMessagingTokenProvider,
    ): PushTokenProvider

    @Binds
    abstract fun bindNotificationPermissionChecker(
        androidNotificationPermissionChecker: AndroidNotificationPermissionChecker,
    ): NotificationPermissionChecker

    @Binds
    abstract fun bindNotificationPermissionPromptHistory(
        dataStoreNotificationPermissionPromptHistory: DataStoreNotificationPermissionPromptHistory,
    ): NotificationPermissionPromptHistory

    companion object {
        @Provides
        @Singleton
        @NotificationPermissionPromptDataStore
        fun provideNotificationPermissionPromptDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(NOTIFICATION_PERMISSION_DATA_STORE_NAME) },
        )

        private const val NOTIFICATION_PERMISSION_DATA_STORE_NAME = "notification_permission"
    }
}
