package com.gamss.android.data.summary

import android.content.Context
import com.gamss.android.domain.summary.DiarySummarizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 온디바이스 원문 요약 포트 구현. 첫 호출 시 kobart ONNX 모델을 지연 로드하고,
 * 네이티브 OrtSession 을 공유하므로 Mutex 로 접근을 직렬화한다.
 */
@Singleton
class AndroidDiarySummarizer @Inject constructor(
    @ApplicationContext private val context: Context,
) : DiarySummarizer {

    private val mutex = Mutex()
    private var summarizer: OnnxKobartSummarizer? = null

    override suspend fun summarize(text: String): String = mutex.withLock {
        withContext(Dispatchers.Default) {
            val ready = summarizer ?: OnnxKobartSummarizer.load(context).also { summarizer = it }
            ready.summarize(text)
        }
    }
}
