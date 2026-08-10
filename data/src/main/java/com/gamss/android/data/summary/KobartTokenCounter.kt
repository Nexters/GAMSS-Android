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

@Singleton
internal class KobartTokenCounter @Inject constructor(
    @ApplicationContext private val context: Context,
) : UtteranceTokenCounter {

    private val mutex = Mutex()
    private var tokenizer: KobartTokenizer? = null

    override suspend fun count(text: String): Int = withContext(Dispatchers.Default) {
        loadTokenizer().encode(text).ids.size
    }

    private suspend fun loadTokenizer(): KobartTokenizer = mutex.withLock {
        tokenizer ?: KobartTokenizer.loadWithoutTruncation(
            context = context,
            tokenizerAsset = KobartSummarySpec.TOKENIZER_ASSET,
        ).also { tokenizer = it }
    }
}
