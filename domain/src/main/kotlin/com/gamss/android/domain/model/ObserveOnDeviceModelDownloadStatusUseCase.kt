package com.gamss.android.domain.model

import com.gamss.android.domain.emotion.EmotionClassifier
import com.gamss.android.domain.summary.DiarySummarizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * emotion·summary 온디바이스 모델 중 하나라도 사용자 확인이 필요한 상태면 true.
 *
 * 두 모델은 트리거 방식이 달라(emotion=fast-follow 는 설치 직후 OS 가 자동 시작, summary=on-demand
 * 는 앱이 명시적으로 요청해야 시작) 서로 다른 시점에 확인이 필요해질 수 있다. 그래서 이 결과는
 * 한 번 확인받았다고 끝나는 게 아니라, 둘 중 하나가 다시 확인이 필요한 상태로 바뀔 때마다
 * 다시 true 가 될 수 있다 — 구독하는 쪽(앱 루트)이 이벤트가 아니라 상시 상태로 다뤄야 한다.
 */
class ObserveOnDeviceModelDownloadStatusUseCase @Inject constructor(
    private val classifier: EmotionClassifier,
    private val summarizer: DiarySummarizer,
) {
    operator fun invoke(): Flow<Boolean> =
        combine(classifier.downloadStatus, summarizer.downloadStatus) { emotion, summary ->
            emotion == ModelDownloadStatus.NEEDS_USER_CONFIRMATION ||
                summary == ModelDownloadStatus.NEEDS_USER_CONFIRMATION
        }
}
