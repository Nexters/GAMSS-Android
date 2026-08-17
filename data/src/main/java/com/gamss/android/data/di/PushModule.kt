package com.gamss.android.data.di

import com.gamss.android.data.push.AndroidNotificationPermissionChecker
import com.gamss.android.data.push.FirebaseMessagingTokenProvider
import com.gamss.android.domain.push.NotificationPermissionChecker
import com.gamss.android.domain.push.PushTokenProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

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
}
