package com.gamss.android.data.model

import android.content.Context
import com.google.android.play.core.assetpacks.AssetLocation
import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import com.google.android.play.core.ktx.requestFetch
import com.google.android.play.core.ktx.requestPackStates
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
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
 * WAITING_FOR_WIFI 상태의 셀룰러 확인 다이얼로그(showCellularDataConfirmation)는
 * Activity 가 있어야 띄울 수 있어 데이터 계층인 이 클래스에서는 처리하지 않는다. 지금은 예외로
 * 실패 처리하고, 다운로드 진행률/Wi-Fi 확인을 사용자에게 보여주는 UI 흐름은 후속 작업으로 분리한다.
 */
internal class OnDemandModelAssets(context: Context) {

    private val manager: AssetPackManager = AssetPackManagerFactory.getInstance(context.applicationContext)

    /** [packName] 팩이 아직 없으면 내려받아 완료될 때까지 대기한 뒤, [relativeAssetPath] 의 위치를 돌려준다. */
    suspend fun resolve(packName: String, relativeAssetPath: String): AssetLocation {
        ensureInstalled(packName)
        return manager.getAssetLocation(packName, relativeAssetPath)
            ?: throw ModelPackUnavailableException(
                "애셋팩 '$packName' 다운로드가 완료됐다고 표시됐지만 '$relativeAssetPath' 위치를 찾을 수 없습니다.",
            )
    }

    private suspend fun ensureInstalled(packName: String) {
        val current = manager.requestPackStates(listOf(packName)).packStates()[packName]
        if (current?.status() == AssetPackStatus.COMPLETED) return

        manager.requestFetch(listOf(packName))
        awaitCompletion(packName)
    }

    private suspend fun awaitCompletion(packName: String) {
        val final = packStateUpdates()
            .filter { it.name() == packName }
            .first { state -> state.status() in TERMINAL_STATUSES }
        if (final.status() != AssetPackStatus.COMPLETED) {
            throw failureFor(packName, final)
        }
    }

    private fun failureFor(packName: String, state: AssetPackState): ModelPackUnavailableException {
        val reason = when (state.status()) {
            AssetPackStatus.FAILED -> "다운로드 실패: ${state.errorCode()}"
            AssetPackStatus.WAITING_FOR_WIFI -> "Wi-Fi 확인 대기 중입니다. UI 에서 사용자 확인이 필요합니다."
            AssetPackStatus.CANCELED -> "다운로드가 취소됐습니다."
            else -> "알 수 없는 상태(${state.status()})"
        }
        return ModelPackUnavailableException("애셋팩 '$packName' $reason")
    }

    private fun packStateUpdates(): Flow<AssetPackState> = callbackFlow {
        val listener = AssetPackStateUpdateListener { state -> trySend(state) }
        manager.registerListener(listener)
        awaitClose { manager.unregisterListener(listener) }
    }

    private companion object {
        val TERMINAL_STATUSES = setOf(
            AssetPackStatus.COMPLETED,
            AssetPackStatus.FAILED,
            AssetPackStatus.WAITING_FOR_WIFI,
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
