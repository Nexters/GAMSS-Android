package com.gamss.android.data.emotion

import com.gamss.android.data.model.ModelAssetSource
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.nio.MappedByteBuffer

/**
 * .tflite 를 메모리 매핑(mmap)으로 로드해 스레드 고정 Interpreter 를 소유하는 홀더. 실제 바이트가
 * PAD 애셋팩에서 오는지 로컬 assets 에서 오는지는 [ModelAssetSource] 뒤에 숨는다.
 */
internal class LiteRtModel private constructor(
    val interpreter: Interpreter,
) : Closeable {

    override fun close() = interpreter.close()

    companion object {
        private const val DEFAULT_THREADS = 4

        suspend fun load(modelAssetSource: ModelAssetSource, packName: String, assetPath: String): LiteRtModel {
            val buffer = mapModelFile(modelAssetSource, packName, assetPath)
            val interpreter = Interpreter(buffer, Interpreter.Options().setNumThreads(DEFAULT_THREADS))
            return LiteRtModel(interpreter)
        }

        /** .tflite 파일을 mmap. PAD 구현이면 팩 미다운로드 시 여기서 내려받길 기다린다. */
        private suspend fun mapModelFile(
            modelAssetSource: ModelAssetSource,
            packName: String,
            assetPath: String,
        ): MappedByteBuffer = modelAssetSource.mmap(packName, assetPath)
    }
}
