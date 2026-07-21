package com.gamss.android.data.emotion

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * assets 의 .tflite 를 메모리 매핑(mmap)으로 로드해 스레드 고정 Interpreter 를 소유하는 공용 홀더.
 * LiteRT 를 쓰는 모든 엔진이 이 로더를 재사용한다(로드/스레드/해제 로직 단일화).
 */
class LiteRtModel private constructor(
    val interpreter: Interpreter,
    val sizeBytes: Long,
) : Closeable {

    override fun close() = interpreter.close()

    companion object {
        const val DEFAULT_THREADS = 4

        fun load(context: Context, assetPath: String, numThreads: Int = DEFAULT_THREADS): LiteRtModel {
            val buffer = mapAsset(context, assetPath)
            val interpreter = Interpreter(buffer, Interpreter.Options().setNumThreads(numThreads))
            return LiteRtModel(interpreter, buffer.capacity().toLong())
        }

        /** APK 내 uncompressed .tflite 를 mmap. (app 의 androidResources.noCompress 로 비압축 보장) */
        private fun mapAsset(context: Context, assetPath: String): MappedByteBuffer {
            context.assets.openFd(assetPath).use { afd ->
                FileInputStream(afd.fileDescriptor).use { fis ->
                    return fis.channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
                }
            }
        }
    }
}
