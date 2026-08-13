package com.gamss.android.domain.emotion

import com.gamss.android.domain.model.ModelDownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** 텍스트 → 감정 분류 결과. 온디바이스 구현이 이 포트를 만족한다. */
interface EmotionClassifier {
    suspend fun classify(text: String): ClassificationResult

    /**
     * 모델(애셋팩) 다운로드를 미리 걸어둔다. 기본은 아무 것도 안 함 — 온디바이스가 아닌 구현이나
     * 테스트 페이크는 다운로드할 게 없다. 결과를 기다리는 쪽이 없어도 안전하게 호출할 수 있어야 한다.
     */
    suspend fun prefetch() {}

    /**
     * 모델 다운로드 상태. 기본은 항상 [ModelDownloadStatus.COMPLETED] — 온디바이스가 아닌 구현이나
     * 테스트 페이크는 다운로드가 필요 없으니 UI 가 확인 배너를 띄울 일도 없다.
     */
    val downloadStatus: Flow<ModelDownloadStatus> get() = flowOf(ModelDownloadStatus.COMPLETED)
}
