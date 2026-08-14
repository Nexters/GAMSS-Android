package com.gamss.android.data.emotion

import android.content.Context
import com.gamss.android.domain.emotion.ClassificationResult
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp

/**
 * WordPiece 인코더 분류기의 실추론 코어. 토큰화 → LiteRT 추론 → softmax → [ClassificationResult].
 */
internal class LiteRtClassifier private constructor(
    private val model: LiteRtModel,
    private val tokenizer: WordPieceTokenizer,
    private val labels: List<String>,
) : Closeable {

    // 입력 텐서 index → 토큰 역할. 이름 매칭을 load 시점에 1회 해결해 재export 시 즉시 실패하게 한다.
    private val inputRoles: IntArray = resolveInputRoles(model.interpreter)

    fun classify(text: String): ClassificationResult {
        val interpreter = model.interpreter
        val encoded = tokenizer.encode(text)

        val inputs = Array<Any>(interpreter.inputTensorCount) { i ->
            val tensor = interpreter.getInputTensor(i)
            val values = when (inputRoles[i]) {
                ROLE_IDS -> encoded.ids
                ROLE_MASK -> encoded.attentionMask
                else -> encoded.typeIds
            }
            toInputBuffer(values, tensor.dataType(), tensor.numBytes())
        }

        val outputTensor = interpreter.getOutputTensor(0)
        val classCount = outputTensor.shape().last()
        require(classCount == labels.size) {
            "모델 출력 클래스 수($classCount) 와 라벨 수(${labels.size}) 가 다릅니다."
        }
        require(outputTensor.dataType() == DataType.FLOAT32) {
            "출력 텐서 타입이 FLOAT32 가 아닙니다: ${outputTensor.dataType()}"
        }
        val output = ByteBuffer.allocateDirect(outputTensor.numBytes()).order(ByteOrder.nativeOrder())
        interpreter.runForMultipleInputsOutputs(inputs, mapOf<Int, Any>(0 to output))

        output.rewind()
        val probs = softmax(FloatArray(classCount) { output.float })
        val best = probs.indices.maxByOrNull { probs[it] } ?: 0
        return ClassificationResult(
            topLabel = labels[best],
            confidence = probs[best],
            scores = labels.indices.associate { labels[it] to probs[it] },
        )
    }

    // 토크나이저는 순수 Kotlin 이라 닫을 네이티브 핸들이 없다. 여기서 닫을 것은 Interpreter 뿐이다.
    override fun close() = model.close()

    private fun toInputBuffer(values: LongArray, dataType: DataType, numBytes: Int): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(numBytes).order(ByteOrder.nativeOrder())
        if (dataType == DataType.INT64) {
            values.forEach { buffer.putLong(it) }
        } else {
            values.forEach { buffer.putInt(it.toInt()) }
        }
        buffer.rewind()
        return buffer
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val max = logits.max()
        val exps = logits.map { exp((it - max).toDouble()) }
        val sum = exps.sum()
        return FloatArray(logits.size) { (exps[it] / sum).toFloat() }
    }

    companion object {
        private const val ROLE_IDS = 0
        private const val ROLE_MASK = 1
        private const val ROLE_TYPE = 2

        private const val NAME_INPUT_IDS = "input_ids"
        private const val NAME_ATTENTION_MASK = "attention_mask"
        private const val NAME_TOKEN_TYPE = "token_type"

        suspend fun load(context: Context, spec: ClassifierSpec): LiteRtClassifier = LiteRtClassifier(
            model = LiteRtModel.load(context, spec.packName, spec.modelAsset),
            tokenizer = WordPieceTokenizer.load(context, spec.packName, spec.tokenizerAsset, spec.seqLen),
            labels = spec.labels,
        )

        private fun resolveInputRoles(interpreter: Interpreter): IntArray =
            IntArray(interpreter.inputTensorCount) { i ->
                val name = interpreter.getInputTensor(i).name()
                when {
                    name.contains(NAME_INPUT_IDS) -> ROLE_IDS
                    name.contains(NAME_ATTENTION_MASK) -> ROLE_MASK
                    name.contains(NAME_TOKEN_TYPE) -> ROLE_TYPE
                    else -> error("매핑되지 않은 입력 텐서: $name")
                }
            }
    }
}
