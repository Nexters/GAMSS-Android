package com.gamss.android.core.billing.di

import com.gamss.android.core.billing.BillingRepository
import com.gamss.android.core.billing.PlayBillingDataSource
import com.gamss.android.core.billing.PlayBillingDataSourceImpl
import com.gamss.android.core.billing.PlayBillingRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/** [PlayBillingRepository]가 연결·구매 콜백 처리를 앱 생명주기 동안 유지하는 데 쓰는 스코프. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class BillingCoroutineScope

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BillingModule {

    @Binds
    abstract fun bindBillingDataSource(playBillingDataSourceImpl: PlayBillingDataSourceImpl): PlayBillingDataSource

    @Binds
    abstract fun bindBillingRepository(playBillingRepository: PlayBillingRepository): BillingRepository

    companion object {
        @Provides
        @Singleton
        @BillingCoroutineScope
        fun provideBillingCoroutineScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
