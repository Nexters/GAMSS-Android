package com.gamss.android.data.summary

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import android.content.Context
import java.io.Closeable

/**
 * kobart tokenizer.json 을 로드해 요약 인코더 입력을 만들고, 디코더 출력 토큰을 문자열로 되돌린다.
 * 감정용 WordPieceTokenizer 와 달리 고정 길이 패딩 없이 실제 길이를 그대로 쓰고(seq2seq 인코더는
 * 가변 길이 입력을 받음), decode 를 제공한다.
 */
internal class KobartTokenizer private constructor(
    private val tokenizer: HuggingFaceTokenizer,
) : Closeable {

    class Encoded(val ids: LongArray, val attentionMask: LongArray)

    fun encode(text: String): Encoded {
        val enc = tokenizer.encode(text)
        return Encoded(enc.ids, enc.attentionMask)
    }

    /** decode 의 true = skipSpecialTokens: </s>·pad 등을 건너뛴다. */
    fun decode(ids: LongArray): String = tokenizer.decode(ids, true).trim()

    override fun close() = tokenizer.close()

    companion object {
        private const val OPT_SPECIAL_TOKENS = "addSpecialTokens"
        private const val OPT_TRUNCATION = "truncation"
        private const val OPT_MAX_LENGTH = "maxLength"

        fun load(context: Context, tokenizerAsset: String, maxInput: Int): KobartTokenizer {
            val options = mapOf(
                OPT_SPECIAL_TOKENS to "true",
                OPT_TRUNCATION to "true",
                OPT_MAX_LENGTH to maxInput.toString(),
            )
            return newInstance(context, tokenizerAsset, options)
        }

        /** 토큰 수를 재려면 절단이 없어야 한다. 절단하면 한계 이상은 전부 같은 값으로 보인다. */
        fun loadWithoutTruncation(context: Context, tokenizerAsset: String): KobartTokenizer =
            newInstance(
                context = context,
                tokenizerAsset = tokenizerAsset,
                options = mapOf(
                    OPT_SPECIAL_TOKENS to "true",
                    OPT_TRUNCATION to "false",
                ),
            )

        private fun newInstance(
            context: Context,
            tokenizerAsset: String,
            options: Map<String, String>,
        ): KobartTokenizer {
            val tokenizer = context.assets.open(tokenizerAsset).use { stream ->
                HuggingFaceTokenizer.newInstance(stream, options)
            }
            return KobartTokenizer(tokenizer)
        }
    }
}
