package com.gamss.android.data.emotion

import android.content.Context
import com.gamss.android.domain.emotion.ClassificationResult
import org.tensorflow.lite.DataType
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp

/**
 * WordPiece 인코더 분류기의 실추론 코어. 토큰화 → LiteRT 추론 → softmax → [ClassificationResult].
 */
class LiteRtClassifier private constructor(
    private val model: LiteRtModel,
    private val tokenizer: WordPieceTokenizer,
    private val labels: List<String>,
) : Closeable {

    val artifactSizeBytes: Long get() = model.sizeBytes

    fun classify(text: String): ClassificationResult {
        val interpreter = model.interpreter
        val encoded = tokenizer.encode(text)

        val inputs = Array<Any>(interpreter.inputTensorCount) { i ->
            val tensor = interpreter.getInputTensor(i)
            val values = when {
                tensor.name().contains("input_ids") -> encoded.ids
                tensor.name().contains("attention_mask") -> encoded.attentionMask
                tensor.name().contains("token_type") -> encoded.typeIds
                else -> error("매핑되지 않은 입력 텐서: ${tensor.name()}")
            }
            toInputBuffer(values, tensor.dataType(), tensor.numBytes())
        }

        val outputTensor = interpreter.getOutputTensor(0)
        val classCount = outputTensor.shape().last()
        require(classCount == labels.size) {
            "모델 출력 클래스 수($classCount) 와 라벨 수(${labels.size}) 가 다릅니다."
        }
        val output = ByteBuffer.allocateDirect(outputTensor.numBytes()).order(ByteOrder.nativeOrder())
        interpreter.runForMultipleInputsOutputs(inputs, hashMapOf<Int, Any>(0 to output))

        output.rewind()
        val probs = softmax(FloatArray(classCount) { output.float })
        val best = probs.indices.maxByOrNull { probs[it] } ?: 0
        return ClassificationResult(
            topLabel = labels[best],
            confidence = probs[best],
            scores = labels.indices.associate { labels[it] to probs[it] },
        )
    }

    override fun close() {
        model.close()
        tokenizer.close()
    }

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
        fun load(context: Context, spec: ClassifierSpec): LiteRtClassifier = LiteRtClassifier(
            model = LiteRtModel.load(context, spec.modelAsset),
            tokenizer = WordPieceTokenizer.load(context, spec.tokenizerAsset, spec.seqLen),
            labels = spec.labels,
        )
    }
}
