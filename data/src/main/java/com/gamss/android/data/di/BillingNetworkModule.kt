package com.gamss.android.data.di

import com.gamss.android.data.remote.billing.BillingService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object BillingNetworkModule {

    @Provides
    @Singleton
    fun provideBillingService(retrofit: Retrofit): BillingService =
        retrofit.create(BillingService::class.java)
}
