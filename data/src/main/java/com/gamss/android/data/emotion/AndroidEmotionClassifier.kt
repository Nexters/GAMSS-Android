package com.gamss.android.data.emotion

import com.gamss.android.data.model.ModelAssetSource
import com.gamss.android.domain.emotion.ClassificationResult
import com.gamss.android.domain.emotion.EmotionClassifier
import com.gamss.android.domain.model.ModelDownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 온디바이스 감정 분류 포트 구현. 첫 호출 시 KoELECTRA 감정 모델을 지연 로드하고,
 * 네이티브 Interpreter 를 공유하므로 Mutex 로 접근을 직렬화한다.
 */
@Singleton
class AndroidEmotionClassifier @Inject constructor(
    private val modelAssetSource: ModelAssetSource,
) : EmotionClassifier {

    private val mutex = Mutex()
    private var classifier: LiteRtClassifier? = null

    override suspend fun classify(text: String): ClassificationResult = mutex.withLock {
        withContext(Dispatchers.Default) {
            val ready = classifier
                ?: LiteRtClassifier.load(modelAssetSource, EmotionModelSpec.SPEC).also { classifier = it }
            ready.classify(text)
        }
    }

    /**
     * [classifier] 캐시/mutex 와 무관하게 애셋팩 다운로드만 미리 걸어둔다. classify() 와 같은
     * mutex 를 타면 아직 안 끝난 prefetch 가 실제 분류 요청을 불필요하게 막게 된다.
     */
    override suspend fun prefetch() {
        modelAssetSource.prefetch(EmotionModelSpec.PACK_NAME)
    }

    /** UI(app 루트)가 셀룰러/크기 확인 배너를 띄울지 판단하는 데 쓴다. */
    override val downloadStatus: Flow<ModelDownloadStatus>
        get() = modelAssetSource.statusFlow(EmotionModelSpec.PACK_NAME)
}
