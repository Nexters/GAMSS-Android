package com.gamss.android.data.model

import android.content.Context
import com.gamss.android.domain.model.ModelDownloadStatus
import com.google.android.play.core.assetpacks.AssetLocation
import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/** on-demand 애셋팩이 다운로드되지 않았거나 실패해 모델 파일을 찾을 수 없을 때. */
internal class ModelPackUnavailableException(message: String) : Exception(message)

/**
 * Play Asset Delivery(on-demand 애셋팩)로 배포되는 대용량 모델 파일(.tflite/.onnx/tokenizer.json)을
 * 필요 시점에 내려받고 [AssetLocation](로컬 파일 경로 + 오프셋/길이)을 돌려주는 공용 헬퍼.
 * emotion(:models:emotion-pack), summary(:models:summary-pack) 두 모델 로더가 이 경로를 공유한다.
 *
 * WAITING_FOR_WIFI/REQUIRES_USER_CONFIRMATION 은 둘 다 Activity 가 있어야 띄울 수 있는 시스템
 * 다이얼로그로 풀리는데, [resolve]/[prefetch] 를 쓰는 감정 분류·요약 호출부는 Activity 를 모른다
 * (data 모듈은 Activity 를 안 씀). 그래서 여기선 실패로만 처리하고, 실제 확인 다이얼로그를 띄우는
 * 책임은 [statusFlow] 를 구독해 Activity 가 있는 app 루트로 넘긴다 — [ModelDownloadConfirmationGateway]
 * 참고.
 *
 * `release` buildType 전용 [ModelAssetSource] 구현(`di/ModelAssetSourceModule.kt` 참고) — Play
 * Store 밖 설치 경로(debug/internal)에서는 [com.gamss.android.data.model.LocalAssetsModelSource]
 * 가 대신 쓰인다.
 */
internal class OnDemandModelAssets(context: Context) : ModelAssetSource {

    private val manager: AssetPackManager = AssetPackManagerFactory.getInstance(context.applicationContext)

    override suspend fun mmap(packName: String, relativeAssetPath: String): MappedByteBuffer =
        resolve(packName, relativeAssetPath).mmap()

    override suspend fun readBytes(packName: String, relativeAssetPath: String): ByteArray =
        resolve(packName, relativeAssetPath).readBytes()

    /** [packName] 팩이 아직 없으면 내려받아 완료될 때까지 대기한 뒤, [relativeAssetPath] 의 위치를 돌려준다. */
    private suspend fun resolve(packName: String, relativeAssetPath: String): AssetLocation {
        ensureInstalled(packName)
        return manager.getAssetLocation(packName, relativeAssetPath)
            ?: throw ModelPackUnavailableException(
                "애셋팩 '$packName' 다운로드가 완료됐다고 표시됐지만 '$relativeAssetPath' 위치를 찾을 수 없습니다.",
            )
    }

    /**
     * [packName] 다운로드를 미리 걸어둔다. 결과를 기다리는 호출자가 없어도 안전하다 — 실패해도
     * 예외를 던지지 않고 무시되며, 실제로 필요한 시점([resolve])에 다시 확인·재시도된다. 이미
     * 진행 중이거나 완료된 팩에 걸어도 중복 다운로드가 생기지 않는다(Play Core 가 팩 이름 기준으로
     * 중복 fetch 요청을 하나로 묶는다).
     *
     * 취소는 삼키지 않는다 — 그냥 runCatching 만 쓰면 [kotlinx.coroutines.CancellationException] 도
     * 잡혀서 취소가 조용히 성공한 것처럼 보인다.
     */
    override suspend fun prefetch(packName: String) {
        runCatching { ensureInstalled(packName) }
        currentCoroutineContext().ensureActive()
    }

    private suspend fun ensureInstalled(packName: String) {
        val current = manager.getPackStates(listOf(packName)).await().packStates()[packName]
        if (current?.status() == AssetPackStatus.COMPLETED) return

        awaitCompletion(packName)
    }

    /**
     * 리스너를 등록한 뒤에야 fetch 를 요청한다(순서가 중요하다). 먼저 fetch 를 요청하고 나중에
     * 리스너를 등록하면, 그 사이에 팩이 종료 상태(COMPLETED/FAILED 등)로 전이될 경우 그 갱신을
     * 놓쳐 아래 first{} 가 끝나지 않는(영구 suspend) 레이스가 생긴다.
     */
    private suspend fun awaitCompletion(packName: String) {
        val final = packStateUpdates(packName)
            .first { state -> state.status() in TERMINAL_STATUSES }
        if (final.status() != AssetPackStatus.COMPLETED) {
            throw failureFor(packName, final)
        }
    }

