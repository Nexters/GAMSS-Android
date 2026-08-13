package com.gamss.android.data.emotion

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import android.content.Context
import com.gamss.android.data.model.OnDemandModelAssets
import com.gamss.android.data.model.readBytes
import java.io.ByteArrayInputStream
import java.io.Closeable

/**
 * HuggingFace tokenizer.json 을 그대로 로드해 학습 토크나이저와 동일하게 인코딩하는 래퍼.
 * 고정 길이(seqLen)로 자르거나 0 패딩해 LiteRT 입력에 맞춘다.
 */
internal class WordPieceTokenizer private constructor(
    private val tokenizer: HuggingFaceTokenizer,
    private val seqLen: Int,
) : Closeable {

    // LongArray 필드라 data class 의 참조기반 equals/hashCode 는 부적절 → 일반 class.
    class Encoded(
        val ids: LongArray,
        val attentionMask: LongArray,
        val typeIds: LongArray,
    )

    fun encode(text: String): Encoded {
        val enc = tokenizer.encode(text)
        return Encoded(
            ids = toFixedLength(enc.ids),
            attentionMask = toFixedLength(enc.attentionMask),
            typeIds = toFixedLength(enc.typeIds),
        )
    }

    override fun close() = tokenizer.close()

    private fun toFixedLength(src: LongArray): LongArray =
        LongArray(seqLen).also { src.copyInto(it, endIndex = minOf(seqLen, src.size)) }

    companion object {
        private const val OPT_SPECIAL_TOKENS = "addSpecialTokens"
        private const val OPT_TRUNCATION = "truncation"
        private const val OPT_MAX_LENGTH = "maxLength"

        suspend fun load(context: Context, packName: String, tokenizerAsset: String, seqLen: Int): WordPieceTokenizer {
            val options = mapOf(
                OPT_SPECIAL_TOKENS to "true",
                OPT_TRUNCATION to "true",
                OPT_MAX_LENGTH to seqLen.toString(),
            )
            val bytes = OnDemandModelAssets(context).resolve(packName, tokenizerAsset).readBytes()
            val tokenizer = ByteArrayInputStream(bytes).use { stream ->
                HuggingFaceTokenizer.newInstance(stream, options)
            }
            return WordPieceTokenizer(tokenizer, seqLen)
        }
    }
}
