package com.gamss.android.app.payment

import com.gamss.android.app.BuildConfig
import com.gamss.android.domain.payment.LicensedPackage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object PurchaseConfigModule {
    @Provides
    @LicensedPackage
    fun provideIsLicensedPackage(): Boolean = BuildConfig.APPLICATION_ID == "com.gamss.android"
}
