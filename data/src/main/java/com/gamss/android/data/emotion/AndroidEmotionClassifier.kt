package com.gamss.android.data.emotion

import android.content.Context
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
}