    private fun failureFor(packName: String, state: AssetPackState): ModelPackUnavailableException {
        val reason = when (state.status()) {
            AssetPackStatus.FAILED -> "다운로드 실패: ${state.errorCode()}"
            AssetPackStatus.WAITING_FOR_WIFI -> "Wi-Fi 확인 대기 중입니다. UI 에서 사용자 확인이 필요합니다."
            AssetPackStatus.REQUIRES_USER_CONFIRMATION -> "다운로드 크기가 커서 사용자 확인이 필요합니다."
            AssetPackStatus.CANCELED -> "다운로드가 취소됐습니다."
            else -> "알 수 없는 상태(${state.status()})"
        }
        // 이 실패는 호출부(감정 분류·요약)에서 대화를 막지 않으려고 조용히 삼켜진다. 그래서 얼마나
        // 자주 겪는지(특히 셀룰러 상태에서의 WAITING_FOR_WIFI) 알 방법이 없는데, 원격 로깅 시스템이
        // 붙으면 여기서 packName/state.status()/state.errorCode() 를 남기도록 연결할 예정이다.
        return ModelPackUnavailableException("애셋팩 '$packName' $reason")
    }

    private fun packStateUpdates(packName: String): Flow<AssetPackState> = callbackFlow {
        val listener = AssetPackStateUpdateListener { state -> trySend(state) }
        manager.registerListener(listener)
        // 리스너가 등록된 뒤에 요청해야 fetch 직후의 상태 전이를 놓치지 않는다.
        manager.fetch(listOf(packName))
            .addOnSuccessListener { states ->
                states.packStates()[packName]?.let { state -> trySend(state) }
            }
            .addOnFailureListener { error -> close(error) }
        awaitClose { manager.unregisterListener(listener) }
    }.filter { it.name() == packName }

    /**
     * [packName] 의 현재/이후 다운로드 상태를 계속 흘려준다. [resolve]/[prefetch] 와 달리 fetch를
     * 직접 요청하지 않는다 — 순수 관찰용이라 UI 가 구독해도 다운로드가 새로 시작되진 않는다
     * (이미 fetch 가 걸려 있어야 진행 상태가 바뀐다).
     */
    override fun statusFlow(packName: String): Flow<ModelDownloadStatus> = callbackFlow {
        val listener = AssetPackStateUpdateListener { state ->
            if (state.name() == packName) {
                trySend(state.status().toDomainStatus())
            }
        }
        manager.registerListener(listener)
        manager.getPackStates(listOf(packName))
            .addOnSuccessListener { states ->
                states.packStates()[packName]?.let { state ->
                    trySend(state.status().toDomainStatus())
                }
            }
            // Play Store 밖에서 설치한 debug APK 등 상태 조회가 불가능한 환경도 앱을 종료시키지 않는다.
            .addOnFailureListener { trySend(ModelDownloadStatus.FAILED) }
        awaitClose { manager.unregisterListener(listener) }
    }

    private fun Int.toDomainStatus(): ModelDownloadStatus = when (this) {
        AssetPackStatus.COMPLETED -> ModelDownloadStatus.COMPLETED
        AssetPackStatus.WAITING_FOR_WIFI,
        AssetPackStatus.REQUIRES_USER_CONFIRMATION,
        -> ModelDownloadStatus.NEEDS_USER_CONFIRMATION
        AssetPackStatus.FAILED, AssetPackStatus.CANCELED -> ModelDownloadStatus.FAILED
        else -> ModelDownloadStatus.DOWNLOADING
    }

    private companion object {
        val TERMINAL_STATUSES = setOf(
            AssetPackStatus.COMPLETED,
            AssetPackStatus.FAILED,
            AssetPackStatus.WAITING_FOR_WIFI,
            AssetPackStatus.REQUIRES_USER_CONFIRMATION,
            AssetPackStatus.CANCELED,
        )
    }
}

/** [AssetLocation] 이 가리키는 구간을 mmap. 큰 바이너리(.tflite/.onnx)를 힙 복사 없이 로드할 때 쓴다. */
internal fun AssetLocation.mmap(): MappedByteBuffer =
    RandomAccessFile(path(), "r").use { raf ->
        raf.channel.map(FileChannel.MapMode.READ_ONLY, offset(), size())
    }

/** [AssetLocation] 이 가리키는 구간을 통째로 읽는다. 작은 tokenizer.json 로드용. */
internal fun AssetLocation.readBytes(): ByteArray =
    RandomAccessFile(path(), "r").use { raf ->
        raf.seek(offset())
        ByteArray(size().toInt()).also(raf::readFully)
    }
