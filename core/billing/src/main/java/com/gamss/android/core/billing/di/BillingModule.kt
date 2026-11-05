package com.gamss.android.core.billing.di

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PendingPurchasesParams
import com.gamss.android.core.billing.BillingRepository
import com.gamss.android.core.billing.ForwardingPurchasesUpdatedListener
import com.gamss.android.core.billing.PlayBillingRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    abstract fun bindBillingRepository(playBillingRepository: PlayBillingRepository): BillingRepository

    companion object {
        @Provides
        @Singleton
        fun provideForwardingPurchasesUpdatedListener(): ForwardingPurchasesUpdatedListener =
            ForwardingPurchasesUpdatedListener()

        // BillingClient.Builder.setListener()는 빌드 시점에 리스너가 필요하지만 실제 처리는
        // PlayBillingRepository(BillingClient를 주입받는 쪽)가 한다 — 생성 순서가 꼬이는 걸
        // ForwardingPurchasesUpdatedListener로 끊는다. 자세한 이유는 그 클래스의 설명 참고.
        @Provides
        @Singleton
        fun provideBillingClient(
            @ApplicationContext context: Context,
            forwardingListener: ForwardingPurchasesUpdatedListener,
        ): BillingClient = BillingClient.newBuilder(context)
            .setListener(forwardingListener)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()

        @Provides
        @Singleton
        @BillingCoroutineScope
        fun provideBillingCoroutineScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
