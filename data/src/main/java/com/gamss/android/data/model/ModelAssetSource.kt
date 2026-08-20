package com.gamss.android.data.model

import com.gamss.android.domain.model.ModelDownloadStatus
import kotlinx.coroutines.flow.Flow
import java.nio.MappedByteBuffer

/**
 * 온디바이스 모델(.tflite/.onnx/tokenizer.json) 바이트에 접근하는 공용 경로. 실제 공급처는
 * buildType별로 갈아 끼워진다(Hilt, `di/ModelAssetSourceModule.kt` 각 소스셋 참고) — 추론
 * 코드(LiteRtModel, OnnxKobartSummarizer, *Tokenizer 계열)는 이 인터페이스만 알면 되고 PAD/로컬
 * 여부를 몰라도 된다.
 *
 * - `release`: [OnDemandModelAssets] — Play Asset Delivery(on-demand/fast-follow)로 내려받는다.
 * - `debug`/`internal`: `LocalAssetsModelSource` — APK 에 그대로 번들된 assets 를 읽는다(PAD 미사용).
 *
 * `internal` 이 아니라 public 이다 — Hilt(Dagger KSP)가 `@Inject constructor`/`@Provides` 반환
 * 타입으로 쓰는 타입에 `internal` 가시성을 주면 컴파일이 깨진다(KSP 가 타입을 못 찾는다).
 */
interface ModelAssetSource {
    /** [packName]/[relativeAssetPath] 를 mmap. 팩 미다운로드 시(PAD 구현) 내려받길 기다린다. */
    suspend fun mmap(packName: String, relativeAssetPath: String): MappedByteBuffer

    /** [packName]/[relativeAssetPath] 를 통째로 읽는다. 작은 tokenizer.json 로드용. */
    suspend fun readBytes(packName: String, relativeAssetPath: String): ByteArray

    /** [packName] 다운로드를 미리 걸어둔다. 로컬 구현은 이미 준비돼 있으므로 no-op. */
    suspend fun prefetch(packName: String)

    /** [packName] 의 현재/이후 다운로드 상태. 로컬 구현은 항상 [ModelDownloadStatus.COMPLETED]. */
    fun statusFlow(packName: String): Flow<ModelDownloadStatus>
}
