package com.gamss.android.data.model

import android.content.Context
import com.gamss.android.domain.model.ModelDownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * `debug`/`internal` buildType 전용 [ModelAssetSource] 구현 — APK 에 번들된 assets 를 읽는다(PAD 미사용).
 * Play Store 를 거치지 않는 설치 경로라 AssetPackManager 가 동작하지 않는다.
 *
 * `packName` 은 무시한다. 로컬 assets 는 단일 네임스페이스라 `relativeAssetPath` 만으로 충분하다.
 *
 * mmap 하려면 해당 확장자가 APK 안에 비압축으로 들어있어야 하므로 `androidResources.noCompress`
 * ("tflite", "onnx")가 함께 필요하다.
 *
 * `internal` 이 아니라 public 이다 — [com.gamss.android.OnDeviceModelEvalTest]가 Hilt 없이 이 클래스를
 * 직접 생성해야 해서 모듈 경계 밖에서도 보여야 한다.
 */
class LocalAssetsModelSource(private val context: Context) : ModelAssetSource {

    override suspend fun mmap(packName: String, relativeAssetPath: String): MappedByteBuffer =
        context.assets.openFd(relativeAssetPath).use { afd ->
            FileInputStream(afd.fileDescriptor).use { fis ->
                fis.channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
            }
        }

    override suspend fun readBytes(packName: String, relativeAssetPath: String): ByteArray =
        context.assets.open(relativeAssetPath).use { it.readBytes() }

    /** 로컬 assets 는 이미 APK 안에 있으므로 내려받을 게 없다. */
    override suspend fun prefetch(packName: String) = Unit

    /** 로컬 assets 는 항상 준비돼 있다 — UI 가 다운로드 확인 배너를 띄울 일이 없다. */
    override fun statusFlow(packName: String): Flow<ModelDownloadStatus> = flowOf(ModelDownloadStatus.COMPLETED)
}
