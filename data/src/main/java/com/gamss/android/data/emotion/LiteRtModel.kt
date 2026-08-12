package com.gamss.android.data.emotion

import android.content.Context
import com.gamss.android.data.model.OnDemandModelAssets
import com.gamss.android.data.model.mmap
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.nio.MappedByteBuffer

/**
 * on-demand 애셋팩에서 내려받은 .tflite 를 메모리 매핑(mmap)으로 로드해 스레드 고정 Interpreter 를 소유하는 홀더.
 */
internal class LiteRtModel private constructor(
    val interpreter: Interpreter,
) : Closeable {

    override fun close() = interpreter.close()

    companion object {
        private const val DEFAULT_THREADS = 4

        suspend fun load(context: Context, packName: String, assetPath: String): LiteRtModel {
            val buffer = mapModelFile(context, packName, assetPath)
            val interpreter = Interpreter(buffer, Interpreter.Options().setNumThreads(DEFAULT_THREADS))
            return LiteRtModel(interpreter)
        }

        /** 애셋팩이 내려받은 로컬 .tflite 파일을 mmap. 팩 미다운로드 시 여기서 내려받길 기다린다. */
        private suspend fun mapModelFile(context: Context, packName: String, assetPath: String): MappedByteBuffer =
            OnDemandModelAssets(context).resolve(packName, assetPath).mmap()
    }
}
