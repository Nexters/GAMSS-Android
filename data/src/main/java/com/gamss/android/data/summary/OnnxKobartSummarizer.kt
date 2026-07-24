package com.gamss.android.data.summary

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import java.io.Closeable
import java.nio.LongBuffer

/**
 * kobart(BART) 요약 ONNX 추론 코어. 인코더 1회 실행 후 no-past 디코더를 반복하는
 * cache-free 그리디 디코딩(매 스텝 늘어난 input_ids 전체를 다시 넣음).
 *
 * 이 export 의 merged KV-cache 디코더는 깨진 export 라 쓰지 않고, no-past 디코더 경로를 쓴다
 * (Python 레퍼런스로 코히런트한 한국어 요약 확인). int8 디코더는 HF fp32 no-past 디코더를
 * 동적 int8 양자화한 것이다. 모델을 byte[] 로 로드하므로 로드 시점 메모리 사용이 크다(지연 로드).
 */
internal class OnnxKobartSummarizer private constructor(
    private val env: OrtEnvironment,
    private val encoder: OrtSession,
    private val decoder: OrtSession,
    private val tokenizer: KobartTokenizer,
) : Closeable {

    fun summarize(text: String): String {
        val enc = tokenizer.encode(text)
        val shape = longArrayOf(1, enc.ids.size.toLong())
        val idsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(enc.ids), shape)
        val maskTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(enc.attentionMask), shape)
        return try {
            encoder.run(
                mapOf(IN_INPUT_IDS to idsTensor, IN_ATTENTION_MASK to maskTensor),
                setOf(OUT_LAST_HIDDEN),
            ).use { encoded ->
                val encoderHidden = encoded.get(OUT_LAST_HIDDEN).get() as OnnxTensor
                val tokens = greedyDecode(enc.attentionMask, encoderHidden)
                tokenizer.decode(tokens.toLongArray())
            }
        } finally {
            idsTensor.close()
            maskTensor.close()
        }
    }

    /** decoder_start 토큰부터 EOS(또는 최대 길이)까지 argmax 로 한 토큰씩 생성한다. */
    private fun greedyDecode(attentionMask: LongArray, encoderHidden: OnnxTensor): List<Long> {
        val maskTensor = OnnxTensor.createTensor(
            env,
            LongBuffer.wrap(attentionMask),
            longArrayOf(1, attentionMask.size.toLong()),
        )
        val generated = ArrayList<Long>()
        var sequence = longArrayOf(KobartSummarySpec.DECODER_START_TOKEN)
        try {
            repeat(KobartSummarySpec.MAX_OUTPUT_TOKENS) {
                val idsTensor =
                    OnnxTensor.createTensor(env, LongBuffer.wrap(sequence), longArrayOf(1, sequence.size.toLong()))
                val banned = bannedNgramTokens(sequence)
                val next = try {
                    decoder.run(
                        mapOf(
                            IN_INPUT_IDS to idsTensor,
                            IN_ENCODER_ATTENTION_MASK to maskTensor,
                            IN_ENCODER_HIDDEN to encoderHidden,
                        ),
                        setOf(OUT_LOGITS),
                    ).use { result ->
                        argmaxLastRow(result.get(OUT_LOGITS).get() as OnnxTensor, sequence.size, banned)
                    }
                } finally {
                    idsTensor.close()
                }
                if (next == KobartSummarySpec.EOS_TOKEN) return generated
                generated.add(next)
                sequence += next
            }
        } finally {
            maskTensor.close()
        }
        return generated
    }

    /** logits[0, seqLen-1, :] 의 argmax 토큰 id. banned 토큰은 제외한다. shape 검증으로 재export 시 오작동 방지. */
    private fun argmaxLastRow(logits: OnnxTensor, seqLen: Int, banned: Set<Long>): Long {
        val shape = logits.info.shape
        require(shape.size == 3 && shape[1].toInt() == seqLen && shape[2].toInt() == KobartSummarySpec.VOCAB_SIZE) {
            "예상치 못한 logits shape: ${shape.contentToString()} (seqLen=$seqLen)"
        }
        val buffer = logits.floatBuffer
        val offset = (seqLen - 1) * KobartSummarySpec.VOCAB_SIZE
        var bestIndex = 0
        var bestValue = Float.NEGATIVE_INFINITY
        for (v in 0 until KobartSummarySpec.VOCAB_SIZE) {
            if (v.toLong() in banned) continue
            val value = buffer.get(offset + v)
            if (value > bestValue) {
                bestValue = value
                bestIndex = v
            }
        }
        return bestIndex.toLong()
    }

    /** no_repeat_ngram: 직전 (n-1)개 토큰 뒤에 와서 이미 나온 n-gram 을 완성하는 토큰들을 금지한다. */
    private fun bannedNgramTokens(sequence: LongArray): Set<Long> {
        val n = KobartSummarySpec.NO_REPEAT_NGRAM
        if (sequence.size < n) return emptySet()
        val prefixStart = sequence.size - (n - 1)
        val banned = HashSet<Long>()
        for (i in 0..sequence.size - n) {
            var match = true
            for (j in 0 until n - 1) {
                if (sequence[i + j] != sequence[prefixStart + j]) {
                    match = false
                    break
                }
            }
            if (match) banned.add(sequence[i + n - 1])
        }
        return banned
    }

    override fun close() {
        encoder.close()
        decoder.close()
        tokenizer.close()
    }

    companion object {
        private const val IN_INPUT_IDS = "input_ids"
        private const val IN_ATTENTION_MASK = "attention_mask"
        private const val IN_ENCODER_ATTENTION_MASK = "encoder_attention_mask"
        private const val IN_ENCODER_HIDDEN = "encoder_hidden_states"
        private const val OUT_LAST_HIDDEN = "last_hidden_state"
        private const val OUT_LOGITS = "logits"

        fun load(context: Context): OnnxKobartSummarizer {
            val env = OrtEnvironment.getEnvironment()
            val encoder = env.createSession(readAsset(context, KobartSummarySpec.ENCODER_ASSET))
            val decoder = env.createSession(readAsset(context, KobartSummarySpec.DECODER_ASSET))
            val tokenizer = KobartTokenizer.load(
                context,
                KobartSummarySpec.TOKENIZER_ASSET,
                KobartSummarySpec.MAX_INPUT_TOKENS,
            )
            return OnnxKobartSummarizer(env, encoder, decoder, tokenizer)
        }

        private fun readAsset(context: Context, name: String): ByteArray =
            context.assets.open(name).use { it.readBytes() }
    }
}
