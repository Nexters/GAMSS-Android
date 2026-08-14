package com.gamss.android.data.summary

import com.gamss.android.data.model.ModelAssetSource
import com.gamss.android.domain.summary.UtteranceTokenCounter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class KobartTokenCounter @Inject constructor(
    private val modelAssetSource: ModelAssetSource,
) : UtteranceTokenCounter {

    private val mutex = Mutex()
    private var tokenizer: KobartTokenizer? = null

    override suspend fun count(text: String): Int = withContext(Dispatchers.Default) {
        loadTokenizer().encode(text).ids.size
    }

    private suspend fun loadTokenizer(): KobartTokenizer = mutex.withLock {
        tokenizer ?: KobartTokenizer.loadWithoutTruncation(
            modelAssetSource = modelAssetSource,
            packName = KobartSummarySpec.PACK_NAME,
            tokenizerAsset = KobartSummarySpec.TOKENIZER_ASSET,
        ).also { tokenizer = it }
    }
}
