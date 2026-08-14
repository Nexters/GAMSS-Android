package com.gamss.android.data.model

import android.content.Context
import com.gamss.android.domain.model.ModelDownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * `debug`/`firebase` buildType 전용 [ModelAssetSource] 구현. 모델을 애셋팩 다운로드가 아니라
 * APK 에 그대로 번들된 assets 에서 읽는다(PAD/AssetPackManager 호출이 전혀 없다) — 두 buildType
 * 모두 Play Store 를 거치지 않는 설치 경로라 AssetPackManager 가 애초에 동작하지 않는다.
 *
 * `packName` 은 무시한다. PAD 는 애셋팩 단위로 파일을 구분하지만 로컬 assets 는 하나의 네임스페이스로
 * 합쳐지므로 `relativeAssetPath`(예: "models/emotion_int8.tflite")만으로 충분하다. 실제 파일은
 * `:models:emotion-pack`/`:models:summary-pack` 의 assets 디렉터리를 `data` 모듈의 debug/firebase
 * sourceSet 에 그대로 srcDir 로 얹은 것이라 경로가 PAD 쪽과 동일하게 유지된다 — `data/build.gradle.kts`
 * 참고.
 *
 * PAD 도입 전(커밋 `d6a9151` 이전) 실제로 쓰였던 `context.assets.openFd()` + mmap 패턴을 그대로
 * 되살렸다. `AssetFileDescriptor` 로 mmap 하려면 해당 확장자가 APK 안에 비압축으로 들어있어야
 * 하므로 `androidResources.noCompress`("tflite", "onnx")가 함께 필요하다.
 *
 * `internal` 이 아니라 public 이다 — `app` 모듈의 온디바이스 모델 평가 테스트
 * ([com.gamss.android.OnDeviceModelEvalTest])가 Hilt DI 없이 `AndroidEmotionClassifier`/
 * `AndroidDiarySummarizer`를 직접 생성할 때 이 클래스를 인자로 넘겨야 해서 모듈 경계 밖에서도
 * 보여야 한다.
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
