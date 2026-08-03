package com.gamss.android.data.summary

import android.content.Context
import com.gamss.android.domain.summary.UtteranceTokenCounter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 청크 경계를 재는 전용 토크나이저. 요약용 인스턴스는 truncation 이 켜져 있어 512 를 넘는 입력의
 * 실제 토큰 수를 셀 수 없으므로 절단 없는 인스턴스를 따로 둔다.
 */
@Singleton
internal class KobartTokenCounter @Inject constructor(
    @ApplicationContext private val context: Context,
) : UtteranceTokenCounter {

    private val mutex = Mutex()
    private var tokenizer: KobartTokenizer? = null

    override suspend fun count(text: String): Int = withContext(Dispatchers.Default) {
        mutex.withLock {
            val loaded = tokenizer ?: KobartTokenizer.loadWithoutTruncation(
                context = context,
                tokenizerAsset = KobartSummarySpec.TOKENIZER_ASSET,
            ).also { tokenizer = it }
            loaded.encode(text).ids.size
        }
    }
}
