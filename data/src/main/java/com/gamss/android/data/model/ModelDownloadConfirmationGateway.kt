package com.gamss.android.data.model

import android.app.Activity
import android.content.Context
import com.gamss.android.data.emotion.EmotionModelSpec
import com.gamss.android.data.summary.KobartSummarySpec
import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * emotion/summary 애셋팩이 [AssetPackStatus.WAITING_FOR_WIFI] 나
 * [AssetPackStatus.REQUIRES_USER_CONFIRMATION] 로 멈춰 있을 때, 시스템 확인 다이얼로그를 띄워
 * 사용자가 진행 여부를 결정하게 한다. 두 상태 모두 Play Core API 가 Activity 를 요구해서, Activity를
 * 가진 app 루트에서만 호출할 수 있다 — [com.gamss.android.domain.model.ObserveOnDeviceModelDownloadStatusUseCase]
 * 로 "확인이 필요하다"는 신호를 받은 뒤 이걸 부르는 흐름을 가정한다.
 *
 * 한 번 확인받았다고 이후 pending 이 되는 모든 다운로드가 영구히 허용되는 게 아니다 — 호출 시점에
 * pending 인 것만 풀린다. emotion(fast-follow)·summary(on-demand)는 트리거 시점이 서로 달라(전자는
 * 설치 직후 OS 자동 시작, 후자는 앱이 명시적으로 요청해야 시작) 다른 시점에 각각 이 상태가 될 수
 * 있으므로, 호출부는 이 함수가 여러 번 불릴 수 있다고 가정해야 한다.
 */
@Singleton
class ModelDownloadConfirmationGateway @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val manager: AssetPackManager = AssetPackManagerFactory.getInstance(context.applicationContext)

    /** @return 호출 시점에 확인이 필요한 팩이 하나라도 있었는데 사용자가 전부 승인했으면 true. */
    suspend fun confirmPendingDownloads(activity: Activity): Boolean {
        val states = manager.getPackStates(PACK_NAMES).await().packStates().values

        val needsCellular = states.any { it.status() == AssetPackStatus.WAITING_FOR_WIFI }
        val needsSizeConfirmation = states.any { it.status() == AssetPackStatus.REQUIRES_USER_CONFIRMATION }

        var confirmed = true
        if (needsCellular) {
            confirmed = confirmed && manager.showCellularDataConfirmation(activity).await() == Activity.RESULT_OK
        }
        if (needsSizeConfirmation) {
            confirmed = confirmed && manager.showConfirmationDialog(activity).await() == Activity.RESULT_OK
        }
        return confirmed
    }

    private companion object {
        val PACK_NAMES = listOf(EmotionModelSpec.PACK_NAME, KobartSummarySpec.PACK_NAME)
    }
}
