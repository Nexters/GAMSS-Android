package com.gamss.android.data.emotion

import android.content.Context
import com.gamss.android.data.model.OnDemandModelAssets
import com.gamss.android.domain.emotion.ClassificationResult
import com.gamss.android.domain.emotion.EmotionClassifier
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
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
    @ApplicationContext private val context: Context,
) : EmotionClassifier {

    private val mutex = Mutex()
    private var classifier: LiteRtClassifier? = null

    override suspend fun classify(text: String): ClassificationResult = mutex.withLock {
        withContext(Dispatchers.Default) {
            val ready = classifier ?: LiteRtClassifier.load(context, EmotionModelSpec.SPEC).also { classifier = it }
            ready.classify(text)
        }
    }

    /**
     * [classifier] 캐시/mutex 와 무관하게 애셋팩 다운로드만 미리 걸어둔다. classify() 와 같은
     * mutex 를 타면 아직 안 끝난 prefetch 가 실제 분류 요청을 불필요하게 막게 된다.
     */
    override suspend fun prefetch() {
        OnDemandModelAssets(context).prefetch(EmotionModelSpec.PACK_NAME)
    }
}
